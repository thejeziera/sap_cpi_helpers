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

    // Retrieve the key from the keystore
    byte[] keyBytes = getKeyFromKeystore(keyAlias, keystoreEntryName)

    // Check if key was retrieved successfully
    if (keyBytes == null) {
        throw new IllegalStateException("Encryption key could not be retrieved")
    }

    // Prepare the AES cipher
    SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES")
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding") // Adjust the algorithm/mode/padding as needed
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)

    // Retrieve and encrypt the payload
    String payload = message.getBody(String)
    byte[] encryptedBytes = cipher.doFinal(payload.getBytes("UTF-8"))

    // Encode the encrypted bytes to Base64 and set as the new message body
    String encryptedPayload = Base64.getEncoder().encodeToString(encryptedBytes)
    message.setBody(encryptedPayload)

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

