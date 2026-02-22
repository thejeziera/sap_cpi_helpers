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

    def "Csv body is transformed to xml"() {
        given: "body is set to a simple csv"
        this.msg.setBody("header_field1,header_field2\nvalue1,value2")

        when: "we execute the Groovy script"
        script.processData(this.msg)

        then: "we get the csv body parsed to xml"
        def expectedXml = new XmlSlurper().parseText('<records><record><header_field1>value1</header_field1><header_field2>value2</header_field2></record></records>')
        def actualXml = new XmlSlurper().parseText(this.msg.getBody())

        // This ensures structural equality, ignoring whitespace and formatting differences
        actualXml == expectedXml
    }
}