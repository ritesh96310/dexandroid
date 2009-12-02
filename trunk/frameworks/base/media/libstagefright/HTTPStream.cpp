/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <sys/socket.h>

#include <arpa/inet.h>
#include <ctype.h>
#include <errno.h>
#include <netdb.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#include <media/stagefright/HTTPStream.h>
#include <media/stagefright/MediaDebug.h>

namespace android {

// static
const char *HTTPStream::kStatusKey = ":status:";

HTTPStream::HTTPStream()
    : mState(READY),
      mSocket(-1) {
}

HTTPStream::~HTTPStream() {
    disconnect();
}

status_t HTTPStream::connect(const char *server, int port) {
    status_t err = OK;

    if (mState == CONNECTED) {
        return ERROR_ALREADY_CONNECTED;
    }

    CHECK_EQ(mSocket, -1);
    mSocket = socket(AF_INET, SOCK_STREAM, 0);
    
    if (mSocket < 0) {
        return UNKNOWN_ERROR;
    }

    struct hostent *ent = gethostbyname(server);
    if (ent == NULL) {
        err = ERROR_UNKNOWN_HOST;
        goto exit1;
    }

    struct sockaddr_in addr;
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    addr.sin_addr.s_addr = *(in_addr_t *)ent->h_addr;
    memset(addr.sin_zero, 0, sizeof(addr.sin_zero));

    if (::connect(mSocket, (const struct sockaddr *)&addr, sizeof(addr)) < 0) {
        err = ERROR_CANNOT_CONNECT;
        goto exit1;
    }

    mState = CONNECTED;

    return OK;

exit1:
    close(mSocket);
    mSocket = -1;

    return err;
}

status_t HTTPStream::disconnect() {
    if (mState != CONNECTED) {
        return ERROR_NOT_CONNECTED;
    }

    CHECK(mSocket >= 0);
    close(mSocket);
    mSocket = -1;

    mState = READY;

    return OK;
}

status_t HTTPStream::send(const char *data, size_t size) {
    if (mState != CONNECTED) {
        return ERROR_NOT_CONNECTED;
    }

    while (size > 0) {
        ssize_t n = ::send(mSocket, data, size, 0);

        if (n < 0) {
            if (errno == EINTR) {
                continue;
            }

            disconnect();

            return ERROR_IO;
        } else if (n == 0) {
            disconnect();

            return ERROR_CONNECTION_LOST;
        }

        size -= (size_t)n;
        data += (size_t)n;
    }

    return OK;
}

status_t HTTPStream::send(const char *data) {
    return send(data, strlen(data));
}

status_t HTTPStream::receive_line(char *line, size_t size) {
    if (mState != CONNECTED) {
        return ERROR_NOT_CONNECTED;
    }

    bool saw_CR = false;
    size_t length = 0;

    for (;;) {
        char c;
        ssize_t n = recv(mSocket, &c, 1, 0);
        if (n < 0) {
            if (errno == EINTR) {
                continue;
            }

            disconnect();

            return ERROR_IO;
        } else if (n == 0) {
            disconnect();

            return ERROR_CONNECTION_LOST;
        }

        if (saw_CR && c == '\n') {
            // We have a complete line.

            line[length - 1] = '\0';
            return OK;
        }

        saw_CR = (c == '\r');

        CHECK(length + 1 < size);
        line[length++] = c;
    }
}

status_t HTTPStream::receive_header(int *http_status) {
    *http_status = -1;
    mHeaders.clear();

    char line[1024];
    status_t err = receive_line(line, sizeof(line));
    if (err != OK) {
        return err;
    }

    mHeaders.add(string(kStatusKey), string(line));

    char *spacePos = strchr(line, ' ');
    if (spacePos == NULL) {
        // Malformed response?
        return UNKNOWN_ERROR;
    }

    char *status_start = spacePos + 1;
    char *status_end = status_start;
    while (isdigit(*status_end)) {
        ++status_end;
    }

    if (status_end == status_start) {
        // Malformed response, status missing?
        return UNKNOWN_ERROR;
    }

    memmove(line, status_start, status_end - status_start);
    line[status_end - status_start] = '\0';

    long tmp = strtol(line, NULL, 10);
    if (tmp < 0 || tmp > 999) {
        return UNKNOWN_ERROR;
    }

    *http_status = (int)tmp;

    for (;;) {
        err = receive_line(line, sizeof(line));
        if (err != OK) {
            return err;
        }

        if (*line == '\0') {
            // Empty line signals the end of the header.
            break;
        }

        // puts(line);

        char *colonPos = strchr(line, ':');
        if (colonPos == NULL) {
            mHeaders.add(string(line), string());
        } else {
            char *end_of_key = colonPos;
            while (end_of_key > line && isspace(end_of_key[-1])) {
                --end_of_key;
            }

            char *start_of_value = colonPos + 1;
            while (isspace(*start_of_value)) {
                ++start_of_value;
            }

            *end_of_key = '\0';

            mHeaders.add(string(line), string(start_of_value));
        }
    }

    return OK;
}

ssize_t HTTPStream::receive(void *data, size_t size) {
    size_t total = 0;
    while (total < size) {
        ssize_t n = recv(mSocket, (char *)data + total, size - total, 0);

        if (n < 0) {
            if (errno == EINTR) {
                continue;
            }

            disconnect();
            return ERROR_IO;
        } else if (n == 0) {
            disconnect();

            return ERROR_CONNECTION_LOST;
        }

        total += (size_t)n;
    }

    return (ssize_t)total;
}

bool HTTPStream::find_header_value(const string &key, string *value) const {
    ssize_t index = mHeaders.indexOfKey(key);
    if (index < 0) {
        value->clear();
        return false;
    }

    *value = mHeaders.valueAt(index);

    return true;
}

}  // namespace android

