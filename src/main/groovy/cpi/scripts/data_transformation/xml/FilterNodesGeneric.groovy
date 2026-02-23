package cpi.scripts.data_transformation.xml

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.xml.XmlParser
import groovy.xml.XmlUtil

def Message processData(Message message) {
    def body = message.getBody() as String

    // Read logMode; default to NONE
    def logMode = message.getProperties().get("logMode") ?: "NONE"

    // Obtain MPL message log handle (may be null in some runtimes)
    def messageLog = messageLogFactory.getMessageLog(message)

    // Validate body: reject null or blank input
    if (!body?.trim()) throw new IllegalArgumentException("XML body is missing or blank")

    // Parse XML
    def parsedXml = new XmlParser().parseText(body)

    // Read headers prefixed with "filter_"; the remaining key is the element name, the value is a regex pattern
    def filters = message.getHeaders()
            .findAll { it.key.startsWith("filter_") }
            .collectEntries { [(it.key - "filter_"): it.value] }

    // Track total nodes removed across all filters
    def removedCount = 0

    // Apply filters
    filters.each { field, criteriaExpression ->
        // Fetch nodes based on field
        def nodes = parsedXml.'**'.findAll { it.name() == field }

        // Filter nodes: remove any whose text content does not match the regex pattern
        nodes.each { node ->
            if (!(node.text() =~ criteriaExpression)) {
                node.replaceNode {}
                removedCount++
            }
        }
    }

    // Convert filtered XML to String
    def xmlOutput = XmlUtil.serialize(parsedXml)

    // Set the XML as the message body
    message.setBody(xmlOutput)

    // Log removedCount when logMode is INFO
    if (logMode == "INFO") messageLog?.addCustomHeaderProperty("ff_removedCount", String.valueOf(removedCount))

    return message
}
