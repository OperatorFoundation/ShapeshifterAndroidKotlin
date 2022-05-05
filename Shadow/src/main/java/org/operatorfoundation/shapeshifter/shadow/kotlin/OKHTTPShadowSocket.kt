package org.operatorfoundation.shapeshifter.shadow.kotlin

import java.net.SocketAddress

//@ExperimentalUnsignedTypes
class OKHTTPShadowSocket(config: ShadowConfig, shadowHost: String, shadowPort: Int) :
    ShadowSocket(config, shadowHost, shadowPort) {

    //@ExperimentalUnsignedTypes
    override fun connect(endpoint: SocketAddress?) {

    }

   // @ExperimentalUnsignedTypes
    override fun connect(endpoint: SocketAddress?, timeout: Int) {

    }

    // Converts this socket to a String.
    override fun toString(): String {
        return "OKHTTPShadowSocket[" + "password = " + config.password + ", cipherName = " + config.cipherName + "]"
    }
}