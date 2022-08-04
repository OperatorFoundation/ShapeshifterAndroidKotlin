package org.operatorfoundation.shadowkotlin

import com.google.common.hash.Funnels
import com.google.common.hash.BloomFilter
import java.io.FileInputStream
import java.io.FileOutputStream

class Bloom
{
    // Create a Bloom Filter instance
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