package cpi.scripts.data_transformation.xml

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
import groovy.xml.XmlParser
import groovy.xml.XmlSlurper
import groovy.xml.XmlUtil
import org.w3c.dom.Node

def Message processData(Message message) {
    def body = message.getBody() as String
    // Parse XML
    def parsedXml = new XmlParser().parseText(body)

    // Fetch filters from headers. Assumes headers are in the format "filter_field/path/example: (123|321)&(!333|!111)"
    def filters = message.getHeaders()
            .findAll { it.key.startsWith("filter_") }
            .collectEntries { [(it.key - "filter_"): it.value] }

    // Apply filters
    filters.each { field, criteriaExpression ->
        // Fetch nodes based on field
        def nodes = parsedXml.'**'.findAll { it.name() == field }

        // Filter nodes
        nodes.each { node ->
            if (!(node.value =~ criteriaExpression)) {
                node.replaceNode {}
            }
        }
    }

    // Convert filtered XML to String
    def xmlOutput = XmlUtil.serialize(parsedXml)

    // Set the XML as the message body
    message.setBody(xmlOutput)

    return message
}