package cpi.scripts.data_transformation.json

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder

def Message processData(Message message) {
    // Retrieve the JSON content from the message body
    def jsonContent = message.getBody() as String

    // Read logMode from properties (default NONE)
    def logMode = message.getProperties().get("logMode") ?: "NONE"

    // Obtain messageLog (null-safe: getMessageLog may return null at runtime)
    def messageLog = messageLogFactory.getMessageLog(message)

    // Validate: null or blank body
    if (!jsonContent?.trim()) {
        throw new IllegalArgumentException("JSON body is missing or blank")
    }

    // Parse the JSON content
    def jsonData = new JsonSlurper().parseText(jsonContent)

    // Validate: body must be a JSON array
    if (!(jsonData instanceof List)) {
        throw new IllegalArgumentException("JSON body must be a JSON array")
    }

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
    message.setBody(writer.toString())

    // logMode INFO: log the number of records converted
    if (logMode == "INFO") {
        messageLog?.addCustomHeaderProperty("json_rowCount", String.valueOf(jsonData.size()))
    }

    return message
}
