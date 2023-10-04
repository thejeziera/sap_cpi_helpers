package cpi.scripts.monitoring

import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)

    message.getHeaders().each{
        entry ->
            messageLog?.addCustomHeaderProperty("HEADER_" + entry.key, entry.value as String)
    }

    message.getProperties().each{
        entry ->
            messageLog?.addCustomHeaderProperty("PROPERTY_" + entry.key, entry.value as String)
    }

    messageLog?.addCustomHeaderProperty("TEST_LOG", message.log as String)
    messageLog?.addCustomHeaderProperty("TEST_EXCHANGE", message.exchange as String)
    messageLog?.addCustomHeaderProperty("TEST_attachments", message.attachments as String)
    messageLog?.addCustomHeaderProperty("TEST_attachment_wrapper_objects", message.attachmentWrapperObjects as String)
    messageLog?.addCustomHeaderProperty("TEST_attachment_objects", message.attachmentObjects as String)

    messageLog?.addAttachmentAsString("Message Body", message.getBody(), "text/plain")

    message.setBody("BodyChanged");

    messageLog?.addAttachmentAsString("Message Body Changed w/o type", message.getBody() as String, "text/plain")

    messageLog?.addAttachmentAsString("Message Body Changed with type", message.getBody(String), "text/plain")

    return message
}
