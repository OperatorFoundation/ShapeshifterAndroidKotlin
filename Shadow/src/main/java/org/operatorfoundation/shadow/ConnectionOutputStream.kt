package org.operatorfoundation.shadow

import org.operatorfoundation.transmission.Connection
import java.io.OutputStream

class ConnectionOutputStream(val connection: Connection): OutputStream() {
    override fun write(b: Int) {
        val unsignedByte = b.toUByte()
        val byte = unsignedByte.toByte()
        val byteArray = byteArrayOf(byte)
        connection.write(byteArray)
    }

    override fun write(b: ByteArray) {
        connection.write(b)
    }

    override fun write(b: ByteArray?, off: Int, len: Int) {
        // if b is null, nothing will happen and that's okay.
        b?.let {
            val buffer = b.sliceArray(off until len)
            write(buffer)
        }
    }
}