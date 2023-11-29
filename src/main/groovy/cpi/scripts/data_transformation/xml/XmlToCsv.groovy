package cpi.scripts.data_transformation.xml

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.xml.XmlSlurper
import java.io.StringWriter

def Message processData(Message message) {
    // Retrieve the XML content from the message body
    def xmlContent = message.getBody() as String

    // Parse the XML content
    def xmlSlurper = new XmlSlurper()
    def xmlData = xmlSlurper.parseText(xmlContent)

    // Assuming XML structure is like <records><record><field1>...<field2>...</record>...</records>
    def records = xmlData.'**'.findAll{ it.name() == 'record' }

    if (records.isEmpty()) {
        throw new RuntimeException("No records found in XML")
    }

    // Extract headers (field names) from the first record
    def headers = records[0].children().collect{ it.name() }

    // StringWriter to hold the CSV data
    def writer = new StringWriter()

    // Write headers to CSV
    writer.write(headers.join(',') + '\n')

    // Write each record to CSV
    records.each { record ->
        def fields = headers.collect { header ->
            record."${header}".text()
        }
        writer.write(fields.join(',') + '\n')
    }

    // Set the CSV content as the message body
    def csvContent = writer.toString()
    message.setBody(csvContent)

    return message
}
