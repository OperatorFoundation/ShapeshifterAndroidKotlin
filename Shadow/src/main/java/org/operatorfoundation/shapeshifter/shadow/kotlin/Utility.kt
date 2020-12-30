package org.operatorfoundation.shapeshifter.shadow.kotlin

import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

@kotlin.ExperimentalUnsignedTypes
// Reads up to a specific number of bytes in a byte array.
fun readNBytes(input: InputStream, numBytes: Int): ByteArray? {
    val buffer = ByteArray(numBytes)
    var offset = input.read(buffer)
    if (offset == -1) {
        Log.e("readNBytes", "Could not find the offset for readNBytes.")
        return null
    }
    while (offset != numBytes) {
        val bytesRead = input.read(buffer, offset, numBytes - offset)
        if (bytesRead == -1) {
            Log.e("readNBytes", "Could not read the specified number of bytes.")
            return null
        }
        offset += bytesRead
    }
    return buffer
}

// Reads up to a specific number of bytes in a byte buffer.
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