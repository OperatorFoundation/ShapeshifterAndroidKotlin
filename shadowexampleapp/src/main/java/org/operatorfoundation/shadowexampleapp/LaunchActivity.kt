package org.operatorfoundation.shadowexampleapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import okhttp3.OkHttpClient
import okhttp3.Request
import org.operatorfoundation.shadow.CipherMode
import org.operatorfoundation.shadowexampleapp.databinding.ActivityLaunchBinding
import org.operatorfoundation.shadow.OKHTTPShadowSocketFactory
import org.operatorfoundation.shadow.ShadowConfig
import org.operatorfoundation.shadow.ShadowSocket
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class LaunchActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityLaunchBinding

    lateinit var resultTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?)
    {

        super.onCreate(savedInstanceState)
        binding = ActivityLaunchBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.runButton.setOnClickListener {
            shadowDarkStarClient()
//            thread(start = true)
//            {
//                restCall()
//            }
        }

        resultTextView = binding.resultText
    }

    fun shadowDarkStarClient()
    {
        println("*******ENTERED SHADOWDARKSTARCLIENT FUNCTION")
        thread(start = true)
        {
            println("*******ENTERED thread")
            // TODO: Make sure password matches the servers public key.
            val config = ShadowConfig(
                "",
                "DarkStar",
                "",
                1234
            )
            
            try
            {
                // TODO: Use a valid server IP address.
                val shadowSocket = ShadowSocket(config, "", 1234)
                val httpRequest = "GET / HTTP/1.0\r\nConnection: close\r\n\r\n"
                val textBytes = httpRequest.toByteArray()

                val wroteBytesMessage = "Wrote ${textBytes.size} bytes."
                val flushedOutputMessage = "Flushed the output stream."

                val shadowOutputStream = shadowSocket.outputStream
                val shadowInputStream = shadowSocket.inputStream

                shadowOutputStream.write(textBytes)

                runOnUiThread {
                println(wroteBytesMessage)
                resultTextView.text = wroteBytesMessage}

                shadowOutputStream.flush()
                runOnUiThread {
                println(flushedOutputMessage)
                resultTextView.text = flushedOutputMessage}

                val buffer = ByteArray(235)
                val numberOfBytesRead = shadowInputStream.read(buffer)
                val readBytesMessage = "Read $numberOfBytesRead bytes."

                if (numberOfBytesRead > 0)
                {
                    runOnUiThread {
                    println(readBytesMessage)
                    resultTextView.text = readBytesMessage}

                    val responseString = buffer.toString(Charset.defaultCharset())
                    val readDataMessage = ("Read some data: " + responseString)
                    val testSuccessMessage = "The test succeeded!"
                    val testFailedMessage =
                        "Test failed: We did not get the response we were expecting."
                    runOnUiThread {
                    println(readDataMessage)
                    resultTextView.text = readDataMessage}

                    if (responseString.contains("Yeah!"))
                    {
                        runOnUiThread {
                        println(testSuccessMessage)
                        resultTextView.text = testSuccessMessage}
                    }
                    else
                    {
                        runOnUiThread {
                        println(testFailedMessage)
                        resultTextView.text = testFailedMessage}
                    }
                }
                else if (numberOfBytesRead == -1)
                {
                    val testFailedEOFMessage =
                        "Test failed: Attempted to read from the network but received EOF."
                    runOnUiThread {
                    println(testFailedEOFMessage)
                    resultTextView.text = testFailedEOFMessage}
                }
                else
                {
                    val testEmptyResponseFailMessage =
                        "Test failed, we got an empty response from the server."
                    runOnUiThread {
                    println(testEmptyResponseFailMessage)
                    resultTextView.text = testEmptyResponseFailMessage}
                }
            }
            catch (error: Exception)
            {
                val receivedErrorMessage =
                    "--> Received an error while attempting to create a connection: $error"
                runOnUiThread {
                println(receivedErrorMessage)
                resultTextView.text = receivedErrorMessage}

                val checkCredentialsMessage = "--> Check your server credentials."
                runOnUiThread {
                println(checkCredentialsMessage)
                resultTextView.text = checkCredentialsMessage}
            }
        }
    }

    fun restCall()
    {
        val sConfig = ShadowConfig("", CipherMode.DarkStar.toString(), "", 0)
        println("config created")
        val client: OkHttpClient.Builder = OkHttpClient
            .Builder()
            .connectTimeout(30000, TimeUnit.MILLISECONDS)
            .readTimeout(30000, TimeUnit.MILLISECONDS)
            .writeTimeout(30000, TimeUnit.MILLISECONDS)
        println("builder created")
        val okhttpShadowSocketFactory = OKHTTPShadowSocketFactory(
            sConfig,
            "",
            0)
        println("factory created")
        val okHttpClient = client.socketFactory(
            okhttpShadowSocketFactory
        ).build()
        println("client finished building")
        val request = Request.Builder()
            .url("")
            .build()
        println("request created")
        try
        {
        okHttpClient.newCall(request).execute().use { response ->
            println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")
            println("Received a response to our okHTTPClient request: $response")
            println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")

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
        }
        catch (ex:Exception)
        {
            println("okHttpClient request was unsuccessful, stack trace: " + ex.stackTrace.toString())
            println("okHttpClient request was unsuccessful, error message: " + ex)
        }
    }
}