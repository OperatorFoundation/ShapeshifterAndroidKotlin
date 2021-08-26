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

import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.*
import java.nio.channels.SocketChannel

// This class implements client sockets (also called just "sockets").
// A socket is an endpoint for communication between two machines.
open class ShadowSocket(val config: ShadowConfig) : Socket() {
    // Fields:
    private var socket: Socket = Socket()
    private var encryptionCipher: ShadowCipher
    private var decryptionCipher: ShadowCipher? = null
    private var connectionStatus: Boolean = false

    init {
        // Create salt for encryptionCipher.
        val salt = ShadowCipher.createSalt(config)
        // Create an encryptionCipher.
        encryptionCipher = ShadowCipher.makeShadowCipherWithSalt(config, salt)
        Log.i("init", "Encryption cipher created.")
    }

    // Constructors:
    // Creates a stream socket and connects it to the specified port number on the named host.
    @ExperimentalUnsignedTypes
    constructor(config: ShadowConfig, host: String, port: Int) : this(config) {
        socket = Socket(host, port)
        connectionStatus = true
        handshake()
    }

    // Creates a socket and connects it to the specified remote host on the specified remote port.
    @ExperimentalUnsignedTypes
    constructor(
        config: ShadowConfig, host: String, port: Int, localAddr: InetAddress, localPort: Int
    ) : this(config) {
        socket = Socket(host, port, localAddr, localPort)
        connectionStatus = true
        handshake()
    }

    // Creates a stream socket and connects it to the specified port number at the specified IP address.
    @ExperimentalUnsignedTypes
    constructor(config: ShadowConfig, address: InetAddress, port: Int) : this(config) {
        socket = Socket(address, port)
        connectionStatus = true
        handshake()
    }

    // Creates a socket and connects it to the specified remote address on the specified remote port.
    @ExperimentalUnsignedTypes
    constructor(
        config: ShadowConfig,
        address: InetAddress,
        port: Int,
        localAddr: InetAddress,
        localPort: Int
    ) : this(
        config
    ) {
        socket = Socket(address, port, localAddr, localPort)
        connectionStatus = true
        handshake()
    }

    // Creates an unconnected socket, specifying the type of proxy, if any, that should be used regardless of any other settings.
    @ExperimentalUnsignedTypes
    constructor(config: ShadowConfig, proxy: Proxy) : this(config) {
        socket = Socket(proxy)
    }

    // Public functions:
    // Binds the socket to a local address.
    override fun bind(bindpoint: SocketAddress?) {
        socket.bind(bindpoint)
    }

    // Closes this socket.
    override fun close() {
        Log.i("close", "Socket closed.")
        socket.close()
    }

    // Connects this socket to the server and initiates the handshake.
    @ExperimentalUnsignedTypes
    override fun connect(endpoint: SocketAddress?) {
        socket.connect(endpoint)
        if (connectionStatus) {
            Log.e("connect", "Already connected.")
            throw IOException()
        }
        connectionStatus = true
        handshake()
        Log.i("connect", "Connect succeeded.")
    }

    // Connects this socket to the server with a specified timeout value and initiates the handshake.
    @ExperimentalUnsignedTypes
    override fun connect(endpoint: SocketAddress?, timeout: Int) {
        socket.connect(endpoint, timeout)
        if (connectionStatus) {
            Log.e("connect", "Already connected.")
            throw IOException()
        }
        handshake()
        Log.i("connect", "Connect succeeded. Timeout is $timeout.")
    }

    // Returns the unique SocketChannel object associated with this socket, if any.
    override fun getChannel(): SocketChannel {
        return socket.channel
    }

    // Returns the address to which the socket is connected.
    override fun getInetAddress(): InetAddress {
        return socket.inetAddress
    }

    // Returns an input stream and the decryption cipher for this socket.
    override fun getInputStream(): InputStream {
        val cipher = decryptionCipher
        cipher?.let {
            Log.i("getInputStream", "Decryption cipher created.")
            return ShadowInputStream(socket.inputStream, cipher, this)
        }
        Log.e("getInputStream", "Decryption cipher was not created.")
        throw IOException()
    }

    // Tests if SO_KEEPALIVE is enabled.
    override fun getKeepAlive(): Boolean {
        return false
    }

    // Gets the local address to which the socket is bound.
    override fun getLocalAddress(): InetAddress {
        return socket.localAddress
    }

    // Returns the local port number to which this socket is bound.
    override fun getLocalPort(): Int {
        return socket.localPort
    }

    // Returns the address of the endpoint this socket is bound to.
    override fun getLocalSocketAddress(): SocketAddress {
        return socket.localSocketAddress
    }

    // Tests if SO_OOBINLINE is enabled.
    override fun getOOBInline(): Boolean {
        return false
    }

    // Returns an output stream and the encryption cipher for this socket.
    override fun getOutputStream(): OutputStream {
        return ShadowOutputStream(socket.outputStream, encryptionCipher)
    }

    // Returns the remote port number to which this socket is connected.
    override fun getPort(): Int {
        return socket.port
    }

    // Gets the value of the SO_RCVBUF option for this Socket, that is the buffer size used by the platform for input on this Socket.
    override fun getReceiveBufferSize(): Int {
        return socket.receiveBufferSize
    }

    // Returns the address of the endpoint this socket is connected to, or null if it is unconnected.
    override fun getRemoteSocketAddress(): SocketAddress {
        return socket.remoteSocketAddress
    }

    // Tests if SO_REUSEADDR is enabled.
    override fun getReuseAddress(): Boolean {
        return false
    }

    // Get value of the SO_SNDBUF option for this Socket, that is the buffer size used by the platform for output on this Socket.
    override fun getSendBufferSize(): Int {
        return socket.sendBufferSize
    }

    // Returns setting for SO_LINGER. -1 implies that the option is disabled.
    override fun getSoLinger(): Int {
        return -1
    }

    // Returns setting for SO_TIMEOUT. 0 returns implies that the option is disabled (i.e., timeout of infinity).
    override fun getSoTimeout(): Int {
        return 0
    }

    // Tests if TCP_NODELAY is enabled.
    override fun getTcpNoDelay(): Boolean {
        return false
    }

    // Gets traffic class or type-of-service in the IP header for packets sent from this Socket.
    override fun getTrafficClass(): Int {
        throw SocketException()
    }

    // Returns the binding state of the socket.
    override fun isBound(): Boolean {
        return socket.isBound
    }

    // Returns the closed state of the socket.
    override fun isClosed(): Boolean {
        return socket.isClosed
    }

    // Returns the connection state of the socket.
    override fun isConnected(): Boolean {
        return socket.isConnected
    }

    // Returns whether the read-half of the socket connection is closed.
    override fun isInputShutdown(): Boolean {
        return socket.isInputShutdown
    }

    // Returns whether the write-half of the socket connection is closed.
    override fun isOutputShutdown(): Boolean {
        return socket.isOutputShutdown
    }

    // Send one byte of urgent data on the socket.
    override fun sendUrgentData(data: Int) {
    }

    // Enable/disable SO_KEEPALIVE.
    override fun setKeepAlive(on: Boolean) {
    }

    // Enable/disable SO_OOBINLINE (receipt of TCP urgent data) By default, this option is disabled and TCP urgent data received on a socket is silently discarded.
    override fun setOOBInline(on: Boolean) {
    }

    // Sets performance preferences for this socket.
    override fun setPerformancePreferences(connectionTime: Int, latency: Int, bandwidth: Int) {
    }

    // Sets the SO_RCVBUF option to the specified value for this Socket.
    override fun setReceiveBufferSize(size: Int) {
        socket.receiveBufferSize = size
    }

    // Enable/disable the SO_REUSEADDR socket option.
    override fun setReuseAddress(on: Boolean) {
    }

    // Sets the SO_SNDBUF option to the specified value for this Socket.
    override fun setSendBufferSize(size: Int) {
        socket.sendBufferSize = size
    }

    // Enable/disable SO_LINGER with the specified linger time in seconds.
    override fun setSoLinger(on: Boolean, linger: Int) {
    }

    // Enable/disable SO_TIMEOUT with the specified timeout, in milliseconds.
    override fun setSoTimeout(timeout: Int) {
    }

    // Enable/disable TCP_NODELAY (disable/enable Nagle's algorithm).
    override fun setTcpNoDelay(on: Boolean) {
    }

    // Sets traffic class or type-of-service octet in the IP header for packets sent from this Socket.
    override fun setTrafficClass(tc: Int) {
        throw SocketException()
    }

    // Places the input stream for this socket at "end of stream".
    override fun shutdownInput() {
        socket.shutdownInput()
    }

    // Disables the output stream for this socket.
    override fun shutdownOutput() {
        socket.shutdownOutput()
    }

    // Private functions:
    // Exchanges the salt.
    @ExperimentalUnsignedTypes
    private fun handshake() {
        sendSalt()
        receiveSalt()
        Log.i("handshake", "handshake completed")
    }

    // Sends the salt through the output stream.
    private fun sendSalt() {
        socket.outputStream.write(encryptionCipher.salt)
        Log.i("sendSalt", "Salt sent.")
    }

    // Receives the salt through the input stream.
    @ExperimentalUnsignedTypes
    private fun receiveSalt() {
        val saltSize = ShadowCipher.determineSaltSize(encryptionCipher.config)
        val result = readNBytes(socket.inputStream, saltSize)
        if (result != null && result.size == encryptionCipher.salt.size) {
            decryptionCipher = ShadowCipher.makeShadowCipherWithSalt(config, result)
            Log.i("receiveSalt", "Salt received.")
        } else {
            Log.e("receiveSalt", "Salt was not received.")
            throw IOException()
        }
    }
}
