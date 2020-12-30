package org.operatorfoundation.shapeshifter.shadow.kotlin;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;

public class SodiumWrapper {
    public SodiumWrapper() {
        NaCl.sodium(); // required to load the native C library
    }

    public byte[] encrypt(byte[] messageBytes, byte[] nonce, byte[] key) {
        byte[] ciphertext = new byte[messageBytes.length + Sodium.crypto_aead_chacha20poly1305_ietf_abytes()];
        int[] ciphertext_length = {};
        byte[] additional = new byte[0];
        int additional_length = 0;
        byte[] nsec = new byte[0];

        //This function writes the authentication tag, whose length is crypto_box_MACBYTES bytes, in cipherText,
        // immediately followed by the encrypted message, whose length is the same as the messageBytes
        Sodium.crypto_aead_chacha20poly1305_ietf_encrypt(
                ciphertext, ciphertext_length,
                messageBytes, messageBytes.length,
                additional, additional_length, nsec, nonce, key);

        return ciphertext;
    }

    public byte[] decrypt(byte[] encrypted, byte[] nonce, byte[] key) {
        byte[] additional = new byte[0];
        int additional_length = 0;

        int[] plaintext_length = {encrypted.length - Sodium.crypto_aead_chacha20poly1305_ietf_abytes()};
        byte[] plaintext = new byte[plaintext_length[0]];
        byte[] nsec = new byte[0];

        Sodium.crypto_aead_chacha20poly1305_ietf_decrypt(
                plaintext, plaintext_length,
                nsec,
                encrypted, encrypted.length,
                additional, additional_length,
                nonce, key
        );

        return plaintext;
    }
}
