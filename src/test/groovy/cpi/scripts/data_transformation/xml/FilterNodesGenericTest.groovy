package cpi.scripts.data_transformation.xml

import com.sap.gateway.ip.core.customdev.util.Message
import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import groovy.xml.XmlSlurper
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class FilterNodesGenericTest extends Specification {

    @Shared Script script
    @Shared private GroovyClassLoader classLoader = new GroovyClassLoader()
    private MessageImpl msg

    def setupSpec() {
        Class scriptClass = classLoader.loadClass("cpi.scripts.data_transformation.xml.FilterNodesGeneric")
        script = scriptClass.getDeclaredConstructor().newInstance() as Script
        CPIScriptEnhancer.enhanceScript(script)
    }

    def setup() {
        msg = new MessageImpl()
    }

    // ---------------------------------------------------------------------------
    // Happy path — single filter: matching kept, non-matching removed
    // ---------------------------------------------------------------------------

    def "Single filter: matching nodes are kept, non-matching nodes are removed"() {
        given:
        msg.setBody("<root><item>AAA</item><item>BBB</item><item>AAA</item></root>")
        msg.setHeader("filter_item", "AAA")

        when:
        script.processData(msg)

        then:
        def result = new XmlSlurper().parseText(msg.getBody() as String)
        result.item.size() == 2
        result.item.every { it.text() == "AAA" }
    }

    // ---------------------------------------------------------------------------
    // Single filter: all nodes match — none removed
    // ---------------------------------------------------------------------------

    def "Single filter: nodes that all match are all kept (no removals)"() {
        given:
        msg.setBody("<root><item>AAA</item><item>AAA</item></root>")
        msg.setHeader("filter_item", "AAA")

        when:
        script.processData(msg)

        then:
        def result = new XmlSlurper().parseText(msg.getBody() as String)
        result.item.size() == 2
    }

    // ---------------------------------------------------------------------------
    // Single filter: no nodes match — all removed
    // ---------------------------------------------------------------------------

    def "Single filter: no nodes match the regex — all are removed"() {
        given:
        msg.setBody("<root><item>AAA</item><item>BBB</item></root>")
        msg.setHeader("filter_item", "ZZZ")

        when:
        script.processData(msg)

        then:
        def result = new XmlSlurper().parseText(msg.getBody() as String)
        result.item.size() == 0
    }

    // ---------------------------------------------------------------------------
    // Multiple filters: each filter independently removes non-matching nodes
    // ---------------------------------------------------------------------------

    def "Multiple filters: each filter independently removes non-matching nodes of its element type"() {
        given: "two filter headers targeting different element names"
        msg.setBody("""
            <root>
                <item>AAA</item>
                <item>BBB</item>
                <code>X1</code>
                <code>Y2</code>
            </root>
        """.stripIndent().trim())
        msg.setHeader("filter_item", "AAA")
        msg.setHeader("filter_code", "X1")

        when:
        script.processData(msg)

        then:
        def result = new XmlSlurper().parseText(msg.getBody() as String)
        result.item.size() == 1
        result.item[0].text() == "AAA"
        result.code.size() == 1
        result.code[0].text() == "X1"
    }

    // ---------------------------------------------------------------------------
    // No filter_ headers — body passes through unchanged (structurally)
    // ---------------------------------------------------------------------------

    def "No filter_ headers: XML body is passed through unchanged"() {
        given:
        def inputXml = "<root><item>AAA</item><item>BBB</item></root>"
        msg.setBody(inputXml)
        // no filter_ headers set

        when:
        script.processData(msg)

        then:
        def result = new XmlSlurper().parseText(msg.getBody() as String)
        result.item.size() == 2
        result.item[0].text() == "AAA"
        result.item[1].text() == "BBB"
    }

    // ---------------------------------------------------------------------------
    // Boundary: null body throws IllegalArgumentException
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
    // Boundary: blank body throws IllegalArgumentException
    // ---------------------------------------------------------------------------

    @Unroll
    def "Blank body throws IllegalArgumentException with missing or blank message — body=#bodyValue"() {
        given:
        msg.setBody(bodyValue)

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "XML body is missing or blank"

        where:
        bodyValue << ["", "   ", "\t\n"]
    }

    // ---------------------------------------------------------------------------
    // Logging: logMode=INFO logs ff_removedCount as MPL custom header property
    // ---------------------------------------------------------------------------

    def "logMode=INFO logs ff_removedCount as MPL custom header property"() {
        given:
        msg.setBody("<root><item>AAA</item><item>BBB</item></root>")
        msg.setHeader("filter_item", "AAA")
        msg.setProperty("logMode", "INFO")

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog.customHeaderPropertiesMap.containsKey("ff_removedCount")
        mplLog.customHeaderPropertiesMap["ff_removedCount"] == "1"
    }

    // ---------------------------------------------------------------------------
    // Logging: logMode=NONE (default) — no MPL custom header properties added
    // ---------------------------------------------------------------------------

    def "logMode=NONE (default): no MPL custom header properties added"() {
        given:
        msg.setBody("<root><item>AAA</item><item>BBB</item></root>")
        msg.setHeader("filter_item", "AAA")
        // logMode property not set — defaults to NONE

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog.customHeaderPropertiesMap.isEmpty()
    }

    // ---------------------------------------------------------------------------
    // Scope-creep guard: no headers written, no properties written, only body changed
    // ---------------------------------------------------------------------------

    def "Scope-creep guard: no headers written, no properties written, only body changed"() {
        given:
        msg.setBody("<root><item>AAA</item><item>BBB</item></root>")
        msg.setHeader("filter_item", "AAA")
        msg.setProperty("logMode", "NONE")

        and: "snapshot of headers and properties before execution (excluding filter_ header which was set by test)"
        def headersBefore = new LinkedHashMap(msg.getHeaders())
        def propsBefore   = new LinkedHashMap(msg.getProperties())

        when:
        script.processData(msg)

        then: "headers are exactly the same as before — script wrote no new headers"
        msg.getHeaders() == headersBefore

        and: "properties are exactly the same as before — script wrote no new properties"
        msg.getProperties() == propsBefore

        and: "body was changed (serialised XML)"
        msg.getBody() != null
    }
}
