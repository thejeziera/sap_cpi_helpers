package cpi.scripts.data_transformation.json

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil

def Message processData(Message message) {
    // Retrieve the JSON content from the message body
    def jsonContent = message.getBody() as String

    // Parse the JSON content
    def jsonSlurper = new JsonSlurper()
    def jsonData = jsonSlurper.parseText(jsonContent)

    // Convert the JSON data to XML
    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)

    xml.records {
        jsonData.each { record ->
            xml.record {
                record.each { key, value ->
                    "${key}"(value)
                }
            }
        }
    }

    // Set the XML content as the message body
    def xmlContent = XmlUtil.serialize(writer.toString())
    message.setBody(xmlContent)

    return message
}
