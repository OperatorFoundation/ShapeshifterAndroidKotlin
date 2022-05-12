package org.operatorfoundation.shadowexampleapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_launch.*
import org.operatorfoundation.shapeshifter.shadow.kotlin.ShadowConfig
import org.operatorfoundation.shapeshifter.shadow.kotlin.ShadowSocket
import java.nio.charset.Charset
import kotlin.concurrent.thread

class LaunchActivity : AppCompatActivity()
{

    lateinit var resultText: TextView

    override fun onCreate(savedInstanceState: Bundle?)
    {
            println("*******ENTERED onCreate FUNCTION")
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_launch)

            println("*******ABOUT TO CALL shadowDarkStarClient()")
            run_button.setOnClickListener {
                shadowDarkStarClient()
            }

            resultText = findViewById(R.id.resultText)
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
                "DarkStar"
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
                resultText.text = wroteBytesMessage}

                shadowOutputStream.flush()
                runOnUiThread {
                println(flushedOutputMessage)
                resultText.text = flushedOutputMessage}

                val buffer = ByteArray(235)
                val numberOfBytesRead = shadowInputStream.read(buffer)
                val readBytesMessage = "Read $numberOfBytesRead bytes."

                if (numberOfBytesRead > 0)
                {
                    runOnUiThread {
                    println(readBytesMessage)
                    resultText.text = readBytesMessage}

                    val responseString = buffer.toString(Charset.defaultCharset())
                    val readDataMessage = ("Read some data: " + responseString)
                    val testSuccessMessage = "The test succeeded!"
                    val testFailedMessage =
                        "Test failed: We did not get the response we were expecting."
                    runOnUiThread {
                    println(readDataMessage)
                    resultText.text = readDataMessage}

                    if (responseString.contains("Yeah!"))
                    {
                        runOnUiThread {
                        println(testSuccessMessage)
                        resultText.text = testSuccessMessage}
                    }
                    else
                    {
                        runOnUiThread {
                        println(testFailedMessage)
                        resultText.text = testFailedMessage}
                    }
                }
                else if (numberOfBytesRead == -1)
                {
                    val testFailedEOFMessage =
                        "Test failed: Attempted to read from the network but received EOF."
                    runOnUiThread {
                    println(testFailedEOFMessage)
                    resultText.text = testFailedEOFMessage}
                }
                else
                {
                    val testEmptyResponseFailMessage =
                        "Test failed, we got an empty response from the server."
                    runOnUiThread {
                    println(testEmptyResponseFailMessage)
                    resultText.text = testEmptyResponseFailMessage}
                }
            }
            catch (error: Exception)
            {
                val receivedErrorMessage =
                    "--> Received an error while attempting to create a connection: $error"
                runOnUiThread {
                println(receivedErrorMessage)
                resultText.text = receivedErrorMessage}

                val checkCredentialsMessage = "--> Check your server credentials."
                runOnUiThread {
                println(checkCredentialsMessage)
                resultText.text = checkCredentialsMessage}
            }
        }
    }
}