//package org.operatorfoundation.shapeshifter.shadow.kotlin
//
//import java.io.IOException
//import java.lang.UnsupportedOperationException
//import java.net.Socket
//import java.net.SocketAddress
//import java.net.SocketOption
//import java.nio.ByteBuffer
//import java.nio.ByteBuffer.wrap
//import java.nio.channels.SocketChannel
//import java.nio.channels.spi.SelectorProvider
//
//class ShadowSocketChannel(selectorProvider: SelectorProvider, val config: ShadowConfig) :
//    SocketChannel(selectorProvider) {
//
//    private var connectionStatus: Boolean = false
//    private var socketChannel: SocketChannel = open()
//    private var encryptionCipher: ShadowCipher
//    private var decryptionCipher: ShadowCipher? = null
//
//    init {
//        // Create salt for encryptionCipher
//        val salt = ShadowCipher.createSalt(config)
//        // Create an encryptionCipher
//        encryptionCipher = ShadowCipher.makeShadowCipherWithSalt(config, salt)
//    }
//
//    override fun bind(local: SocketAddress): SocketChannel {
//        return socketChannel.bind(local)
//    }
//
//    @ExperimentalUnsignedTypes
//    override fun connect(remote: SocketAddress?): Boolean {
//        socketChannel = open(remote)
//        if (connectionStatus) {
//            return false
//        }
//        connectionStatus = true
//        handshake()
//        return true
//    }
//
//    override fun finishConnect(): Boolean {
//        return connectionStatus
//    }
//
//    override fun getLocalAddress(): SocketAddress {
//        return socketChannel.localAddress
//    }
//
//    override fun <T : Any?> getOption(name: SocketOption<T>?): T {
//        throw UnsupportedOperationException()
//    }
//
//    override fun getRemoteAddress(): SocketAddress {
//        return socketChannel.remoteAddress
//    }
//
//    override fun implCloseSelectableChannel() {
//    }
//
//    override fun implConfigureBlocking(block: Boolean) {
//    }
//
//    override fun isConnected(): Boolean {
//        return connectionStatus
//    }
//
//    override fun isConnectionPending(): Boolean {
//        return finishConnect()
//    }
//
//    override fun read(dsts: Array<out ByteBuffer>?, offset: Int, length: Int): Long {
//        dsts?.get(offset)?.remaining()
//        dsts?.get(offset + 1)?.remaining()
//        val remaining = dsts?.get(offset + length - 1)?.remaining()
//        return remaining!!.toLong()
//    }
//
//    override fun read(dst: ByteBuffer?): Int {
//        var buffer: ByteArray = byteArrayOf()
//        val data = ByteArray(dst!!.capacity())
//        if (data.isEmpty()) {
//            return 0
//        }
//
//        // puts the bytes in a buffer.
//        if (data.size <= buffer.size) {
//            val resultSize = Integer.min(data.size, buffer.size)
//            buffer.copyInto(data, 0, 0, resultSize)
//            buffer.sliceArray(resultSize + 1 until buffer.size)
//
//            return resultSize
//        }
//
//        // get encrypted length
//        val lengthDataSize = ShadowCipher.lengthWithTagSize
//
//        // read bytes up to size of encrypted lengthSize into a byte buffer
//        val encryptedLengthData = readNBytes(socketChannel, lengthDataSize)
//
//        // decrypt encrypted length to find out payload length
//        val lengthData = decryptionCipher!!.decrypt(ByteArray(encryptedLengthData.capacity()))
//
//        // change lengthData from BigEndian representation to int length
//        val leftByte = lengthData[0]
//        val rightByte = lengthData[1]
//        val rightInt = rightByte.toInt()
//        val leftInt = leftByte.toInt()
//        val payloadLength = (leftInt * 256) + rightInt
//
//        // read and decrypt payload with the resulting length
//        val encryptedPayload = readNBytes(socketChannel, payloadLength + ShadowCipher.tagSize)
//        val payload = decryptionCipher!!.decrypt(ByteArray(encryptedPayload.capacity()))
//
//        // put payload into buffer
//        buffer += payload
//        val resultSize = Integer.min(data.size, buffer.size)
//        buffer.copyInto(data, 0, 0, resultSize)
//
//        // take bytes out of buffer
//        buffer.sliceArray(resultSize + 1 until buffer.size)
//
//        return resultSize
//    }
//
//    @ExperimentalUnsignedTypes
//    override fun write(src: ByteBuffer?): Int {
//        var buffer: ByteArray = byteArrayOf()
//        val data = ByteArray(src!!.capacity())
//        if (data.isEmpty()) {
//            return 0
//        }
//
//        // put into buffer
//        buffer += data
//
//        // keep writing until the buffer is empty in case user exceeds maximum
//        while (buffer.isNotEmpty()) {
//            val numBytesToSend = Integer.min(ShadowCipher.maxPayloadSize, buffer.size)
//
//            // make a copy of the buffer
//            val bytesToSend = buffer.copyOfRange(0, numBytesToSend)
//
//            // take bytes out of buffer
//            buffer = buffer.sliceArray(numBytesToSend until buffer.size)
//
//            val cipherText = encryptionCipher.pack(bytesToSend)
//
//            socketChannel.write(wrap(cipherText))
//        }
//        return src.remaining()
//    }
//
//    override fun write(srcs: Array<out ByteBuffer>?, offset: Int, length: Int): Long {
//        srcs?.get(offset)?.remaining()
//        srcs?.get(offset + 1)?.remaining()
//        val remaining = srcs?.get(offset + length - 1)?.remaining()
//        return remaining!!.toLong()
//    }
//
//    override fun <T : Any?> setOption(name: SocketOption<T>?, value: T): SocketChannel {
//        return socketChannel.setOption(name, value)
//    }
//
//    override fun shutdownInput(): SocketChannel {
//        return socketChannel.shutdownInput()
//    }
//
//    override fun shutdownOutput(): SocketChannel {
//        return socketChannel.shutdownOutput()
//    }
//
//    override fun socket(): Socket {
//        return socket()
//    }
//
//    override fun supportedOptions(): MutableSet<SocketOption<*>> {
//        return mutableSetOf()
//    }
//
//    @ExperimentalUnsignedTypes
//    private fun handshake() {
//        sendSalt()
//        receiveSalt()
//    }
//
//    // Sends the salt through the output stream
//    private fun sendSalt() {
//        val buffer = wrap(encryptionCipher.salt)
//        socketChannel.write(buffer)
//    }
//
//    // Receives the salt through the input stream
//    @ExperimentalUnsignedTypes
//    private fun receiveSalt() {
//        val channel = socketChannel
//        val result = readNBytes(socket.inputStream, saltSize)        if (result != null) {
//            if (result.position() == encryptionCipher.salt.size) {
//                decryptionCipher = ShadowCipher(config, result.array())
//            } else {
//                throw IOException()
//            }
//        }
//    }
//}