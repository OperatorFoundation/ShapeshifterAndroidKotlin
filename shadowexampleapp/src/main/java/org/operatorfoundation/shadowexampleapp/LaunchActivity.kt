package org.operatorfoundation.shadowexampleapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_launch.*
import org.operatorfoundation.shapeshifter.shadow.kotlin.ShadowConfig
import org.operatorfoundation.shapeshifter.shadow.kotlin.ShadowSocket
import java.nio.charset.Charset
import kotlin.concurrent.thread

class LaunchActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        run_button.setOnClickListener {
            shadowDarkStarClient()
        }
    }

    fun shadowDarkStarClient()
    {
        thread(start = true)
        {
            // TODO: Make sure password matches the servers public key.
            val config = ShadowConfig(
                "9caa4132c724f137c67928e9338c72cfe37e0dd28b298d14d5b5981effa038c9",
                "DarkStar"
            )

            // TODO: Use a valid server IP address.
            // This initializer throws if the connection is refused
            // In a real application the exception should be handled so that the application does not crash when a connection is refused
            val shadowSocket = ShadowSocket(config, "", 1234)

            val httpRequest = "GET / HTTP/1.0\r\nConnection: close\r\n\r\n"
            val textBytes = httpRequest.toByteArray()

            shadowSocket.outputStream.write(textBytes)
            println("Wrote some bytes.")

            shadowSocket.outputStream.flush()
            println("Flushed the output stream.")
            
            val buffer = ByteArray(235)
            val numberOfBytesRead = shadowSocket.inputStream.read(buffer)

            if (numberOfBytesRead > 0)
            {
                println("Read $numberOfBytesRead bytes.")

                val responseString = buffer.toString(Charset.defaultCharset())
                println("Read some data: " + responseString)

                if (responseString.contains("Yeah!"))
                {
                    println("The test succeeded!")
                }
                else
                {
                    println("Test failed: We did not get the response we were expecting.")
                }
            }
            else if (numberOfBytesRead == -1)
            {
                println("Test failed: Attempted to read from the network but received EOF.")
            }
            else
            {
                println("Test failed, we got an empty response from the server.")
            }
        }
    }
}