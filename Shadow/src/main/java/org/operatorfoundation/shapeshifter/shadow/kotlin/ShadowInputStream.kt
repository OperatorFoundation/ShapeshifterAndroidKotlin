/*
	MIT License

	Copyright (c) 2020 Operator Foundation

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in all
	copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
	SOFTWARE.
*/

package org.operatorfoundation.shapeshifter.shadow.kotlin

import android.util.Log
import java.io.InputStream
import java.lang.Integer.min

// This abstract class is the superclass of all classes representing an input stream of bytes.
class ShadowInputStream(
    private val networkInputStream: InputStream,
    private val decryptionCipher: ShadowCipher
) :
    InputStream() {

    private var buffer: ByteArray = byteArrayOf()
    private var decryptionFailed = false

    // Closes this input stream and releases any system resources associated with the stream.
    override fun close() {
        networkInputStream.close()
    }

    // Reads some number of bytes from the input stream and stores them into the buffer array b.
    @ExperimentalUnsignedTypes
    override fun read(b: ByteArray): Int {
        if (decryptionFailed) {
            Log.e("read", "Decryption failed on read.")
            return -1
        }
        if (b.isEmpty()) {
            Log.e("read", "read was given an empty byte array.")
            return 0
        }

        // puts the bytes in a buffer.
        if (b.size <= buffer.size) {
            val resultSize = min(b.size, buffer.size)
            buffer.copyInto(b, 0, 0, resultSize)
            buffer = buffer.sliceArray(resultSize until buffer.size)

            return resultSize
        }

        // get encrypted length
        val lengthDataSize = ShadowCipher.lengthWithTagSize

        // read bytes up to size of encrypted lengthSize into a byte buffer
        val encryptedLengthData = readNBytes(networkInputStream, lengthDataSize)
        if (encryptedLengthData == null) {
            Log.e("read", "Could not read encrypted length bytes.")
            return -1
        }

        try {
            // decrypt encrypted length to find out payload length
            val lengthData = decryptionCipher.decrypt(encryptedLengthData!!)


            // change lengthData from BigEndian representation to int length
            val leftByte = lengthData[0]?.toUByte()
            val rightByte = lengthData[1]?.toUByte()
            val rightInt = rightByte.toInt()
            val leftInt = leftByte.toInt()
            val payloadLength = (leftInt * 256) + rightInt

            // read and decrypt payload with the resulting length
            val encryptedPayload =
                readNBytes(networkInputStream, payloadLength + ShadowCipher.tagSize)
            if (encryptedPayload == null) {
                Log.e("read", "Could not read encrypted length data.")
                return -1
            }


            val payload = decryptionCipher.decrypt(encryptedPayload)

            // put payload into buffer
            buffer += payload
            val resultSize = min(b.size, buffer.size)
            buffer.copyInto(b, 0, 0, resultSize)

            // take bytes out of buffer
            buffer = buffer.sliceArray(resultSize until buffer.size)

            return resultSize

        } catch (e: Exception) {
            Log.e("read", "Decryption failed on read.")
            decryptionFailed = true
            return -1
        }
    }

    @ExperimentalUnsignedTypes
    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        b?.let {
            val readbuf = ByteArray(len)
            val buflen = read(readbuf)
            readbuf.copyInto(b, off, 0, buflen)
            return buflen
        }

        // If given an empty byte array, no bytes will be read.
        Log.e("read", "No bytes were read.")
        return 0
    }

    // Reads the next byte of data from the input stream.
    @ExperimentalUnsignedTypes
    override fun read(): Int {
        val result: ByteArray = byteArrayOf(0)
        // read bytes up to payload length (4)
        read(result)
        return result[0].toInt()
    }

}
