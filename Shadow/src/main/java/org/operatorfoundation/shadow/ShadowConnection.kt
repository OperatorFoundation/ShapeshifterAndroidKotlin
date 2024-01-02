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
import org.operatorfoundation.transmission.BaseConnection
import org.operatorfoundation.transmission.Connection
import org.operatorfoundation.transmission.ConnectionType
import org.operatorfoundation.transmission.TransmissionConnection
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.logging.Level
import java.util.logging.Logger

class ShadowConnection(config: ShadowConfig, logger: Logger? = null, connection: Connection? = null) : BaseConnection(logger)
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

    private var inputStream: InputStream
    private var outputStream: OutputStream
    private var handshakeBytes: ByteArray = byteArrayOf()
    private lateinit var encryptionCipher: ShadowCipher
    private var decryptionCipher: ShadowCipher? = null
    private var connectionStatus: Boolean = false
    private var darkStar: DarkStar? = null
    private var host: String? = null
    private var port: Int? = null
    private var connection: Connection

    init
    {
        this.host = config.serverIP
        this.port = config.port
        this.logger = logger

        if (connection == null)
        {
            this.connection = TransmissionConnection(config.serverIP, config.port, ConnectionType.TCP, logger)
        }
        else
        {
            this.connection = connection
        }

        try
        {
            this.darkStar = DarkStar(config, config.serverIP, config.port)
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

    override fun networkRead(size: Int): ByteArray?
    {
        logger?.log(Level.FINE, "Network Read: (size: $size)")
        val networkBuffer = ByteArray(size)
        var bytesRead = 0

        while (bytesRead < size)
        {
            try
            {
                val readResult = inputStream.read(networkBuffer, bytesRead, size - bytesRead)

                if (readResult > 0)
                {
                    bytesRead += readResult
                }
                else
                {
                    close()
                    return null
                }
            }
            catch (readError: java.lang.Exception)
            {
                logger?.log(Level.WARNING, "ShadowConnection: Connection inputStream encountered an error while trying to read a specific size: $readError")
                readError.printStackTrace()
                close()
                return null
            }
        }

        return networkBuffer
    }

    override fun networkWrite(data: ByteArray): Boolean
    {
        try
        {
            outputStream.write(data)
            outputStream.flush()
            return true
        }
        catch (writeError: java.lang.Exception)
        {
            logger?.log(Level.SEVERE, "ShadowConnection: Error while attempting to write data to the network: $writeError")
            close()
            return false
        }
    }

    // Returns an input stream and the decryption cipher for this socket.
    fun getInputStream(): InputStream
    {
        val cipher = decryptionCipher
        cipher?.let {
            return ShadowConnectionInputStream(this.connection, cipher)
        }
        Log.e("getInputStream", "Decryption cipher was not created.")
        throw IOException()
    }

    // Returns an output stream and the encryption cipher for this socket.
    fun getOutputStream(): OutputStream
    {
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

    override fun networkClose() {
        Log.i("ShadowConnection", "Connection closed.")
        this.connection.close()
    }
}
