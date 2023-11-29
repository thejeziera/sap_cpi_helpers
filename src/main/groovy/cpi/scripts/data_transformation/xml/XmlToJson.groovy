package cpi.scripts.data_transformation.xml

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
import groovy.xml.XmlSlurper

def Message processData(Message message) {
    // Retrieve the XML content from the message body
    def xmlContent = message.getBody() as String

    // Parse the XML content
    def xmlSlurper = new XmlSlurper()
    def xmlData = xmlSlurper.parseText(xmlContent)

    // Assuming XML structure is like <records><record><field1>...<field2>...</record>...</records>
    def records = xmlData.'**'.findAll{ it.name() == 'record' }.collect { node ->
        def recordMap = [:]
        node.children().each { child ->
            recordMap[child.name()] = child.text()
        }
        return recordMap
    }

    // Convert the list of maps to JSON
    def jsonContent = JsonOutput.toJson(records)

    // Set the JSON content as the message body
    message.setBody(jsonContent)

    return message
}