package org.operatorfoundation.shadowkotlin

import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress

//@ExperimentalUnsignedTypes
class OKHTTPShadowSocket(val shadowHost: String, val shadowPort: Int): Socket() {

    //@ExperimentalUnsignedTypes
    override fun connect(endpoint: SocketAddress?) {
        val address = InetSocketAddress(shadowHost, shadowPort)
        super.connect(address)
    }

   // @ExperimentalUnsignedTypes
    override fun connect(endpoint: SocketAddress?, timeout: Int) {
       val address = InetSocketAddress(shadowHost, shadowPort)
       super.connect(address, timeout)
    }

    // Converts this socket to a String.
    override fun toString(): String {
        return "OKHTTPShadowSocket"
    }
}