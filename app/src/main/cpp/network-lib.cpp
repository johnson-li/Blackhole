//
// Created by johnson on 10/9/20.
//

#include <sys/socket.h>
#include <netinet/in.h>
#include <cstring>
#include <netdb.h>
#include "network-lib.h"

#define BUF_SIZE 102400

extern "C" void
Java_com_xuebingli_blackhole_network_UdpClient_udpPourRead(JNIEnv *env, jobject thiz, jstring ip_j,
                                                           jint port, jstring request_j,
                                                           jobject listener) {
    __android_log_print(ANDROID_LOG_INFO, "johnson", "asdf");
    sockaddr_in addr{};
    auto ip = env->GetStringUTFChars(ip_j, nullptr);
    auto request = env->GetStringUTFChars(request_j, nullptr);
    auto host = gethostbyname(ip);
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    addr.sin_addr = *((in_addr *) host->h_addr);

    int fd = socket(AF_INET, SOCK_DGRAM, 0);
    sendto(fd, request, strlen(request), MSG_CONFIRM, (sockaddr *) &addr, sizeof(addr));
    char buffer[BUF_SIZE];
    socklen_t len;
    int nread;
    while (1) {
        nread = recvfrom(fd, (char *) buffer, BUF_SIZE, MSG_WAITALL, (sockaddr *) &addr, &len);
        int seq = ((unsigned int *) buffer)[0];
        long remote_timestamp = ((unsigned long *) (buffer + 4))[0];
        long local_timestamp = 0;
        __android_log_print(ANDROID_LOG_INFO, "johnson", "Read %d bytes, seq: %d, remoteTs: %ld",
                            nread, seq, remote_timestamp);
    }

    // clean up
    env->ReleaseStringUTFChars(ip_j, ip);
    env->ReleaseStringUTFChars(request_j, request);
}
