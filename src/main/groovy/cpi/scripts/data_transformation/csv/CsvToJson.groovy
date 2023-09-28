package cpi.scripts.data_transformation.csv

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput

def Message processData(Message message) {
    // Retrieve the CSV content from the message body
    def csvContent = message.getBody() as String

    // Split the CSV content into lines
    def lines = csvContent.split('\n')
    if (lines.size() < 2) {
        throw new RuntimeException("CSV content must have at least one header line and one data line")
    }

    // Extract the header fields from the first line
    def headers = lines[0].split(',')

    // Process the data lines and convert them to a list of maps
    def data = lines[1..-1].collect { line ->
        def fields = line.split(',')
        def rowMap = [:]
        headers.eachWithIndex { header, index ->
            rowMap[header] = fields[index]
        }
        return rowMap
    }

    // Convert the list of maps to JSON
    def jsonContent = JsonOutput.toJson(data)

    // Set the JSON content as the message body
    message.setBody(jsonContent)

    return message
}
