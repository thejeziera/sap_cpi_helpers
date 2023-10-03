package cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message
import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import spock.lang.Shared
import spock.lang.Specification

class ScriptTemplateTest extends Specification{

    @Shared
    Script script
    private Message msg
    @Shared
    private classLoader = new GroovyClassLoader()

    def setupSpec() {
        // Load Groovy Script by its package and class name
        Class scriptClass = classLoader.loadClass("cpi.scripts.ScriptTemplate")

        // Create an instance of the script
        script = scriptClass.getDeclaredConstructor().newInstance() as Script

        // Mix in the trait to add extra methods and fields
        CPIScriptEnhancer.enhanceScript(script)
    }

    def setup() {
        this.msg = new MessageImpl()
    }

    def "Given a non-error response code, the message body should remain unchanged"() {
        given: "body is set to a sample message"
        this.msg.setBody("TEST VALUE")
        this.msg.setProperty("p_error_code", "200")

        when: "we execute the Groovy script"
        script.processData(this.msg)

        then: "body is not changed"
        this.msg.getBody() == "TEST VALUE"
    }

    def "Given an HTTP error response code, the property 'p_error_code' should be prefixed with 'HTTP_'"() {
        def errorCode = 400

        given: "body is set to a sample message"
        this.msg.setBody("TEST VALUE")
        this.msg.setProperty("p_error_code", errorCode)

        when: "we execute the Groovy script"
        script.processData(this.msg)

        then: "p_error_code is set with HTTP_ prefix"
        this.msg.getProperty("p_error_code") == ("HTTP_" + errorCode)
    }
}