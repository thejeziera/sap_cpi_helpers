package cpi.scripts.data_transformation.csv

import com.sap.gateway.ip.core.customdev.util.Message
import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import groovy.json.JsonSlurper
import spock.lang.Shared
import spock.lang.Specification

class CsvToJsonTest extends Specification {

    @Shared
    Script script
    private Message msg
    @Shared
    private classLoader = new GroovyClassLoader()

    def setupSpec() {
        Class scriptClass = classLoader.loadClass("cpi.scripts.data_transformation.csv.CsvToJson")
        script = scriptClass.getDeclaredConstructor().newInstance() as Script
        CPIScriptEnhancer.enhanceScript(script)
    }

    def setup() {
        this.msg = new MessageImpl()
    }

    def "Happy path: single data row is converted to correct JSON array"() {
        given:
        msg.setBody("header_field1,header_field2\nvalue1,value2")

        when:
        script.processData(msg)

        then:
        msg.getBody() == '[{"header_field1":"value1","header_field2":"value2"}]'
    }

    def "Happy path: multiple data rows produce one JSON object per row"() {
        given:
        msg.setBody("name,city\nAlice,Berlin\nBob,Paris")

        when:
        script.processData(msg)

        then:
        def list = new JsonSlurper().parseText(msg.getBody() as String)
        list.size() == 2
        list[0].name == "Alice" && list[0].city == "Berlin"
        list[1].name == "Bob"   && list[1].city == "Paris"
    }

    def "Whitespace around headers and values is trimmed"() {
        given:
        msg.setBody(" name , city \n Alice , Berlin ")

        when:
        script.processData(msg)

        then:
        def list = new JsonSlurper().parseText(msg.getBody() as String)
        list[0]["name"] == "Alice" && list[0]["city"] == "Berlin"
    }

    def "Windows CRLF line endings are handled correctly"() {
        given:
        msg.setBody("name,city\r\nAlice,Berlin\r\nBob,Paris")

        when:
        script.processData(msg)

        then:
        def list = new JsonSlurper().parseText(msg.getBody() as String)
        list.size() == 2
        list[0].name == "Alice" && list[0].city == "Berlin"
        list[1].name == "Bob"   && list[1].city == "Paris"
        // No trailing \r in any value
        list[0].name  == list[0].name.trim()
        list[0].city  == list[0].city.trim()
        list[1].name  == list[1].name.trim()
        list[1].city  == list[1].city.trim()
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

    def "Null body throws IllegalArgumentException with 'missing or blank'"() {
        given:
        msg.setBody(null)

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "CSV body is missing or blank"
    }

    def "Blank body throws IllegalArgumentException with 'missing or blank'"() {
        given:
        msg.setBody("   ")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "CSV body is missing or blank"
    }

    def "Data row with fewer fields than headers maps missing fields to empty string"() {
        given:
        msg.setBody("a,b,c\n1,2")

        when:
        script.processData(msg)

        then:
        def list = new JsonSlurper().parseText(msg.getBody() as String)
        list[0].a == "1" && list[0].b == "2" && list[0].c == ""
    }

    def "logMode=INFO logs csv_rowCount as MPL custom header property"() {
        given:
        msg.setBody("h1,h2\nv1,v2\nv3,v4")
        msg.setProperty("logMode", "INFO")
        msg.setMessageLog(script.messageLogFactory.getMessageLog("CsvToJson"))

        when:
        script.processData(msg)

        then:
        script.messageLogFactory.messageLog.customHeaderPropertiesMap.get("csv_rowCount") == "2"
    }

    def "logMode=NONE (default): no MPL custom header properties added"() {
        given:
        msg.setBody("h1,h2\nv1,v2")
        // logMode not set — defaults to NONE
        msg.setMessageLog(script.messageLogFactory.getMessageLog("CsvToJson"))

        when:
        script.processData(msg)

        then:
        script.messageLogFactory.messageLog.customHeaderPropertiesMap.isEmpty()
    }

    def "Scope-creep guard: no headers set, no properties written, only body changed to JSON"() {
        given:
        msg.setBody("name,age\nAlice,30")
        def headersBefore = new LinkedHashMap(msg.getHeaders())
        def propsBefore   = new LinkedHashMap(msg.getProperties())

        when:
        script.processData(msg)

        then:
        msg.getHeaders()    == headersBefore
        msg.getProperties() == propsBefore
        (msg.getBody() as String).startsWith("[")
    }
}
