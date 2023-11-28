package cpi.scripts.monitoring

import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    def messageLog = messageLogFactory.getMessageLog(message)

    //28.11.2023 After Camel Upgrade for HTTPS adapter a null is inserted as path header value (if it was empty)
    message.getHeaders().each {
        entry ->
            messageLog?.addCustomHeaderProperty("HEADER_" + entry.key, entry as String)
    }

    message.getProperties().each {
        entry ->
            messageLog?.addCustomHeaderProperty("PROPERTY_" + entry.key, entry.value as String)
    }

    message.exchange.getIn().getHeaders().each {
        entry ->
            messageLog?.addCustomHeaderProperty("EX_IN_HEADER_" + entry.key, entry.value as String)
    }

    message.exchange.getProperties().each {
        entry ->
            messageLog?.addCustomHeaderProperty("EX_PROP_" + entry.key, entry.value as String)
    }

    message.attachments.each {
        messageLog?.addCustomHeaderProperty("ATTACHMENT_" + entry.key, entry.value as String)
    }

    message.attachmentObjects.each {
        messageLog?.addCustomHeaderProperty("ATT_OBJECT" + entry.key, entry.value as String)
    }

    message.attachmentWrapperObjects.each {
        messageLog?.addCustomHeaderProperty("ATT_WRAP_OBJ" + entry.key, entry.value as String)
    }

    messageLog?.addCustomHeaderProperty("TEST_LOG", message.log as String)
    messageLog?.addCustomHeaderProperty("TEST_EXCHANGE", message.exchange as String)
    messageLog?.addCustomHeaderProperty("TEST_SAP_SOAP_HEADER_WRITE", message.SAP_SOAP_HEADER_WRITE as String)
    messageLog?.addCustomHeaderProperty("TEST_SAP_SOAP_HEADER_READ", message.SAP_SOAP_HEADER_READ as String)

    messageLog?.addAttachmentAsString("Message Body", message.payload as String, "text/plain")
    messageLog?.addAttachmentAsString("Exchange Message Body", message.exchange.getIn().getBody() as String, "text/plain")

    return message
}
