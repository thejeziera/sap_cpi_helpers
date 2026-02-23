package cpi.scripts.data_transformation.json

import com.sap.gateway.ip.core.customdev.util.Message
import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import groovy.xml.XmlSlurper
import spock.lang.Shared
import spock.lang.Specification

class JsonToXmlTest extends Specification {

    @Shared Script script
    @Shared private GroovyClassLoader classLoader = new GroovyClassLoader()
    private Message msg

    def setupSpec() {
        Class scriptClass = classLoader.loadClass("cpi.scripts.data_transformation.json.JsonToXml")
        script = scriptClass.getDeclaredConstructor().newInstance() as Script
        CPIScriptEnhancer.enhanceScript(script)
    }

    def setup() {
        msg = new MessageImpl()
    }

    def "Happy path: single-record JSON array produces records root with one record child"() {
        given:
        msg.setBody('[{"name":"Alice","city":"Berlin"}]')

        when:
        script.processData(msg)

        then:
        def root = new XmlSlurper().parseText(msg.getBody() as String)
        root.record.size() == 1
        root.record[0].name.text() == "Alice"
        root.record[0].city.text() == "Berlin"
    }

    def "Happy path: multiple records produce one record element per JSON object"() {
        given:
        msg.setBody('[{"name":"Alice","city":"Berlin"},{"name":"Bob","city":"Paris"}]')

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

    def "Null body throws IllegalArgumentException with missing or blank message"() {
        given:
        msg.setBody(null)

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "JSON body is missing or blank"
    }

    def "Blank body throws IllegalArgumentException with missing or blank message"() {
        given:
        msg.setBody("   ")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "JSON body is missing or blank"
    }

    def "Non-array JSON throws IllegalArgumentException with must be a JSON array message"() {
        given:
        msg.setBody('{"name":"Alice"}')

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "JSON body must be a JSON array"
    }

    def "Empty JSON array produces valid records root with no children"() {
        given:
        msg.setBody("[]")

        when:
        script.processData(msg)

        then:
        def root = new XmlSlurper().parseText(msg.getBody() as String)
        root.record.size() == 0
    }

    def "Number and boolean values are preserved as text content in XML elements"() {
        given:
        msg.setBody('[{"count":42,"active":true}]')

        when:
        script.processData(msg)

        then:
        def root = new XmlSlurper().parseText(msg.getBody() as String)
        root.record[0].count.text() == "42"
        root.record[0].active.text() == "true"
    }

    def "logMode=INFO logs json_rowCount as MPL custom header property"() {
        given:
        msg.setBody('[{"a":"1"},{"a":"2"},{"a":"3"}]')
        msg.setProperty("logMode", "INFO")
        msg.setMessageLog(script.messageLogFactory.getMessageLog("JsonToXml"))

        when:
        script.processData(msg)

        then:
        script.messageLogFactory.messageLog.customHeaderPropertiesMap.get("json_rowCount") == "3"
    }

    def "logMode=NONE (default): no MPL custom header properties added"() {
        given:
        msg.setBody('[{"a":"1"}]')
        // logMode not set — default is NONE
        msg.setMessageLog(script.messageLogFactory.getMessageLog("JsonToXml"))

        when:
        script.processData(msg)

        then:
        script.messageLogFactory.messageLog.customHeaderPropertiesMap.isEmpty()
    }

    def "Scope-creep guard: no headers set, no properties written, only body changed to XML"() {
        given:
        msg.setBody('[{"name":"Alice","age":"30"}]')
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
