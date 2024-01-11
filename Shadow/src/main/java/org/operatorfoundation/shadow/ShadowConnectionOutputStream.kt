package org.operatorfoundation.shadow

import android.os.Build
import android.util.Log
import org.operatorfoundation.transmission.Connection
import java.io.OutputStream

class ShadowConnectionOutputStream(private val connection: Connection, private val encryptionCipher: ShadowCipher) : OutputStream()
{
    private var buffer = byteArrayOf()

    // Closes this output stream and releases any system resources associated with this stream.
    override fun close() {
        connection.close()
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
                Integer.min(ShadowCipher.maxPayloadSize, buffer.size)
            }
            else
            {
                min(ShadowCipher.maxPayloadSize, buffer.size)
            }

            // copy the first numBytesToSend bytes into a new byte array.
            val bytesToSend = buffer.copyOfRange(0, numBytesToSend)

            // remove the first numBytesToSend bytes from the buffer.
            buffer = buffer.sliceArray(numBytesToSend until buffer.size)

            val cipherText = encryptionCipher.pack(bytesToSend)

            connection.write(cipherText)
        }
    }
}