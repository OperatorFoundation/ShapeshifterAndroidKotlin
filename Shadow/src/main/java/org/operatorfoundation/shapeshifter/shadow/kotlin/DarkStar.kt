package org.operatorfoundation.shapeshifter.shadow.kotlin

import android.util.Log
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.security.*
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class DarkStar(var config: ShadowConfig, private var host: String, private var port: Int) {
    private var sharedKeyClientToServer: SecretKey? = null
    private var sharedKeyServerToClient: SecretKey? = null
    private var clientEphemeralKeyPair: KeyPair? = null
    private var serverPersistentPublicKey: PublicKey? = null

    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeySpecException::class,
        UnknownHostException::class,
        NoSuchProviderException::class
    )
    fun createSalt(): ByteArray {
        // take ServerPersistentPublicKey out of password string
        val serverPersistentPublicKeyData = hexToBytes(config.password)
        serverPersistentPublicKey = bytesToPublicKey(serverPersistentPublicKeyData)

        // generate an ephemeral keypair
        clientEphemeralKeyPair = generateECKeys()

        // get the client private and public key
        var clientEphemeralPrivateKey: PrivateKey? = null
        var clientEphemeralPublicKey: PublicKey? = null
        if (clientEphemeralKeyPair != null) {
            clientEphemeralPrivateKey = clientEphemeralKeyPair!!.private
            clientEphemeralPublicKey = clientEphemeralKeyPair!!.public
        }

        // convert the public key into data to be sent to the server
        val clientEphemeralPublicKeyData = publicKeyToBytes(clientEphemeralPublicKey)
        var salt = clientEphemeralPublicKeyData

        // Generate client confirmation code
        val clientConfirmationCode = generateClientConfirmationCode(
            host,
            port,
            serverPersistentPublicKey,
            clientEphemeralPublicKey,
            clientEphemeralPrivateKey
        )
        salt += clientConfirmationCode

        return salt
    }

    private fun splitSalt(
        salt: ByteArray,
        ephemeralPublicKeyBuf: ByteArray,
        confirmationCodeBuf: ByteArray,
    ) {
        if (salt.size != 64) {
            Log.e("DarkStar", "incorrect salt size")
        }
        System.arraycopy(salt, 0, ephemeralPublicKeyBuf, 0, 32)
        System.arraycopy(salt, 32, confirmationCodeBuf, 0, 32)
    }

    @Throws(
        InvalidKeySpecException::class,
        NoSuchAlgorithmException::class,
        UnknownHostException::class,
        InvalidKeyException::class
    )
    fun makeCipher(isClientToServer: Boolean, salt: ByteArray): ShadowCipher {
        val serverEphemeralPublicKeyData = ByteArray(32)
        val serverConfirmationCode = ByteArray(32)
        splitSalt(salt, serverEphemeralPublicKeyData, serverConfirmationCode)

        // turn the server's public key data back to a public key type
        val serverEphemeralPublicKey = bytesToPublicKey(serverEphemeralPublicKeyData)

        // derive shared keys
        val sharedKey = generateSharedKey(
            isClientToServer,
            host,
            port,
            clientEphemeralKeyPair,
            serverEphemeralPublicKey,
            serverPersistentPublicKey
        )

        if (isClientToServer) {
            sharedKeyClientToServer = sharedKey
        } else {
            sharedKeyServerToClient = sharedKey
        }

        // check confirmationCode
        val clientCopyServerConfirmationCode = generateServerConfirmationCode(
            host,
            port,
            clientEphemeralKeyPair!!.public,
            clientEphemeralKeyPair!!.private,
            serverPersistentPublicKey!!
        )
        if (!clientCopyServerConfirmationCode.contentEquals(serverConfirmationCode)) {
            throw InvalidKeyException()
        }

        return if (isClientToServer)
        {
            ShadowDarkStarCipher(sharedKeyClientToServer!!)
        }
        else
        {
            ShadowDarkStarCipher(sharedKeyServerToClient)
        }
    }

    companion object {
        private var darkStarBytes = "DarkStar".toByteArray()
        private var clientStringBytes = "client".toByteArray()
        private var serverStringBytes = "server".toByteArray()
        fun generateECKeys(): KeyPair? {
            return try {
                val parameterSpec = ECNamedCurveTable.getParameterSpec("secp256r1")
                val keyPairGenerator = KeyPairGenerator.getInstance(
                    "EC", BouncyCastleProvider()
                )
                keyPairGenerator.initialize(parameterSpec)
                keyPairGenerator.generateKeyPair()
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
                null
            } catch (e: InvalidAlgorithmParameterException) {
                e.printStackTrace()
                null
            }
        }

        fun loadECKeys(privateKeyString: String, publicKeyString: String): KeyPair? {
            return try {
                val keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider())
                val privateKeyBytes = hexToBytes(privateKeyString)
                val spec = PKCS8EncodedKeySpec(privateKeyBytes)
                val privateKey = keyFactory.generatePrivate(spec)
                val publicKeyBytes = hexToBytes(publicKeyString)
                val publicKey = bytesToPublicKey(publicKeyBytes)
                KeyPair(publicKey, privateKey)
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
                null
            } catch (e: InvalidKeySpecException) {
                e.printStackTrace()
                null
            }
        }

        private fun generateSharedSecret(
            privateKey: PrivateKey?,
            publicKey: PublicKey?
        ): SecretKey? {
            return try {
                val keyAgreement =
                    KeyAgreement.getInstance("ECDH", BouncyCastleProvider())
                keyAgreement.init(privateKey)
                keyAgreement.doPhase(publicKey, true)
                keyAgreement.generateSecret("secp256r1")
            } catch (e: InvalidKeyException) {
                e.printStackTrace()
                null
            } catch (e: NoSuchAlgorithmException) {
                e.printStackTrace()
                null
            }
        }

        @Throws(UnknownHostException::class, NoSuchAlgorithmException::class)
        fun generateSharedKey(isClientToServer: Boolean, host: String?, port: Int, clientEphemeral: KeyPair?, serverEphemeralPublicKey: PublicKey?, serverPersistentPublicKey: PublicKey?): SecretKey
        {
            val ecdh1 = generateSharedSecret(clientEphemeral!!.private, serverEphemeralPublicKey)
            val ecdh2 = generateSharedSecret(clientEphemeral.private, serverPersistentPublicKey)
            val serverIdentifier = makeServerIdentifier(host, port)
            val digest = MessageDigest.getInstance("SHA-256")

            if (ecdh1 != null)
            {
                digest.update(ecdh1.encoded)
            }

            if (ecdh2 != null)
            {
                digest.update(ecdh2.encoded)
            }

            digest.update(serverIdentifier)
            digest.update(publicKeyToBytes(clientEphemeral.public))
            digest.update(publicKeyToBytes(serverEphemeralPublicKey))
            digest.update(darkStarBytes)

            if (isClientToServer)
            {
                digest.update(serverStringBytes)
            }
            else
            {
                digest.update(clientStringBytes)
            }

            val result = digest.digest()

            return SecretKeySpec(result, 0, result.size, "AES")
        }

        @Throws(UnknownHostException::class)
        fun makeServerIdentifier(host: String?, port: Int): ByteArray
        {
            val ip = InetAddress.getByName(host)
            val address = ip.address
            val buf = ByteBuffer.allocate(2)
            buf.putShort(port.toShort())
            val portBytes = buf.array()

            return address + portBytes
        }

        @Throws(NoSuchAlgorithmException::class, UnknownHostException::class, InvalidKeyException::class)
        fun generateServerConfirmationCode(host: String?, port: Int, clientEphemeralPublicKey: PublicKey, clientEphemeralPrivateKey: PrivateKey, serverPersistentPublicKey: PublicKey): ByteArray
        {
            val serverIdentifier = makeServerIdentifier(host, port)
            val serverPersistentPublicKeyData = publicKeyToBytes(serverPersistentPublicKey)
            val clientEphemeralPublicKeyData = publicKeyToBytes(clientEphemeralPublicKey)
            val sharedSecret = generateSharedSecret(clientEphemeralPrivateKey, serverPersistentPublicKey)
            val digest = MessageDigest.getInstance("SHA-256")

            if (sharedSecret != null)
            {
                digest.update(sharedSecret.encoded)
            }

            digest.update(serverIdentifier)
            digest.update(serverPersistentPublicKeyData)
            digest.update(clientEphemeralPublicKeyData)
            digest.update(darkStarBytes)
            digest.update(serverStringBytes)

            return digest.digest()
        }

        @Throws(NoSuchAlgorithmException::class, UnknownHostException::class)
        fun generateClientConfirmationCode(
            host: String?,
            port: Int,
            serverPersistentPublicKey: PublicKey?,
            clientEphemeralPublicKey: PublicKey?,
            clientEphemeralPrivateKey: PrivateKey?
        ): ByteArray
        {
            val sharedSecret =
                generateSharedSecret(clientEphemeralPrivateKey, serverPersistentPublicKey)
            val serverIdentifier = makeServerIdentifier(host, port)
            val serverPersistentPublicKeyData = publicKeyToBytes(serverPersistentPublicKey)
            val clientEphemeralPublicKeyData = publicKeyToBytes(clientEphemeralPublicKey)
            val digest = MessageDigest.getInstance("SHA-256")

            if (sharedSecret != null)
            {
                digest.update(sharedSecret.encoded)
            }

            digest.update(serverIdentifier)
            digest.update(serverPersistentPublicKeyData)
            digest.update(clientEphemeralPublicKeyData)
            digest.update(darkStarBytes)
            digest.update(clientStringBytes)

            return digest.digest()
        }

        fun publicKeyToBytes(pubKey: PublicKey?): ByteArray {
            val bcecPublicKey = pubKey as BCECPublicKey
            val point = bcecPublicKey.q
            val encodedPoint = point.getEncoded(true)
            val result = ByteArray(32)
            System.arraycopy(encodedPoint, 1, result, 0, 32)
            return result
        }

        @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
        fun bytesToPublicKey(bytes: ByteArray): PublicKey {
            val keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider())
            val ecSpec: ECParameterSpec = ECNamedCurveTable.getParameterSpec("secp256r1")
            val encodedPoint = ByteArray(33)
            System.arraycopy(bytes, 0, encodedPoint, 1, 32)
            encodedPoint[0] = 3
            val point = ecSpec.curve.decodePoint(encodedPoint)
            val pubSpec = ECPublicKeySpec(point, ecSpec)
            return keyFactory.generatePublic(pubSpec)
        }
    }
}