#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/msg.h>
#include <sys/ipc.h>
#include <sys/wait.h>
#include <errno.h>
#include <signal.h>
#include <time.h>
#include <string.h>
#include <pthread.h>

#define LB_CARS 5
#define UB_CARS 100
#define LB_X 500
#define UB_X 1000
#define LB_Y 1000
#define UB_Y 3000
#define LB_Z 100
#define UB_Z 2000
#define MAX_CARS_SAME_DIR 3
#define DIR_DEFAULT 1
#define PASS_DEFAULT 1000
#define PASSED_DEFAULT 2000
#define OK_DEFAULT 3000

#define MSG_QUEUE_PERMS 00600 // read+write for user
#define MSG_TEXT_SIZE 20
#define MSG_WAIT "Čekam"
#define MSG_PASS "Prijeđi"
#define MSG_PASSED "Prešao"
#define MSG_OK "OK"
#define MSG_TYPE_DIR(dir) (dir + DIR_DEFAULT)
#define MSG_TYPE_PASS(reg) (PASS_DEFAULT + reg)
#define MSG_TYPE_PASSED(reg) (PASSED_DEFAULT + reg)
#define MSG_TYPE_OK(reg) (OK_DEFAULT + reg)

int msg_queue_id;

struct msg_buffer {
    long msg_type;
    char msg_text[MSG_TEXT_SIZE];
    int reg_number;
};

#define MSG_LENGTH sizeof(struct msg_buffer) - sizeof(long)

void release_msg_queue(int signal) {
    printf("\n");
    if (signal < 0) {
        // check if this function was called manually from main,
        // and not because of SIGINT
        if (msgget(getuid(), 0) == -1) {
            return;
        }
    }
    if (msgctl(msg_queue_id, IPC_RMID, NULL) == -1) {
        perror("[MAIN] msgctl");
        exit(EXIT_FAILURE);
    }
}

// Generates random integer from [lb, ub] interval.
int generate_random_integer(int lb, int ub) {
    return lb + rand() % (ub - lb + 1);
}

int change_direction(int direction) {
    return 1 - direction;
}

// Sleeps for X milliseconds and then invokes SIGUSR1 signal.
void *semaphore_thread(void *ptr) {
    printf("[SEMAPHORE_THREAD] Započinjem spavanje!\n");
    sleep(generate_random_integer(LB_X, UB_X) / 1000);
    printf("[SEMAPHORE_THREAD] Probudila sam se i šaljem signal!\n");
    kill(getpid(), SIGUSR1);
    return NULL;
}

void do_nothing(int signal) {
}

void semaphore_procedure() {
    // Map SIGINT signal for termination and SIGUSR1 for sleeping thread.
    if (signal(SIGINT, SIG_DFL) == SIG_ERR || signal(SIGUSR1, do_nothing) == SIG_ERR) {
        perror("[SEMAPHORE] signal");
        exit(EXIT_FAILURE);
    }
    struct msg_buffer buf;
    int dir = generate_random_integer(0, 1);
    int counter = 0;
    int reg_numbers[MAX_CARS_SAME_DIR];
    pthread_t thread_id;
    while (1) {
        printf("\nSmjer semafora je %d\n", dir);
        // Invoke sleeping thread.
        if (pthread_create(&thread_id, NULL, semaphore_thread, NULL) != 0) {
            perror("[SEMAPHORE] pthread_create");
            exit(EXIT_FAILURE);
        }

        // Receive messages whose msg_type is equal to semaphores direction.
        while (1) {
            if (msgrcv(msg_queue_id, (struct msg_buffer *) &buf, MSG_LENGTH, MSG_TYPE_DIR(dir), 0) == -1) {
                // Sleeping thread has finished and invoked signal.
                if (errno == EINTR) {
                    break;
                }
                // Something bad occured.
                perror("[SEMAPHORE] msgrcv1");
                exit(EXIT_FAILURE);
            }
            // Save car's registration number and stop receiving requests if reached MAX_CARS_SAME_DIR.
            reg_numbers[counter++] = buf.reg_number;
            if (counter == MAX_CARS_SAME_DIR) {
                break;
            }
        }

        // Wait for sleeping thread if it hasn't finish.
        if (pthread_join(thread_id, NULL) != 0) {
            perror("[SEMAPHORE] pthread_join");
            exit(EXIT_FAILURE);
        }

        // Seng MSG_PASS to cars.
        strcpy(buf.msg_text, MSG_PASS);
        for (int i = 0; i < counter; i++) {
            buf.msg_type = MSG_TYPE_PASS(reg_numbers[i]);
            if (msgsnd(msg_queue_id, (struct msg_buffer *) &buf, MSG_LENGTH, 0) == -1) {
                perror("[SEMAPHORE] msgsnd1");
                exit(EXIT_FAILURE);
            }
        }

        // Recieve MSG_PASSED from cars.
        for (int i = 0; i < counter; i++) {
            int reg = reg_numbers[i];
            if (msgrcv(msg_queue_id, (struct msg_buffer *) &buf, MSG_LENGTH, MSG_TYPE_PASSED(reg), 0) == -1) {
                perror("[SEMAPHORE] msgrcv2");
                exit(EXIT_FAILURE);
            }
            printf("Automobil %3d smjera %d je prešao most!\n", reg, dir);
            // Send MSG_OK.
            buf.msg_type = MSG_TYPE_OK(reg);
            strcpy(buf.msg_text, MSG_OK);
            if (msgsnd(msg_queue_id, (struct msg_buffer *) &buf, MSG_LENGTH, 0) == -1) {
                perror("[SEMAPHORE] msgsnd2");
                exit(EXIT_FAILURE);
            }
        }

        // Change direction.
        dir = change_direction(dir);
        counter = 0;
    }
}

void car_procedure(int reg_number, int direction) {
    // Map SIGINT signal for termination.
    if (signal(SIGINT, SIG_DFL) == SIG_ERR) {
        perror("[CAR] signal");
        exit(EXIT_FAILURE);
    }
    struct msg_buffer buf;
    while (1) {
        // Sleep Z ms.
        sleep(generate_random_integer(LB_Z, UB_Z) / 1000);

        // Send wait message.
        buf.msg_type = MSG_TYPE_DIR(direction);
        strcpy(buf.msg_text, MSG_WAIT);
        buf.reg_number = reg_number;
        if (msgsnd(msg_queue_id, (struct msg_buffer *) &buf, MSG_LENGTH, 0) == -1) {
            perror("[CAR] msgsnd1");
            exit(EXIT_FAILURE);
        }
        printf("Automobil %3d smjera %d čeka na prelazak preko mosta!\n", reg_number, direction);

        // Receive pass message.
        if (msgrcv(msg_queue_id, (struct msg_buffer *) &buf, MSG_LENGTH, MSG_TYPE_PASS(reg_number), 0) == -1) {
            perror("[CAR] msgrcv1");
            exit(EXIT_FAILURE);
        }
        printf("Automobil %3d smjera %d se popeo na most!\n", reg_number, direction);
        // Sleep Y ms.
        sleep(generate_random_integer(LB_Y, UB_Y) / 1000);

        // Send passed message.
        buf.msg_type = MSG_TYPE_PASSED(reg_number);
        strcpy(buf.msg_text, MSG_PASSED);
        buf.reg_number = reg_number;
        if (msgsnd(msg_queue_id, (struct msg_buffer *) &buf, MSG_LENGTH, 0) == -1) {
            perror("[CAR] msgsnd2");
            exit(EXIT_FAILURE);
        }

        // Receive OK message.
        if (msgrcv(msg_queue_id, (struct msg_buffer *) &buf, MSG_LENGTH, MSG_TYPE_OK(reg_number), 0) == -1) {
            perror("[CAR] msgrcv2");
            exit(EXIT_FAILURE);
        }

        // Change car direction.
        direction = change_direction(direction);
    }
}

int main(int argc, char const *argv[]) {
    // Parse command line arguments.
    if (argc != 2) {
        printf("Program očekuje broj automobila kao argument!\n");
        return EXIT_FAILURE;
    }
    int n = atoi(argv[1]);
    if (n <= 0 || n < LB_CARS || n > UB_CARS) {
        printf("Broj automobila mora biti nenegativan broj iz intervala [%d, %d]!\n", LB_CARS, UB_CARS);
        return EXIT_FAILURE;
    }

    // Create a message queue.
    if ((msg_queue_id = msgget(getuid(), MSG_QUEUE_PERMS | IPC_CREAT)) == -1) {
        perror("[MAIN] msgget");
        return EXIT_FAILURE;
    }

    // Map SIGINT signal for release_msg_queue function.
    if (signal(SIGINT, release_msg_queue) == SIG_ERR) {
        perror("[MAIN] signal");
        return EXIT_FAILURE;
    }

    // Set seed for randomness.
    srand((unsigned) time(NULL));

    // Create n car processes.
    printf("[MAIN] Inicijalizacija automobila i spavaj 3s prije inicijalizacije semafora:\n");
    int i;
    for (i = 0; i < n; i++) {
        int reg_number = i + 1;
        int direction = generate_random_integer(0, 1);
        switch (fork()) {
            case 0:
                car_procedure(reg_number, direction);
                return EXIT_SUCCESS;
            case -1:
                perror("[MAIN] fork car");
                return EXIT_FAILURE;
        }
    }

    // For simulation purposes...
    sleep(3);

    // Create semaphore process.
    switch (fork()) {
        case 0:
            semaphore_procedure();
            return EXIT_SUCCESS;
        case -1:
            perror("[MAIN] fork semaphore");
            return EXIT_FAILURE;
    }

    // Add one because there are n+1 processes.
    i++;
    // Wait for processes to finish.
    while (i--) {
        wait(NULL);
    }
    release_msg_queue(-1);
    printf("Svi procesi su uspješno uništeni kao i red poruka!\n");
    return EXIT_SUCCESS;
}
