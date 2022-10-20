package org.operatorfoundation.shadowkotlin

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.operatorfoundation.locketkotlin.LocketFactory
import java.io.IOException
import java.net.ServerSocket
import java.net.URL
import java.security.spec.InvalidKeySpecException
import java.util.*
import kotlin.concurrent.thread

internal class ShadowSocketTest
{
    @Test
    fun okhttpTestServer() {
        val config = ShadowConfig("", "DarkStar")
        val client: OkHttpClient.Builder = OkHttpClient.Builder()
            .connectTimeout(15000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(15000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .writeTimeout(15000, java.util.concurrent.TimeUnit.MILLISECONDS)
        val okHttpClient = client.socketFactory(ShadowSocketFactory(config, "", 1234)).build()

        val request = Request.Builder()
            .url("https://")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            for ((name, value) in response.headers) {
                println("$name: $value")
            }
            val body = response.body!!.string().trim()
            println(body)
        }
    }

    @Test
    fun okhttpTestServerLocket() {
        val config = ShadowConfig("", "DarkStar")
        val client: OkHttpClient.Builder = OkHttpClient.Builder()
            .connectTimeout(15000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(15000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .writeTimeout(15000, java.util.concurrent.TimeUnit.MILLISECONDS)
        val shadowSocketFactory = ShadowSocketFactory(config, "", 2222)
        val locketFactory = LocketFactory(null, "", shadowSocketFactory, "ShadowClient")
        val okHttpClient = client.socketFactory(locketFactory).build()

        val request = Request.Builder()
            .url("https://www.google.com")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            for ((name, value) in response.headers) {
                println("$name: $value")
            }
            val body = response.body!!.string().trim()
            println(body)
        }
    }

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
    fun configSerializationNoHost()
    {
        val config = ShadowConfig("password", "DarkStar")
        val configString = Json.encodeToString(config)

        println("\n--> Shadow Config as Json: \n$configString")

        val decodedConfig: ShadowConfig = Json.decodeFromString(configString)
        assert(config.password == decodedConfig.password)
    }

    @Test
    fun configSerializationWithHost()
    {
        val config = ShadowConfig("password", "DarkStar", "0.0.0.0", 0)
        val configString = Json.encodeToString(config)

        println("\n--> Shadow Config as Json: \n$configString")

        val decodedConfig: ShadowConfig = Json.decodeFromString(configString)
        assert(config.password == decodedConfig.password)
    }
}
