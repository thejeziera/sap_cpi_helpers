package cpi.scripts.protocol_adapters.http

import com.sap.gateway.ip.core.customdev.processor.MessageImpl
import com.sap.gateway.ip.core.customdev.util.Message
import cpi.utils.CPIScriptEnhancer
import spock.lang.Shared
import spock.lang.Specification

class PrepareMultipartMessageTest extends Specification{

    @Shared
    Script script
    private Message msg
    @Shared
    private classLoader = new GroovyClassLoader()

    def setupSpec() {
        // Load Groovy Script by its package and class name
        Class scriptClass = classLoader.loadClass("cpi.scripts.protocol_adapters.http.PrepareMultipartMessage")

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
        this.msg.setProperty("p_attachment_filename", "test.png")
        this.msg.setProperty("p_attachment_filetype", "png")
        this.msg.setProperty("p_expense_body", "{root: \"test\"}")

        when: "we execute the Groovy script"
        script.processData(this.msg)

        then: "script is executed"
        this.msg.getBody() != null
    }
}