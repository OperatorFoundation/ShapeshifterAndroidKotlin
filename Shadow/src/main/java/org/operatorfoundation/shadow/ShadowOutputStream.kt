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

import android.os.Build
import android.util.Log
import java.io.OutputStream
import java.lang.Integer.min

// This abstract class is the superclass of all classes representing an output stream of bytes.
// An output stream accepts output bytes and sends them to some sink.
class ShadowOutputStream(
    private val outputStream: OutputStream,
    private val encryptionCipher: ShadowCipher
) : OutputStream() {

    private var buffer = byteArrayOf()

    // Closes this output stream and releases any system resources associated with this stream.
    override fun close() {
        outputStream.close()
    }

    // Writes the specified byte to this output stream.
    override fun write(b: Int) {
        val plainText: ByteArray = byteArrayOf(b.toByte())
        write(plainText)
    }

    override fun write(b: ByteArray?, off: Int, len: Int) {
        // if b is null, nothing will happen and that's okay.
        b?.let {
            val buffer = b.sliceArray(off until len)
            write(buffer)
        }
    }

    // Writes b.length bytes from the specified byte array to this output stream.
    override fun write(b: ByteArray)
    {
        if (b.isEmpty())
        {
            Log.e("write", "Write function was given an empty byte array.")
            return
        }

        // appends b to buffer
        buffer += b

        // keep writing until the buffer is empty
        while (buffer.isNotEmpty())
        {
            // Don't send more than max payload size.
            val numBytesToSend: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            {
                min(ShadowCipher.maxPayloadSize, buffer.size)
            }
            else
            {
                org.operatorfoundation.shadow.min(ShadowCipher.maxPayloadSize, buffer.size)
            }

            // copy the first numBytesToSend bytes into a new byte array.
            val bytesToSend = buffer.copyOfRange(0, numBytesToSend)

            // remove the first numBytesToSend bytes from the buffer.
            buffer = buffer.sliceArray(numBytesToSend until buffer.size)

            val cipherText = encryptionCipher.pack(bytesToSend)

            println("ShadowOutputStream wrote ${cipherText.size} bytes.")
            outputStream.write(cipherText)
        }
    }

    // Flushes this output stream and forces any buffered output bytes to be written out.
    override fun flush() {
        outputStream.flush()
    }
}
