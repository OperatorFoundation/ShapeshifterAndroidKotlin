package org.operatorfoundation.shapeshifter.shadow.kotlin

import javax.crypto.SecretKey

class ShadowChaChaCipher(config: ShadowConfig): ShadowCipher(config)
{
    init
    {
        ShadowChaChaCipher(config, createSalt(config))
    }

    constructor(config: ShadowConfig, salt: ByteArray) : this(config)
    {
        //
    }

    override fun createSecretKey(config: ShadowConfig?, salt: ByteArray?): SecretKey {
        TODO("Not yet implemented")
    }

    override fun hkdfSha1(config: ShadowConfig?, salt: ByteArray?, psk: ByteArray?): SecretKey {
        TODO("Not yet implemented")
    }

    override fun kdf(config: ShadowConfig?): ByteArray {
        TODO("Not yet implemented")
    }

    override fun pack(plaintext: ByteArray?): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun encrypt(plaintext: ByteArray?): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun decrypt(encrypted: ByteArray?): ByteArray? {
        TODO("Not yet implemented")
    }
}