package cpi.scripts.data_transformation.csv

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

def Message processData(Message message) {
    // Retrieve the CSV content from the message body
    def csvContent = message.getBody() as String

    // Read logMode property (default: NONE)
    def logMode = message.getProperties().get("logMode") ?: "NONE"

    // Obtain message log (null-safe; may be null if MPL is not active)
    def messageLog = messageLogFactory.getMessageLog(message)

    // Validate: body must not be null or blank
    if (!csvContent?.trim()) {
        throw new IllegalArgumentException("CSV body is missing or blank")
    }

    // Split the CSV content into lines — handles both LF and CRLF
    def lines = csvContent.split(/\r?\n/)
    if (lines.size() < 2) {
        throw new IllegalArgumentException("CSV must contain at least a header row and one data row")
    }

    // Extract the header fields from the first line
    def headers = lines[0].split(',')

    // Process the data lines and convert them to a list of maps
    def data = lines[1..-1].collect { line ->
        def fields = line.split(',')
        def rowMap = [:]
        headers.eachWithIndex { header, index ->
            // ASSUMPTION: missing fields in short rows are mapped to empty string
            def value = index < fields.size() ? fields[index].trim() : ""
            rowMap[header.trim()] = value
        }
        return rowMap
    }

    // Convert the list of maps to JSON
    def jsonContent = JsonOutput.toJson(data)

    // Set the JSON content as the message body
    message.setBody(jsonContent)

    // Log row count to MPL when logMode is INFO
    if (logMode == "INFO") {
        messageLog?.addCustomHeaderProperty("csv_rowCount", String.valueOf(data.size()))
    }

    return message
}
