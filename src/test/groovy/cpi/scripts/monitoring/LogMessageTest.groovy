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

    def "Initial test"() {
        given: "body is set to a sample message"
        this.msg.setBody("TEST")

        when: "we execute the Groovy script"
        script.processData(this.msg)

        then: "script is executed"
        this.msg.getBody() != null
    }

    def "Given set of headers and properties when script is executed then they are put in messageLog properties"() {
        given: "a sample headers, properties and body"
        this.msg.setBody("TEST")
        this.msg.setHeader("Header1", "HeaderValue1")
        this.msg.setHeader("Header2", "HeaderValue2")
        this.msg.setProperty("Property1", "PropertyValue1")
        this.msg.setMessageLog(this.script.messageLogFactory.getMessageLog(" "))

        when: "we execute the Groovy script"
        script.processData(this.msg)

        then: "messageLog contains all the headers, properties and body as attachment"
        this.msg.getMessageLog().printMessageLogContent()
    }
}