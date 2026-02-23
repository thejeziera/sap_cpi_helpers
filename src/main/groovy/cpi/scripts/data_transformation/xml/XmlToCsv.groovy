package cpi.scripts.data_transformation.xml

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.xml.XmlSlurper

def Message processData(Message message) {
    // Retrieve the XML content from the message body
    def xmlContent = message.getBody() as String

    // Read logMode from exchange properties (default: NONE)
    def logMode = message.getProperties().get("logMode") ?: "NONE"

    // Obtain MPL message log handle (may be null in some runtimes)
    def messageLog = messageLogFactory.getMessageLog(message)

    // Validate body is present and non-blank
    if (!xmlContent?.trim()) throw new IllegalArgumentException("XML body is missing or blank")

    // Parse the XML content
    def xmlData = new XmlSlurper().parseText(xmlContent)

    // Locate all <record> elements anywhere in the tree via depth-first search
    def records = xmlData.'**'.findAll{ it.name() == 'record' }

    if (records.isEmpty()) {
        throw new IllegalArgumentException("No record elements found in XML")
    }

    // Extract headers (field names) from the first record's child element names
    def headers = records[0].children().collect{ it.name() }

    // ASSUMPTION: column order follows first-record child element order; extra elements in
    //             subsequent records are ignored; missing elements produce empty string via GPath .text()
    // ASSUMPTION: field values converted to string as-is; values containing commas or newlines are not escaped
    def sb = new StringBuilder()
    sb.append(headers.join(','))
    records.each { record ->
        sb.append('\n')
        sb.append(headers.collect { header -> record."${header}".text() }.join(','))
    }

    // Set the CSV string (no trailing newline) as the message body
    message.setBody(sb.toString())

    // Log row count when logMode is INFO
    if (logMode == "INFO") messageLog?.addCustomHeaderProperty("xml_rowCount", String.valueOf(records.size()))

    return message
}
