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
#include <stdbool.h>

#define LB_CARS 5
#define UB_CARS 100
#define LB_X 500
#define UB_X 1000
#define LB_Y 1000
#define UB_Y 3000
#define LB_Z 100
#define UB_Z 2000
#define MAX_CARS_SAME_DIR 3

// read+write for user
#define MSG_QUEUE_PERMS 00600
#define MSG_TEXT_SIZE 10
#define MSG_WAIT "Čekam"
#define MSG_PASS "Prijeđi"
#define MSG_PASSED "Prešao"

struct node {
    int key;
    int data;
    struct node *next;
};

int msg_queue_id;

struct msg_buffer {
    long msg_type;
    char msg_text[MSG_TEXT_SIZE];
    int car_direction;
};

// Releases the message queue.
void release_msg_queue(int signal) {
    printf("\n");
    if (msgctl(msg_queue_id, IPC_RMID, NULL) == -1) {
        perror("[MAIN] msgctl");
        exit(EXIT_FAILURE);
    }
}

// Generates random integer from [lb, ub] interval.
int generate_random_integer(int lb, int ub) {
    return lb + rand() % (ub - lb + 1);
}

void change_direction(int *direction) {
    *direction = 1 - *direction;
}

void semaphore_procedure() {
    // Map SIGINT signal for termination.
    if (signal(SIGINT, SIG_DFL) == SIG_ERR) {
        perror("[SEMAPHORE] signal");
        exit(EXIT_FAILURE);
    }
    struct msg_buffer buf;
    int current_direction = generate_random_integer(0, 1);
    int direction_counter = 0;
    struct node *head = NULL;
    struct node *last = NULL;
    while (1) {
        // Receive a message from car.
        if (msgrcv(msg_queue_id, (struct msg_buffer *)&buf, MSG_TEXT_SIZE, 0, 0) == -1) {
            perror("[SEMAPHORE] msgrcv");
            exit(EXIT_FAILURE);
        }

        // Cache current car's data.
        struct node *el = (struct node *)malloc(sizeof(struct node));
        el->key = buf.msg_type;
        el->data = buf.car_direction;
        if (head == NULL) {
            head = last = el;
        } else {
            last->next = el;
            last = el;
        }

        // Increment counter if current car's direction is equal to semaphore's.
        if (buf.car_direction == current_direction) {
            direction_counter++;
        }

        // If counter reached maximum number of requests for current direction.
        if (direction_counter == MAX_CARS_SAME_DIR) {
            int i = 0;
            int reg_numbers[MAX_CARS_SAME_DIR];
            struct node *current = head;
            while (direction_counter--) {
                if (current->data == current_direction) {
                    reg_numbers[i++] = current->key;
                }
                struct node *temp = current;
                current = current->next;
                if (temp == head) {
                    head = current;
                } else if (temp->next == last) {
                    last = current;
                }
                free(temp);
            }
            strcpy(buf.msg_text, MSG_PASS);
            buf.car_direction = current_direction;
            for (i = 0; i < MAX_CARS_SAME_DIR; i++) {
                buf.msg_type = reg_numbers[i];
                if (msgsnd(msg_queue_id, (struct msg_buffer *)&buf, MSG_TEXT_SIZE, 0) == -1) {
                        perror("[SEMAPHORE] msgsnd");
                        exit(EXIT_FAILURE);
                    }
            }
        }
    }
}

void car_procedure(int reg_number, int direction) {
    // Map SIGINT signal for termination.
    if (signal(SIGINT, SIG_DFL) == SIG_ERR) {
        perror("[CAR] signal");
        exit(EXIT_FAILURE);
    }
    struct msg_buffer buf;
    buf.msg_type = reg_number;
    buf.car_direction = direction;
    while (1) {
        // Sleep Z ms
        sleep(generate_random_integer(LB_Z, UB_Z) / 1000);
        strcpy(buf.msg_text, MSG_WAIT);
        // Send wait message.
        if (msgsnd(msg_queue_id, (struct msg_buffer *)&buf, MSG_TEXT_SIZE, 0) == -1) {
            perror("[CAR] msgsnd");
            exit(EXIT_FAILURE);
        }
        printf("Automobil %d smjera %d čeka na prelazak preko mosta!\n", reg_number, buf.car_direction);
        // // Receive pass/passed message.
        // if (msgrcv(msg_queue_id, (struct msg_buffer *)&buf, MSG_TEXT_SIZE, buf.msg_type, 0) == -1) {
        //     perror("[CAR] msgrcv");
        //     exit(EXIT_FAILURE);
        // }
        // if (strcmp(buf.msg_text, MSG_PASS) == 0) {
        //     printf("Automobil %d smjera %d se popeo na most!\n", reg_number, buf.car_direction);
        // } else if (strcmp(buf.msg_text, MSG_PASSED) == 0) {
        //     printf("Automobil %d smjera %d je prešao most!\n", reg_number, buf.car_direction);
        // }
        // // // Change car direction.
        // // change_direction(&buf.car_direction);
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
    srand((unsigned)time(NULL));

    // Create semaphore process.
    switch (fork()) {
        case 0:
            semaphore_procedure();
        case -1:
            perror("[MAIN] fork semaphore");
            return EXIT_FAILURE;
    }

    // Create n car processes.
    int i;
    for (i = 0; i < n; i++) {
        int reg_number = i + 1;
        int direction = generate_random_integer(0, 1);
        switch(fork()) {
            case 0:
                car_procedure(reg_number, direction);
            case -1:
                perror("[MAIN] fork car");
                return EXIT_FAILURE;
        }  
    }
    // Add one because there are n+1 processes.
    i++;
    // Wait for processes to finish.
    while (i--) {
       wait(NULL);
    }
    printf("Svi procesi su uspješno uništeni kao i red poruka!\n");
    return EXIT_SUCCESS;
}
