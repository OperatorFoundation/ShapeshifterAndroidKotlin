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

import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.SecretKey

// ShadowCipher contains the encryption and decryption methods.
abstract class ShadowCipher
{
    lateinit var config: ShadowConfig
    lateinit var salt: ByteArray
    lateinit var cipher: Cipher

    var tagSizeBits = 16 * 8
    open var key: SecretKey? = null
    var counter = 0

    companion object
    {
        var finalSaltSize = 0
        var tagSize = 16
        var lengthWithTagSize = 2 + 16
        var maxPayloadSize = 16417

        // Creates a byteArray of a specified length containing random bytes.
        fun createSalt(config: ShadowConfig): ByteArray
        {
            val saltSize: Int = when (config.cipherMode)
            {
                CipherMode.DarkStar -> 32
            }

            val salt = ByteArray(saltSize)
            val random = java.security.SecureRandom()
            random.nextBytes(salt)

            return salt
        }

        fun determineSaltSize(): Int
        {
            finalSaltSize = 64
            return finalSaltSize
        }
    }

    // Create a secret key using the two key derivation functions.
    @Throws(NoSuchAlgorithmException::class)
    abstract fun createSecretKey(config: ShadowConfig, salt: ByteArray): SecretKey

    // Key derivation functions:
    // Derives the secret key from the preshared key and adds the salt.
    abstract fun hkdfSha1(config: ShadowConfig, salt: ByteArray, psk: ByteArray): SecretKey

    // Derives the pre-shared key from the config.
    @Throws(NoSuchAlgorithmException::class)
    abstract fun kdf(config: ShadowConfig): ByteArray

    // [encrypted payload length][length tag] + [encrypted payload][payload tag]
    // Pack takes the data above and packs them into a singular byte array.
    @Throws(
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class
    )
    abstract fun pack(plaintext: ByteArray): ByteArray

    // Encrypts the data and increments the nonce counter.
    @Throws(
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class
    )
    abstract fun encrypt(plaintext: ByteArray): ByteArray

    // Decrypts data and increments the nonce counter.
    @Throws(
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class
    )

    abstract fun decrypt(encrypted: ByteArray): ByteArray

    // Create a nonce using our counter.
    //@ExperimentalUnsignedTypes
    abstract fun nonce(): ByteArray?
}

// CipherMode establishes what algorithm and version you are using.
enum class CipherMode
{
    //  AES 196 is not currently supported by go-shadowsocks2.
    //  We are not supporting it at this time either.
    DarkStar
}
