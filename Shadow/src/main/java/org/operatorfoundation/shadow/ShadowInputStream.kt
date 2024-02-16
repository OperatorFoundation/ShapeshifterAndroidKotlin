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

package org.operatorfoundation.shadow

import android.util.Log
import java.io.IOException
import java.io.InputStream

// This abstract class is the superclass of all classes representing an input stream of bytes.
class ShadowInputStream(
    private val networkInputStream: InputStream,
    private val decryptionCipher: ShadowCipher,
    private val shadowSocket: ShadowSocket
) : InputStream()
{
    private var buffer: ByteArray = byteArrayOf()

    // Closes this input stream and releases any system resources associated with the stream.
    override fun close() {
        networkInputStream.close()
    }

    // Reads some number of bytes from the input stream and stores them into the buffer array b.
    // Returns a -1 if we are at end of stream
    override fun read(outputBuffer: ByteArray): Int
    {
        if (outputBuffer.isEmpty())
        {
            Log.e("ShapeshifterKotlin", "ShadowInputStream read was given an empty byte array.")
            return 0
        }

        while (buffer.size < outputBuffer.size)
        {
            try
            {
                // get encrypted length
                val lengthDataSize = ShadowCipher.lengthWithTagSize

                // read bytes up to size of encrypted lengthSize into a byte buffer
                val encryptedLengthData = readNBytes(networkInputStream, lengthDataSize)
                if (encryptedLengthData == null)
                {
                    Log.e("ShapeshifterKotlin", "ShadowInputStream could not read length data.")
                    return if (buffer.isEmpty()) {
                        -1
                    } else {
                        // Copy into the output buffer from our class buffer everything we have since we will not be able to do anymore reads
                        buffer.copyInto(outputBuffer, buffer.size)
                        // We've sent them all of the data, fresh array
                        buffer = byteArrayOf()
                        buffer.size
                    }
                }

                // decrypt encrypted length to find out payload length
                val lengthData = decryptionCipher.decrypt(encryptedLengthData)

                // change lengthData from BigEndian representation to int length
                val payloadLength = getIntFromBigEndian(lengthData)

                // read and decrypt payload with the resulting length
                val encryptedPayload =
                    readNBytes(networkInputStream, payloadLength + ShadowCipher.tagSize)
                if (encryptedPayload == null)
                {
                    Log.e("ShapeshifterKotlin", "ShadowInputStream could not read encrypted length data.")
                    return if (buffer.isEmpty()) {
                        -1
                    } else {
                        // Copy into the output buffer from our class buffer everything we have since we will not be able to do anymore reads
                        buffer.copyInto(outputBuffer, buffer.size)
                        // We've sent them all of the data, fresh array
                        buffer = byteArrayOf()
                        buffer.size
                    }
                }

                // put payload into the class buffer
                val payload = decryptionCipher.decrypt(encryptedPayload)
                buffer += payload
            }
            catch (e: Exception)
            {
                if (e is IOException) // readNBytes failed
                {
                    Log.e("ShadowInputStream.read", "Received an IOException")
                }
                else // Decrypt Failed
                {
                    Log.e("ShadowInputStream.read", "Decryption failure.")
                    shadowSocket.redial() // Currently redial sets a decryptionFailed bool on the socket itself
                }

//                e.printStackTrace()

                return if (buffer.isEmpty()) {
                    -1
                } else {
                    // Copy into the output buffer from our class buffer everything we have since we will not be able to do anymore reads
                    buffer.copyInto(outputBuffer, buffer.size)
                    // We've sent them all of the data, fresh array
                    buffer = byteArrayOf()
                    buffer.size
                }
            }
        }

        // Copy into the output buffer from our class buffer up to the full capacity of output buffer
        buffer.copyInto(outputBuffer, outputBuffer.size)

        // take bytes out of buffer
        buffer = buffer.sliceArray(outputBuffer.size until buffer.size)

        return outputBuffer.size
    }

    //@ExperimentalUnsignedTypes
    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        b?.let {
            val readbuf = ByteArray(len)
            val buflen = read(readbuf)
            if (buflen == -1) {
                return -1
            }

            if (buflen == 0) {
                return 0
            }

            readbuf.copyInto(b, off, 0, buflen)
            return buflen
        }

        // If given an empty byte array, no bytes will be read.
        Log.e("read", "No bytes were read.")
        return 0
    }

    // Reads the next byte of data from the input stream.
    override fun read(): Int {
        val result: ByteArray = byteArrayOf(0)
        // read bytes up to payload length (4)
        val lengthRead = read(result)
        if (lengthRead == -1) {
            return -1
        }

        if (lengthRead != 1) {
            print("bad read")
            return -1
        }

        return result[0].toUByte().toInt()
    }
}
