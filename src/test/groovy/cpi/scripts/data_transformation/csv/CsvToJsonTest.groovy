package cpi.scripts.data_transformation.csv

import com.sap.gateway.ip.core.customdev.util.Message
import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import spock.lang.Shared
import spock.lang.Specification

class CsvToJsonTest extends Specification {

    @Shared
    Script script
    private Message msg
    @Shared
    private classLoader = new GroovyClassLoader()

    def setupSpec() {
        // Load Groovy Script by its package and class name
        Class scriptClass = classLoader.loadClass("cpi.scripts.data_transformation.csv.CsvToJson")

        // Create an instance of the script
        script = scriptClass.getDeclaredConstructor().newInstance() as Script

        // Mix in the trait to add extra methods and fields
        CPIScriptEnhancer.enhanceScript(script)
    }

    def setup() {
        this.msg = new MessageImpl()
    }

    def "Csv body is transformed to json"() {

        given: "body is set to a simple csv"
        this.msg.setBody("header_field1,header_field2\nvalue1,value2")

        when: "we execute the Groovy script"
        script.processData(this.msg)

        then: "we get the csv body parsed to json"
        this.msg.getBody() == "[{\"header_field1\":\"value1\",\"header_field2\":\"value2\"}]"
    }
}