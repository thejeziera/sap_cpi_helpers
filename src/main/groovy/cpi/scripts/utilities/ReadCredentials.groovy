package cpi.scripts.utilities

import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    def service = ITApiFactory.getApi(SecureStoreService.class, null)
    def credential = service.getUserCredential("CredentialName")

    if(credential == null) {
        throw new IllegalStateException("CredentialName credential doesn't exist. Unable to send logs.")
    } else {
        message.setHeader("Authorization", credential.getPassword() as String)
    }

    return message
}
