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

package org.operatorfoundation.shadow

import android.util.Log
import org.operatorfoundation.shadow.ShadowCipher.Companion.handshakeSize
import org.operatorfoundation.transmission.Connection
import org.operatorfoundation.transmission.ConnectionType
import org.operatorfoundation.transmission.Transmission
import org.operatorfoundation.transmission.TransmissionConnection
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.logging.Level
import java.util.logging.Logger


// This class implements client sockets (also called just "sockets").
// A socket is an endpoint for communication between two machines.
open class ShadowConnection(val config: ShadowConfig) : Connection
{
    companion object {
        private val bloom = Bloom()

        fun saveBloom(fileName: String) {
            bloom.save(fileName)
        }

        fun loadBloom(fileName: String) {
            bloom.load(fileName)
        }
    }

    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream
    private var handshakeBytes: ByteArray = byteArrayOf()
    private lateinit var encryptionCipher: ShadowCipher
    private var decryptionCipher: ShadowCipher? = null
    private var connectionStatus: Boolean = false
    private var darkStar: DarkStar? = null
    private var host: String? = null
    private var port: Int? = null
    private lateinit var connection: Connection
    private var logger: Logger? = null
    private val rwl = ReentrantReadWriteLock()
    private val r: Lock = rwl.readLock()
    private val w: Lock = rwl.writeLock()

    constructor(connection: Connection, config: ShadowConfig, logger: Logger?): this(config) {
        this.connection = connection
        this.logger = logger
        val host = config.serverIP
        val port = config.port

        this.host = host
        this.port = port
        try
        {
            this.darkStar = DarkStar(config, host, port)
            val darkStarInstance = this.darkStar ?: throw Exception("failed to initialize DarkStar")
            println("Creating handshake bytes")
            this.handshakeBytes = darkStarInstance.createHandshake()
            println("Attempting handshake with handshake size: ${handshakeBytes.count()}")
            handshake()
            connectionStatus = true
            println("handshake was successful")
            this.inputStream = getInputStream()
            this.outputStream = getOutputStream()
        }
        catch (handshakeError: Exception)
        {
            Log.e("ShadowSocket.init", "ShadowConnection constructor: Handshake failed.")
            println(handshakeError.message)
            connectionStatus = false

            throw handshakeError
        }

    }

    // Creates a stream socket and connects it to the specified port number on the named host.
    //@ExperimentalUnsignedTypes
    constructor(config: ShadowConfig, logger: Logger?) : this(config)
    {
        val host = config.serverIP
        val port = config.port

        this.host = host
        this.port = port

        this.connection = TransmissionConnection(host, port, ConnectionType.TCP, logger)
        this.logger = logger
        try
        {
            this.darkStar = DarkStar(config, host, port)
            val darkStarInstance = this.darkStar ?: throw Exception("failed to initialize DarkStar")
            this.handshakeBytes = darkStarInstance.createHandshake()
            handshake()
            connectionStatus = true
            println("handshake was successful")
            this.inputStream = getInputStream()
            this.outputStream = getOutputStream()
        }
        catch (handshakeError: Exception)
        {
            Log.e("ShadowSocket.init", "ShadowConnection constructor with logger: Handshake failed.")
            println(handshakeError.message)
            connectionStatus = false

            throw handshakeError
        }
    }

    // Public functions:
    override fun close() {
        Log.i("ShadowConnection", "Connection closed.")
        this.connection.close()
    }

    override fun read(size: Int): ByteArray? {
        r.lock();
        try
        {
            val readBuffer = ByteArray(size)
            var totalBytesRead = 0
            while (totalBytesRead < size) {
                val bytesRead = this.inputStream.read(readBuffer, totalBytesRead, size - totalBytesRead)
                totalBytesRead += bytesRead
            }

            return readBuffer
        }
        finally { r.unlock(); }
    }

    // like read, but doesn't mind a short read
    override fun readMaxSize(maxSize: Int): ByteArray? {
        r.lock();
        try
        {
            val readBuffer = ByteArray(maxSize)
            val bytesRead = this.inputStream.read(readBuffer)
            return readBuffer.sliceArray(0 until bytesRead)
        }
        finally { r.unlock(); }
    }

    // determines length by first reading the length prefix
    override fun readWithLengthPrefix(prefixSizeInBits: Int): ByteArray? {
        r.lock();
        try
        {
            return byteArrayOf(0)
//            return Transmission.readWithLengthPrefix(this, prefixSizeInBits, null)
        }
        finally { r.unlock(); }
    }

    override fun unsafeRead(size: Int): ByteArray? {
        val readBuffer = ByteArray(size)
        var totalBytesRead = 0
        while (totalBytesRead < size) {
            val bytesRead = this.inputStream.read(readBuffer, totalBytesRead, size - totalBytesRead)
            totalBytesRead += bytesRead
        }

        return readBuffer
    }

    override fun write(data: ByteArray): Boolean {
        w.lock();
        try
        {
            return try {
                this.outputStream.write(data)
                true
            } catch(error: Exception) {
                false
            }
        }
        finally { w.unlock(); }
    }

    override fun write(string: String): Boolean {
        return try {
            val bytes = string.toByteArray()
            write(bytes)
            true
        } catch(error: Exception) {
            false
        }
    }

    override fun writeWithLengthPrefix(data: ByteArray, prefixSizeInBits: Int): Boolean
    {
//        return Transmission.writeWithLengthPrefix(this, data, prefixSizeInBits, null)

        val messageSize = data.size
        val messageSizeBytes: ByteBuffer

        when (prefixSizeInBits) {
            8 -> {
                messageSizeBytes = ByteBuffer.allocate(1)
                messageSizeBytes.put(messageSize.toByte())
            }
            16 -> {
                messageSizeBytes = ByteBuffer.allocate(2)
                messageSizeBytes.putShort(messageSize.toShort())
            }
            32 -> {
                messageSizeBytes = ByteBuffer.allocate(4)
                messageSizeBytes.putInt(messageSize)
            }
            64 -> {
                messageSizeBytes = ByteBuffer.allocate(8)
                messageSizeBytes.putLong(messageSize.toLong())
            }
            else ->
            {
                logger?.log(Level.SEVERE, "Transmission: Unable to complete a write request, the size in bits of the requested length prefix is invalid. Requested size in bits: $prefixSizeInBits")
                return false
            }
        }

        val atomicData = messageSizeBytes.array() + data

        return this.write(atomicData)
    }

    // Private functions:
    // Returns an input stream and the decryption cipher for this socket.
    fun getInputStream(): InputStream {
        val connectionInputStream = ConnectionInputStream(this.connection)
        val cipher = decryptionCipher
        cipher?.let {
            return ShadowConnectionInputStream(this.connection, cipher)
        }
        Log.e("getInputStream", "Decryption cipher was not created.")
        throw IOException()
    }

    // Returns an output stream and the encryption cipher for this socket.
    fun getOutputStream(): OutputStream {
        val connectionOutputStream = ConnectionOutputStream(this.connection)
        return ShadowConnectionOutputStream(this.connection, encryptionCipher)
    }

    // Exchanges the salt.
    //@ExperimentalUnsignedTypes
    private fun handshake() {
        sendHandshake()
        receiveHandshake()
    }

    // Sends the handshake bytes through the output stream.
    private fun sendHandshake() {
        val connectionOutputStream = ConnectionOutputStream(this.connection)
        connectionOutputStream.write(handshakeBytes)
    }

    // Receives the salt through the input stream.
    private fun receiveHandshake()
    {
        val handshakeSize = handshakeSize
//        val connectionInputStream = ConnectionInputStream(this.connection)
//        val result = connectionInputStream.readNBytes(handshakeSize)

        val result = this.connection.read(handshakeSize)

        if (result != null && result.size == handshakeBytes.size)
        {
            if (bloom.checkInBloom(result))
            {
                Log.e("receiveHandshake", "duplicate handshake found.")
                throw IOException()
            }

            val darkStarInstance = this.darkStar ?: throw Exception("failed to initialize DarkStar")
            decryptionCipher = darkStarInstance.makeCipher(false, result)
            encryptionCipher = darkStarInstance.makeCipher(true, result)
        }
        else
        {
            Log.e("receiveHandshake", "Handshake was not received.")
            throw IOException()
        }
    }

    // FIXME: Come back to this eventually
//    fun connect(endpoint: SocketAddress?)
//    {
//        socket.connect(endpoint)
//
//        if (connectionStatus)
//        {
//            Log.e("ShadowSocket.connect", "Already connected.")
//            throw IOException()
//        }
//        else
//        {
//            handshake()
//            connectionStatus = true
//            Log.i("ShadowSocket.connect", "Connection succeeded.")
//        }
//    }
//
//    /// This should be called only if decryption has failed
//    fun redial()
//    {
//        if (decryptFailed)
//        {
//            close()
//        }
//        else
//        {
//            decryptFailed = true
//            close()
//
//            if (host != null && port != null)
//            {
//                val socketAddress = InetSocketAddress(host, port)
//                socket = Socket(host, port)
//                connect(socketAddress)
//            }
//            else
//            {
//                Log.e("ShadowSocket.redial", "host and port not found")
//                throw IOException()
//            }
//        }
//    }
}
