package org.operatorfoundation.shapeshifter.shadow.kotlin

import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec

class ShadowDarkStarCipher(override var key: SecretKey?) : ShadowCipher() {
    // Create a secret key using the two key derivation functions.
    @Throws(NoSuchAlgorithmException::class)
    override fun createSecretKey(config: ShadowConfig, salt: ByteArray): SecretKey {
        return KeyGenerator.getInstance("AES").generateKey()
    }

    override fun hkdfSha1(config: ShadowConfig, salt: ByteArray, psk: ByteArray): SecretKey {
        return KeyGenerator.getInstance("AES").generateKey()
    }

    @Throws(NoSuchAlgorithmException::class)
    override fun kdf(config: ShadowConfig): ByteArray {
        return ByteArray(0)
    }

    // [encrypted payload length][length tag] + [encrypted payload][payload tag]
    // Pack takes the data above and packs them into a singular byte array.
    @Throws(
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class
    )
    override fun pack(plaintext: ByteArray): ByteArray {
        // find the length of plaintext
        val plaintextLength = plaintext.size

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
        val encryptedPayload = encrypt(plaintext)
        return encryptedLengthBytes + encryptedPayload
    }

    // Encrypts the data and increments the nonce counter.
    @Throws(
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class
    )
    override fun encrypt(plaintext: ByteArray): ByteArray {
        val ivSpec: AlgorithmParameterSpec
        val nonce = nonce()
        ivSpec = GCMParameterSpec(tagSizeBits, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        return cipher.doFinal(plaintext)
    }

    // Decrypts data and increments the nonce counter.
    @Throws(
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class
    )
    override fun decrypt(encrypted: ByteArray): ByteArray {
        val ivSpec: AlgorithmParameterSpec
        val nonce = nonce()
        ivSpec = GCMParameterSpec(tagSizeBits, nonce)
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        return cipher.doFinal(encrypted)
    }

    // ShadowCipher contains the encryption and decryption methods.
    init {
        try {
            cipher = Cipher.getInstance("AES_256/GCM/NoPadding")
        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        }
    }
}