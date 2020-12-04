package org.operatorfoundation.shapeshifter.shadow.kotlin

import org.bouncycastle.crypto.digests.SHA1Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class ShadowAESCipher : ShadowCipher {
    constructor(config: ShadowConfig) {
        this.config = config
        createSalt(config)
    }

    constructor(config: ShadowConfig, salt: ByteArray) {
        this.config = config
        this.salt = salt

        key = createSecretKey(config, salt)

        cipher = when (config.cipherMode) {
            CipherMode.AES_128_GCM -> Cipher.getInstance("AES_128/GCM/NoPadding")
            CipherMode.AES_256_GCM -> Cipher.getInstance("AES_256/GCM/NoPadding")
            else -> throw NoSuchPaddingException()
        }
    }

    override fun createSecretKey(config: ShadowConfig, salt: ByteArray): SecretKey {
        val presharedKey = kdf(config)
        return hkdfSha1(config, salt, presharedKey)
    }

    override fun hkdfSha1(config: ShadowConfig, salt: ByteArray, psk: ByteArray): SecretKey {
        val keyAlgorithm = "AES"
        val infoString = "ss-subkey"
        val info = infoString.toByteArray()
        val okm = ByteArray(psk.size)

        val hkdf = HKDFBytesGenerator(SHA1Digest())
        hkdf.init(HKDFParameters(psk, salt, info))
        hkdf.generateBytes(okm, 0, psk.size)

        return SecretKeySpec(okm, keyAlgorithm)
    }

    override fun kdf(config: ShadowConfig): ByteArray {
        val hash = MessageDigest.getInstance("MD5")
        var buffer = ByteArray(0)
        var prev = ByteArray(0)
        val keylen = when (config.cipherMode) {
            CipherMode.AES_128_GCM -> 16
            CipherMode.AES_256_GCM -> 32
            // TODO(make a better else case)
            else -> 0
        }

        while (buffer.size < keylen) {
            hash.update(prev)
            hash.update(config.password.encodeToByteArray())
            buffer += hash.digest()
            val index = buffer.size - hash.digestLength
            prev = buffer.copyOfRange(index, buffer.size)
            hash.reset()
        }

        return buffer.copyOfRange(0, keylen)
    }

    // [encrypted payload length][length tag] + [encrypted payload][payload tag]
    // Pack takes the data above and packs them into a singular byte array.
    @ExperimentalUnsignedTypes
    override fun pack(plaintext: ByteArray): ByteArray {
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

    override fun encrypt(plaintext: ByteArray): ByteArray {
        val nonceBytes = nonce()
        val ivSpec = GCMParameterSpec(
            tagSizeBits,
            nonceBytes
        )

        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val encrypted = cipher.doFinal(plaintext)

        // increment counter every time nonce is used (encrypt/decrypt)
        counter += 1

        return encrypted
    }

    override fun decrypt(encrypted: ByteArray): ByteArray {
        val nonceBytes = nonce()
        val ivSpec = GCMParameterSpec(
            tagSizeBits,
            nonceBytes
        )

        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        val decrypted = cipher.doFinal(encrypted)

        //increment counter every time nonce is used (encrypt/decrypt)
        counter += 1

        return decrypted
    }
}