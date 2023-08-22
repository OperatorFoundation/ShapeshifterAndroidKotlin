package org.operatorfoundation.shadow

import android.util.Base64
import android.util.Log
import org.operatorfoundation.keychainandroid.*
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class DarkStar(var config: ShadowConfig, private var host: String, private var port: Int)
{
    private var sharedKeyClientToServer: SymmetricKey? = null
    private var sharedKeyServerToClient: SymmetricKey? = null
    private var clientEphemeralKeyPair: KeyPair? = null
    private var serverPersistentPublicKey: PublicKey? = null

    @Throws(
        UnknownHostException::class,
        Exception::class,
    )
    fun createHandshake(): ByteArray
    {
        // take ServerPersistentPublicKey out of password string
        val serverPersistentPublicKeyData = Base64.decode(config.password, Base64.DEFAULT)

        if (serverPersistentPublicKeyData.size != 33)
        {
            throw Exception("Invalid key size")
        }

        when (val keyType = KeyType.fromInt(serverPersistentPublicKeyData[0].toInt()))
        {
            KeyType.P256KeyAgreement ->
            {
                println("P256 Key Type found")
                // serverPersistentPublicKeyData = serverPersistentPublicKeyData.sliceArray(1 until serverPersistentPublicKeyData.size)
            }
            else -> throw Exception("Unsupported KeyType found ${keyType.name}")
        }

        this.serverPersistentPublicKey = PublicKey.new(serverPersistentPublicKeyData)
        this.clientEphemeralKeyPair = keychain.generateEphemeralKeypair(KeyType.P256KeyAgreement)

        val clientEphemeralKeyPair = this.clientEphemeralKeyPair
            ?: throw java.lang.Exception("could not find clientEphemeralKeyPair")

        val clientEphemeralPrivateKey = clientEphemeralKeyPair.privateKey
        val clientEphemeralPublicKey = clientEphemeralKeyPair.publicKey

        // convert the ephemeral public key into data and save it to the handshakeData array.
        val clientEphemeralPublicKeyData = PublicKey.publicKeyToBytesDarkstarFormat(clientEphemeralPublicKey)
        var handshakeData = clientEphemeralPublicKeyData
        val serverPersistentPublicKey = this.serverPersistentPublicKey
            ?: throw java.lang.Exception("could't find serverPersistentPublicKey")

        // Generate client confirmation code
        val clientConfirmationCode = generateClientConfirmationCode(
            host,
            port,
            serverPersistentPublicKey,
            clientEphemeralPublicKey,
            clientEphemeralPrivateKey
        )
        handshakeData += clientConfirmationCode

        return handshakeData
    }

    private fun splitHandshake(handshakeData: ByteArray, ephemeralPublicKeyBuf: ByteArray, confirmationCodeBuf: ByteArray)
    {
        if (handshakeData.size != 64)
        {
            Log.e("DarkStar", "incorrect handshake size")
        }

        System.arraycopy(handshakeData, 0, ephemeralPublicKeyBuf, 0, 32)
        System.arraycopy(handshakeData, 32, confirmationCodeBuf, 0, 32)
    }

    @Throws(
        UnknownHostException::class,
    )
    fun makeCipher(isClientToServer: Boolean, handshakeBytes: ByteArray): ShadowCipher
    {
        val serverEphemeralPublicKeyData = ByteArray(32)
        val serverConfirmationCode = ByteArray(32)
        splitHandshake(handshakeBytes, serverEphemeralPublicKeyData, serverConfirmationCode)

        // turn the server's public key data back to a public key type
        val serverEphemeralPublicKey = PublicKey.P256KeyAgreement(serverEphemeralPublicKeyData)
        val clientEphemeral = this.clientEphemeralKeyPair
            ?: throw java.lang.Exception("could not find clientEphemeralKeyPair")

        val serverPersistentPublic = this.serverPersistentPublicKey
            ?: throw java.lang.Exception("could not find serverPersistentPublicKey")

        // derive shared keys
        val sharedKey = generateSharedKey(
            isClientToServer,
            host,
            port,
            clientEphemeral,
            serverEphemeralPublicKey,
            serverPersistentPublic
        )

        if (isClientToServer)
        {
            sharedKeyClientToServer = sharedKey
        }
        else
        {
            sharedKeyServerToClient = sharedKey
        }

        // check confirmationCode
        val clientCopyServerConfirmationCode = generateServerConfirmationCode(
            host,
            port,
            clientEphemeral.publicKey,
            clientEphemeral.privateKey,
            serverPersistentPublic
        )
        if (!clientCopyServerConfirmationCode.contentEquals(serverConfirmationCode))
        {
            throw InvalidKeyException()
        }

        return if (isClientToServer)
        {
            ShadowDarkStarCipher(sharedKeyClientToServer)
        }
        else
        {
            ShadowDarkStarCipher(sharedKeyServerToClient)
        }
    }

    companion object
    {
        var keychain = Keychain()
        private const val keySize = 33
        private var darkStarBytes = "DarkStar".toByteArray()
        private var clientStringBytes = "client".toByteArray()
        private var serverStringBytes = "server".toByteArray()

        @Throws(UnknownHostException::class, NoSuchAlgorithmException::class)
        fun generateSharedKey(isClientToServer: Boolean, host: String, port: Int, clientEphemeral: KeyPair, serverEphemeralPublicKey: PublicKey, serverPersistentPublicKey: PublicKey): SymmetricKey
        {
            val ecdh1 = keychain.ecdh(clientEphemeral.privateKey, serverEphemeralPublicKey)
            val ecdh2 = keychain.ecdh(clientEphemeral.privateKey, serverPersistentPublicKey)
            val serverIdentifier = makeServerIdentifier(host, port)
            val digest = MessageDigest.getInstance("SHA-256")

            if (ecdh1 != null)
            {
                digest.update(ecdh1.data)
            }

            if (ecdh2 != null)
            {
                digest.update(ecdh2.data)
            }

            digest.update(serverIdentifier)
            digest.update(PublicKey.publicKeyToBytesDarkstarFormat(clientEphemeral.publicKey))
            digest.update(PublicKey.publicKeyToBytesDarkstarFormat(serverEphemeralPublicKey))
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

            return SymmetricKey(result)
        }

        @Throws(UnknownHostException::class)
        fun makeServerIdentifier(host: String, port: Int): ByteArray
        {
            val ip = InetAddress.getByName(host)
            val address = ip.address
            val buf = ByteBuffer.allocate(2)
            buf.putShort(port.toShort())
            val portBytes = buf.array()

            return address + portBytes
        }

        @Throws(NoSuchAlgorithmException::class, UnknownHostException::class, InvalidKeyException::class)
        fun generateServerConfirmationCode(host: String, port: Int, clientEphemeralPublicKey: PublicKey, clientEphemeralPrivateKey: PrivateKey, serverPersistentPublicKey: PublicKey): ByteArray
        {
            val serverIdentifier = makeServerIdentifier(host, port)
            val serverPersistentPublicKeyData = PublicKey.publicKeyToBytesDarkstarFormat(serverPersistentPublicKey)
            val clientEphemeralPublicKeyData = PublicKey.publicKeyToBytesDarkstarFormat(clientEphemeralPublicKey)
            val sharedSecret = keychain.ecdh(clientEphemeralPrivateKey, serverPersistentPublicKey)
            val digest = MessageDigest.getInstance("SHA-256")

            if (sharedSecret != null)
            {
                digest.update(sharedSecret.data)
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
            host: String,
            port: Int,
            serverPersistentPublicKey: PublicKey,
            clientEphemeralPublicKey: PublicKey,
            clientEphemeralPrivateKey: PrivateKey
        ): ByteArray
        {
            val sharedSecret =
                keychain.ecdh(clientEphemeralPrivateKey, serverPersistentPublicKey)
            val serverIdentifier = makeServerIdentifier(host, port)
            val serverPersistentPublicKeyData = PublicKey.publicKeyToBytesDarkstarFormat(serverPersistentPublicKey)
            val clientEphemeralPublicKeyData = PublicKey.publicKeyToBytesDarkstarFormat(clientEphemeralPublicKey)
            val digest = MessageDigest.getInstance("SHA-256")

            if (sharedSecret != null)
            {
                digest.update(sharedSecret.data)
            }

            digest.update(serverIdentifier)
            digest.update(serverPersistentPublicKeyData)
            digest.update(clientEphemeralPublicKeyData)
            digest.update(darkStarBytes)
            digest.update(clientStringBytes)

            return digest.digest()
        }
    }
}