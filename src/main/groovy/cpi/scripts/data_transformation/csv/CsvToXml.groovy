package cpi.scripts.data_transformation.csv

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.xml.MarkupBuilder

def Message processData(Message message) {
    // Retrieve the CSV content from the message body
    def csvContent = message.getBody() as String

    // Read logMode from properties (default NONE)
    def logMode = message.getProperties().get("logMode") ?: "NONE"

    // Obtain messageLog (null-safe: getMessageLog may return null at runtime)
    def messageLog = messageLogFactory.getMessageLog(message)

    // Validate: null or blank body
    if (!csvContent?.trim()) {
        throw new IllegalArgumentException("CSV body is missing or blank")
    }

    // Split on both LF and CRLF line endings
    def lines = csvContent.split(/\r?\n/)
    if (lines.size() < 2) {
        throw new IllegalArgumentException("CSV must contain at least a header row and one data row")
    }

    // Extract the header fields from the first line
    def headers = lines[0].split(',')

    // Initialize a StringWriter for the XML output
    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)

    // Start the XML document
    xml.records {
        // Process each data line
        lines[1..-1].each { line ->
            def fields = line.split(',')
            xml.record {
                headers.eachWithIndex { header, index ->
                    // Bounds-safe field access; trim header name and value
                    def value = index < fields.size() ? fields[index].trim() : ""
                    "${header.trim()}"(value)
                }
            }
        }
    }

    // Set the XML content as the message body
    message.setBody(writer.toString())

    // logMode INFO: log the number of data rows processed
    if (logMode == "INFO") {
        messageLog?.addCustomHeaderProperty("csv_rowCount", String.valueOf(lines.size() - 1))
    }

    return message
}
