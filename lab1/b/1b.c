#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <limits.h>
#include <unistd.h>
#include <signal.h>
#include <string.h>
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
int *g_pipes; // Nx(N-1)x2

/*
 * Calculates memory offset for 3D array of ints.
 * If j > i, then j = j - 1, because we don't want same processes in memory.
 * So, [0][0] means a connection between P0-P1 and not P0-P0, [1][1] P1-P2 and so on...
 * i==j will never occur in this program.
 */
int memory_offset(int i, int j) {
    return i * (g_n - 1) * 2 + (j > i ? j - 1 : j) * 2;
}

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
        if (write(g_pipes[memory_offset(id, i) + Write_Operation], &buf, sizeof(buf)) == -1) {
            perror("[PROCESS] send_requests::write");
            exit(EXIT_FAILURE);
        }
        fprintf(stdout, "Proces P%d šalje zahtjev(%d,%d) procesu P%d!\n", id, id, logic_clock, i);
    }
}

/* Receive request messages from other processes. */
void receive_requests(const struct process *process) {
    int id = process->id;
    int logic_clock = process->logic_clock;
    struct msg_buf buf;
    for (int i = 0; i < g_n; ++i) {
        if (i == id) continue;
        if (read(g_pipes[memory_offset(i, id) + Read_Operation], &buf, sizeof(buf)) == -1) {
            perror("[PROCESS] receive_requests::read");
            exit(EXIT_FAILURE);
        }
        struct process other_process = buf.process;
        int other_logic_clock = other_process.logic_clock;
        fprintf(stdout, "Proces P%d prima zahtjev(%d,%d) procesa P%d!\n", id, i, other_logic_clock, i);
        logic_clock = MAX(logic_clock, other_logic_clock) + 1;
    }
}

/* Process's working procedure. */
_Noreturn void process_procedure(int id) {
    // Map SIGINT to its default behaviour.
    if (signal(SIGINT, SIG_DFL) == SIG_ERR) {
        perror("[PROCESS] process_procedure::signal");
        exit(EXIT_FAILURE);
    }
    // Create process structure.
    struct process process = {
            .id = id,
            .logic_clock = 1
    };
    // Close read descriptors from current process to others
    // and close write descriptors to current process from others.
    for (int i = 0; i < g_n; i++) {
        if (i == id) continue;
        if (close(g_pipes[memory_offset(id, i) + Read_Operation]) == -1
            || close(g_pipes[memory_offset(i, id) + Write_Operation]) == -1) {
            perror("[PROCESS] process_procedure::close");
            exit(EXIT_FAILURE);
        }
    }

    // Do work...
    while (1) {

        send_requests(&process);
        receive_requests(&process);
        sleep(10);
    }
}

/* Creates Nx(N-1)x2 array of ints. Every two different processes have two pipelines open. */
void prepare_pipelines() {
    for (int i = 0; i < g_n; i++) {
        for (int j = 0; j < g_n; j++) {
            if (i == j) continue;
            int offset = memory_offset(i, j);
            int fd[2];
            if (pipe(fd) == -1) {
                perror("[MAIN] prepare_pipelines::pipe");
                exit(EXIT_FAILURE);
            }
            g_pipes[offset + Read_Operation] = fd[Read_Operation];
            g_pipes[offset + Write_Operation] = fd[Write_Operation];
        }
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
    size_t pipelines_size = g_n * (g_n - 1) * 2 * sizeof(int);
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
void parse_command_line_arguments(int argc, char const *argv[]) {
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

int main(int argc, char const *argv[]) {
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
