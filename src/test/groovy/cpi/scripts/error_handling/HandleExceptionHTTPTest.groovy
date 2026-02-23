package cpi.scripts.error_handling

import com.sap.gateway.ip.core.customdev.util.Message
import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class HandleExceptionHTTPTest extends Specification {

    @Shared Script script
    @Shared private GroovyClassLoader classLoader = new GroovyClassLoader()
    private Message msg

    def setupSpec() {
        Class scriptClass = classLoader.loadClass("cpi.scripts.error_handling.HandleExceptionHTTP")
        script = scriptClass.getDeclaredConstructor().newInstance() as Script
        CPIScriptEnhancer.enhanceScript(script)
    }

    def setup() {
        msg = new MessageImpl()
    }

    @Unroll
    def "HTTP #code response: p_error_code is HTTP_#code and p_status is error"() {
        given:
        msg.setHeader("CamelHttpResponseCode", code)
        msg.setProperty("p_current_step", "TestStep")
        msg.setProperty("CamelExceptionCaught", "HTTP error from server")

        when:
        script.processData(msg)

        then:
        msg.getProperty("p_error_code") == "HTTP_${code}"
        msg.getProperty("p_status") == "error"

        where:
        code << [400, 401, 500, 503]
    }

    def "HTTP response: p_message contains both the step name and the HTTP status code"() {
        given:
        msg.setHeader("CamelHttpResponseCode", 404)
        msg.setProperty("p_current_step", "CallReceiver")
        msg.setProperty("CamelExceptionCaught", "404 Not Found")

        when:
        script.processData(msg)

        then:
        (msg.getProperty("p_message") as String).contains("CallReceiver")
        (msg.getProperty("p_message") as String).contains("404")
    }

    def "Exception containing 'mapping' (no HTTP code): p_error_code is MAP_ERROR and p_status is error"() {
        given:
        msg.setProperty("CamelExceptionCaught", "Field mapping failed: missing required field")
        msg.setProperty("p_current_step", "MappingStep")

        when:
        script.processData(msg)

        then:
        msg.getProperty("p_error_code") == "MAP_ERROR"
        msg.getProperty("p_status") == "error"
        (msg.getProperty("p_message") as String).contains("MappingStep")
    }

    def "Generic exception (no HTTP code, no 'mapping' keyword): p_error_code is CPI_ERROR"() {
        given:
        msg.setProperty("CamelExceptionCaught", "Connection refused to remote system")
        msg.setProperty("p_current_step", "OutboundStep")

        when:
        script.processData(msg)

        then:
        msg.getProperty("p_error_code") == "CPI_ERROR"
        msg.getProperty("p_status") == "error"
    }

    def "Null CamelExceptionCaught is handled without NPE and defaults to CPI_ERROR"() {
        given:
        msg.setProperty("p_current_step", "SomeStep")
        // CamelExceptionCaught intentionally absent

        when:
        script.processData(msg)

        then:
        notThrown(Exception)
        msg.getProperty("p_error_code") == "CPI_ERROR"
        msg.getProperty("p_status") == "error"
    }

    def "MPL receives payload, headers, and properties attachments"() {
        given:
        msg.setBody("TEST_BODY")
        msg.setHeader("H", "V")
        msg.setProperty("CamelExceptionCaught", "err")
        msg.setMessageLog(script.messageLogFactory.getMessageLog("HandleExceptionHTTP"))

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog.attachmentMap.get("payload;_;text/plain") == "TEST_BODY"
        mplLog.attachmentMap.containsKey("headers;_;text/plain")
        mplLog.attachmentMap.containsKey("properties;_;text/plain")
    }

    def "Scope-creep guard: body unchanged, no headers written, only p_status/p_message/p_error_code properties added"() {
        given:
        msg.setBody("ORIG")
        msg.setProperty("CamelExceptionCaught", "err")
        msg.setProperty("p_current_step", "S")
        def headersBefore = new LinkedHashMap(msg.getHeaders())
        def propsBefore   = new LinkedHashMap(msg.getProperties())

        when:
        script.processData(msg)

        then:
        msg.getBody() == "ORIG"
        msg.getHeaders() == headersBefore
        msg.getProperties().keySet() - propsBefore.keySet() == ["p_status", "p_message", "p_error_code"] as Set
    }
}
