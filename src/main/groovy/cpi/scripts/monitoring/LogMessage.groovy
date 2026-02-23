package cpi.scripts.monitoring

import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)

    // Log each message header as HEADER_<key> with value-only string (null-safe)
    message.getHeaders().each {
        entry ->
            messageLog?.addCustomHeaderProperty("HEADER_" + entry.key, entry.value?.toString() ?: "")
    }

    // Log each message property as PROPERTY_<key> with value-only string (null-safe)
    message.getProperties().each {
        entry ->
            messageLog?.addCustomHeaderProperty("PROPERTY_" + entry.key, entry.value?.toString() ?: "")
    }

    // Attach the message body; use public API getBody() and handle null body
    messageLog?.addAttachmentAsString("Message Body", message.getBody()?.toString() ?: "", "text/plain")

    return message
}
