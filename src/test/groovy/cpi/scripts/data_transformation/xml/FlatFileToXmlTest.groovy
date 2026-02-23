package cpi.scripts.data_transformation.xml

import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import groovy.xml.XmlSlurper
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class FlatFileToXmlTest extends Specification {

    @Shared Script script
    @Shared private GroovyClassLoader classLoader = new GroovyClassLoader()
    private MessageImpl msg

    def setupSpec() {
        Class scriptClass = classLoader.loadClass("cpi.scripts.data_transformation.xml.FlatFileToXml")
        script = scriptClass.getDeclaredConstructor().newInstance() as Script
        CPIScriptEnhancer.enhanceScript(script)
    }

    def setup() {
        msg = new MessageImpl()
    }

    // -------------------------------------------------------------------------
    // Happy path: single record
    // -------------------------------------------------------------------------
    def "Happy path: single-record flat file produces records root with one record child"() {
        given: "a single-line fixed-width flat file and field spec"
        // name:10, city:8 — total 18 chars
        msg.setBody("John      NewYork ")
        msg.setProperty("flatFile_fieldSpec", "name:10,city:8")

        and: "snapshot for no-scope-creep"
        def headersBefore = new LinkedHashMap(msg.getHeaders())
        def propsBefore   = new LinkedHashMap(msg.getProperties())

        when:
        script.processData(msg)

        then: "body is XML with <records> root and one <record>"
        def xml = new XmlSlurper().parseText(msg.getBody() as String)
        xml.name() == "records"
        xml.record.size() == 1
        xml.record[0].name.text() == "John"
        xml.record[0].city.text() == "NewYork"

        and: "no-scope-creep: no headers set, no properties written"
        msg.getHeaders() == headersBefore
        // properties that were set before must still be present; no new keys added
        def propsAfter = msg.getProperties()
        propsAfter.keySet().containsAll(propsBefore.keySet())
        propsAfter.size() == propsBefore.size()
    }

    // -------------------------------------------------------------------------
    // Happy path: multi-record
    // -------------------------------------------------------------------------
    def "Happy path: multi-record flat file produces one record element per line"() {
        given: "two fixed-width lines"
        msg.setBody("Alice     BostonXXX\nBob       Chicago  ")
        msg.setProperty("flatFile_fieldSpec", "name:10,city:9")

        when:
        script.processData(msg)

        then:
        def xml = new XmlSlurper().parseText(msg.getBody() as String)
        xml.record.size() == 2
        xml.record[0].name.text() == "Alice"
        xml.record[0].city.text() == "BostonXXX"
        xml.record[1].name.text() == "Bob"
        xml.record[1].city.text() == "Chicago"
    }

    // -------------------------------------------------------------------------
    // CRLF support
    // -------------------------------------------------------------------------
    def "Windows CRLF line endings are handled correctly"() {
        given: "lines separated by CRLF"
        msg.setBody("Ann       Paris    \r\nEve       London   ")
        msg.setProperty("flatFile_fieldSpec", "name:10,city:9")

        when:
        script.processData(msg)

        then:
        def xml = new XmlSlurper().parseText(msg.getBody() as String)
        xml.record.size() == 2
        xml.record[0].name.text() == "Ann"
        xml.record[1].name.text() == "Eve"
    }

    // -------------------------------------------------------------------------
    // Trailing spaces are trimmed
    // -------------------------------------------------------------------------
    def "Trailing spaces in field values are trimmed"() {
        given: "field value padded with trailing spaces"
        msg.setBody("Tom       ")
        msg.setProperty("flatFile_fieldSpec", "name:10")

        when:
        script.processData(msg)

        then:
        def xml = new XmlSlurper().parseText(msg.getBody() as String)
        xml.record[0].name.text() == "Tom"
    }

    // -------------------------------------------------------------------------
    // Line shorter than field spec
    // -------------------------------------------------------------------------
    def "Line shorter than field spec produces truncated field value without error"() {
        given: "body covers first field exactly but has no chars for second field"
        // name:10 consumes all 10 chars; city:8 gets substring(10,10) = "" — no exception
        msg.setBody("Hi        ")
        msg.setProperty("flatFile_fieldSpec", "name:10,city:8")

        when:
        script.processData(msg)

        then: "no exception; name is extracted, city is empty"
        def xml = new XmlSlurper().parseText(msg.getBody() as String)
        xml.record[0].name.text() == "Hi"
        xml.record[0].city.text() == ""
    }

    // -------------------------------------------------------------------------
    // Null / blank body guard
    // -------------------------------------------------------------------------
    @Unroll
    def "Null or blank body throws IllegalArgumentException: #label"() {
        given:
        msg.setBody(bodyValue)
        msg.setProperty("flatFile_fieldSpec", "name:10")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Flat file body is missing or blank"

        where:
        label   | bodyValue
        "null"  | null
        "empty" | ""
        "blank" | "   "
    }

    // -------------------------------------------------------------------------
    // Missing flatFile_fieldSpec
    // -------------------------------------------------------------------------
    def "Missing flatFile_fieldSpec property throws IllegalArgumentException with required message"() {
        given: "valid body but no flatFile_fieldSpec set"
        msg.setBody("SomeData  ")
        // flatFile_fieldSpec is intentionally not set

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Property flatFile_fieldSpec is required (e.g. 'field1:10,field2:5')"
    }

    // -------------------------------------------------------------------------
    // logMode=INFO logs ff_rowCount
    // -------------------------------------------------------------------------
    def "logMode=INFO logs ff_rowCount as MPL custom header property"() {
        given: "two-line body and logMode=INFO"
        msg.setBody("Alice     \nBob       ")
        msg.setProperty("flatFile_fieldSpec", "name:10")
        msg.setProperty("logMode", "INFO")

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog.customHeaderPropertiesMap.get("ff_rowCount") == "2"
    }

    // -------------------------------------------------------------------------
    // logMode=NONE (default): no MPL custom header properties
    // -------------------------------------------------------------------------
    def "logMode=NONE (default): no MPL custom header properties added"() {
        given: "body without logMode set (defaults to NONE)"
        msg.setBody("Alice     ")
        msg.setProperty("flatFile_fieldSpec", "name:10")
        // logMode not set — defaults to NONE

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog.customHeaderPropertiesMap.isEmpty()
    }

    // -------------------------------------------------------------------------
    // Scope-creep guard
    // -------------------------------------------------------------------------
    def "Scope-creep guard: no headers set, no properties written beyond logMode, only body changed to XML"() {
        given: "a valid flat file with one extra header and property already present"
        msg.setBody("Carol     Denver    ")
        msg.setProperty("flatFile_fieldSpec", "name:10,city:10")
        msg.setProperty("logMode", "NONE")
        msg.setHeader("someExistingHeader", "headerValue")
        msg.setProperty("someExistingProp", "propValue")

        and: "snapshot before execution"
        def headersBefore = new LinkedHashMap(msg.getHeaders())
        def propsBefore   = new LinkedHashMap(msg.getProperties())
        def bodyBefore    = msg.getBody()

        when:
        script.processData(msg)

        then: "body has changed to XML"
        msg.getBody() != bodyBefore
        (msg.getBody() as String).contains("<records>")

        and: "no new headers added or removed"
        msg.getHeaders() == headersBefore

        and: "no new properties added beyond those already present"
        def propsAfter = new LinkedHashMap(msg.getProperties())
        def _ = propsAfter.remove("flatFile_fieldSpec")
        def _2 = propsBefore.remove("flatFile_fieldSpec")
        def _3 = propsAfter.remove("logMode")
        def _4 = propsBefore.remove("logMode")
        def _5 = propsAfter.remove("someExistingProp")
        def _6 = propsBefore.remove("someExistingProp")
        propsAfter.isEmpty()
        propsBefore.isEmpty()
    }
}
