package org.operatorfoundation.shapeshifter.shadow.kotlin

import org.bouncycastle.crypto.digests.SHA1Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.MessageDigest
import java.security.Security
import java.security.spec.AlgorithmParameterSpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class ShadowAESCipher(config: ShadowConfig): ShadowCipher(config) {

    init
    {
        ShadowAESCipher(config, createSalt(config))
    }

    constructor(config: ShadowConfig, salt: ByteArray) : this(config)
    {
        this.config = config
        this.salt = salt

        key = createSecretKey(config, salt)
        when (config.cipherMode) {
            CipherMode.AES_128_GCM -> try {
                cipher = Cipher.getInstance("AES_128/GCM/NoPadding")
                saltSize = 16
            } catch (e: NoSuchPaddingException) {
                e.printStackTrace()
            }
            CipherMode.AES_256_GCM -> try {
                cipher = Cipher.getInstance("AES_256/GCM/NoPadding")
                saltSize = 32
            } catch (e: NoSuchPaddingException) {
                e.printStackTrace()
            }
            CipherMode.CHACHA20_IETF_POLY1305 -> try {
                Security.addProvider(BouncyCastleProvider())
                cipher = Cipher.getInstance("CHACHA7539")
                saltSize = 32
            } catch (e: NoSuchPaddingException) {
                e.printStackTrace()
            }
        }
    }

    override fun createSecretKey(config: ShadowConfig?, salt: ByteArray?): SecretKey
    {
        val presharedKey = kdf(config)
        return hkdfSha1(config, salt, presharedKey)
    }

    override fun hkdfSha1(config: ShadowConfig?, salt: ByteArray?, psk: ByteArray?): SecretKey
    {
        val infoString = "ss-subkey"
        val info = infoString.toByteArray()
        val hkdf = HKDFBytesGenerator(SHA1Digest())
        hkdf.init(HKDFParameters(psk, salt, info))
        val okm = ByteArray(psk!!.size)
        hkdf.generateBytes(okm, 0, psk.size)
        var keyAlgorithm: String? = null
        keyAlgorithm = when (config!!.cipherMode) {
            CipherMode.AES_128_GCM, CipherMode.AES_256_GCM -> "AES"
            CipherMode.CHACHA20_IETF_POLY1305 -> "ChaCha20"
            else -> throw IllegalStateException("Unexpected or unsupported Algorithm value: $keyAlgorithm")
        }
        return SecretKeySpec(okm, keyAlgorithm)
    }

    override fun kdf(config: ShadowConfig?): ByteArray
    {
        val hash = MessageDigest.getInstance("MD5")
        var buffer = ByteArray(0)
        var prev = ByteArray(0)

        var keylen = 0
        when (config!!.cipherMode) {
            CipherMode.AES_128_GCM -> keylen = 16
            CipherMode.AES_256_GCM, CipherMode.CHACHA20_IETF_POLY1305 -> keylen = 32
        }

        while (buffer.size < keylen) {
            hash.update(prev)
            hash.update(config.password.encodeToByteArray())
            buffer = buffer + hash.digest()
            val index = buffer.size - hash.digestLength
            prev = Arrays.copyOfRange(buffer, index, buffer.size)
            hash.reset()
        }

        return buffer.copyOfRange(0, keylen)
    }

    override fun pack(plaintext: ByteArray?): ByteArray?
    {
        // find the length of plaintext
        val plaintextLength = plaintext!!.size

        // turn the length into two shorts and put them into an array
        // this is encoded in big endian
        val shortPlaintextLength = plaintextLength.toShort()
        val leftShort = (shortPlaintextLength / 256).toShort()
        val rightShort = (shortPlaintextLength % 256).toShort()
        val leftByte = leftShort.toByte()
        val rightByte = rightShort.toByte()
        val lengthBytes = byteArrayOf(leftByte, rightByte)

        // encrypt the length and the payload, adding a tag to each
        val encryptedLengthBytes = encrypt(lengthBytes)

        if (encryptedLengthBytes != null) {
            val encryptedPayload = encrypt(plaintext)

            if (encryptedPayload != null) {
                return encryptedLengthBytes + encryptedPayload
            } else {
                return null
            }
        } else {
            return null
        }
    }

    override fun encrypt(plaintext: ByteArray?): ByteArray?
    {
        val nonceBytes = nonce()
        val ivSpec: AlgorithmParameterSpec
        ivSpec = when (config!!.cipherMode) {
            CipherMode.AES_128_GCM, CipherMode.AES_256_GCM -> GCMParameterSpec(
                tagSizeBits,
                nonceBytes
            )
            CipherMode.CHACHA20_IETF_POLY1305 -> IvParameterSpec(nonceBytes)
        }
        cipher!!.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val encrypted = cipher!!.doFinal(plaintext)

        // increment counter every time nonce is used (encrypt/decrypt)
        counter += 1

        return encrypted
    }

    override fun decrypt(encrypted: ByteArray?): ByteArray?
    {
        val nonceBytes = nonce()
        val ivSpec: AlgorithmParameterSpec
        ivSpec = when (config!!.cipherMode) {
            CipherMode.AES_128_GCM, CipherMode.AES_256_GCM -> GCMParameterSpec(
                tagSizeBits,
                nonceBytes
            )
            CipherMode.CHACHA20_IETF_POLY1305 -> IvParameterSpec(nonceBytes)
        }
        cipher!!.init(Cipher.DECRYPT_MODE, key, ivSpec)

        //increment counter every time nonce is used (encrypt/decrypt)
        counter += 1

        return cipher!!.doFinal(encrypted)
    }
}