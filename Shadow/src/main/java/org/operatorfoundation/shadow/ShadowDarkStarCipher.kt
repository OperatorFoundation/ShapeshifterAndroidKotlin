package org.operatorfoundation.shadow

import android.os.Build
import org.bouncycastle.jce.provider.BouncyCastleProvider

import org.operatorfoundation.keychainandroid.Keychain
import org.operatorfoundation.keychainandroid.SealedBox
import org.operatorfoundation.keychainandroid.SymmetricKey
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import javax.crypto.*

class ShadowDarkStarCipher(override var key: SymmetricKey?) : ShadowCipher()
{
    val keychain = Keychain()
    val encryptionCipher = SealedBox.AESGCM()
    val decryptionCipher = SealedBox.AESGCM()

    var longCounter: ULong = 0u

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
        val nonce = nonce() ?: throw Exception("failed to create nonce")
        val symmetricKey = key ?: throw Exception("symmetric key was not saved")
        return encryptionCipher.seal(nonce, symmetricKey, plaintext)
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
        val nonce = nonce() ?: throw Exception("failed to create nonce")
        val symmetricKey = key ?: throw Exception("symmetric key was not saved")

        return decryptionCipher.open(nonce, symmetricKey, encrypted)
    }

    // Create a nonce using our counter.
    //@ExperimentalUnsignedTypes
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

        if (longCounter < ULong.MAX_VALUE) {
            longCounter += 1u
        } else {
            throw CounterOverFlowException("nonce counter overflow")
        }

        return buffer.array()
    }

    // ShadowCipher contains the encryption and decryption methods.
    init {
        try {
            cipher = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            {
                Cipher.getInstance("AES/GCM/NoPadding", BouncyCastleProvider())
            }
            else
            {
                Cipher.getInstance("AES_256/GCM/NoPadding")
            }

        } catch (e: NoSuchPaddingException) {
            e.printStackTrace()
        }
    }
}

class CounterOverFlowException(message:String): Exception(message)