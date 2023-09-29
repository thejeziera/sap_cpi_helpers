package cpi.scripts.data_transformation.xml

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonOutput
import groovy.xml.XmlParser
import groovy.xml.XmlSlurper
import groovy.xml.XmlUtil

def Message processData(Message message) {
    def body = message.getBody(String.class)
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

        // Split the expression into parts by '&' and '|'
        def andParts = criteriaExpression.split("&")
        def orParts = criteriaExpression.split("\\|")

        // Filter nodes
        nodes.each { node ->
            if (!evaluateExpression(node.text(), andParts, orParts)) {
                node.replaceNode {}
            }
        }
    }

    // Convert filtered XML to JSON
    def jsonOutput = JsonOutput.toJson(new XmlSlurper().parseText(XmlUtil.serialize(parsedXml)))

    // Set the JSON as the message body
    message.setBody(jsonOutput)

    return message
}

def boolean evaluateExpression(String nodeValue, List<String> andParts, List<String> orParts) {
    def andResult = andParts.every { part ->
        def value = part.replace("(", "").replace(")", "")
        value.startsWith("!") ? nodeValue != value.substring(1) : nodeValue == value
    }

    def orResult = orParts.any { part ->
        def value = part.replace("(", "").replace(")", "")
        value.startsWith("!") ? nodeValue != value.substring(1) : nodeValue == value
    }

    return andResult || orResult
}
