CFLAGS+=-Wall -Werror -Wextra -std=c99 -Wno-unused-parameter
CC?=gcc

all: compile

compile: bin
	${CC} ${CFLAGS} -Iinclude src/main.c -o bin/main

bin:
	mkdir -p bin

clean:
	rm -rf bin
