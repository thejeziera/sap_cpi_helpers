package cpi.scripts.utilities

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.securestore.SecureStoreService
import com.sap.it.api.securestore.UserCredential
import com.sap.it.api.ITApiFactory

def Message processData(Message message) {
    // --- Step 1: Read logMode from properties; default to "NONE" ---
    def properties = message.getProperties()
    def logMode = (properties.get("logMode") ?: "NONE") as String

    // --- Step 2: Read credentialAlias from properties ---
    def alias = properties.get("credentialAlias") as String

    // --- Step 3: Validate credentialAlias — throw if null or blank ---
    if (!alias) {
        throw new IllegalArgumentException("credentialAlias property is required but was not set")
    }

    // --- Step 4: Obtain messageLog (null-safe; standard CPI MPL plumbing) ---
    def messageLog = messageLogFactory.getMessageLog(message)

    try {
        // --- Step 5: Retrieve SecureStoreService via ITApiFactory ---
        def secureStoreService = ITApiFactory.getApi(SecureStoreService.class, null)

        // --- Step 6: Validate SecureStoreService is not null ---
        if (secureStoreService == null) {
            throw new IllegalStateException("SecureStoreService is not available")
        }

        // --- Step 7: Look up the UserCredential by alias ---
        def credential = secureStoreService.getUserCredential(alias)

        // --- Step 8: Validate the returned credential is not null ---
        if (credential == null) {
            throw new IllegalStateException("No credential found for alias: " + alias)
        }

        // --- Steps 9–12: Extract username and password; set as message properties ---
        def username = credential.getUsername()
        // new String() safely handles both String and char[] return types from getPassword()
        def password = new String(credential.getPassword())
        message.setProperty("username", username)
        message.setProperty("password", password)

        // --- Step 13: Log credentialAlias to MPL if logMode is INFO ---
        if ("INFO" == logMode) {
            messageLog?.addCustomHeaderProperty("credentialAlias", alias)
        }

    } catch (Exception e) {
        // Re-throw all exceptions unchanged — no header mutation on error
        throw e
    }

    // --- Step 14: Return message; payload is not modified ---
    return message
}
