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

package org.operatorfoundation.shadowkotlin

import com.google.gson.Gson
import java.net.InetAddress
import java.net.Socket
import java.net.URL
import java.util.*
import javax.net.SocketFactory

class JsonConfig {
    data class ShadowJsonConfig(
        val version: Int,
        val servers: Array<ServerConfig>,
    )

    data class ServerConfig(
        val id: String,
        val server: String,
        val server_port: Int,
        val password: String,
        val method: String
    )
}

/** A built-in SocketFactory to make it easy for developers to plug this into existing HTTP libraries, such as OkHTTP **/
class ShadowSocketFactory(
    private val shadowConfig: ShadowConfig,
    private val shadowHost: String,
    private val shadowPort: Int
) : SocketFactory() {
    companion object {
        fun factoryFromUrl(url: URL, uuid: UUID): ShadowSocketFactory {
            require (url.protocol == "https") {
                "protocol must be https"
            }

            val jsonText = url.readText()
            val gson = Gson()
            val jsonConfig = gson.fromJson(jsonText, JsonConfig.ShadowJsonConfig::class.java)
            val serverConfig = jsonConfig.servers.first { UUID.fromString(it.id) == uuid }

            val shadowConfig = ShadowConfig(serverConfig.password, serverConfig.method)
            val host = serverConfig.server
            val port = serverConfig.server_port

            return ShadowSocketFactory(shadowConfig, host, port)
        }
    }

    //@ExperimentalUnsignedTypes
    override fun createSocket(remoteHost: String?, remotePort: Int): Socket {
        return ShadowSocket(shadowConfig, shadowHost, shadowPort)
    }

    //@ExperimentalUnsignedTypes
    override fun createSocket(p0: String?, p1: Int, p2: InetAddress?, p3: Int): Socket {
        return ShadowSocket(shadowConfig, shadowHost, shadowPort)

    }

    //@ExperimentalUnsignedTypes
    override fun createSocket(p0: InetAddress?, p1: Int): Socket {
        return ShadowSocket(shadowConfig, shadowHost, shadowPort)

    }

    //@ExperimentalUnsignedTypes
    override fun createSocket(p0: InetAddress?, p1: Int, p2: InetAddress?, p3: Int): Socket {
        return ShadowSocket(shadowConfig, shadowHost, shadowPort)

    }

    //@ExperimentalUnsignedTypes
    override fun createSocket(): Socket {
        return ShadowSocket(shadowConfig, shadowHost, shadowPort)
    }
}