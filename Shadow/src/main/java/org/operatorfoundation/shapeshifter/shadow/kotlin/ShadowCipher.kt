/*
	MIT License

	Copyright (c) 2020 Operator Foundation

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in all
	copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
	SOFTWARE.
*/

package org.operatorfoundation.shapeshifter.shadow.kotlin

import org.bouncycastle.crypto.digests.SHA1Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

// ShadowCipher contains the encryption and decryption methods.
class ShadowCipher(private val config: ShadowConfig, var salt: ByteArray) {

    private val cipher: Cipher

    private var counter = 0
    private var key: SecretKey

    companion object {
        // encrypted length plus tag in bytes
        const val lengthWithTagSize = 2 + 16
        const val tagSizeBits = 16 * 8
        const val tagSize = 16
        const val maxPayloadSize = 16417

        var saltSize: Int? = null
        // this is in bytes
//         val maxRead = maxPayloadSize + overhead
//         val minRead = 1 + overhead

        // Create a secret key using the two key derivation functions.
        private fun createSecretKey(config: ShadowConfig, salt: ByteArray): SecretKey {
            val presharedKey = kdf(config)
            return hkdfSha1(config, salt, presharedKey)
        }

        // Key derivation functions:
        // Derives the secret key from the preshared key and adds the salt.
        private fun hkdfSha1(config: ShadowConfig, salt: ByteArray, psk: ByteArray): SecretKey {
            val infoString = "ss-subkey"
            val info = infoString.toByteArray()
            val hkdf = HKDFBytesGenerator(SHA1Digest())
            hkdf.init(HKDFParameters(psk, salt, info))
            val okm = ByteArray(psk.size)
            hkdf.generateBytes(okm, 0, psk.size)

            val keyAlgorithm = when (config.cipherMode) {
                CipherMode.AES_128_GCM -> "AES"
                CipherMode.AES_256_GCM -> "AES"
                CipherMode.CHACHA20_IETF_POLY1305 -> "ChaCha20"
            }

            return SecretKeySpec(okm, keyAlgorithm)
        }

        // Derives the pre-shared key from the config.
        private fun kdf(config: ShadowConfig): ByteArray {
            val hash = MessageDigest.getInstance("MD5")
            var buffer: ByteArray = byteArrayOf()
            var prev: ByteArray = byteArrayOf()

            val keyLen = when (config.cipherMode) {
                CipherMode.AES_128_GCM -> 16
                CipherMode.AES_256_GCM -> 32
                CipherMode.CHACHA20_IETF_POLY1305 -> 32
            }

            while (buffer.size < keyLen) {
                hash.update(prev)
                hash.update(config.password.toByteArray())
                buffer += hash.digest()
                val index = buffer.size - hash.digestLength
                prev = buffer.sliceArray(index until buffer.size)
                hash.reset()
            }

            return buffer.sliceArray(0 until keyLen)
        }

        // Creates a byteArray of a specified length containing random bytes.
        fun createSalt(config: ShadowConfig): ByteArray {
            val saltSize = when (config.cipherMode) {
                CipherMode.AES_128_GCM -> 16
                CipherMode.AES_256_GCM -> 32
                CipherMode.CHACHA20_IETF_POLY1305 -> 32
            }
            ShadowCipher.saltSize = saltSize
            return Random.nextBytes(saltSize)
        }
    }

    // Init block:
    init {
        key = createSecretKey(config, salt)
        cipher = when (config.cipherMode) {
            CipherMode.AES_128_GCM -> Cipher.getInstance("AES_128/GCM/NoPadding")
            CipherMode.AES_256_GCM -> Cipher.getInstance("AES_256/GCM/NoPadding")
            CipherMode.CHACHA20_IETF_POLY1305 -> Cipher.getInstance("ChaCha20/Poly1305/NoPadding")
        }
    }

    // [encrypted payload length][length tag] + [encrypted payload][payload tag]
    // Pack takes the data above and packs them into a singular byte array.
    @ExperimentalUnsignedTypes
    fun pack(plaintext: ByteArray): ByteArray {

        // find length of plaintext
        val plaintextLength = plaintext.size

        // turn the length into two shorts and put them into an array
        val shortPlaintextLength = plaintextLength.toUShort()
        val leftShort = shortPlaintextLength / 256u
        val rightShort = shortPlaintextLength % 256u
        val leftByte = leftShort.toByte()
        val rightByte = rightShort.toByte()
        val lengthBytes = byteArrayOf(leftByte, rightByte)

        // encrypt the length and the payload, adding a tag to each
        val encryptedLengthBytes = encrypt(lengthBytes)
        val encryptedPayload = encrypt(plaintext)

        return encryptedLengthBytes + encryptedPayload
    }

    // Encrypts the data and increments the nonce counter.
    private fun encrypt(plaintext: ByteArray): ByteArray {
        val nonceBytes = nonce()
        val ivSpec = when (config.cipherMode) {
            CipherMode.AES_128_GCM -> GCMParameterSpec(tagSizeBits, nonceBytes)
            CipherMode.AES_256_GCM -> GCMParameterSpec(tagSizeBits, nonceBytes)
            CipherMode.CHACHA20_IETF_POLY1305 -> IvParameterSpec(nonceBytes)
        }
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val encrypted = cipher.doFinal(plaintext)

        // increment counter every time nonce is used (encrypt/decrypt)
        counter += 1

        return encrypted
    }

    // Decrypts data and increments the nonce counter.
    fun decrypt(encrypted: ByteArray): ByteArray {
        val nonceBytes = nonce()
        val ivSpec = when (config.cipherMode) {
            CipherMode.AES_128_GCM -> GCMParameterSpec(tagSizeBits, nonceBytes)
            CipherMode.AES_256_GCM -> GCMParameterSpec(tagSizeBits, nonceBytes)
            CipherMode.CHACHA20_IETF_POLY1305 -> IvParameterSpec(nonceBytes)
        }
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        val plaintext: ByteArray = cipher.doFinal(encrypted)

        // increment counter every time nonce is used (encrypt/decrypt)
        counter += 1

        return plaintext
    }

    // Create a nonce using our counter.
    private fun nonce(): ByteArray {
        // nonce must be 12 bytes
        val buffer = ByteBuffer.allocate(12)
        // nonce is little Endian
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        // create a byte array from counter
        buffer.putLong(counter.toLong())
        buffer.put(0)
        buffer.put(0)
        buffer.put(0)
        buffer.put(0)

        return buffer.array()
    }
}

// CipherMode establishes what algorithm and version you are using.
enum class CipherMode {
    //  AES 196 is not currently supported by go-shadowsocks2.
    //  We are not supporting it at this time either.
    AES_128_GCM,
    AES_256_GCM,
    CHACHA20_IETF_POLY1305
}
