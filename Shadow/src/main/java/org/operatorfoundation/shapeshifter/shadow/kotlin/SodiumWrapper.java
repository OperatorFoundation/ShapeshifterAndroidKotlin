package org.operatorfoundation.shapeshifter.shadow.kotlin;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;
import org.libsodium.jni.SodiumConstants;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

// https://doc.libsodium.org/public-key_cryptography/authenticated_encryption
public class SodiumWrapper
{
    public SodiumWrapper(){
        NaCl.sodium(); // required to load the native C library
    }

    public byte[] encrypt(byte[] messageBytes, byte[] nonce, byte[] key)
    {
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

        // Return nonce + cipher text
        byte[] fullMessage = new byte[nonce.length + ciphertext.length];
        System.arraycopy(nonce, 0, fullMessage, 0, nonce.length);
        System.arraycopy(ciphertext, 0, fullMessage, nonce.length, ciphertext.length);

        return fullMessage;
    }

    public byte[] decrypt(byte[] encrypted, byte[] nonce, byte[] key) throws InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
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

//    public byte[] decrypt(byte[] encryptedBytes, byte[] senderPublicKey, byte[] receiverPrivateKey)
//    {
//        // Get the nonce from the encrypted bytes
//        byte[] nonce = new byte[SodiumConstants.NONCE_BYTES];
//        System.arraycopy(encryptedBytes, 0, nonce, 0, nonce.length);
//
//        // get the cipher text from the encrypted bytes
//        byte[] cipherText = new byte[encryptedBytes.length - nonce.length];
//        System.arraycopy(encryptedBytes, nonce.length, cipherText, 0, cipherText.length);
//
//        // container for the decrypt results
//        byte[] decryptedMessageBytes = new byte[(int) (cipherText.length - Sodium.crypto_box_macbytes())];
//
//
//        Sodium.crypto_box_open_easy(
//                decryptedMessageBytes,
//                cipherText,
//                cipherText.length,
//                nonce,
//                senderPublicKey,
//                receiverPrivateKey
//        );
//
//        return decryptedMessageBytes;
//    }
}
