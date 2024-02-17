package org.operatorfoundation.shadow

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import javax.net.SocketFactory
import kotlin.concurrent.thread

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class OKHTTPShadowSocketFactory(
    private val shadowConfig: ShadowConfig,
    private val context: Context
    ): SocketFactory() {

    private fun connect(): Socket {
        val shadowSocket = ShadowSocket(shadowConfig, context)
        val remoteSocket: Socket = shadowSocket
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
