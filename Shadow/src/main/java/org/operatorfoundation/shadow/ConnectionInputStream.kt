package org.operatorfoundation.shadow

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
}