package cpi.scripts.data_transformation.json

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper

def Message processData(Message message) {
    // Retrieve the JSON content from the message body
    def jsonContent = message.getBody() as String

    // Parse the JSON content
    def jsonSlurper = new JsonSlurper()
    def jsonData = jsonSlurper.parseText(jsonContent)

    // Initialize a StringWriter for the CSV output
    def writer = new StringWriter()

    // Check if the jsonData is a list of records
    if (jsonData instanceof List) {
        // Extract headers (field names)
        def headers = jsonData[0].keySet().join(',')
        writer.append(headers).append('\n')

        // Process each record
        jsonData.each { record ->
            def fields = record.values().join(',')
            writer.append(fields).append('\n')
        }
    } else {
        throw new RuntimeException("JSON content is not a list of records")
    }

    // Set the CSV content as the message body
    message.setBody(writer.toString())

    return message
}