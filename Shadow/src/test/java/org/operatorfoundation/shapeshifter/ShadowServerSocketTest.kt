package org.operatorfoundation.shapeshifter

import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.operatorfoundation.shapeshifter.shadow.kotlin.ShadowConfig
import java.net.InetAddress

internal class ShadowServerSocketTest {
    @Test
    fun shadowServerSocketConstructorTest() {
        val password = "1234"
        val config = ShadowConfig(password, "DarkStar")
        val shadowServerSocket = ShadowServerSocket(config, 2222)
        assertNotEquals(shadowServerSocket, null)
    }

    @Test
    fun shadowServerSocketConstructorTest2() {
        val password = "1234"
        val config = ShadowConfig(password, "DarkStar")
        val shadowServerSocket = ShadowServerSocket(config, 2222, 1)
        assertNotEquals(shadowServerSocket, null)
    }

    @Test
    fun shadowServerSocketConstructorTest3() {
        val password = "1234"
        val address: InetAddress = InetAddress.getByName("127.0.0.1")
        val config = ShadowConfig(password, "DarkStar")
        val shadowServerSocket = ShadowServerSocket(config, 2222, 1, address)
        assertNotEquals(shadowServerSocket, null)
    }
}