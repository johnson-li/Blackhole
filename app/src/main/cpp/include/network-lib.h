//
// Created by johnson on 10/9/20.
//

#ifndef BLACKHOLE_NETWORK_LIB_H
#define BLACKHOLE_NETWORK_LIB_H

#include <cstdint>
#include <android/log.h>
#include "jni.h"

extern "C"
JNIEXPORT void JNICALL
Java_com_xuebingli_blackhole_network_UdpClient_udpPourRead(JNIEnv *env, jobject thiz, jstring ip,
                                                           jint port, jstring request,
                                                           jobject listener);

#endif //BLACKHOLE_NETWORK_LIB_H
