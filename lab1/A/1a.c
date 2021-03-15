#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/msg.h>
#include <sys/ipc.h>
#include <errno.h>
#include <signal.h>
#include <time.h>
#include <string.h>

#define LB_CARS 5
#define UB_CARS 100
#define LB_X 500
#define UB_X 1000
#define LB_Y 1000
#define UB_Y 3000
#define LB_Z 100
#define UB_Z 2000
#define MAX_CARS_SAME_DIR 3
#define NUM_OF_REQUESTS_FOR_PASS 3

// read+write for user
#define MSG_QUEUE_PERMS 00600
#define MSG_TEXT_SIZE 10

int msg_queue_id;

struct msg_buffer {
    long msg_type;
    char msg_text[MSG_TEXT_SIZE];
};

// Releases the message queue.
void release_msg_queue(int signal) {
    printf("\n");
    if (msgctl(msg_queue_id, IPC_RMID, NULL) == -1) {
        perror("[MAIN] msgctl");
        exit(EXIT_FAILURE);
    }
    exit(EXIT_SUCCESS);
}

// Generates random integer from [lb, ub] interval.
int generate_random_integer(int lb, int ub) {
    return lb + rand() % (ub - lb + 1);
}

void semaphore_procedure() {
    // Map SIGINT signal for termination.
    if (signal(SIGINT, SIG_DFL) == SIG_ERR) {
        perror("[SEMAPHORE] signal");
        exit(EXIT_FAILURE);
    }
    struct msg_buffer buf;
    while(1) {
        if (msgrcv(msg_queue_id, (struct msg_buffer *)&buf, MSG_TEXT_SIZE, 0, 0) == -1) {
            perror("[SEMAPHORE] msgrcv");
            exit(EXIT_FAILURE);
        }
        printf("[CAR %ld]: %s\n", buf.msg_type, buf.msg_text);
    }
}

void change_direction(int *direction) {
    *direction = 1 - *direction;
}

void car_procedure(int reg_number, int direction) {
    // Map SIGINT signal for termination.
    if (signal(SIGINT, SIG_DFL) == SIG_ERR) {
        perror("[CAR] signal");
        exit(EXIT_FAILURE);
    }
    struct msg_buffer buf;
    buf.msg_type = reg_number;
    while(1) {
        // Sleep Z ms
        sleep(generate_random_integer(LB_Z, UB_Z) / 1000);
        strcpy(buf.msg_text, "Čekam!");
        if (msgsnd(msg_queue_id, (struct msg_buffer *)&buf, MSG_TEXT_SIZE, 0) == -1) {
            perror("[CAR] msgsnd");
            exit(EXIT_FAILURE);
        }
        printf("Automobil %d čeka na prelazak preko mosta!\n", reg_number);
        if (msgrcv(msg_queue_id, (struct msg_buffer *)&buf, MSG_TEXT_SIZE, buf.msg_type, 0) == -1) {
            perror("[CAR] msgrcv");
            exit(EXIT_FAILURE);
        }
        if (strcmp(buf.msg_text, "Prijeđi") == 0) {
            printf("Automobil %d se popeo na most!\n", reg_number);
            // Sleep Y ms
            sleep(generate_random_integer(LB_Y, UB_Y) / 1000);
        }
        if (msgrcv(msg_queue_id, (struct msg_buffer *)&buf, MSG_TEXT_SIZE, buf.msg_type, 0) == -1) {
            perror("[CAR] msgrcv");
            exit(EXIT_FAILURE);
        }
        if (strcmp(buf.msg_text, "Prešao") == 0) {
            printf("Automobil %d je prešao most!\n", reg_number);
        }
    }
}


int main(int argc, char const *argv[]) {
    // Parse command line arguments.
    if (argc != 2) {
        printf("Program očekuje broj automobila kao argument!\n");
        exit(EXIT_FAILURE);
    }
    int n = atoi(argv[1]);
    if (n <= 0 || n < LB_CARS || n > UB_CARS) {
        printf("Broj automobila mora biti nenegativan broj iz intervala [%d,%d]!\n", LB_CARS, UB_CARS);
        exit(EXIT_FAILURE);
    }

    // Prepare registration numbers.
    int reg_numbers[n];
    for (int i = 0; i < n; i++) {
        reg_numbers[i] = i + 1;
    }

    // Create a message queue.
    if ((msg_queue_id = msgget(getuid(), MSG_QUEUE_PERMS | IPC_CREAT)) == -1) {
        perror("[MAIN] msgget");
        exit(EXIT_FAILURE);
    }

    // Map SIGINT signal for release_msg_queue function.
    if (signal(SIGINT, release_msg_queue) == SIG_ERR) {
        perror("[MAIN] signal");
        exit(EXIT_FAILURE);
    }

    // Set seed for randomness.
    srand((unsigned)time(NULL));

    // Create semaphore process.
    switch(fork()) {
        case 0:
            semaphore_procedure();
            exit(EXIT_SUCCESS);
        case -1:
            perror("[MAIN] fork semaphore");
            exit(EXIT_FAILURE);
    }

    // Create car processes.
    switch(fork()) {
        case 0:
            car_procedure(1, 0);
            exit(EXIT_SUCCESS);
        case -1:
            perror("[MAIN] fork car");
            exit(EXIT_FAILURE);
    }

    while(1);
}
