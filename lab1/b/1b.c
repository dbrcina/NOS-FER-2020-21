#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <limits.h>
#include <unistd.h>
#include <signal.h>
#include <string.h>
#include <time.h>
#include <sys/ipc.h>
#include <sys/shm.h>

#define MIN_PROCESSES 3
#define MAX_PROCESSES 10
#define PERMS 00600
#define LB_X 100
#define UB_X 2000
#define REPEAT 5

#define MAX(x, y)(((x) > (y)) ? (x) : (y))

enum pipe_operation {
    Read_Operation = 0,
    Write_Operation = 1
};

enum msg_type {
    Request = 0,
    Response = 1
};

struct process {
    int id;
    int l_clock;
};

struct msg_buf {
    enum msg_type type;
    struct process proc;
};

struct db_entry_database {
    struct process proc;
    int entries;
};

long int g_n;
int g_smh_id;
struct db_entry_database *g_db; // N
int *g_pipes; // Nx2
int g_enter_cs = 1; // if true, process wants to go in critical section.
int *g_responses; // g_n size, postponed responses

/* Send response message to other process. */
void send_response(const struct process *proc, const struct process *other_proc) {
    struct msg_buf buf = {
            .type = Response,
            .proc = {
                    .id = proc->id,
                    .l_clock = other_proc->l_clock
            }
    };
    if (write(g_pipes[other_proc->id * 2 + Write_Operation], &buf, sizeof(buf)) == -1) {
        perror("[PROCESS] send_response::write");
        exit(EXIT_FAILURE);
    }
}

/* Send remaining response messages to waiting processes. */
void send_remaining_responses(const struct process *proc) {
    for (int i = 0; i < g_n; ++i) {
        if (i == proc->id) continue;
        int other_proc_l_clock = g_responses[i];
        if (other_proc_l_clock == 0) continue;
        struct process other_proc = {
                .id = i,
                .l_clock = other_proc_l_clock
        };
        send_response(proc, &other_proc);
        fprintf(stdout, "P%d šalje ODGOVOR(%d,%d) prema P%d!\n", proc->id, proc->id, other_proc_l_clock, i);
        g_responses[i] = 0;
    }
}

/* Critical section procedure. */
void critical_section(const struct process *proc) {
    fprintf(stdout, "P%d pristupa KO!\n", proc->id);
    // Print database and update database.
    for (int i = 0; i < g_n; ++i) {
        struct db_entry_database *entry = g_db + i;
        if (i == proc->id) {
            // Update database.
            entry->proc = *proc;
            entry->entries++;
            if (entry->entries == REPEAT) {
                g_enter_cs = 0;
            }
        }
        fprintf(stdout, "| P%d | c%d=%2d | entries=%d |\n", i, i, entry->proc.l_clock, entry->entries);
    }
    // Sleep for X ms.
    sleep((LB_X + rand() % (UB_X - LB_X + 1)) / 1000);
    fprintf(stdout, "P%d napušta KO!\n", proc->id);
}

/* Receives request/responses messages from other processes, updates local clock of current process by the definition
 * of global clock and sends responses to other processes if conditions are satisfied, otherwise responses are stored
 * and answered later. */
void receive_requests_responses(struct process *proc) {
    int responses_counter = 0;
    int id = proc->id;
    int current_l_clock = proc->l_clock;
    struct msg_buf buf;
    while (1) {
        if (read(g_pipes[id * 2 + Read_Operation], &buf, sizeof(buf)) == -1) {
            perror("[PROCESS] receive_requests_responses::read");
            exit(EXIT_FAILURE);
        }
        struct process msg_proc = buf.proc;
        int msg_id = msg_proc.id;
        int msg_l_clock = msg_proc.l_clock;
        int new_l_clock = MAX(proc->l_clock, msg_l_clock) + 1;
        if (buf.type == Request) {
            char resp_msg[40];
            if (!g_enter_cs
                || current_l_clock > msg_l_clock
                || (current_l_clock == msg_l_clock && id > msg_id)) {
                send_response(proc, &msg_proc);
                sprintf(resp_msg, " te šalje ODGOVOR(%d,%d) prema P%d!", id, msg_l_clock, msg_id);
            } else { // Don't send response, save it!
                g_responses[msg_id] = msg_l_clock;
                sprintf(resp_msg, " te NE šalje ODGOVOR(%d,%d) prema P%d!", id, msg_l_clock, msg_id);
            }
            fprintf(stdout,
                    "P%d prima ZAHTJEV(%d,%d) od P%d i ažurira lokalni sat c%d=max(%d,%d)+1=%d%s\n",
                    id, msg_id, msg_l_clock, msg_id, id, proc->l_clock, msg_l_clock, new_l_clock, resp_msg
            );
        } else { // Response
            responses_counter++;
            fprintf(stdout,
                    "P%d prima ODGOVOR(%d,%d) od P%d i ažurira lokalni sat c%d=max(%d,%d)+1=%d, ODGOVORI:%d!\n",
                    id, msg_id, msg_l_clock, msg_id, id, proc->l_clock, msg_l_clock, new_l_clock, responses_counter
            );
        }
        proc->l_clock = new_l_clock;
        if (responses_counter == g_n - 1) break;
    }
}

/* Sends request messages to other processes. */
void send_requests(const struct process *proc) {
    if (!g_enter_cs) return;
    struct msg_buf buf = {
            .type = Request,
            .proc = *proc
    };
    int id = proc->id;
    int logic_clock = proc->l_clock;
    for (int i = 0; i < g_n; ++i) {
        if (i == id) continue;
        if (write(g_pipes[i * 2 + Write_Operation], &buf, sizeof(buf)) == -1) {
            perror("[PROCESS] send_requests::write");
            exit(EXIT_FAILURE);
        }
        fprintf(stdout,
                "P%d, čiji je lokalni sat c%d=%d, šalje ZAHTJEV(%d,%d) prema P%d!\n",
                id, id, logic_clock, id, logic_clock, i
        );
    }
}

/* Frees pipelines and responses arrays from memory. */
void free_pipelines_responses(int signal) {
    free(g_pipes);
    free(g_responses);
    exit(EXIT_SUCCESS);
}

/* Process's working procedure. */
_Noreturn void process_procedure(int id) {
    // Map SIGINT to clear responses and pipelines arrays.
    if (signal(SIGINT, free_pipelines_responses) == SIG_ERR) {
        perror("[PROCESS] process_procedure::signal");
        exit(EXIT_FAILURE);
    }
    // Initialize responses array.
    g_responses = (int *) malloc(sizeof(int) * g_n);
    // Initialize randomness.
    srand((unsigned) time(NULL) ^ getpid());
    // Create process structure.
    struct process proc = {
            .id = id,
            .l_clock = rand() % g_n + 1
    };
    // Do work...
    while (1) {
        send_requests(&proc);
        receive_requests_responses(&proc);
        critical_section(&proc);
        send_remaining_responses(&proc);
    }
}

/* Creates N pipelines. */
void prepare_pipelines() {
    g_pipes = (int *) malloc(g_n * 2 * sizeof(int));
    for (int i = 0; i < g_n; ++i) {
        int fd[2];
        if (pipe(fd) == -1) {
            perror("[MAIN] prepare_pipelines::pipe");
            exit(EXIT_FAILURE);
        }
        g_pipes[i * 2 + Read_Operation] = fd[Read_Operation];
        g_pipes[i * 2 + Write_Operation] = fd[Write_Operation];
    }
}

/* First detaches memory with shmdt function, and then removes g_shm_id with shmctl function. Frees g_pipes and
 * g_responses. */
void free_memory_and_quit(int signal) {
    fprintf(stdout, "\n");
    if (shmdt(g_db) == -1) {
        perror("[MAIN] free_memory_and_quit::shmdt");
        exit(EXIT_FAILURE);
    }
    fprintf(stdout, "Memorija vezana za segment shmid=%d je uspješno očišćena!\n", g_smh_id);
    if (shmctl(g_smh_id, IPC_RMID, NULL) == -1) {
        perror("[MAIN] free_memory_and_quit::shmctl");
        exit(EXIT_FAILURE);
    }
    fprintf(stdout, "Segment shmid=%d je uspješno uništen!\n", g_smh_id);
    free(g_pipes);
    free(g_responses);
    exit(EXIT_SUCCESS);
}

/* Registers signals for main program. */
void register_signals() {
    if (signal(SIGINT, free_memory_and_quit) == SIG_ERR) {
        perror("[MAIN] register_signals::signal");
        exit(EXIT_FAILURE);
    }
}

/* Prepares database using shared memory segment. */
void prepare_shared_memory() {
    if ((g_smh_id = shmget(getuid(), g_n * sizeof(struct db_entry_database), IPC_CREAT | PERMS)) == -1) {
        perror("[MAIN] prepare_shared_memory::shmget");
        exit(EXIT_FAILURE);
    }
    g_db = (struct db_entry_database *) shmat(g_smh_id, NULL, 0);
    if (*((int *) g_db) == -1) {
        perror("[MAIN] prepare_shared_memory::shmat");
        exit(EXIT_FAILURE);
    }
}

/* Parses command line arguments and informs with appropriate messages. */
void parse_command_line_arguments(int argc, const char *argv[]) {
    if (argc != 2) {
        fprintf(stderr, "Program očekuje broj procesa kao argument!\n");
        exit(EXIT_FAILURE);
    }
    char *endptr;
    errno = 0;
    g_n = strtol(argv[1], &endptr, 10);
    if ((errno == ERANGE && (g_n == LONG_MAX || g_n == LONG_MIN)) || (errno != 0 && g_n == 0)) {
        perror("[MAIN] parse_command_line_arguments::strtol");
        exit(EXIT_FAILURE);
    }
    if (*endptr != '\0' || endptr == argv[1] || g_n < MIN_PROCESSES || g_n > MAX_PROCESSES) {
        fprintf(stderr, "Program očekuje nenegativan broj iz intervala [%d, %d]!\n", MIN_PROCESSES, MAX_PROCESSES);
        exit(EXIT_FAILURE);
    }
}

int main(int argc, const char *argv[]) {
    parse_command_line_arguments(argc, argv);
    prepare_shared_memory();
    register_signals();
    prepare_pipelines();

    // Create N processes.
    for (int i = 0; i < g_n; i++) {
        pid_t pid = fork();
        if (pid == -1) {
            perror("[MAIN] fork");
            return EXIT_FAILURE;
        } else if (pid == 0) {
            process_procedure(i);
        }
    }

    while (1);
}
