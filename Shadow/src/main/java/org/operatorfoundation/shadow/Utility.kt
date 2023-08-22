package org.operatorfoundation.shadow

import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

//@kotlin.ExperimentalUnsignedTypes
// Reads up to a specific number of bytes in a byte array.
fun readNBytes(input: InputStream, numBytes: Int): ByteArray?
{
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

fun min(firstNumber: Int, secondNumber: Int): Int
{
    return if (firstNumber < secondNumber)
    {
        firstNumber
    }
    else
    {
        secondNumber
    }
}

// changes data (bigEData) from BigEndian representation to an int
fun getIntFromBigEndian(bigEData: ByteArray): Int
{
    val leftByte = bigEData[0].toUByte()
    val rightByte = bigEData[1].toUByte()
    val rightInt = rightByte.toInt()
    val leftInt = leftByte.toInt()
    val payloadLength = (leftInt * 256) + rightInt
    return payloadLength
}

fun InputStream.transferTo(out: OutputStream): Long {
    var bytesRead: Long = 0

    while (true) {
        try {
            val maybeByte = this.read()
            if (maybeByte < -1) {
                println("maybeByte less than zero")
            }
            if (maybeByte == -1) {
                return bytesRead
            } else {
                out.write(maybeByte)
                bytesRead += 1
            }
        } catch(exception: IOException) {
            this.close()
            out.close()
            return bytesRead
        }
    }
}