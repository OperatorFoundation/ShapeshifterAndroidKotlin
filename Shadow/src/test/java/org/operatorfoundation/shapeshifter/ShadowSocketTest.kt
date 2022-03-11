package org.operatorfoundation.shapeshifter

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.operatorfoundation.shapeshifter.shadow.kotlin.*
import java.io.IOException
import java.net.ServerSocket
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.spec.InvalidKeySpecException
import java.util.*
import kotlin.concurrent.thread

internal class ShadowSocketTest {

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
        val userHomeDir = System.getProperty("user.home")
        val bloom = Bloom()
        bloom.load("$userHomeDir/Desktop/Configs/bloom.txt")
        val serverPersistentPublicKeyBytes = Files.readAllBytes(Paths.get("$userHomeDir/Desktop/Configs/serverPersistentPublicKey.txt"))
        val serverPersistentPublicKeyString = String(serverPersistentPublicKeyBytes, StandardCharsets.UTF_8)
        println(serverPersistentPublicKeyString)
        // generate public key on swift for SPPK
        val config = ShadowConfig(
            serverPersistentPublicKeyString,
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
        bloom.save("$userHomeDir/Desktop/Configs/bloom.txt")
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

