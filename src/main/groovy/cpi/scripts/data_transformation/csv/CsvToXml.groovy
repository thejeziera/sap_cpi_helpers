package cpi.scripts.data_transformation.csv

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.xml.MarkupBuilder

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
                    "${header}".value(fields[index])
                }
            }
        }
    }

    // Set the XML content as the message body
    message.setBody(writer.toString())

    return message
}