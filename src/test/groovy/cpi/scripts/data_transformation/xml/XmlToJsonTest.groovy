package cpi.scripts.data_transformation.xml

import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import groovy.json.JsonSlurper
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class XmlToJsonTest extends Specification {

    @Shared Script script
    @Shared private GroovyClassLoader classLoader = new GroovyClassLoader()
    private MessageImpl msg

    def setupSpec() {
        Class scriptClass = classLoader.loadClass("cpi.scripts.data_transformation.xml.XmlToJson")
        script = scriptClass.getDeclaredConstructor().newInstance() as Script
        CPIScriptEnhancer.enhanceScript(script)
    }

    def setup() {
        msg = new MessageImpl()
    }

    // ---------------------------------------------------------------------------
    // Happy path — multi-record XML
    // ---------------------------------------------------------------------------
    def "Happy path: multi-record XML produces a JSON array with one object per record"() {
        given:
        msg.setBody("<recordSet><record><a>Text</a><b>1</b></record><record><a>Text2</a><b>2</b></record></recordSet>")

        when:
        script.processData(msg)

        then:
        def result = new JsonSlurper().parseText(msg.getBody() as String)
        result instanceof List
        result.size() == 2
        result[0].a == "Text"
        result[0].b == "1"
        result[1].a == "Text2"
        result[1].b == "2"
    }

    // ---------------------------------------------------------------------------
    // Happy path — single-record XML
    // ---------------------------------------------------------------------------
    def "Happy path: single-record XML produces a JSON array with one object"() {
        given:
        msg.setBody("<data><record><name>Alice</name><age>30</age></record></data>")

        when:
        script.processData(msg)

        then:
        def result = new JsonSlurper().parseText(msg.getBody() as String)
        result instanceof List
        result.size() == 1
        result[0].name == "Alice"
        result[0].age == "30"
    }

    // ---------------------------------------------------------------------------
    // Error: null body
    // ---------------------------------------------------------------------------
    def "Null body throws IllegalArgumentException with missing or blank message"() {
        given:
        msg.setBody(null)

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "XML body is missing or blank"
    }

    // ---------------------------------------------------------------------------
    // Error: blank body
    // ---------------------------------------------------------------------------
    @Unroll
    def "Blank body '#body' throws IllegalArgumentException with missing or blank message"() {
        given:
        msg.setBody(body)

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "XML body is missing or blank"

        where:
        body << ["", "   ", "\t\n"]
    }

    // ---------------------------------------------------------------------------
    // XML with no <record> elements produces an empty JSON array
    // ---------------------------------------------------------------------------
    def "XML with no record elements produces an empty JSON array"() {
        given:
        msg.setBody("<empty/>")

        when:
        script.processData(msg)

        then:
        def result = new JsonSlurper().parseText(msg.getBody() as String)
        result instanceof List
        result.size() == 0
    }

    // ---------------------------------------------------------------------------
    // Alternative root element — records found anywhere in the tree
    // ---------------------------------------------------------------------------
    def "Alternative root element is supported — records found anywhere in the tree"() {
        given:
        msg.setBody("<orders><batch><record><id>99</id></record></batch></orders>")

        when:
        script.processData(msg)

        then:
        def result = new JsonSlurper().parseText(msg.getBody() as String)
        result instanceof List
        result.size() == 1
        result[0].id == "99"
    }

    // ---------------------------------------------------------------------------
    // Numeric-looking field values are serialised as JSON strings, not numbers
    // ---------------------------------------------------------------------------
    def "Numeric-looking field values are serialised as JSON strings not numbers"() {
        given:
        msg.setBody("<root><record><amount>42</amount></record></root>")

        when:
        script.processData(msg)

        then:
        // Raw JSON must contain the value quoted, e.g. "amount":"42"
        (msg.getBody() as String).contains('"42"')
        def result = new JsonSlurper().parseText(msg.getBody() as String)
        result[0].amount instanceof String
        result[0].amount == "42"
    }

    // ---------------------------------------------------------------------------
    // logMode=INFO: xml_recordCount custom header property added to MPL
    // ---------------------------------------------------------------------------
    def "logMode=INFO logs xml_recordCount as MPL custom header property"() {
        given:
        msg.setBody("<root><record><x>1</x></record><record><x>2</x></record></root>")
        msg.setProperty("logMode", "INFO")

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog.customHeaderPropertiesMap["xml_recordCount"] == "2"
    }

    // ---------------------------------------------------------------------------
    // logMode=NONE (default): no MPL custom header properties added
    // ---------------------------------------------------------------------------
    def "logMode=NONE (default): no MPL custom header properties added"() {
        given:
        msg.setBody("<root><record><x>1</x></record></root>")
        // logMode not set — default is NONE

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog.customHeaderPropertiesMap.isEmpty()
    }

    // ---------------------------------------------------------------------------
    // Scope-creep guard: no headers set, no properties written, only body changed
    // ---------------------------------------------------------------------------
    def "Scope-creep guard: no headers set, no properties written, only body changed to JSON"() {
        given:
        msg.setBody("<root><record><field>val</field></record></root>")
        msg.setHeader("existingHeader", "headerValue")
        msg.setProperty("existingProp", "propValue")

        and: "capture snapshot of headers and properties before execution"
        def headersBefore = new LinkedHashMap(msg.getHeaders())
        def propsBefore   = new LinkedHashMap(msg.getProperties())

        when:
        script.processData(msg)

        then: "body is now JSON"
        def result = new JsonSlurper().parseText(msg.getBody() as String)
        result instanceof List

        and: "no new headers were added or existing ones modified"
        msg.getHeaders() == headersBefore

        and: "no new properties were added or existing ones modified"
        msg.getProperties() == propsBefore
    }
}
