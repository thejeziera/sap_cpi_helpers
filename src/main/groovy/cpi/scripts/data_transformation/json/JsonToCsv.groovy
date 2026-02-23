package cpi.scripts.data_transformation.json

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper

def Message processData(Message message) {
    // Retrieve the JSON content from the message body
    def jsonContent = message.getBody() as String

    // Read logMode; default to NONE if not set
    def logMode = message.getProperties().get("logMode") ?: "NONE"

    // Obtain message processing log (may be null)
    def messageLog = messageLogFactory.getMessageLog(message)

    // Validate body is not null or blank
    if (!jsonContent?.trim()) throw new IllegalArgumentException("JSON body is missing or blank")

    // Parse JSON inline
    def jsonData = new JsonSlurper().parseText(jsonContent)

    // Validate input is a JSON array
    if (!(jsonData instanceof List)) throw new IllegalArgumentException("JSON body must be a JSON array")

    // Validate array is non-empty
    if (jsonData.isEmpty()) throw new IllegalArgumentException("JSON array must contain at least one record")

    // ASSUMPTION: headers extracted from first record; extra keys in subsequent records silently ignored; missing keys produce empty string
    // ASSUMPTION: field values converted to string as-is; values containing commas or newlines are not escaped
    def headers = new ArrayList(jsonData[0].keySet())

    // Build CSV using StringBuilder; no trailing newline after last row
    def sb = new StringBuilder()
    sb.append(headers.join(','))
    jsonData.each { record ->
        sb.append('\n')
        sb.append(headers.collect { h -> record.containsKey(h) ? record[h]?.toString() ?: "" : "" }.join(','))
    }

    // Set the CSV content as the message body
    message.setBody(sb.toString())

    // Log row count when logMode is INFO
    if (logMode == "INFO") messageLog?.addCustomHeaderProperty("json_rowCount", String.valueOf(jsonData.size()))

    return message
}
