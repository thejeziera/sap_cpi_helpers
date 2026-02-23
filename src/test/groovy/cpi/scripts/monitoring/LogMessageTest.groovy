package cpi.scripts.monitoring

import com.sap.gateway.ip.core.customdev.util.Message
import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import spock.lang.Shared
import spock.lang.Specification

class LogMessageTest extends Specification {

    @Shared
    Script script
    private Message msg
    @Shared
    private classLoader = new GroovyClassLoader()

    def setupSpec() {
        // Load Groovy Script by its package and class name
        Class scriptClass = classLoader.loadClass("cpi.scripts.monitoring.LogMessage")

        // Create an instance of the script
        script = scriptClass.getDeclaredConstructor().newInstance() as Script

        // Mix in the trait to add extra methods and fields
        CPIScriptEnhancer.enhanceScript(script)
    }

    def setup() {
        this.msg = new MessageImpl()
    }

    def "Message headers are logged to MPL as HEADER underscore key custom header properties"() {
        given:
        msg.setHeader("Header1", "V1")
        msg.setHeader("Header2", "V2")
        msg.setMessageLog(script.messageLogFactory.getMessageLog("LogMessage"))

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog.customHeaderPropertiesMap.get("HEADER_Header1") == "V1"
        mplLog.customHeaderPropertiesMap.get("HEADER_Header2") == "V2"
    }

    def "Message properties are logged to MPL as PROPERTY underscore key custom header properties"() {
        given:
        msg.setProperty("Prop1", "PV1")
        msg.setProperty("Prop2", "PV2")
        msg.setMessageLog(script.messageLogFactory.getMessageLog("LogMessage"))

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog.customHeaderPropertiesMap.get("PROPERTY_Prop1") == "PV1"
        mplLog.customHeaderPropertiesMap.get("PROPERTY_Prop2") == "PV2"
    }

    def "Message body is added to MPL as Message Body attachment with content type text/plain"() {
        given:
        msg.setBody("BODY_CONTENT")
        msg.setMessageLog(script.messageLogFactory.getMessageLog("LogMessage"))

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog.attachmentMap.get("Message Body;_;text/plain") == "BODY_CONTENT"
    }

    def "Null header value is handled gracefully and logged as empty string"() {
        given:
        msg.setHeader("NullHeader", null)
        msg.setMessageLog(script.messageLogFactory.getMessageLog("LogMessage"))

        when:
        script.processData(msg)

        then:
        notThrown(Exception)
        def mplLog = script.messageLogFactory.messageLog
        mplLog.customHeaderPropertiesMap.get("HEADER_NullHeader") == ""
    }

    def "Null body is handled gracefully and attachment content is empty string"() {
        given:
        msg.setBody(null)
        msg.setMessageLog(script.messageLogFactory.getMessageLog("LogMessage"))

        when:
        script.processData(msg)

        then:
        notThrown(Exception)
        def mplLog = script.messageLogFactory.messageLog
        mplLog.attachmentMap.get("Message Body;_;text/plain") == ""
    }

    def "No messageLog configured: script completes without exception and body is unchanged"() {
        given:
        msg.setBody("UNCHANGED")
        // intentionally do NOT call setMessageLog — messageLog in script will be null

        when:
        script.processData(msg)

        then:
        notThrown(Exception)
        msg.getBody() == "UNCHANGED"
    }

    def "Scope-creep guard: body unchanged, no headers or properties written to message"() {
        given:
        msg.setHeader("H", "V")
        msg.setProperty("P", "Q")
        msg.setBody("BODY")
        def headersBefore = new LinkedHashMap(msg.getHeaders())
        def propsBefore = new LinkedHashMap(msg.getProperties())

        when:
        script.processData(msg)

        then:
        msg.getBody() == "BODY"
        msg.getHeaders() == headersBefore
        msg.getProperties() == propsBefore
    }
}
