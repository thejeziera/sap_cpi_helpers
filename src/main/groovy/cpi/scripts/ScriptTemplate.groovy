package cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    def errorCode = message.getProperty("p_error_code") as Integer
    def messageText = message.getBody() as String
    if (errorCode != null && (errorCode >= 400 && errorCode < 600)) {
        messageText = "HTTP Error ${errorCode} occurred during processing."
        errorCode = "HTTP_${errorCode}"
    }
    message.setProperty("p_error_code", errorCode)
    message.setBody(messageText)
    return message
}