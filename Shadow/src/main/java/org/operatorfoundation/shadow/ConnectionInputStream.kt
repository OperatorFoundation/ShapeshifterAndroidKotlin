package org.operatorfoundation.shadow

import android.util.Log
import org.operatorfoundation.transmission.Connection
import java.io.InputStream

class ConnectionInputStream(val connection: Connection): InputStream() {
    override fun read(): Int {
        val byteArray = connection.read(1)
        if (byteArray == null) {
            return -1
        }

        val signedByte = byteArray.get(0)
        val byte = signedByte.toUByte()
        return byte.toInt()
    }

    override fun read(b: ByteArray?): Int
    {
        if (b == null)
        {
            Log.e("ShapeshifterKotlin", "ShadowInputStream read was given a null byte array.")
            return 0
        }

        if (b.isEmpty())
        {
            Log.e("ShapeshifterKotlin", "ShadowInputStream read was given an empty byte array.")
            return 0
        }

        val buffer = connection.readMaxSize(b.size)

        if (buffer == null)
        {
            Log.e("ShapeshifterKotlin", "ShadowInputStream read returned a null byte array.")
            return 0
        }

        if (!buffer.isEmpty())
        {
            Log.e("ShapeshifterKotlin", "ShadowInputStream read returned an empty byte array.")
            return 0
        }


        // if the class buffer already has data, put it in the output buffer
        buffer.copyInto(b, 0, 0, buffer.size)

        return buffer.size
    }

    override fun read(b: ByteArray?, off: Int, len: Int): Int
    {
        if (b == null)
        {
            Log.e("ShapeshifterKotlin", "ShadowInputStream read(b: ByteArray?, off: Int, len: Int) was given a null byte array.")
            return 0
        }

        if (b.isEmpty())
        {
            Log.e("ShapeshifterKotlin", "ShadowInputStream read(b: ByteArray?, off: Int, len: Int) was given an empty byte array.")
            return 0
        }

        if (b.size < (off + len))
        {
            Log.e("ShapeshifterKotlin", "ShadowInputStream read(b: ByteArray?, off: Int, len: Int) was given a byte array that is not big enough.")
            return 0
        }

        val buffer = connection.readMaxSize(len)

        if (buffer == null)
        {
            Log.e("ShapeshifterKotlin", "ShadowInputStream read(b: ByteArray?, off: Int, len: Int) received a null byte array.")
            return 0
        }

        if (!buffer.isEmpty())
        {
            Log.e("ShapeshifterKotlin", "ShadowInputStream read(b: ByteArray?, off: Int, len: Int) received an empty byte array.")
            return 0
        }

        // if the class buffer already has data, put it in the output buffer
        buffer.copyInto(b, off, 0, buffer.size)

        return buffer.size
    }

    override fun readNBytes(b: ByteArray?, off: Int, len: Int): Int
    {
        if (b == null)
        {
            Log.e("ShapeshifterKotlin", "ShadowInputStream read(b: ByteArray?, off: Int, len: Int) was given a null byte array.")
            return 0
        }

        if (b.isEmpty())
        {
            Log.e("ShapeshifterKotlin", "ShadowInputStream read(b: ByteArray?, off: Int, len: Int) was given an empty byte array.")
            return 0
        }

        if (b.size < (off + len))
        {
            Log.e("ShapeshifterKotlin", "ShadowInputStream read(b: ByteArray?, off: Int, len: Int) was given a byte array that is not big enough.")
            return 0
        }

        val buffer = connection.read(len)

        if (buffer == null)
        {
            Log.e("ShapeshifterKotlin", "ShadowInputStream read(b: ByteArray?, off: Int, len: Int) received a null byte array.")
            return 0
        }

        if (!buffer.isEmpty())
        {
            Log.e("ShapeshifterKotlin", "ShadowInputStream read(b: ByteArray?, off: Int, len: Int) received an empty byte array.")
            return 0
        }

        // if the class buffer already has data, put it in the output buffer
        buffer.copyInto(b, off, 0, buffer.size)

        return buffer.size
    }

    override fun readNBytes(len: Int): ByteArray
    {
        if (len == 0)
        {
            return byteArrayOf()
        }

        var result = byteArrayOf()

        while (result.size < len)
        {
            try
            {
                val buffer = connection.readMaxSize(len)

                if (buffer == null)
                {
                    return result
                }

                result += buffer
            }
            catch (error: Exception)
            {
                Log.e("ShapeshifterKotlin", "ShadowInputStream readNBytes(len: Int) received an error while trying to readMaxSize(). Error: $error")
                return result
            }
        }

        return result
    }
}