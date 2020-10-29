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

import java.net.*
import java.nio.channels.ServerSocketChannel

// This class implements server sockets. A server socket waits for requests to come in over the network.
// It performs some operation based on that request, and then possibly returns a result to the requester.
open class ShadowServerSocket(private val config: ShadowConfig) : ServerSocket() {
    // fields
    private var serverSocket: ServerSocket = ServerSocket()

    // Constructors:
    // Creates a server socket, bound to the specified port.
    constructor(config: ShadowConfig, port: Int) : this(config) {
        serverSocket = ServerSocket(port)
    }

    // Creates a server socket and binds it to the specified local port number, with the specified backlog.
    constructor(config: ShadowConfig, port: Int, backlog: Int) : this(config) {
        serverSocket = ServerSocket(port, backlog)
    }

    // Create a server with the specified port, listen backlog, and local IP address to bind to.
    constructor(config: ShadowConfig, port: Int, backlog: Int, bindAddr: InetAddress) : this(config) {
        serverSocket = ServerSocket(port, backlog, bindAddr)
    }

    // Public methods:
    // Listens for a connection to be made to this socket and accepts it.
    override fun accept(): Socket {
        return serverSocket.accept()
    }

    // Binds the ServerSocket to a specific address (IP address and port number).
    override fun bind(endpoint: SocketAddress?) {
        serverSocket.bind(endpoint)
    }

    // Binds the ServerSocket to a specific address (IP address and port number).
    override fun bind(endpoint: SocketAddress?, backlog: Int) {
        serverSocket.bind(endpoint, backlog)
    }

    // Closes this socket.
    override fun close() {
        serverSocket.close()
    }

    // Returns the unique ServerSocketChannel object associated with this socket, if any.
    override fun getChannel(): ServerSocketChannel {
        return serverSocket.channel
    }

    // Returns the local address of this server socket.
    override fun getInetAddress(): InetAddress {
        return serverSocket.inetAddress
    }

    // Returns the port number on which this socket is listening.
    override fun getLocalPort(): Int {
        return serverSocket.localPort
    }

    // Returns the address of the endpoint this socket is bound to.
    override fun getLocalSocketAddress(): SocketAddress {
        return serverSocket.localSocketAddress
    }

    // Gets the value of the SO_RCVBUF option for this ServerSocket, that is the proposed buffer size that will be used for Sockets accepted from this ServerSocket.
    override fun getReceiveBufferSize(): Int {
        return serverSocket.receiveBufferSize
    }

    // Tests if SO_REUSEADDR is enabled.
    override fun getReuseAddress(): Boolean {
        return serverSocket.reuseAddress
    }

    // Retrieve setting for SO_TIMEOUT. 0 returns implies that the option is disabled (i.e., timeout of infinity).
    override fun getSoTimeout(): Int {
        return serverSocket.soTimeout
    }

    // Returns the binding state of the ServerSocket.
    override fun isBound(): Boolean {
        return serverSocket.isBound
    }

    // Returns the closed state of the ServerSocket.
    override fun isClosed(): Boolean {
        return serverSocket.isClosed
    }

    // Sets performance preferences for this ServerSocket.
    override fun setPerformancePreferences(connectionTime: Int, latency: Int, bandwidth: Int) {
    }

    // Sets a default proposed value for the SO_RCVBUF option for sockets accepted from this ServerSocket.
    override fun setReceiveBufferSize(size: Int) {
        serverSocket.receiveBufferSize = size
    }

    // Enable/disable the SO_REUSEADDR socket option.
    override fun setReuseAddress(on: Boolean) {
    }

    // Enable/disable SO_TIMEOUT with the specified timeout, in milliseconds.
    override fun setSoTimeout(timeout: Int) {
    }

    // Returns the implementation address and implementation port of this socket as a String.
    override fun toString(): String {
        return "ShadowServerSocket[" + "password = " + config.password + ", cipherName = " + config.cipherName + "]"
    }
}
