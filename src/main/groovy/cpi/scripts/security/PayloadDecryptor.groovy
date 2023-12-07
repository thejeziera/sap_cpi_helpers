package cpi.scripts.security

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.ITApiFactory
import com.sap.it.api.keystore.KeystoreService
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

def Message processData(Message message) {
    // Retrieve key alias and keystore entry name from message headers
    String keyAlias = message.getHeader("keyAlias", String)
    String keystoreEntryName = message.getHeader("keystoreEntryName", String)

    // Retrieve the decryption key from the keystore
    byte[] keyBytes = getKeyFromKeystore(keyAlias, keystoreEntryName)

    // Check if key was retrieved successfully
    if (keyBytes == null) {
        throw new IllegalStateException("Decryption key could not be retrieved")
    }

    // Prepare the AES cipher for decryption
    SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES")
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // Adjust the algorithm/mode/padding as needed
    cipher.init(Cipher.DECRYPT_MODE, secretKey)

    // Retrieve and decrypt the payload
    String encryptedPayload = message.getBody(String)
    byte[] decodedBytes = Base64.getDecoder().decode(encryptedPayload)
    byte[] decryptedBytes = cipher.doFinal(decodedBytes)

    // Set the decrypted content as the new message body
    String decryptedPayload = new String(decryptedBytes, "UTF-8")
    message.setBody(decryptedPayload)

    return message
}

byte[] getKeyFromKeystore(String keyAlias, String keystoreEntryName) {
    try {
        KeystoreService keystoreService = ITApiFactory.getApi(KeystoreService.class, null)
        return keystoreService.getPrivateKeyEntry(keyAlias, keystoreEntryName).getPrivateKey().getEncoded()
    } catch (Exception e) {
        // Log error or handle exception
        return null
    }
}
