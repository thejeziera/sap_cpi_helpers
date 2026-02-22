package cpi.scripts.data_transformation.json

import com.sap.gateway.ip.core.customdev.util.Message
import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import spock.lang.Shared
import spock.lang.Specification

class JsonToCsvTest extends Specification{

    @Shared
    Script script
    private Message msg
    @Shared
    private classLoader = new GroovyClassLoader()

    def setupSpec() {
        // Load Groovy Script by its package and class name
        Class scriptClass = classLoader.loadClass("cpi.scripts.data_transformation.json.JsonToCsv")

        // Create an instance of the script
        script = scriptClass.getDeclaredConstructor().newInstance() as Script

        // Mix in the trait to add extra methods and fields
        CPIScriptEnhancer.enhanceScript(script)
    }

    def setup() {
        this.msg = new MessageImpl()
    }

    def "Converts a simple JSON array of objects to CSV"() {
        given: "A JSON body representing a list of records"
        def jsonBody = '''
        [
            {"header_field1":"value1","header_field2":"value2"},
            {"header_field1":"value3","header_field2":"value4"}
        ]
        '''
        msg.setBody(jsonBody)

        when: "The script processes the message"
        script.processData(msg)

        then: "We get a CSV with headers on the first line and values on subsequent lines"
        msg.getBody() ==
                """header_field1,header_field2
value1,value2
value3,value4
"""
    }

    def "Converts a single-record JSON array to CSV"() {
        given: "A JSON body with just one record"
        def jsonBody = '''
        [
            {"single_header":"single_value"}
        ]
        '''
        msg.setBody(jsonBody)

        when:
        script.processData(msg)

        then:
        msg.getBody() == """single_header
single_value
"""
    }

    def "Throws an exception for an empty JSON array"() {
        given: "An empty JSON array"
        def jsonBody = '[]'
        msg.setBody(jsonBody)

        when:
        script.processData(msg)

        then:
        RuntimeException ex = thrown()
        ex.message.contains("CSV content must have at least one header line and one data line")
        // The actual message may differ; you may need to adjust your code or this test accordingly.
        // If your code doesn't currently throw a custom exception here, consider updating it.
    }

    def "Throws an exception if JSON is not a list of records"() {
        given: "A JSON object instead of an array"
        def jsonBody = '{"not":"an array"}'
        msg.setBody(jsonBody)

        when:
        script.processData(msg)

        then:
        RuntimeException ex = thrown()
        ex.message.contains("JSON content is not a list of records")
    }

    def "Handles records with differing fields gracefully"() {
        given: "A JSON body where the second record has different fields"
        def jsonBody = '''
        [
            {"header_field1":"value1","header_field2":"value2"},
            {"header_field1":"value3","header_field3":"value5"} 
        ]
        '''
        // Note: Your current code just takes headers from the first record.
        // The second record doesn't have `header_field2` and introduces `header_field3`.
        // Current logic: headers = header_field1,header_field2 from the first record.
        // For the second record, fields() call will just join whatever values it has.
        // This might lead to mismatched columns. If that’s unacceptable, you may need
        // to adjust your code to handle missing or extra fields.
        // For now, let's test the current behavior.

        msg.setBody(jsonBody)

        when:
        script.processData(msg)

        then:
        // The output will only have headers from the first record:
        msg.getBody() ==
                """header_field1,header_field2
value1,value2
value3,
"""
        // Notice the second line ends with a comma for the missing field2, since values() from a map
        // with different keys may not align. If you want consistent behavior, you'll need to
        // adjust your code to handle differing fields more robustly.
    }

    def "Throws an exception for invalid JSON"() {
        given: "A non-parseable JSON string"
        def jsonBody = 'this is not valid json'
        msg.setBody(jsonBody)

        when:
        script.processData(msg)

        then:
        thrown(groovy.json.JsonException)
        // You may catch a JsonException or other parse exceptions.
        // Adjust the test to match the actual exception thrown.
    }
}