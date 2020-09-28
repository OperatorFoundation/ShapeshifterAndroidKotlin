package org.operatorfoundation.shapeshifter.shadow.kotlin

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

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

fun readNBytes(input: SocketChannel, numBytes: Int): ByteBuffer {
    val buffer = ByteBuffer.allocate(numBytes)
    val bufferArray = arrayOf((buffer))
    var offset = input.read(buffer)
    while (offset != numBytes) {
        val bytesRead = input.read(bufferArray, offset, numBytes - offset).toInt()
        offset += bytesRead
    }
    return buffer
}