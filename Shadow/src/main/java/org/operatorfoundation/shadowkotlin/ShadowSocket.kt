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

import android.util.Log
import org.operatorfoundation.shadowkotlin.ShadowCipher.Companion.handshakeSize
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.*
import java.nio.channels.SocketChannel

// This class implements client sockets (also called just "sockets").
// A socket is an endpoint for communication between two machines.
open class ShadowSocket(val config: ShadowConfig) : Socket() {
    companion object {
        private val bloom = Bloom()

        fun saveBloom(fileName: String) {
           bloom.save(fileName)
        }

        fun loadBloom(fileName: String) {
            bloom.load(fileName)
        }
    }

    // Fields:
    private var socket: Socket = Socket()
    private lateinit var handshakeBytes: ByteArray
    private lateinit var encryptionCipher: ShadowCipher
    private var decryptionCipher: ShadowCipher? = null
    private var connectionStatus: Boolean = false
    private var darkStar: DarkStar? = null
    private var host: String? = null
    private var port: Int? = null
    private var decryptFailed: Boolean = false

    // Constructors:

    // Creates a stream socket and connects it to the specified port number on the named host.
    //@ExperimentalUnsignedTypes
    constructor(config: ShadowConfig, host: String, port: Int) : this(config)
    {
        val socketAddress = InetSocketAddress(host, port)
        this.host = host
        this.port = port
        this.socket = Socket()
        this.socket.connect(socketAddress)

        try
        {
            this.darkStar = DarkStar(config, host, port)
            this.handshakeBytes = darkStar!!.createHandshake()
            handshake()
            connectionStatus = true
            println("handshake was successful")
        }
        catch (handshakeError: Exception)
        {
            Log.e("ShadowSocket.init", "Handshake failed.")
            println(handshakeError.message)
            connectionStatus = false

            throw handshakeError
        }
    }

    // Creates a socket and connects it to the specified remote host on the specified remote port.
    @ExperimentalUnsignedTypes
    constructor(config: ShadowConfig, host: String, port: Int, localAddr: InetAddress, localPort: Int) : this(config)
    {
        socket = Socket(host, port, localAddr, localPort)

        try
        {
            handshake()
            connectionStatus = true
        }
        catch(error: Exception)
        {
            Log.e("ShadowSocket.init", "Handshake failed.")
            socket.close()
            connectionStatus = false
            throw error
        }
    }

    // Creates a stream socket and connects it to the specified port number at the specified IP address.
    @ExperimentalUnsignedTypes
    constructor(config: ShadowConfig, address: InetAddress, port: Int) : this(config)
    {
        socket = Socket(address, port)

        try
        {
            handshake()
            connectionStatus = true
        }
        catch(error: IOException)
        {
            Log.e("ShadowSocket.init", "Handshake failed, closing connection.")
            socket.close()
            connectionStatus = false
            throw error
        }
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
    )
    {
        socket = Socket(address, port, localAddr, localPort)

        try
        {
            handshake()
            connectionStatus = true
        }
        catch(error: Exception)
        {
            Log.e("ShadowSocket.init", "Handshake failed, closing connection.")
            socket.close()
            connectionStatus = false
            throw error
        }
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
        Log.i("ShadowSocket", "Socket closed.")
        socket.close()
    }

    // Connects this socket to the server and initiates the handshake.
    //@ExperimentalUnsignedTypes
    override fun connect(endpoint: SocketAddress?)
    {
        socket.connect(endpoint)

        if (connectionStatus)
        {
            Log.e("ShadowSocket.connect", "Already connected.")
            throw IOException()
        }
        else
        {
            handshake()
            connectionStatus = true
            Log.i("ShadowSocket.connect", "Connection succeeded.")
        }
    }

    // Connects this socket to the server with a specified timeout value and initiates the handshake.
    //@ExperimentalUnsignedTypes
    override fun connect(endpoint: SocketAddress?, timeout: Int)
    {
        socket.connect(endpoint, timeout)

        if (connectionStatus)
        {
            Log.e("ShadowSocket.connect", "Already connected.")
            throw IOException()
        }
        else
        {
            handshake()
            connectionStatus = true
            Log.i("ShadowSocket.connect", "Connection succeeded.")
        }
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
    //@ExperimentalUnsignedTypes
    private fun handshake() {
        sendHandshake()
        print("cryptographic handshake sent")
        receiveHandshake()
        print("cryptographic handshake received")
    }

    // Sends the handshake bytes through the output stream.
    private fun sendHandshake() {
        socket.outputStream.write(handshakeBytes)
    }

    // Receives the salt through the input stream.
    //@ExperimentalUnsignedTypes
    private fun receiveHandshake()
    {
        val handshakeSize = handshakeSize
        val result = readNBytes(socket.inputStream, handshakeSize)

        if (result != null && result.size == handshakeBytes.size)
        {
            if (bloom.checkInBloom(result))
            {
                Log.e("receiveHandshake", "duplicate handshake found.")
                throw IOException()
            }

            decryptionCipher = darkStar!!.makeCipher(false, result)
            encryptionCipher = darkStar!!.makeCipher(true, result)
        }
        else
        {
            Log.e("receiveHandshake", "Handshake was not received.")
            throw IOException()
        }
    }

    /// This should be called only if decryption has failed
    fun redial()
    {
        if (decryptFailed)
        {
            close()
        }
        else
        {
            decryptFailed = true
            close()

            if (host != null && port != null)
            {
                val socketAddress = InetSocketAddress(host, port!!)
                socket = Socket(host, port!!)
                connect(socketAddress)
            }
            else
            {
                Log.e("ShadowSocket.redial", "host and port not found")
                throw IOException()
            }
        }
    }
}
