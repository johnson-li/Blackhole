package com.xuebingli.blackhole.utils

class ArrayUtils {

    companion object {
        @ExperimentalUnsignedTypes
        fun bytes2int(byteArray: ByteArray, offset: Int = 0): Int {
            var res = 0
            for (i in 0..3) {
                res = res shl 8
                res += byteArray[i + offset].toUByte().toInt()
            }
            return res
        }
    }
}