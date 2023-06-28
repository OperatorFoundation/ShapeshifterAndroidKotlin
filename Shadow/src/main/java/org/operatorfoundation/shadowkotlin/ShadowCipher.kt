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

package org.operatorfoundation.shadowkotlin

import org.operatorfoundation.keychainandroid.SymmetricKey
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException

// ShadowCipher contains the encryption and decryption methods.
abstract class ShadowCipher
{
    lateinit var config: ShadowConfig
    lateinit var cipher: Cipher

    open var key: SymmetricKey? = null

    companion object
    {
        var tagSize = 16
        var tagSizeBits = tagSize * 8
        var lengthWithTagSize = 2 + tagSize
        var maxPayloadSize = 16417
        val handshakeSize = 64
    }

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
