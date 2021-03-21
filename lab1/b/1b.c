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
    int logic_clock;
};

struct msg_buf {
    enum msg_type type;
    struct process process;
};

struct db_entry_database {
    struct process process_data;
    int entries;
};

long int g_n;
int g_smh_id;
struct db_entry_database *g_db;
int *g_pipes; // Nx2
int g_enter_cs = 1; // if true, process wants to go in critical section.

/* Send request messages to other processes. */
void send_requests(const struct process *process) {
    struct msg_buf buf = {
            .type = Request,
            .process = *process
    };
    int id = process->id;
    int logic_clock = process->logic_clock;
    for (int i = 0; i < g_n; ++i) {
        if (i == id) continue;
        if (write(g_pipes[i * 2 + Write_Operation], &buf, sizeof(buf)) == -1) {
            perror("[PROCESS] send_requests::write");
            exit(EXIT_FAILURE);
        }
        fprintf(stdout,
                "P%d, čiji je lokalni logički sat c%d=%d, šalje zahtjev(%d,%d) prema P%d!\n",
                id, id, logic_clock, id, logic_clock, i
        );
    }
}

/* Send response message to other process. */
void send_response(const struct process *process, const struct process *other_process) {
    int id = process->id;
    int other_process_id = other_process->id;
    int other_logic_clock = other_process->logic_clock;
    struct msg_buf buf = {
            .type = Response,
            .process = {
                    .id = id,
                    .logic_clock = other_process->logic_clock
            }
    };
    if (write(g_pipes[other_process_id * 2 + Write_Operation], &buf, sizeof(buf)) == -1) {
        perror("[PROCESS] send_response::write");
        exit(EXIT_FAILURE);
    }
    fprintf(stdout,
            "P%d šalje odgovor(%d,%d) prema P%d!\n",
            id, id, other_logic_clock, other_process_id
    );
}

/* Receive request/responses messages from other processes and returns an array of processes response. */
struct process *receive_requests_responses(struct process *process) {
    struct process *responses = (struct process *) malloc(sizeof(struct process) * (g_n - 1));
    int responses_counter = 0;
    int id = process->id;
    struct msg_buf buf;
    while (1) {
        if (read(g_pipes[id * 2 + Read_Operation], &buf, sizeof(buf)) == -1) {
            perror("[PROCESS] receive_requests::read");
            exit(EXIT_FAILURE);
        }
        struct process msg_process = buf.process;
        int msg_id = msg_process.id;
        int msg_logic_clock = msg_process.logic_clock;
        int logic_clock = process->logic_clock;
        int new_logic_clock = MAX(logic_clock, msg_logic_clock) + 1;
        if (buf.type == Request) {
            char resp_msg[40] = "!";
            if (!g_enter_cs || logic_clock > msg_logic_clock || (logic_clock == msg_logic_clock && id > msg_id)) {
                send_response(process, &msg_process);
                sprintf(resp_msg, " te šalje odgovor(%d,%d) prema P%d!", id, msg_logic_clock, msg_id);
            }
            fprintf(stdout,
                    "P%d prima zahtjev(%d,%d) od P%d i ažurira lokalni logički sat c%d=max(%d,%d)+1=%d%s\n",
                    id, msg_id, msg_logic_clock, msg_id, id, logic_clock, msg_logic_clock, new_logic_clock, resp_msg
            );
        } else { // Response

        }
        process->logic_clock = new_logic_clock;
        if (responses_counter == g_n - 1) break;
    }
    return responses;
}

/* Process's working procedure. */
_Noreturn void process_procedure(int id) {
    // Map SIGINT to its default behaviour.
    if (signal(SIGINT, SIG_DFL) == SIG_ERR) {
        perror("[PROCESS] process_procedure::signal");
        exit(EXIT_FAILURE);
    }
    // Initialize randomness.
    srand((unsigned) time(NULL) ^ getpid());
    // Create process structure.
    struct process process = {
            .id = id,
            .logic_clock = rand() % g_n
    };
    // Do work...
    while (1) {
        send_requests(&process);
        sleep(1);
        receive_requests_responses(&process);
        sleep(10);
    }
}

/* Creates N pipelines. */
void prepare_pipelines() {
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

/* First detaches memory with shmdt function, and then removes g_shm_id with shmctl function. */
void free_memory_and_quit(int signal) {
    fprintf(stdout, "\n");
    if (shmdt(g_db) == -1 || shmdt(g_pipes) == -1) {
        perror("[MAIN] free_memory_and_quit::shmdt");
        exit(EXIT_FAILURE);
    }
    fprintf(stdout, "Memorija vezana za segment shmid=%d je uspješno očišćena!\n", g_smh_id);
    if (shmctl(g_smh_id, IPC_RMID, NULL) == -1) {
        perror("[MAIN] free_memory_and_quit::shmctl");
        exit(EXIT_FAILURE);
    }
    fprintf(stdout, "Segment shmid=%d je uspješno uništen!\n", g_smh_id);
    exit(EXIT_SUCCESS);
}

/* Registers signals for main program. */
void register_signals() {
    if (signal(SIGINT, free_memory_and_quit) == SIG_ERR) {
        perror("[MAIN] register_signals::signal");
        exit(EXIT_FAILURE);
    }
}

/* Prepares database and pipelines using shared memory segment. */
void prepare_shared_memory() {
    size_t db_size = g_n * sizeof(struct db_entry_database);
    size_t pipelines_size = g_n * 2 * sizeof(int);
    if ((g_smh_id = shmget(getuid(), db_size + pipelines_size, IPC_CREAT | PERMS)) == -1) {
        perror("[MAIN] prepare_shared_memory::shmget");
        exit(EXIT_FAILURE);
    }
    g_db = (struct db_entry_database *) shmat(g_smh_id, NULL, 0);
    g_pipes = (int *) shmat(g_smh_id, NULL, 0);
    if (*((int *) g_db) == -1 || *g_pipes == -1) {
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
    int i;
    for (i = 0; i < g_n; i++) {
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
