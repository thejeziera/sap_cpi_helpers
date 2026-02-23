package cpi.scripts.data_transformation.csv

import com.sap.gateway.ip.core.customdev.util.Message
import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import groovy.xml.XmlSlurper
import spock.lang.Shared
import spock.lang.Specification

class CsvToXmlTest extends Specification {

    @Shared
    Script script
    private Message msg
    @Shared
    private classLoader = new GroovyClassLoader()

    def setupSpec() {
        // Load Groovy Script by its package and class name
        Class scriptClass = classLoader.loadClass("cpi.scripts.data_transformation.csv.CsvToXml")

        // Create an instance of the script
        script = scriptClass.getDeclaredConstructor().newInstance() as Script

        // Mix in the trait to add extra methods and fields
        CPIScriptEnhancer.enhanceScript(script)
    }

    def setup() {
        this.msg = new MessageImpl()
    }

    def "Happy path: single data row produces valid XML with records root and one record child"() {
        given:
        msg.setBody("header_field1,header_field2\nvalue1,value2")

        when:
        script.processData(msg)

        then:
        def root = new XmlSlurper().parseText(msg.getBody() as String)
        root.record.size() == 1
        root.record[0].header_field1.text() == "value1"
        root.record[0].header_field2.text() == "value2"
    }

    def "Happy path: multiple data rows produce one record element per row"() {
        given:
        msg.setBody("name,city\nAlice,Berlin\nBob,Paris")

        when:
        script.processData(msg)

        then:
        def root = new XmlSlurper().parseText(msg.getBody() as String)
        root.record.size() == 2
        root.record[0].name.text() == "Alice"
        root.record[0].city.text() == "Berlin"
        root.record[1].name.text() == "Bob"
        root.record[1].city.text() == "Paris"
    }

    def "Whitespace around headers and values is trimmed"() {
        given:
        msg.setBody(" name , city \n Alice , Berlin ")

        when:
        script.processData(msg)

        then:
        def root = new XmlSlurper().parseText(msg.getBody() as String)
        root.record[0].name.text() == "Alice"
        root.record[0].city.text() == "Berlin"
    }

    def "Windows CRLF line endings are handled correctly"() {
        given:
        msg.setBody("name,city\r\nAlice,Berlin\r\nBob,Paris")

        when:
        script.processData(msg)

        then:
        def root = new XmlSlurper().parseText(msg.getBody() as String)
        root.record.size() == 2
        root.record[0].name.text() == "Alice"
        root.record[1].city.text() == "Paris"
        // Verify no trailing \r in element text
        root.record[0].name.text() == root.record[0].name.text().trim()
    }

    def "Header-only CSV throws IllegalArgumentException"() {
        given:
        msg.setBody("name,city")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "CSV must contain at least a header row and one data row"
    }

    def "Null body throws IllegalArgumentException with missing or blank message"() {
        given:
        msg.setBody(null)

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "CSV body is missing or blank"
    }

    def "Blank body throws IllegalArgumentException with missing or blank message"() {
        given:
        msg.setBody("   ")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "CSV body is missing or blank"
    }

    def "Data row with fewer fields than headers maps missing fields to empty element content"() {
        given:
        msg.setBody("a,b,c\n1,2")

        when:
        script.processData(msg)

        then:
        def root = new XmlSlurper().parseText(msg.getBody() as String)
        root.record[0].a.text() == "1"
        root.record[0].b.text() == "2"
        root.record[0].c.text() == ""
    }

    def "logMode=INFO logs csv_rowCount as MPL custom header property"() {
        given:
        msg.setBody("h1,h2\nv1,v2\nv3,v4")
        msg.setProperty("logMode", "INFO")
        msg.setMessageLog(script.messageLogFactory.getMessageLog("CsvToXml"))

        when:
        script.processData(msg)

        then:
        script.messageLogFactory.messageLog.customHeaderPropertiesMap.get("csv_rowCount") == "2"
    }

    def "logMode=NONE (default): no MPL custom header properties added"() {
        given:
        msg.setBody("h1,h2\nv1,v2")
        // logMode not set — default is NONE
        msg.setMessageLog(script.messageLogFactory.getMessageLog("CsvToXml"))

        when:
        script.processData(msg)

        then:
        script.messageLogFactory.messageLog.customHeaderPropertiesMap.isEmpty()
    }

    def "Scope-creep guard: no headers set, no properties written, only body changed to XML"() {
        given:
        msg.setBody("name,age\nAlice,30")
        def headersBefore = new LinkedHashMap(msg.getHeaders())
        def propsBefore   = new LinkedHashMap(msg.getProperties())

        when:
        script.processData(msg)

        then:
        msg.getHeaders() == headersBefore
        msg.getProperties() == propsBefore
        (msg.getBody() as String).contains("<records>")
    }
}
