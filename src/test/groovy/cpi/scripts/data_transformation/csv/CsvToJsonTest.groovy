package cpi.scripts.data_transformation.csv

import com.sap.gateway.ip.core.customdev.processor.MessageImpl
import com.sap.gateway.ip.core.customdev.util.Message
import org.apache.camel.Exchange
import org.apache.camel.impl.DefaultCamelContext
import org.apache.camel.impl.DefaultExchange
import spock.lang.Shared
import spock.lang.Specification

class CsvToJsonTest extends Specification {

    @Shared Script script
    private Message msg

    def setupSpec() {
        // Initialize Groovy Class Loader
        GroovyClassLoader classLoader = new GroovyClassLoader()

        // Load Groovy Script by its package and class name
        Class scriptClass = classLoader.loadClass("cpi.scripts.data_transformation.csv.CsvToJson")

        // Create an instance of the script
        script = scriptClass.getDeclaredConstructor().newInstance() as Script
    }

    def setup() {
        // Initialize Camel context and create a new Exchange
        DefaultCamelContext camelContext = new DefaultCamelContext()
        Exchange exchange = new DefaultExchange(camelContext)

        // Set the payload in the Exchange's In message
        String payload = "{key: \"value\"}}"
        exchange.getIn().setBody(payload)

        // Set HTTP-specific headers in the Exchange's In message
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "GET")
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json")
        exchange.getIn().setHeader(Exchange.HTTP_URI, "https://example.com/api/resource")

        // Initialize MessageImpl with the created Exchange
        this.msg = new MessageImpl(exchange)
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