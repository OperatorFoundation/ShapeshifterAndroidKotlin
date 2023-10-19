package org.operatorfoundation.shadow

import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress

class OKHTTPShadowSocket(val shadowHost: String, val shadowPort: Int): Socket() {

    override fun connect(endpoint: SocketAddress?) {
        val address = InetSocketAddress(shadowHost, shadowPort)
        super.connect(address)
    }

    override fun connect(endpoint: SocketAddress?, timeout: Int) {
       val address = InetSocketAddress(shadowHost, shadowPort)
       super.connect(address, timeout)
    }

    // Converts this socket to a String.
    override fun toString(): String {
        return "OKHTTPShadowSocket"
    }
}