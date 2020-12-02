//package org.operatorfoundation.shapeshifter
//
//import org.junit.Assert
//import org.junit.Test
//import org.operatorfoundation.shapeshifter.shadow.kotlin.ShadowConfig
//import org.operatorfoundation.shapeshifter.shadow.kotlin.ShadowSocketChannel
//import org.operatorfoundation.shapeshifter.shadow.kotlin.readNBytes
//import java.net.InetSocketAddress
//import java.net.ServerSocket
//import java.net.SocketAddress
//import java.nio.ByteBuffer
//import java.nio.channels.spi.SelectorProvider.provider
//import kotlin.concurrent.thread
//
//internal class ShadowSocketChannelTest {
//
//    @ExperimentalUnsignedTypes
//    private fun runTestServer() {
//        val testServer = ServerSocket(3333)
//        val socket = testServer.accept()
//        readNBytes(socket.inputStream, 2)
//        socket.outputStream.write("Yo".toByteArray())
//    }
//
//    @ExperimentalUnsignedTypes
//    @Test
//    fun shadowSocketInitTest() {
//        val password = "1234"
//        val config = ShadowConfig(password, "AES-128-GCM")
//        val shadowSocket = ShadowSocketChannel(provider(), config)
//        Assert.assertNotNull(shadowSocket)
//    }
//
//    @ExperimentalUnsignedTypes
//    @Test
//    fun shadowSocketWriteTest() {
//        thread {
//            runTestServer()
//        }
//        val socksAddress: SocketAddress = InetSocketAddress("127.0.0.1", 2222)
//        val password = "1234"
//        val config = ShadowConfig(password, "AES-128-GCM")
//        val shadowSocket = ShadowSocketChannel(provider(), config)
//        shadowSocket.connect(socksAddress)
//        val plaintext = "Hi"
//        val textBytes = plaintext.toByteArray()
//        val textByteBuffer = ByteBuffer.wrap(textBytes)
//        shadowSocket.write(textByteBuffer)
//    }
//
//    @ExperimentalUnsignedTypes
//    @Test
//    fun shadowSocketReadTest() {
//        thread {
//            runTestServer()
//        }
//        val socksAddress: SocketAddress = InetSocketAddress("127.0.0.1", 2222)
//        val password = "1234"
//        val config = ShadowConfig(password, "AES-128-GCM")
//        val shadowSocket = ShadowSocketChannel(provider(), config)
//        shadowSocket.connect(socksAddress)
//        val plaintext = "Hi"
//        val textBytes = plaintext.toByteArray()
//        val textByteBuffer = ByteBuffer.wrap(textBytes)
//        shadowSocket.write(textByteBuffer)
//        val byteArray = ByteArray(2)
//        val buffer = ByteBuffer.wrap(byteArray)
//        shadowSocket.read(buffer)
//        Assert.assertEquals(buffer.toString(), "Yo")
//    }
//}
//
//
