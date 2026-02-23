package cpi.scripts.data_transformation.xml

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.xml.MarkupBuilder

def Message processData(Message message) {
    // Read the flat file content from the message body
    def flatFileContent = message.getBody() as String

    // Read logMode property (default: NONE)
    def logMode = message.getProperties().get("logMode") ?: "NONE"

    // Obtain the MPL message log (may be null in some runtime contexts)
    def messageLog = messageLogFactory.getMessageLog(message)

    // Validate body is not null or blank
    if (!flatFileContent?.trim()) {
        throw new IllegalArgumentException("Flat file body is missing or blank")
    }

    // Read and validate the flatFile_fieldSpec property
    def fieldSpecProp = message.getProperties().get("flatFile_fieldSpec") as String
    if (!fieldSpecProp?.trim()) {
        throw new IllegalArgumentException("Property flatFile_fieldSpec is required (e.g. 'field1:10,field2:5')")
    }
    // ASSUMPTION: flatFile_fieldSpec format is comma-separated name:length pairs (e.g. "firstName:15,lastName:20"); malformed tokens will propagate as runtime errors
    def fieldStructure = fieldSpecProp.trim().split(',').collect { token ->
        def parts = token.trim().split(':')
        [name: parts[0].trim(), length: Integer.parseInt(parts[1].trim())]
    }

    // Split the file content into lines, supporting both LF and CRLF line endings
    def lines = flatFileContent.split(/\r?\n/)

    // Initialize a StringWriter for the XML output
    def writer = new StringWriter()
    def xml = new MarkupBuilder(writer)

    // Build XML with <records> root element and one <record> per line
    xml.records {
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

    // Set the generated XML as the message body
    message.setBody(writer.toString())

    // Log row count when logMode is INFO
    if (logMode == "INFO") {
        messageLog?.addCustomHeaderProperty("ff_rowCount", String.valueOf(lines.size()))
    }

    return message
}
