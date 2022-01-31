package org.operatorfoundation.shapeshifter.shadow.kotlin

import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import com.google.common.hash.Funnels
import java.util.Calendar

import com.google.common.hash.BloomFilter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.Charset
import java.util.concurrent.ScheduledExecutorService

class Bloom {
    // Question for Brandon: where do we use this?  i know you said on decrypt fail, but if theyre using a repeat salt,
    // wouldnt the decrypt succeed since theyre using a valid salt?

    // Create a Bloom Filter instance
    // FIXME: how many expected insertions
    // FIXME: how do i keep the data from previous sessions
    var saltHistory = BloomFilter.create(
        Funnels.byteArrayFunnel(),
        10000
    )

    fun checkInBloom(salt: ByteArray): Boolean {
        if (saltHistory.mightContain(salt)) {
            return true
        } else {
            saltHistory.put(salt)
            return false
        }
    }

    fun save(fileName: String) {
        val output = FileOutputStream(fileName)
        saltHistory.writeTo(output)
    }

    fun load(fileName: String) {
        val input = FileInputStream(fileName)
        saltHistory = BloomFilter.readFrom(input, Funnels.byteArrayFunnel())
    }
}