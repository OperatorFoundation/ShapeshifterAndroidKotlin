package org.operatorfoundation.shapeshifter.shadow.kotlin

import android.os.Build
import android.util.Log
import org.bouncycastle.jcajce.spec.AEADParameterSpec
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec

class ShadowDarkStarCipher(override var key: SecretKey?) : ShadowCipher() {
    var longCounter: ULong = 0u

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
        if (plaintextLength > Short.MAX_VALUE) {
            throw IllegalBlockSizeException()
        }

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
        IllegalBlockSizeException::class,
        CounterOverFlowException::class
    )
    override fun encrypt(plaintext: ByteArray): ByteArray {
        val ivSpec: AlgorithmParameterSpec
        val nonce = nonce()
        ivSpec = AEADParameterSpec(nonce, tagSizeBits)

        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val ciphertext = cipher.doFinal(plaintext)

        print("\nEnrypting some bytes:")

        val keyHex = key!!.encoded.toHexString()
        println("key: $keyHex")

        val nonceHex = nonce!!.toHexString()
        println("nonce: $nonceHex")

        val cipherHex = ciphertext.toHexString()
        println("encrypted: $cipherHex")

        return ciphertext
    }

    // Decrypts data and increments the nonce counter.
    @Throws(
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class,
        CounterOverFlowException::class
    )
    override fun decrypt(encrypted: ByteArray): ByteArray {
        print("\nDecrypting some bytes:")
        val ivSpec: AlgorithmParameterSpec
        val nonce = nonce()
        ivSpec = AEADParameterSpec(nonce, tagSizeBits)
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)

        val keyHex = key!!.encoded.toHexString()
        println("key: $keyHex")
        val nonceHex = nonce!!.toHexString()
        println("nonce: $nonceHex")
        println("ciphertext: ${encrypted.toHexString()}")

        return cipher.doFinal(encrypted)
    }

    // Create a nonce using our counter.
    @ExperimentalUnsignedTypes
    @Throws(CounterOverFlowException::class)
    override fun nonce(): ByteArray? {
        // NIST Special Publication 800-38D - Recommendation for Block Cipher Modes of Operation: Galois/Counter Mode (GCM) and GMAC
        // https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38d.pdf
        // Section 8.2.1 - Deterministic Construction
        // Applicable to nonces of 96 bytes or less.

        /*
         In the deterministic construction, the IV is the concatenation of two
         fields, called the fixed field and the invocation field. The fixed field
         shall identify the device, or, more generally, the context for the
         instance of the authenticated encryption function. The invocation field
         shall identify the sets of inputs to the authenticated encryption
         function in that particular device.

         For any given key, no two distinct devices shall share the same fixed
         field, and no two distinct sets of inputs to any single device shall
         share the same invocation field. Compliance with these two requirements
         implies compliance with the uniqueness requirement on IVs in Sec. 8.

         If desired, the fixed field itself may be constructed from two or more
         smaller fields. Moreover, one of those smaller fields could consist of
         bits that are arbitrary (i.e., not necessarily deterministic nor unique
         to the device), as long as the remaining bits ensure that the fixed
         field is not repeated in its entirety for some other device with the
         same key.

         Similarly, the entire fixed field may consist of arbitrary bits when
         there is only one context to identify, such as when a fresh key is
         limited to a single session of a communications protocol. In this case,
         if different participants in the session share a common fixed field,
         then the protocol shall ensure that the invocation fields are distinct
         for distinct data inputs.
        */


        val buffer = ByteBuffer.allocate(12) // 4 bytes = 32 bits

        // nonce is big Endian
        buffer.order(ByteOrder.BIG_ENDIAN)
        // create a byte array from counter

        buffer.put(0x1A.toByte())
        buffer.put(0x1A.toByte())
        buffer.put(0x1A.toByte())
        buffer.put(0x1A.toByte())
        /*
         The invocation field typically is either 1) an integer counter or 2) a
         linear feedback shift register that is driven by a primitive polynomial
         to ensure a maximal cycle length. In either case, the invocation field
         increments upon each invocation of the authenticated encryption
         function.

         The lengths and positions of the fixed field and the invocation field
         shall be fixed for each supported IV length for the life of the key. In
         order to promote interoperability for the default IV length of 96 bits,
         this Recommendation suggests, but does not require, that the leading
         (i.e., leftmost) 32 bits of the IV hold the fixed field; and that the
         trailing (i.e., rightmost) 64 bits hold the invocation field.
        */

        buffer.putLong(longCounter.toLong())
        Log.i("nonce", "Nonce created. Counter is $longCounter.")
        if (longCounter < ULong.MAX_VALUE) {
            longCounter += 1u
        } else {
            throw CounterOverFlowException("64 bit nonce counter overflow")
        }

        return buffer.array()
    }

    // ShadowCipher contains the encryption and decryption methods.
    init {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            {
                cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC")
            }
            else
            {
                cipher = Cipher.getInstance("AES/GCM/NoPadding")
            }

        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        }
    }
}

class CounterOverFlowException(message:String): Exception(message)