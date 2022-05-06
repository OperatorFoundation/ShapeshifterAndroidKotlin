package org.operatorfoundation.shapeshifter.shadow.kotlin

import android.util.Log
import java.io.InputStream
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

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

// Reads up to a specific number of bytes in a byte buffer.
fun readNBytes(input: SocketChannel, numBytes: Int): ByteBuffer
{
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

fun bytesToHex(data: ByteArray): String
{
    val hexArray = "0123456789ABCDEF".toCharArray()

    val hexChars = CharArray(data.size * 2)
    for (j in data.indices) {
        val v = data[j].toInt() and 0xFF

        hexChars[j * 2] = hexArray[v ushr 4]
        hexChars[j * 2 + 1] = hexArray[v and 0x0F]
    }
    return String(hexChars)
}

fun hexToBytes(string: String): ByteArray
{
    val length = string.length
    val data = ByteArray(length / 2)
    var i = 0
    while (i < length) {
        data[i / 2] = ((Character.digit(string[i], 16) shl 4) + Character
            .digit(string[i + 1], 16)).toByte()
        i += 2
    }
    return data
}

// generate a random number between the two numbers
fun betweenRNG(maxNumber: Int, minNumber: Int): Int
{
    // if we want maxNumber to be inclusive, add one
    // minNumber is inclusive
    val r = java.security.SecureRandom()
    return r.nextInt(maxNumber - minNumber) + minNumber
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