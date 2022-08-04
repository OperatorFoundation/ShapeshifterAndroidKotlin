package org.operatorfoundation.shapeshifter

import java.io.IOException
import java.net.ServerSocket
import java.net.URL
import java.security.spec.InvalidKeySpecException
import java.util.*
import kotlin.concurrent.thread
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

import org.operatorfoundation.shadowkotlin.DarkStar
import org.operatorfoundation.shadowkotlin.ShadowConfig
import org.operatorfoundation.shadowkotlin.ShadowSocketFactory
import org.operatorfoundation.shadowkotlin.readNBytes


import kotlin.concurrent.thread


internal class ShadowSocketTest
{
    @ExperimentalUnsignedTypes
    fun runJsonTestServer()
    {
        val testServer = ServerSocket(1234)
        val socket = testServer.accept()
        readNBytes(socket.inputStream, 2)
        socket.outputStream.write("Yo".toByteArray())
    }

    //Bad Arguments Tests

    @ExperimentalUnsignedTypes
    @Test(expected = IllegalArgumentException::class)
    fun checkBadCipher()
    {
        val password = "1234"
        ShadowConfig(password, "cipherNoCiphing")
    }

    @ExperimentalUnsignedTypes
    @Test(expected = InvalidKeySpecException::class)
    fun badKeySize()
    {
        val password = "1234"
        val config = ShadowConfig(password, "DarkStar")
        val darkStar = DarkStar(config, "127.0.0.1", 2222)
        darkStar.createHandshake()
    }

    @Test
    @ExperimentalUnsignedTypes
    fun sipTest()
    {
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
    fun maxLongTest()
    {
        val uLongNumber = ULong.MAX_VALUE
        println("uLongNumber: $uLongNumber")

        val longNumber = uLongNumber.toLong()
        println("longNumber: $longNumber")

        val backToULongNumber = longNumber.toULong()
        println("backToULongNumber: $backToULongNumber")

        assert(uLongNumber == backToULongNumber)
    }

    @Test
    @Throws(IOException::class)
    fun createFactoryTest() {
        val url =
            URL("https://raw.githubusercontent.com/OperatorFoundation/ShadowSwift/main/Tests/ShadowSwiftTests/testsip008.json")
        val uuid = UUID.fromString("27b8a625-4f4b-4428-9f0f-8a2317db7c79")
        val factory = ShadowSocketFactory.factoryFromUrl(url, uuid)
        assertNotNull(factory)
    }

    @Test
    fun configSerialization()
    {
        val config = ShadowConfig("password", "DarkStar")
        val configString = Json.encodeToString(config)
        println(configString)

        val decodedConfig: ShadowConfig = Json.decodeFromString(configString)
        assert(config.password == decodedConfig.password)
    }
}
