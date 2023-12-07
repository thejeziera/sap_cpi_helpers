package cpi.scripts.utilities

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.securestore.SecureStoreService
import com.sap.it.api.securestore.UserCredential
import com.sap.it.api.ITApiFactory
import com.sap.it.api.securestore.exception.SecureStoreException

def Message processData(Message message) {
    try {
        // Get the SecureStoreService API
        SecureStoreService secureStoreService = ITApiFactory.getApi(SecureStoreService.class, null)
        if (secureStoreService == null) {
            throw new IllegalStateException("SecureStoreService is not available")
        }

        // Retrieve User Credentials
        String userCredAlias = "YourUserCredentialAlias" // Replace with your actual alias
        UserCredential userCredentials = secureStoreService.getUserCredential(userCredAlias)
        if (userCredentials != null) {
            String username = userCredentials.getUsername()
            String password = userCredentials.getPassword()
            message.setProperty("username", username)
            message.setProperty("password", password)
        }

        // Retrieve OAuth2 Client Credentials
        String oauth2Alias = "YourOAuth2CredentialAlias" // Replace with your actual alias
        Map<String, String> oauth2Credentials = secureStoreService.getOAuth2ClientCredentials(oauth2Alias)
        if (oauth2Credentials != null) {
            String clientId = oauth2Credentials.get("client_id")
            String clientSecret = oauth2Credentials.get("client_secret")
            message.setProperty("clientId", clientId)
            message.setProperty("clientSecret", clientSecret)
        }

        // Add more credential types as needed

    } catch (SecureStoreException e) {
        message.setHeader("ScriptException", e.getMessage())
        throw e
    }

    return message
}
