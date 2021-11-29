//
// Created by johnson on 10/9/20.
//

#include <sys/socket.h>
#include <netinet/in.h>
#include <cstring>
#include <netdb.h>
#include <ctime>
#include <fcntl.h>
#include <malloc.h>
#include <unistd.h>
#include "network-lib.h"

#define BUF_SIZE 102400

long elapsedTime(JNIEnv *env) {
//    timespec now{};
//    clock_gettime(CLOCK_MONOTONIC, &now);
//    return (int64_t) now.tv_sec * 1000LL + now.tv_nsec / 1000000;
    auto clazz = env->FindClass("android/os/SystemClock");
    auto method_id = env->GetStaticMethodID(clazz, "elapsedRealtime", "()J");
    return env->CallStaticLongMethod(clazz, method_id);
}

long currentTs(JNIEnv *env) {
//    struct timeval tv;
//    gettimeofday(&tv,NULL);
//    return tv.tv_sec * 1000 + tv.tv_usec / 1000;
    timespec now{};
    clock_gettime(CLOCK_MONOTONIC, &now);
    return now.tv_sec * 1000LL + now.tv_nsec / 1000000;
}

extern "C" void
Java_com_xuebingli_blackhole_network_UdpClient_udpPourRead(JNIEnv *env, jobject thiz, jstring ip_j,
                                                           jint port, jstring request_j,
                                                           jobject listener) {
    sockaddr_in addr{};
    auto clazz = env->FindClass("com/xuebingli/blackhole/network/DatagramListener");
    auto method_id = env->GetMethodID(clazz, "onReceived", "(IIJJ)Z");
    auto ip = env->GetStringUTFChars(ip_j, nullptr);
    auto request = env->GetStringUTFChars(request_j, nullptr);
    auto host = gethostbyname(ip);
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    addr.sin_addr = *((in_addr *) host->h_addr);

    int fd = socket(AF_INET, SOCK_DGRAM, 0);
    fcntl(fd, F_SETFL, O_NONBLOCK);
    sendto(fd, request, strlen(request), MSG_CONFIRM, (sockaddr *) &addr, sizeof(addr));
    unsigned char buffer[BUF_SIZE];
    socklen_t len;
    int nread;
    unsigned int seq = 0;
    unsigned long long remote_timestamp = 0;
    bool finish = false;
    long long data_size = 0;
    while (!finish) {
        nread = recvfrom(fd, buffer, BUF_SIZE, MSG_WAITALL, (sockaddr *) &addr, &len);
        if (nread <= 0) {
            continue;
        }
//        for (int i = 0; i < 4; i++) {
//            seq = (seq << 8u) | buffer[i];
//        }
//        for (int i = 4; i < 12; i++) {
//            remote_timestamp = (remote_timestamp << 8u) | buffer[i];
//        }
//        __android_log_print(ANDROID_LOG_INFO, "johnson", "Read %d bytes, seq: %d, remoteTs: %lld, localTs: %lld",
//                            nread, seq, remote_timestamp, elapsedTime(env));
//        finish = !env->CallBooleanMethod(listener, method_id, (int) seq, nread,
//                                         (long long) remote_timestamp, elapsedTime(env));
        data_size += nread;
//        if (nread == 1) {
        __android_log_print(ANDROID_LOG_INFO, "johnson", "Data size: %lld bytes, %d",
                            data_size, nread);
//        }
    }

    // clean up
    env->ReleaseStringUTFChars(ip_j, ip);
    env->ReleaseStringUTFChars(request_j, request);
}

extern "C" void
Java_com_xuebingli_blackhole_network_UdpClient_udpEcho(JNIEnv *env, jobject thiz, jstring ip_j,
                                                       jint port, jint datarate,
                                                       jint pkg_size, jobject controller,
                                                       jboolean logging) {
    auto clazz = env->FindClass("com/xuebingli/blackhole/network/Controller");
    auto method_id = env->GetMethodID(clazz, "terminated", "()Z");
    sockaddr_in addr{};
    auto ip = env->GetStringUTFChars(ip_j, nullptr);
    auto host = gethostbyname(ip);
    auto bytes = (uint8_t *) calloc(pkg_size, sizeof(uint8_t));
    auto ts_len = 100 * 1024 * 1024;
    auto send_ts = new long[ts_len];
    auto recv_ts = new long[ts_len];
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    addr.sin_addr = *((in_addr *) host->h_addr);
    int fd = socket(AF_INET, SOCK_DGRAM, 0);
    fcntl(fd, F_SETFL, O_NONBLOCK);
    unsigned char buffer[BUF_SIZE];
    int nread;
    socklen_t len;
    unsigned int seq = 0;
    auto start_ts = currentTs(env);
    while (!env->CallBooleanMethod(controller, method_id)) {
        auto wait = start_ts + seq * pkg_size * 1000 / datarate - currentTs(env);
        if (wait < 0) {
            if (logging) {
                send_ts[seq % ts_len] = currentTs(env);
            }
            for (int i = 0; i < 8; i++) {
                bytes[i] = (uint8_t) ((seq >> (i * 8)) & 0xff);
            }
            sendto(fd, bytes, pkg_size, MSG_CONFIRM, (sockaddr *) &addr, sizeof(addr));
//            __android_log_print(ANDROID_LOG_INFO, "johnson", "Sent: %d, %d", seq, pkg_size);
            seq += 1;
        }
        nread = recvfrom(fd, buffer, BUF_SIZE, MSG_WAITALL, (sockaddr *) &addr, &len);
        if (nread > 0) {
            unsigned int sent_seq = 0;
            for (int i = 0; i < 8; i++) {
                sent_seq = (sent_seq << 8) | buffer[7 - i];
            }
            if (logging) {
                recv_ts[sent_seq] = currentTs(env);
            }
//            __android_log_print(ANDROID_LOG_INFO, "johnson",
//                                "Received id: %d, size: %d, latency: %ld ms",
//                                sent_seq, nread,
//                                recv_ts[sent_seq % ts_len] - send_ts[sent_seq % ts_len]);
        }
    }
    __android_log_print(ANDROID_LOG_INFO, "johnson", "Exporting UDP echo logs");
}
