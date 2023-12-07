package cpi.scripts.protocol_adapters.soap

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

def Message processData(Message message) {
    def body = message.getBody(String)

    // Validate if the message is a valid XML
    if (!isXmlValid(body)) {
        throw new RuntimeException("Invalid XML content")
    }

    // Remove XML declaration if present
    def xmlContent = body.replaceAll("(?s)<\\?xml.*?\\?>", "").trim()

    // Define SOAP Envelope with correct namespace
    def soapEnvelope = {
        'soapenv:Envelope'(xmlns: ['soapenv': 'http://schemas.xmlsoap.org/soap/envelope/']) {
            'soapenv:Body'() {
                mkp.yieldUnescaped xmlContent // Inserts XML content
            }
        }
    }

    // Convert to SOAP Envelope
    def writer = new StringWriter()
    new StreamingMarkupBuilder().bind(soapEnvelope).writeTo(writer)
    def soapMessage = writer.toString()

    // Set the modified SOAP message as the body
    message.setBody(soapMessage)

    // Set Content-Type header
    message.setHeader("Content-Type", "text/xml;charset=UTF-8")

    return message
}

boolean isXmlValid(String xml) {
    try {
        XmlUtil.parseText(xml)
        return true
    } catch (Exception e) {
        return false
    }
}
