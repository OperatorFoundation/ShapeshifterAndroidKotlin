package org.operatorfoundation.shapeshifter.shadow.kotlin

import java.net.*
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.MembershipKey
import java.nio.channels.spi.SelectorProvider

class ShadowDatagramChannel(provider: SelectorProvider) : DatagramChannel(provider) {
    override fun implCloseSelectableChannel() {
        TODO("Not yet implemented")
    }

    override fun implConfigureBlocking(block: Boolean) {
        TODO("Not yet implemented")
    }

    override fun read(dst: ByteBuffer?): Int {
        TODO("Not yet implemented")
    }

    override fun read(dsts: Array<out ByteBuffer>?, offset: Int, length: Int): Long {
        TODO("Not yet implemented")
    }

    override fun write(src: ByteBuffer?): Int {
        TODO("Not yet implemented")
    }

    override fun write(srcs: Array<out ByteBuffer>?, offset: Int, length: Int): Long {
        TODO("Not yet implemented")
    }

    override fun bind(local: SocketAddress?): DatagramChannel {
        TODO("Not yet implemented")
    }

    override fun getLocalAddress(): SocketAddress {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> setOption(name: SocketOption<T>?, value: T): DatagramChannel {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> getOption(name: SocketOption<T>?): T {
        TODO("Not yet implemented")
    }

    override fun supportedOptions(): MutableSet<SocketOption<*>> {
        TODO("Not yet implemented")
    }

    override fun join(group: InetAddress?, interf: NetworkInterface?): MembershipKey {
        TODO("Not yet implemented")
    }

    override fun join(
        group: InetAddress?,
        interf: NetworkInterface?,
        source: InetAddress?
    ): MembershipKey {
        TODO("Not yet implemented")
    }

    override fun socket(): DatagramSocket {
        TODO("Not yet implemented")
    }

    override fun isConnected(): Boolean {
        TODO("Not yet implemented")
    }

    override fun connect(remote: SocketAddress?): DatagramChannel {
        TODO("Not yet implemented")
    }

    override fun disconnect(): DatagramChannel {
        TODO("Not yet implemented")
    }

    override fun getRemoteAddress(): SocketAddress {
        TODO("Not yet implemented")
    }

    override fun receive(dst: ByteBuffer?): SocketAddress {
        TODO("Not yet implemented")
    }

    override fun send(src: ByteBuffer?, target: SocketAddress?): Int {
        TODO("Not yet implemented")
    }

}