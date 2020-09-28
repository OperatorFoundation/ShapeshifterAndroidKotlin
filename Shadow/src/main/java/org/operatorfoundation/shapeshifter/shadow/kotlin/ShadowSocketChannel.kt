package org.operatorfoundation.shapeshifter.shadow.kotlin

import java.io.IOException
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketOption
import java.nio.ByteBuffer
import java.nio.ByteBuffer.wrap
import java.nio.channels.SocketChannel
import java.nio.channels.spi.SelectorProvider

class ShadowSocketChannel(private val selectorProvider: SelectorProvider, val config: ShadowConfig) :
    SocketChannel(selectorProvider) {

    private var connectionStatus: Boolean = false
    private var socketChannel: SocketChannel? = null
    private var encryptionCipher: ShadowCipher
    private var decryptionCipher: ShadowCipher? = null

    init {
        // Create salt for encryptionCipher
        val salt = ShadowCipher.createSalt(config)
        // Create an encryptionCipher
        encryptionCipher = ShadowCipher(config, salt)
    }

    override fun implCloseSelectableChannel() {
        TODO("Not yet implemented")
    }

    override fun implConfigureBlocking(block: Boolean) {
        TODO("Not yet implemented")
    }

    override fun read(dst: ByteBuffer?): Int {
        TODO("Not yet implemented")
    }

    override fun read(dsts: Array<out ByteBuffer>?, offset: Int, length: Int): Long {
        TODO("Not yet implemented")
    }

    override fun write(src: ByteBuffer?): Int {
        TODO("Not yet implemented")
    }

    override fun write(srcs: Array<out ByteBuffer>?, offset: Int, length: Int): Long {
        TODO("Not yet implemented")
    }

    override fun bind(local: SocketAddress?): SocketChannel {
        TODO("Not yet implemented")
    }

    override fun getLocalAddress(): SocketAddress {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> setOption(name: SocketOption<T>?, value: T): SocketChannel {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> getOption(name: SocketOption<T>?): T {
        TODO("Not yet implemented")
    }

    override fun supportedOptions(): MutableSet<SocketOption<*>> {
        TODO("Not yet implemented")
    }

    override fun shutdownInput(): SocketChannel {
        TODO("Not yet implemented")
    }

    override fun shutdownOutput(): SocketChannel {
        TODO("Not yet implemented")
    }

    override fun socket(): Socket {
        TODO("Not yet implemented")
    }

    override fun isConnected(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isConnectionPending(): Boolean {
        TODO("Not yet implemented")
    }

    @ExperimentalUnsignedTypes
    override fun connect(remote: SocketAddress?): Boolean {
        socketChannel = open(remote)
        if (connectionStatus) {
            return false
        }
        connectionStatus = true
        handshake()
        return true
    }

    override fun finishConnect(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getRemoteAddress(): SocketAddress {
        TODO("Not yet implemented")
    }

    @ExperimentalUnsignedTypes
    private fun handshake() {
        sendSalt()
        receiveSalt()
    }

    // Sends the salt through the output stream
    private fun sendSalt() {
        val buffer = wrap(encryptionCipher.salt)
        socketChannel!!.write(buffer)
    }

    // Receives the salt through the input stream
    @ExperimentalUnsignedTypes
    private fun receiveSalt() {
        val channel = socketChannel!!
        val result = readNBytes(channel, ShadowCipher.saltSize)
        if (result.position() == encryptionCipher.salt.size) {
            decryptionCipher = ShadowCipher(config, result.array())
        } else {
            throw IOException()
        }
    }
}