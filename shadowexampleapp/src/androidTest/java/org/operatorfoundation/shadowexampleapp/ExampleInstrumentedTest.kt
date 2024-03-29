package org.operatorfoundation.shadowexampleapp

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.utf8Size

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
//import org.operatorfoundation.locketkotlin.*
import org.operatorfoundation.shadow.*
import org.operatorfoundation.transmission.ConnectionType
import org.operatorfoundation.transmission.TransmissionConnection
import java.net.*
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.concurrent.thread

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun connectShadowToEchoServer()
    {
        val logger = Logger.getLogger("ShadowToEchoTestLogger")
        val serverAddress = "0.0.0.0:1234"
        val serverPublicKey = ""
        val shadowConfig = ShadowConfig(serverPublicKey, "Darkstar", serverAddress)
        val connection = TransmissionConnection(shadowConfig.serverIP, shadowConfig.port, ConnectionType.TCP, logger)
        val shadowConnection = ShadowConnection(shadowConfig, appContext, logger, connection)

        println("Successfully made a shadow connection")

        val successString = "pass"
        val shadowWrite = shadowConnection.write(successString)
        println("Shadow wrote: ${shadowWrite}")

        val shadowRead = shadowConnection.read(successString.count())

        if (shadowRead == null)
        {
            println("Tried to read but got no response.")
        }
        else
        {
            val readString = String(shadowRead)
            println("Read from server.")

            assert(successString == readString)
        }
    }

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
        val sConfig = ShadowConfig("", CipherMode.DarkStar.toString(), "")

        val client: OkHttpClient.Builder = OkHttpClient
            .Builder()
            .connectTimeout(30000, TimeUnit.MILLISECONDS)
            .readTimeout(30000, TimeUnit.MILLISECONDS)
            .writeTimeout(30000, TimeUnit.MILLISECONDS)

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val okhttpShadowSocketFactory = OKHTTPShadowSocketFactory(sConfig, appContext)

        val okHttpClient = client.socketFactory(
            okhttpShadowSocketFactory
        ).build()

        val request = Request.Builder()
            .url("")
            .build()

//        try
//        {
            okHttpClient.newCall(request).execute().use { response ->
                println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")
                println("Received a response to our okHTTPClient request: $response")
                println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")

                assert(response.isSuccessful)

                if (!response.isSuccessful) {
                    println("okHttpClient request was unsuccessful")
                } else {
                    println("okHttpClient request was successful")

                    for ((name, value) in response.headers) {
                        println("okHttpClient request $name: $value")
                    }

                    val responseBody = response.body ?: throw Exception("response body was null")
                    val body = responseBody.string().trim()
                    println(body)
                }
            }
        //}
//        catch (ex:Exception)
//        {
//            println("okHttpClient request was unsuccessful, stack trace: " + ex.stackTrace.toString())
//            println("okHttpClient request was unsuccessful, error message: " + ex.message)
//            fail()
//        }
    }

    @Test
    fun basicTest() {
        val shadowConfig = ShadowConfig("", CipherMode.DarkStar.toString(), "")
        val shadowSocket = ShadowSocket(shadowConfig, appContext)
        shadowSocket.outputStream.write("GET / HTTP/1.0\\r\\nConnection: close\\r\\n\\r\\n".toByteArray())
        val readBuffer = ByteArray(10)
        shadowSocket.inputStream.read(readBuffer)
    }

//    @Test
//    fun okhttpTestServerLocket() {
//        val config = ShadowConfig("", "DarkStar")
//        val client: OkHttpClient.Builder = OkHttpClient.Builder()
//            .connectTimeout(15000, java.util.concurrent.TimeUnit.MILLISECONDS)
//            .readTimeout(15000, java.util.concurrent.TimeUnit.MILLISECONDS)
//            .writeTimeout(15000, java.util.concurrent.TimeUnit.MILLISECONDS)
//        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//        println("locket directory: ${appContext.filesDir}")
//        val shadowSocketFactory = OKHTTPShadowSocketFactory(config, "", 2222, appContext, null, "ShadowClient")
//        //val shadowSocketFactory = OKHTTPShadowSocketFactory(config, "", 2222, null, null, null)
//        val okHttpClient = client.socketFactory(shadowSocketFactory).build()
//        val request = Request.Builder()
//            .url("https://www.google.com")
//            .build()
//// TODO: Catch exceptions and print them
//        val newCall = okHttpClient.newCall(request)
//        val execute = newCall.execute()
//        execute.use { response ->
//            if (!response.isSuccessful) {
//                println("okhttp client request response was not successful")
//                println("response: $response")
//                fail()
//            } else {
//                for ((name, value) in response.headers) {
//                    println("$name: $value")
//                }
//                val body = response.body.string().trim()
//                println(body)
//            }
//        }
//    }

    @Test
    fun testClientAndServer()
    {
        thread {
            runTestServer()
        }

        try
        {
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val shadowConfig = ShadowConfig("", "DarkStar", "")
            val okhhtpSocketFactory = OKHTTPShadowSocketFactory(shadowConfig, appContext)
            val socket = okhhtpSocketFactory.createSocket()
            val serverBytes = ByteArray(4)
            socket.getInputStream().read(serverBytes)
            val clientBytes = byteArrayOf(0x74, 0x63, 0x73, 0x74)
            socket.getOutputStream().write(clientBytes)
            assert(clientBytes.contentEquals(serverBytes))
        }
        catch (error: Exception)
        {
            println("testClientAndServer received an error: $error")
        }

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