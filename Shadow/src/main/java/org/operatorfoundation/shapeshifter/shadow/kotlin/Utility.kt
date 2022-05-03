package org.operatorfoundation.shapeshifter.shadow.kotlin

import android.util.Log
import java.io.InputStream
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

@kotlin.ExperimentalUnsignedTypes
// Reads up to a specific number of bytes in a byte array.
fun readNBytes(input: InputStream, numBytes: Int): ByteArray? {
    try
    {
        val buffer = ByteArray(numBytes)
        var offset = input.read(buffer)

        if (offset == -1) {
            Log.d("Shapeshifter.readNBytes", "The end of the stream has been reached")
            return null
        }
        while (offset != numBytes) {
            val bytesRead = input.read(buffer, offset, numBytes - offset)
            if (bytesRead == -1) {
                Log.e("Shapeshifter.readNBytes", "Could not read the specified number of bytes ${numBytes - offset} because the end of the stream has been reached")
                return null
            }
            offset += bytesRead
        }
        return buffer
    }
    catch(readError: Exception)
    {
        Log.e("Shapeshifter.readNBytes", "Error: ${readError.message}")
        throw readError
    }

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

fun ByteArray.toHexString() = asUByteArray().joinToString("") {
    it.toString(16).padStart(2, '0')
}