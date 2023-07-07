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
) :
    InputStream() {

    private var buffer: ByteArray = byteArrayOf()
    //private var decryptionFailed = false

    // Closes this input stream and releases any system resources associated with the stream.
    override fun close() {
        networkInputStream.close()
    }

    // Reads some number of bytes from the input stream and stores them into the buffer array b.
    // Returns a -1 if we are at end of stream
    //@ExperimentalUnsignedTypes
    override fun read(outputBuffer: ByteArray): Int
    {
//        if (decryptionFailed)
//        {
//            Log.e("ShapeshifterKotlin", "ShadowInputStream Decryption failed on read.")
//            shadowSocket.close()
//            throw IOException()
//        }

        if (outputBuffer.isEmpty())
        {
            Log.e("ShapeshifterKotlin", "ShadowInputStream read was given an empty byte array.")
            return 0
        }

        val returnSize = outputBuffer.size
        val bufferSize = buffer.size

        // if the class buffer already has data, put it in the output buffer
        if (returnSize <= bufferSize)
        {
            buffer.copyInto(outputBuffer, 0, 0, returnSize)
            buffer = buffer.sliceArray(returnSize until bufferSize)

            return outputBuffer.size
        }
        else if (!buffer.isEmpty())
        {
            // we were passed an array that is bigger than what we already stored in the buffer
            // but the buffer isn't empty, so lets pass what's in our buffer back to the caller
            buffer.copyInto(outputBuffer, 0, 0, bufferSize)

            // Empty our buffer now that we've handed off all of the data in it
            buffer = byteArrayOf()

            return outputBuffer.size
        }

        try
        {
            // get encrypted length
            val lengthDataSize = ShadowCipher.lengthWithTagSize

            // read bytes up to size of encrypted lengthSize into a byte buffer
            val encryptedLengthData = readNBytes(networkInputStream, lengthDataSize)
            if (encryptedLengthData == null)
            {
                Log.e("ShapeshifterKotlin", "ShadowInputStream could not read length data.")
                return -1
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
                return -1
            }

            // put payload into the class buffer
            val payload = decryptionCipher.decrypt(encryptedPayload)
            buffer += payload
            var outputBufferSize = outputBuffer.size

            if (buffer.size < outputBufferSize)
            {
                outputBufferSize = buffer.size
            }
            buffer.copyInto(outputBuffer, 0, 0, outputBufferSize)

            // take bytes out of buffer
            buffer = buffer.sliceArray(outputBufferSize until buffer.size)

            return outputBufferSize
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

            e.printStackTrace()
            throw IOException()
        }
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
