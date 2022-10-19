package org.operatorfoundation.shadowkotlin

import android.content.Context
import org.operatorfoundation.locketkotlin.LocketConnection
import java.lang.IllegalArgumentException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import javax.net.SocketFactory
import kotlin.concurrent.thread

class OKHTTPShadowSocketFactory(
    private val shadowConfig: ShadowConfig,
    private val host: String,
    private val port: Int,
    private val context: Context? = null,
    private val nonAppDirectory: String? = null,
    private val logFileName: String? = null
    ): SocketFactory() {
    private fun connect(): Socket {
        val shadowSocket = ShadowSocket(shadowConfig, host, port)
        var remoteSocket: Socket = shadowSocket
        if (logFileName != null) {
            try {
                remoteSocket = LocketConnection(context, nonAppDirectory, remoteSocket, logFileName)
            } catch (exception: IllegalArgumentException) {
                // if LocketConnection fails, do not override remoteSocket, use shadowSocket instead
            }
        }

        val serverSocket = ServerSocket(0)

        thread {
            val serverConn = serverSocket.accept()
            val serverInput = serverConn.inputStream
            val serverOutput = serverConn.outputStream
            val shadowInput = remoteSocket.inputStream
            val shadowOutput = remoteSocket.outputStream

            thread {
                serverInput.transferTo(shadowOutput)
            }

            thread {
                shadowInput.transferTo(serverOutput)
            }
        }

        val okhttpSocket = OKHTTPShadowSocket("127.0.0.1", serverSocket.localPort)
        println("server socket localPort: ${serverSocket.localPort}")
        return okhttpSocket
    }

    override fun createSocket(p0: String?, p1: Int): Socket {
        return connect()
    }

    override fun createSocket(p0: String?, p1: Int, p2: InetAddress?, p3: Int): Socket {
        return connect()
    }

    override fun createSocket(p0: InetAddress?, p1: Int): Socket {
        return connect()
    }

    override fun createSocket(p0: InetAddress?, p1: Int, p2: InetAddress?, p3: Int): Socket {
        return connect()
    }

    override fun createSocket(): Socket {
        return connect()
    }
}
