package org.operatorfoundation.shapeshifter.shadow.kotlin

import java.io.InputStream

//@kotlin.ExperimentalUnsignedTypes
//fun ByteArray.toHexString() = asUByteArray().joinToString("") {
//    it.toString(16).padStart(2, '0')
//}

@kotlin.ExperimentalUnsignedTypes
fun readNBytes(input: InputStream, numBytes: Int): ByteArray {
    val buffer = ByteArray(numBytes)
    var offset = input.read(buffer)
    while (offset != numBytes) {
        val bytesRead = input.read(buffer, offset, numBytes - offset)
        offset += bytesRead
    }
    return buffer
}