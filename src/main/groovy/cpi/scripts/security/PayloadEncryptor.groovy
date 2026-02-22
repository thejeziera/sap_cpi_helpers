package cpi.scripts.security

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.ITApiFactory
import com.sap.it.api.securestore.SecureStoreService
import com.sap.it.api.securestore.UserCredential
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
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

    // --- Step 7: Read plaintext payload as bytes ---
    def payloadBytes = (message.getBody() as String).getBytes("UTF-8")

    // --- Step 8: Generate a cryptographically random 16-byte IV ---
    def iv = new byte[16]
    new SecureRandom().nextBytes(iv)

    // --- Step 9: Initialise AES/CBC cipher in ENCRYPT_MODE with the random IV ---
    def secretKey = new SecretKeySpec(keyBytes, "AES")
    def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv))

    // --- Step 10: Encrypt the payload ---
    def ciphertext = cipher.doFinal(payloadBytes)

    // --- Step 11: Prepend IV to ciphertext — output = IV[16] || ciphertext ---
    def combined = new byte[16 + ciphertext.length]
    System.arraycopy(iv, 0, combined, 0, 16)
    System.arraycopy(ciphertext, 0, combined, 16, ciphertext.length)

    // --- Step 12: Base64-encode the combined IV+ciphertext and set as body ---
    def encryptedPayload = Base64.getEncoder().encodeToString(combined)
    message.setBody(encryptedPayload)

    // --- Step 14: Log diagnostics if logMode is INFO ---
    if ("INFO" == logMode) {
        messageLog?.addCustomHeaderProperty("enc_credentialAlias", alias)
        messageLog?.addCustomHeaderProperty("enc_payloadLength", String.valueOf(payloadBytes.length))
    }

    return message
}
