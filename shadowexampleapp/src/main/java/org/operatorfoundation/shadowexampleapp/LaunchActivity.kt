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

    suspend fun shadowDarkStarClient() {


        externalScope.launch(defaultDispatcher)
        {

            // generate public key on swift for SPPK
            val config = ShadowConfig(
                "d089c225ef8cda8d477a586f062b31a756270124d94944e458edf1a9e1e41ed6",
                "DarkStar"
            )
            val shadowSocket = ShadowSocket(config, "164.92.71.230", 1234)
            val httpRequest = "GET / HTTP/1.0\r\n\r\n"
            val textBytes = httpRequest.toByteArray()
            shadowSocket.outputStream.write(textBytes)
            shadowSocket.outputStream.flush()
            var buffer = ByteArray(5)
            System.out.println("bytes available: " + shadowSocket.inputStream.available())
            shadowSocket.inputStream.read(buffer)
        }

    }
}