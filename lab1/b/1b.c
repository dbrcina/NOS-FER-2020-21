#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <limits.h>
#include <unistd.h>
#include <signal.h>
#include <sys/ipc.h>
#include <sys/shm.h>

#define MIN_PROCESSES 3
#define MAX_PROCESSES 10
#define PERMS 00600
#define LB_X 100
#define UB_X 2000
#define REPEAT 5

struct db_entry_database {
    int id;
    int logic_clock;
    int entries;
};

long int N;
int SHM_ID;
struct db_entry_database *DB;

/* First detaches memory with shmdt function, and then removes SHM_ID with shmctl function. */
void free_memory(int signal) {
    fprintf(stdout, "\n");
    if (shmdt(DB) == -1) {
        perror("[MAIN] shmdt");
        exit(EXIT_FAILURE);
    }
    fprintf(stdout, "Memorija vezana za segment shmid=%d je uspješno očišćena!\n", SHM_ID);
    if (shmctl(SHM_ID, IPC_RMID, NULL) == -1) {
        perror("[MAIN] shmctl");
        exit(EXIT_FAILURE);
    }
    fprintf(stdout, "Segment shmid=%d je uspješno uništen!\n", SHM_ID);
    // If this function was invoked by some kind of signal...
    if (signal > 0) {
        exit(EXIT_SUCCESS);
    }
}

/* Prepares database using shared memory segment. */
void prepare_database() {
    if ((SHM_ID = shmget(getuid(), sizeof(struct db_entry_database) * N, IPC_CREAT | PERMS)) == -1) {
        perror("[MAIN] shmget");
        exit(EXIT_FAILURE);
    }
    DB = (struct db_entry_database *) shmat(SHM_ID, NULL, 0);
    if (*((int *) DB) == -1) {
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
    N = strtol(argv[1], &endptr, 10);
    if ((errno == ERANGE && (N == LONG_MAX || N == LONG_MIN)) || (errno != 0 && N == 0)) {
        perror("strtol");
        exit(EXIT_FAILURE);
    }
    if (*endptr != '\0' || endptr == argv[1] || N < MIN_PROCESSES || N > MAX_PROCESSES) {
        fprintf(stderr, "Program očekuje nenegativan broj iz intervala [%d, %d]!\n", MIN_PROCESSES, MAX_PROCESSES);
        exit(EXIT_FAILURE);
    }
}

int main(int argc, char const *argv[]) {
    parse_command_line_arguments(argc, argv);
    prepare_database();
    if (signal(SIGINT, free_memory) == SIG_ERR) {
        perror("[MAIN] signal");
        return EXIT_FAILURE;
    }
    while (1);
    free_memory(-1);
    return EXIT_SUCCESS;
}