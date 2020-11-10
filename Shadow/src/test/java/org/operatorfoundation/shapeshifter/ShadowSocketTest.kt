package org.operatorfoundation.shapeshifter

import org.junit.Assert.*
import org.junit.Test
import org.operatorfoundation.shapeshifter.shadow.kotlin.ShadowConfig
import org.operatorfoundation.shapeshifter.shadow.kotlin.ShadowSocket
import org.operatorfoundation.shapeshifter.shadow.kotlin.readNBytes
import java.net.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

internal class ShadowSocketTest {

    @ExperimentalUnsignedTypes
    fun runTestServer() {
        val testServer = ServerSocket(3333)
        val socket = testServer.accept()
        readNBytes(socket.inputStream, 2)
        socket.outputStream.write("Yo".toByteArray())
    }

    //IPv4 Tests

    @Test
    fun nonceTest() {
        val counter = 1
        // nonce must be 12 bytes
        val buffer = ByteBuffer.allocate(12)
        // nonce is little Endian
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        // create a byte array from counter
        buffer.putLong(counter.toLong())
        buffer.put(0)
        buffer.put(0)
        buffer.put(0)
        buffer.put(0)

        println(buffer)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun shadowSocketInitTest() {
        val password = "1234"
        val config = ShadowConfig(password, "AES-128-GCM")
        val shadowSocket = ShadowSocket(config, "127.0.0.1", 2222)
        assertNotNull(shadowSocket)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun shadowSocketConstructorTest() {
        val password = "1234"
        val address: InetAddress = InetAddress.getByName(null)
        val config = ShadowConfig(password, "AES-128-GCM")
        val shadowSocket = ShadowSocket(config, "127.0.0.1", 2222, address, 0)
        assertNotEquals(shadowSocket, null)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun shadowSocketConstructorTest2() {
        val password = "1234"
        val address: InetAddress = InetAddress.getByName("127.0.0.1")
        val config = ShadowConfig(password, "AES-128-GCM")
        val shadowSocket = ShadowSocket(config, address, 2222)
        assertNotEquals(shadowSocket, null)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun shadowSocketConstructorTest3() {
        val password = "1234"
        val address: InetAddress = InetAddress.getByName("127.0.0.1")
        val localAddr: InetAddress = InetAddress.getByName(null)
        val config = ShadowConfig(password, "AES-128-GCM")
        val shadowSocket = ShadowSocket(config, address, 2222, localAddr, 0)
        assertNotEquals(shadowSocket, null)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun shadowSocketConstructorTest4() {
        val password = "1234"
        val socksAddress: SocketAddress = InetSocketAddress("127.0.0.1", 1443)
        val proxyType = Proxy.Type.SOCKS
        val socksProxy = Proxy(proxyType, socksAddress)
        val config = ShadowConfig(password, "AES-128-GCM")
        val shadowSocket = ShadowSocket(config, socksProxy)
        val socketAddress = InetSocketAddress("127.0.0.1", 2222)
        shadowSocket.connect(socketAddress)
        assertNotEquals(shadowSocket, null)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun shadowSocketWriteTest() {
        thread {
            runTestServer()
        }

        val password = "1234"
        val config = ShadowConfig(password, "AES-128-GCM")
        val shadowSocket = ShadowSocket(config, "127.0.0.1", 2222)
        assertNotEquals(shadowSocket, null)
        val plaintext = "Hi"
        val textBytes = plaintext.toByteArray()
        shadowSocket.outputStream.write(textBytes)
        shadowSocket.outputStream.flush()
    }

    @ExperimentalUnsignedTypes
    @Test
    fun shadowSocketReadTestAES128() {
        thread {
            runTestServer()
        }

        val password = "1234"
        val config = ShadowConfig(password, "AES-128-GCM")
        val shadowSocket = ShadowSocket(config, "127.0.0.1", 2222)
        assertNotNull(shadowSocket)
        val plaintext = "Hi"
        val textBytes = plaintext.toByteArray()
        shadowSocket.outputStream.write(textBytes)
        shadowSocket.outputStream.flush()
        val buffer = ByteArray(2)
        shadowSocket.inputStream.read(buffer)
       assertEquals(String(buffer), "Yo")
    }

    @ExperimentalUnsignedTypes
    @Test
    fun shadowSocketReadTestAES256() {
        thread {
            runTestServer()
        }

        val password = "1234"
        val config = ShadowConfig(password, "AES-256-GCM")
        val shadowSocket = ShadowSocket(config, "127.0.0.1", 2222)
        assertNotNull(shadowSocket)
        val plaintext = "Hi"
        val textBytes = plaintext.toByteArray()
        shadowSocket.outputStream.write(textBytes)
        shadowSocket.outputStream.flush()
        val buffer = ByteArray(2)
        shadowSocket.inputStream.read(buffer)
        assertEquals(String(buffer), "Yo")
    }

    @ExperimentalUnsignedTypes
    @Test
    fun shadowSocketReadTestCHACHA() {
        thread {
            runTestServer()
        }

        val password = "1234"
        val config = ShadowConfig(password, "CHACHA20-IETF-POLY1305")
        val shadowSocket = ShadowSocket(config, "127.0.0.1", 2222)
        assertNotNull(shadowSocket)
        val plaintext = "Hi"
        val textBytes = plaintext.toByteArray()
        shadowSocket.outputStream.write(textBytes)
        shadowSocket.outputStream.flush()
        val buffer = ByteArray(2)
        shadowSocket.inputStream.read(buffer)
        assertEquals(String(buffer), "Yo")
    }

    //IPv6 Tests

    @ExperimentalUnsignedTypes
    @Test
    fun shadowSocketIPv6InitTest() {
        val password = "1234"
        val config = ShadowConfig(password, "AES-128-GCM")
        val shadowSocket = ShadowSocket(config, "::1", 2222)
        assertNotEquals(shadowSocket, null)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun shadowSocketIPv6InitTest2() {
        val password = "1234"
        val address: InetAddress = InetAddress.getByName("::1")
        val config = ShadowConfig(password, "AES-128-GCM")
        val shadowSocket = ShadowSocket(config, address, 2222)
        assertNotEquals(shadowSocket, null)
    }

    @ExperimentalUnsignedTypes
    @Test
    fun shadowSocketIPv6WriteTest() {
        thread {
            runTestServer()
        }

        val password = "1234"
        val config = ShadowConfig(password, "AES-128-GCM")
        val shadowSocket = ShadowSocket(config, "::1", 2222)
        assertNotEquals(shadowSocket, null)
        val plaintext = "Hi"
        val textBytes = plaintext.toByteArray()
        shadowSocket.outputStream.write(textBytes)
        shadowSocket.outputStream.flush()
    }

    @ExperimentalUnsignedTypes
    @Test
    fun shadowSocketIPv6ReadTest() {
        thread {
            runTestServer()
        }

        val password = "1234"
        val config = ShadowConfig(password, "AES-128-GCM")
        val shadowSocket = ShadowSocket(config, "::1", 2222)
        assertNotNull(shadowSocket)
        val plaintext = "Hi"
        val textBytes = plaintext.toByteArray()
        shadowSocket.outputStream.write(textBytes)
        shadowSocket.outputStream.flush()
        val buffer = ByteArray(2)
        shadowSocket.inputStream.read(buffer)
        assertEquals(String(buffer), "Yo")
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
        val config = ShadowConfig(password, "AES-128-GCM")
        ShadowSocket(config, "127.0.0.1", 2222)
    }

}

