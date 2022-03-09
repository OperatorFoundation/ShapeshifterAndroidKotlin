package org.operatorfoundation.shapeshifter

import org.junit.Assert.*
import org.junit.Test
import org.operatorfoundation.shapeshifter.shadow.kotlin.ShadowConfig
import org.operatorfoundation.shapeshifter.shadow.kotlin.ShadowSocket
import org.operatorfoundation.shapeshifter.shadow.kotlin.ShadowSocketFactory
import org.operatorfoundation.shapeshifter.shadow.kotlin.readNBytes
import java.io.IOException
import java.net.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.spec.InvalidKeySpecException
import java.util.*
import kotlin.concurrent.thread

internal class ShadowSocketTest {

    @ExperimentalUnsignedTypes
    fun runTestServer() {
        val testServer = ServerSocket(3333)
        val socket = testServer.accept()
        readNBytes(socket.inputStream, 2)
        socket.outputStream.write("Yo".toByteArray())
    }

    @ExperimentalUnsignedTypes
    fun runTestServerVol2() {
        val testServer = ServerSocket(3333)
        val socket = testServer.accept()
        readNBytes(socket.inputStream, 2)
        socket.outputStream.write("Yeah!".toByteArray())
    }

    @ExperimentalUnsignedTypes
    fun runJsonTestServer() {
        val testServer = ServerSocket(1234)
        val socket = testServer.accept()
        readNBytes(socket.inputStream, 2)
        socket.outputStream.write("Yo".toByteArray())
    }

    //Bad Arguments Tests

    @ExperimentalUnsignedTypes
    @Test(expected = IllegalStateException::class)
    fun checkBadCipher() {
        val password = "1234"
        val config = ShadowConfig(password, "cipherNoCiphing")
        ShadowSocket(config, "127.0.0.1", 2222)
    }

    @ExperimentalUnsignedTypes
    @Test(expected = IllegalStateException::class)
    fun badKeySize() {
        val password = "1234"
        val config = ShadowConfig(password, "DarkStar")
        ShadowSocket(config, "127.0.0.1", 2222)
    }

    @Test
    @Throws(
        IOException::class,
        NoSuchAlgorithmException::class,
        InvalidKeySpecException::class,
        NoSuchProviderException::class
    )

    fun shadowDarkStarClientTest() {
        // generate public key on swift for SPPK
        val config = ShadowConfig(
            "d089c225ef8cda8d477a586f062b31a756270124d94944e458edf1a9e1e41ed6",
            "DarkStar"
        )
        val shadowSocket = ShadowSocket(config, "127.0.0.1", 1234)
        assertNotNull(shadowSocket)
        val httpRequest = "GET / HTTP/1.0\r\n\r\n"
        val textBytes = httpRequest.toByteArray()
        shadowSocket.outputStream.write(textBytes)
        shadowSocket.outputStream.flush()
        var buffer = ByteArray(5)
        System.out.println("bytes available: " + shadowSocket.inputStream.available())
        shadowSocket.inputStream.read(buffer)
    }

    @Test
    @ExperimentalUnsignedTypes
    fun sipTest() {
        thread {
            runJsonTestServer()
        }
        val url = URL("https://raw.githubusercontent.com/OperatorFoundation/ShadowSwift/main/Tests/ShadowSwiftTests/testsip008.json")
        val uuid = UUID.fromString("27b8a625-4f4b-4428-9f0f-8a2317db7c79")
        val factory = ShadowSocketFactory.factoryFromUrl(url, uuid)
        factory.createSocket()
    }

    @Test
    @ExperimentalUnsignedTypes
    fun maxLongTest() {
        // var buffer: ByteBuffer = ByteBuffer.allocate(8)
        println(ULong.MAX_VALUE)
        println(ULong.MAX_VALUE.toLong())
        println((-1).toULong())
    }
}

