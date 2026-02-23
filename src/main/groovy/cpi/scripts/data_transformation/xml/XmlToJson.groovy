package cpi.scripts.data_transformation.xml

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
import groovy.xml.XmlSlurper

def Message processData(Message message) {
    // Read the XML body as a String
    def xmlContent = message.getBody() as String

    // Read logMode property (default: NONE)
    def logMode = message.getProperties().get("logMode") ?: "NONE"

    // Obtain the MPL message log handle
    def messageLog = messageLogFactory.getMessageLog(message)

    // Guard: null or blank body is not acceptable
    if (!xmlContent?.trim()) throw new IllegalArgumentException("XML body is missing or blank")

    // Parse XML inline
    def xmlData = new XmlSlurper().parseText(xmlContent)

    // Find all <record> elements anywhere in the tree and convert each to a field-name:field-text map
    def records = xmlData.'**'.findAll{ it.name() == 'record' }.collect { node ->
        def recordMap = [:]
        node.children().each { child ->
            recordMap[child.name()] = child.text()
        }
        return recordMap
    }

    // Serialise the list of maps to a JSON array
    def jsonContent = JsonOutput.toJson(records)

    // Write JSON body
    message.setBody(jsonContent)

    // INFO logging: record count as MPL custom header property
    if (logMode == "INFO") messageLog?.addCustomHeaderProperty("xml_recordCount", String.valueOf(records.size()))

    return message
}
