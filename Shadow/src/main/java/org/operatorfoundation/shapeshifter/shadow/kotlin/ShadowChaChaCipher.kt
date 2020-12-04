package org.operatorfoundation.shapeshifter.shadow.kotlin

import org.bouncycastle.crypto.digests.SHA1Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import org.libsodium.jni.NaCl
import java.security.MessageDigest
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class ShadowChaChaCipher : ShadowCipher {
    constructor(config: ShadowConfig) {
        this.config = config
        createSalt(config)
    }

    constructor(config: ShadowConfig, salt: ByteArray) {
        NaCl.sodium()

        this.config = config
        this.salt = salt

        key = createSecretKey(config, salt)
    }

    // Create a secret key using the two key derivation functions.
    override fun createSecretKey(config: ShadowConfig, salt: ByteArray): SecretKey {
        val presharedKey = kdf(config)
        return hkdfSha1(config, salt, presharedKey)
    }

    // Key derivation functions:
    // Derives the secret key from the preshared key and adds the salt.
    override fun hkdfSha1(config: ShadowConfig, salt: ByteArray, psk: ByteArray): SecretKey {
        val keyAlgorithm = "ChaCha20"
        val infoString = "ss-subkey"
        val info = infoString.toByteArray()
        val okm = ByteArray(psk.size)

        val hkdf = HKDFBytesGenerator(SHA1Digest())
        hkdf.init(HKDFParameters(psk, salt, info))
        hkdf.generateBytes(okm, 0, psk.size)

        return SecretKeySpec(okm, keyAlgorithm)
    }

    // Derives the pre-shared key from the config.
    override fun kdf(config: ShadowConfig): ByteArray {
        val hash = MessageDigest.getInstance("MD5")
        var buffer: ByteArray = byteArrayOf()
        var prev: ByteArray = byteArrayOf()
        val keyLen = 32

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
        val nonce = nonce()
        val key = key?.encoded
        counter += 1
        return SodiumWrapper().encrypt(plaintext, nonce, key)
    }

    override fun decrypt(encrypted: ByteArray): ByteArray {
        val nonce = nonce()
        val key = key?.encoded
        counter += 1
        return SodiumWrapper().decrypt(encrypted, nonce, key)
    }
}