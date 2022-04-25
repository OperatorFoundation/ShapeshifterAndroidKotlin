package org.operatorfoundation.shadowexampleapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_launch.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.operatorfoundation.shapeshifter.shadow.kotlin.ShadowConfig
import org.operatorfoundation.shapeshifter.shadow.kotlin.ShadowSocket
import org.operatorfoundation.shapeshifter.shadow.kotlin.toHexString
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class LaunchActivity : AppCompatActivity()
{
    val coroutineContext: CoroutineContext = EmptyCoroutineContext
    val externalScope: CoroutineScope = CoroutineScope(coroutineContext)
    val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        run_button.setOnClickListener {
            externalScope.launch { shadowDarkStarClient() }
        }
    }
    // TODO: Replace this with a good example of how to use our library. What does this library do and how do we use it...
    suspend fun shadowDarkStarClient()
    {
        externalScope.launch(defaultDispatcher)
        {
            // TODO: Make sure password matches the servers public key.
            val config = ShadowConfig(
                "9caa4132c724f137c67928e9338c72cfe37e0dd28b298d14d5b5981effa038c9",
                "DarkStar"
            )

            // TODO: Use a valid server IP address.
            val shadowSocket = ShadowSocket(config, "0.0.0.0", 1234)
            val httpRequest = "GET / HTTP/1.0\r\n\r\n"
            val textBytes = httpRequest.toByteArray()

            shadowSocket.outputStream.write(textBytes)
            println("Wrote some bytes.")

            shadowSocket.outputStream.flush()
            println("Flushed the output stream.")
            
            var buffer = ByteArray(5)
            System.out.println("bytes available: " + shadowSocket.inputStream.available())
            val numberOfBytesRead = shadowSocket.inputStream.read(buffer)
            println("Read $numberOfBytesRead bytes.")
            println("Read some data: " + buffer.toHexString())
            println("Test complete")
        }
    }
}