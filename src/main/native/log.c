#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include "log.h"

static void getTime(char *tm) {
	time_t rawtime;
	struct tm * timeinfo;

	time (&rawtime);
	timeinfo = localtime (&rawtime);

	strftime(tm, 50, "%a, %d %b %Y %H:%M:%S CEST", timeinfo);
}

static void log_message(const char *msg, const char *lvl) {
	char time[50];
	getTime(time);
	fprintf(stderr, "[%s] %s: %s\n", lvl, time, msg);
}

void log_error(const char *msg) { log_message(msg, "ERROR"); }
void log_warning(const char *msg) { log_message(msg, "WARNING");}
void log_info(const char *msg) { log_message(msg, "INFO");}
void log_debug(const char *msg) { log_message(msg, "DEBUG");}
