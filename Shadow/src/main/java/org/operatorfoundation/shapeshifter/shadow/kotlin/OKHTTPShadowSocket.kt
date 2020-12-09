package org.operatorfoundation.shapeshifter.shadow.kotlin

import java.net.SocketAddress

@ExperimentalUnsignedTypes
class OKHTTPShadowSocket(config: ShadowConfig, shadowHost: String, shadowPort: Int) : ShadowSocket(config, shadowHost, shadowPort) {

    @ExperimentalUnsignedTypes
    override fun connect(endpoint: SocketAddress?) {

    }

}