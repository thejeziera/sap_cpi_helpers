package cpi.scripts.data_transformation.xml

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.xml.MarkupBuilder
import java.io.StringWriter

def Message processData(Message message) {
    // Retrieve the flat file content from the message body
    def flatFileContent = message.getBody() as String

    // Define the structure of the flat file (field name and length)
    def fieldStructure = [
            [name: "field1", length: 10],
            [name: "field2", length: 5],
            [name: "field3", length: 8]
            // Add more fields as per your flat file structure
    ]

    // Root node name
    def rootNodeName = "records" // Replace with your root node name

    // Initialize a StringWriter for the XML output
    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)

    // Split the file content into lines (rows)
    def lines = flatFileContent.split('\n')

    // Start the XML document with the root node
    xml."${rootNodeName}" {
        lines.each { line ->
            def currentPosition = 0
            xml.record {
                fieldStructure.each { field ->
                    def endIndex = Math.min(currentPosition + field.length, line.length())
                    def fieldValue = line.substring(currentPosition, endIndex).trim()
                    currentPosition += field.length
                    "${field.name}"(fieldValue)
                }
            }
        }
    }

    // Set the XML content as the message body
    message.setBody(writer.toString())

    return message
}