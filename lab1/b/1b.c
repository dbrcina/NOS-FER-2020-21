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
#define BUFFER_SIZE 30
#define MSG_LENGTH 15
#define REQUEST "zahtjev"
#define RESPONSE "odgovor"
#define LB_X 100
#define UB_X 2000
#define REPEAT 5
#define MAX(x, y)(((x) > (y)) ? (x) : (y))

struct process {
    int id;
    int logic_clock;
};

struct db_entry_database {
    struct process process_data;
    int entries;
};

enum pipe_operation {
    Read_Operation = 0,
    Write_Operation = 1
};

long int g_n;
int g_smh_id;
struct db_entry_database *g_db;
int *g_pipes; // NxNx2

char g_receive_buffer[BUFFER_SIZE];

/* Calculates memory offset for 3D array of ints. */
int memory_offset(int i, int j) {
    return i * g_n * 2 + j * 2;
}

/* Process Pi sends msg to process Pj. */
void send_message(struct process *proc, int proc_receive_id, const char *msg) {
    char send_buffer[BUFFER_SIZE];
    sprintf(send_buffer, "%s(%d,%d)", msg, proc->id, proc->logic_clock);
    fprintf(stdout, "Proces P%d šalje poruku %s procesu P%d!\n", proc->id, send_buffer, proc_receive_id);
    int offset = memory_offset(proc->id, proc_receive_id);
    if (write(g_pipes[offset + Write_Operation], send_buffer, strlen(send_buffer) + 1) == -1) {
        perror("[PROCESS] write");
        exit(EXIT_FAILURE);
    }
}

/* Process's working procedure. */
void process_procedure(int id) {
    // Map SIGINT to its default behaviour.
    if (signal(SIGINT, SIG_DFL) == SIG_ERR) {
        perror("[PROCESS] signal");
        exit(EXIT_FAILURE);
    }
    struct process process = {
            .id = id,
            .logic_clock = 1
    };
    for (int i = 0; i < g_n; i++) {
        if (i == process.id) {
            continue;
        }

    }
    while (1);
}

/* Creates NxNx2 array of ints. Every two processes have two pipelines open. */
void prepare_pipelines() {
    g_pipes = (int *) malloc(g_n * g_n * 2 * sizeof(int));
    for (int i = 0; i < g_n; i++) {
        for (int j = 0; j < g_n; j++) {
            if (i == j) {
                continue;
            }
            int offset = memory_offset(i, j);
            int fd[2];
            if (pipe(fd) == -1) {
                perror("[MAIN] pipe");
                exit(EXIT_FAILURE);
            }
            g_pipes[offset + Read_Operation] = fd[Read_Operation];
            g_pipes[offset + Write_Operation] = fd[Write_Operation];
        }
    }
}

/* First detaches memory with shmdt function, and then removes SHM_ID with shmctl function. */
void free_memory(int signal) {
    fprintf(stdout, "\n");
    if (shmdt((struct db_entry_database *) g_db) == -1) {
        perror("[MAIN] shmdt");
        exit(EXIT_FAILURE);
    }
    fprintf(stdout, "Memorija vezana za segment shmid=%d je uspješno očišćena!\n", g_smh_id);
    if (shmctl(g_smh_id, IPC_RMID, NULL) == -1) {
        perror("[MAIN] shmctl");
        exit(EXIT_FAILURE);
    }
    fprintf(stdout, "Segment shmid=%d je uspješno uništen!\n", g_smh_id);
    free(g_pipes);
    fprintf(stdout, "Memorija za cjevovode je očišćena!\n");
    exit(EXIT_SUCCESS);
}

/* Registers signals for main program. */
void register_signals() {
    if (signal(SIGINT, free_memory) == SIG_ERR) {
        perror("[MAIN] signal");
        exit(EXIT_FAILURE);
    }
}

/* Prepares database using shared memory segment. */
void prepare_database() {
    if ((g_smh_id = shmget(getuid(), sizeof(struct db_entry_database) * g_n, IPC_CREAT | PERMS)) == -1) {
        perror("[MAIN] shmget");
        exit(EXIT_FAILURE);
    }
    g_db = (struct db_entry_database *) shmat(g_smh_id, NULL, 0);
    if (*((int *) g_db) == -1) {
        perror("[MAIN] shmat");
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
        perror("[MAIN] strtol");
        exit(EXIT_FAILURE);
    }
    if (*endptr != '\0' || endptr == argv[1] || g_n < MIN_PROCESSES || g_n > MAX_PROCESSES) {
        fprintf(stderr, "Program očekuje nenegativan broj iz intervala [%d, %d]!\n", MIN_PROCESSES, MAX_PROCESSES);
        exit(EXIT_FAILURE);
    }
}

int main(int argc, char const *argv[]) {
    parse_command_line_arguments(argc, argv);
    prepare_database();
    register_signals();
    prepare_pipelines();

    // Create N processes.
//    for (int i = 0; i < g_n; i++) {
//        pid_t pid = fork();
//        if (pid == -1) {
//            perror("[MAIN] fork");
//            return EXIT_FAILURE;
//        } else if (pid == 0) {
//            process_procedure(i);
//        }
//    }

    while (1);
}