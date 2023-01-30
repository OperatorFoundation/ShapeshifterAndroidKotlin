package org.operatorfoundation.shadowkotlin

import org.operatorfoundation.transmission.Connection
import java.io.OutputStream

class ConnectionOutputStream(val connection: Connection): OutputStream() {
    override fun write(b: Int) {
        val unsignedByte = b.toUByte()
        val byte = unsignedByte.toByte()
        val byteArray = byteArrayOf(byte)
        connection.write(byteArray)
    }
}