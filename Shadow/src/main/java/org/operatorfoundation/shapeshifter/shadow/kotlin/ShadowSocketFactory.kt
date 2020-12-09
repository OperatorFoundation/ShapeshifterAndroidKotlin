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

package org.operatorfoundation.shapeshifter.shadow.kotlin

import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory

/** A built-in SocketFactory to make it easy for developers to plug this into existing HTTP libraries, such as OkHTTP **/
class ShadowSocketFactory(private val shadowConfig: ShadowConfig, private val shadowHost : String, private val shadowPort : Int) : SocketFactory() {

    @ExperimentalUnsignedTypes
    override fun createSocket(remoteHost: String?, remotePort: Int): Socket {

        return ShadowSocket (shadowConfig, shadowHost, shadowPort)
    }

    @ExperimentalUnsignedTypes
    override fun createSocket(p0: String?, p1: Int, p2: InetAddress?, p3: Int): Socket {
        return ShadowSocket (shadowConfig, shadowHost, shadowPort)

    }

    @ExperimentalUnsignedTypes
    override fun createSocket(p0: InetAddress?, p1: Int): Socket {
        return ShadowSocket (shadowConfig, shadowHost, shadowPort)

    }

    @ExperimentalUnsignedTypes
    override fun createSocket(p0: InetAddress?, p1: Int, p2: InetAddress?, p3: Int): Socket {
        return ShadowSocket (shadowConfig, shadowHost, shadowPort)

    }

    @ExperimentalUnsignedTypes
    override fun createSocket(): Socket {
        return OKHTTPShadowSocket (shadowConfig, shadowHost, shadowPort)
    }
}