package cpi.scripts.monitoring

import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)

    message.getHeaders().each{
        entry ->
            messageLog?.addCustomHeaderProperty("HEADER_" + entry.key, entry.value)
    }

    message.getProperties().each{
        entry ->
            messageLog?.addCustomHeaderProperty("PROPERTY_" + entry.key, entry.value)
    }

    messageLog?.addAttachmentAsString("Message Body", message.getBody(), "text/plain")

    return message
}
