package org.operatorfoundation.shadowexampleapp

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.OkHttpClient
import okhttp3.Request

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.internal.runners.statements.Fail
import org.operatorfoundation.locketkotlin.LocketFactory
import org.operatorfoundation.shadowkotlin.*
import java.io.IOException
import java.net.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("org.operatorfoundation.shadowexampleapp", appContext.packageName)
    }

    @Test
    fun testSocket() {
        val serverSocket = ServerSocket(0)

        thread {
            serverSocket.accept()
        }

        val socket = OKHTTPShadowSocket("127.0.0.1", serverSocket.localPort)
        val address =InetSocketAddress("127.0.0.1", serverSocket.localPort)
        socket.connect(address)
        socket.getInputStream()
    }

    @Test
    fun restCall()
    {
        val sConfig = ShadowConfig("", CipherMode.DarkStar.toString())

        val client: OkHttpClient.Builder = OkHttpClient
            .Builder()
            .connectTimeout(30000, TimeUnit.MILLISECONDS)
            .readTimeout(30000, TimeUnit.MILLISECONDS)
            .writeTimeout(30000, TimeUnit.MILLISECONDS)

        val okhttpShadowSocketFactory = OKHTTPShadowSocketFactory(
            sConfig,
            "",
            111)

        val okHttpClient = client.socketFactory(
            okhttpShadowSocketFactory
        ).build()

        val request = Request.Builder()
            .url("")
            .build()

        try
        {
            okHttpClient.newCall(request).execute().use { response ->
                println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")
                println("Received a response to our okHTTPClient request: $response")
                println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")

                assert(response.isSuccessful)

                if (!response.isSuccessful) {
                    println("okHttpClient request was unsuccessful")
                    //throw IOException("Unexpected code $response")
                } else {
                    println("okHttpClient request was successful")

                    for ((name, value) in response.headers) {
                        println("okHttpClient request $name: $value")
                    }
                    val body = response.body!!.string().trim()
                    println(body)
                }
            }
        }
        catch (ex:Exception)
        {
            println("okHttpClient request was unsuccessful, stack trace: " + ex.stackTrace.toString())
            println("okHttpClient request was unsuccessful, error message: " + ex.message)
        }
    }

    @Test
    fun okhttpTestServerLocket() {
        val config = ShadowConfig("", "DarkStar")
        val client: OkHttpClient.Builder = OkHttpClient.Builder()
            .connectTimeout(15000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .readTimeout(15000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .writeTimeout(15000, java.util.concurrent.TimeUnit.MILLISECONDS)
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        println("locket directory: ${appContext.filesDir}")
        val shadowSocketFactory = OKHTTPShadowSocketFactory(config, "", 2222, appContext, null, "ShadowClient")
        //val shadowSocketFactory = OKHTTPShadowSocketFactory(config, "", 2222, null, null, null)
        val okHttpClient = client.socketFactory(shadowSocketFactory).build()
        val request = Request.Builder()
            .url("https://www.google.com")
            .build()
// TODO: Catch exceptions and print them
        val newCall = okHttpClient.newCall(request)
        val execute = newCall.execute()
        execute.use { response ->
            if (!response.isSuccessful) {
                println("okhttp client request response was not successful")
                println("response: $response")
                fail()
            } else {
                for ((name, value) in response.headers) {
                    println("$name: $value")
                }
                val body = response.body!!.string().trim()
                println(body)
            }
        }
    }

    @Test
    fun testClientAndServer() {
        thread {
            runTestServer()
        }

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val shadowConfig = ShadowConfig("", "DarkStar")
        val okhhtpSocketFactory = OKHTTPShadowSocketFactory(shadowConfig, "", 7070, appContext, null, "okhhtpClient")
        val socket = okhhtpSocketFactory.createSocket()
        val serverBytes = ByteArray(4)
        socket.getInputStream().read(serverBytes)
        val clientBytes = byteArrayOf(0x74, 0x63, 0x73, 0x74)
        socket.getOutputStream().write(clientBytes)
        assert(clientBytes.contentEquals(serverBytes))
    }

    private fun runTestServer() {
        val address = InetAddress.getByName("")
        val serverSocket = ServerSocket(7070, 1, address)
        val server = serverSocket.accept()
        val serverBytes = byteArrayOf(0x74, 0x63, 0x73, 0x74)
        server.getOutputStream().write(serverBytes)
        val clientBytes = ByteArray(4)
        server.getInputStream().read(clientBytes)
    }
}