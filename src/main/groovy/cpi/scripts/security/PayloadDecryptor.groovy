package cpi.scripts.security

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.ITApiFactory
import com.sap.it.api.securestore.SecureStoreService
import com.sap.it.api.securestore.UserCredential
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Arrays
import java.util.Base64

def Message processData(Message message) {
    def properties = message.getProperties()

    // --- Step 1: Read logMode from properties; default "NONE" ---
    def logMode = (properties.get("logMode") ?: "NONE") as String

    // --- Step 2: Read and validate enc_credentialAlias ---
    def alias = properties.get("enc_credentialAlias") as String
    if (!alias) {
        throw new IllegalArgumentException("enc_credentialAlias property is required but was not set")
    }

    // --- Step 3: Obtain messageLog ---
    def messageLog = messageLogFactory.getMessageLog(message)

    // --- Step 4: Get SecureStoreService; validate not null ---
    def secureStoreService = ITApiFactory.getApi(SecureStoreService.class, null)
    if (secureStoreService == null) {
        throw new IllegalStateException("SecureStoreService is not available")
    }

    // --- Step 5: Get UserCredential by alias; validate not null ---
    def credential = secureStoreService.getUserCredential(alias)
    if (credential == null) {
        throw new IllegalStateException("No credential found for alias: " + alias)
    }

    // --- Step 6: Decode the Base64-encoded AES key from the credential password ---
    def rawPass = new String(credential.getPassword())
    def keyBytes = Base64.getDecoder().decode(rawPass.trim())

    // --- Step 7: Read and validate the encrypted body ---
    def body = message.getBody() as String
    if (!body) {
        throw new IllegalArgumentException("Encrypted payload is missing or blank")
    }

    // --- Step 8: Base64-decode the body to get IV+ciphertext bytes ---
    def rawEncryptedBytes = Base64.getDecoder().decode(body)

    // --- Step 9: Validate that the decoded bytes are longer than the 16-byte IV ---
    if (rawEncryptedBytes.length <= 16) {
        throw new IllegalArgumentException("Encrypted payload is too short to contain an IV")
    }

    // --- Step 10: Extract IV (first 16 bytes) ---
    def iv = Arrays.copyOf(rawEncryptedBytes, 16)

    // --- Step 11: Extract ciphertext (bytes after the IV) ---
    def ciphertext = Arrays.copyOfRange(rawEncryptedBytes, 16, rawEncryptedBytes.length)

    // --- Step 12: Initialise AES/CBC cipher in DECRYPT_MODE using the extracted IV ---
    def secretKey = new SecretKeySpec(keyBytes, "AES")
    def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv))

    // --- Step 13-14: Decrypt and decode to UTF-8 string ---
    def decryptedBytes = cipher.doFinal(ciphertext)
    def decryptedPayload = new String(decryptedBytes, "UTF-8")

    // --- Step 15: Set decrypted plaintext as message body ---
    message.setBody(decryptedPayload)

    // --- Step 16: Log diagnostics if logMode is INFO ---
    if ("INFO" == logMode) {
        messageLog?.addCustomHeaderProperty("enc_credentialAlias", alias)
        messageLog?.addCustomHeaderProperty("enc_encryptedLength", String.valueOf(rawEncryptedBytes.length))
    }

    return message
}
