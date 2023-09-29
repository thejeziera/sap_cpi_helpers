package cpi.scripts.data_transformation.xml

import com.sap.gateway.ip.core.customdev.processor.MessageImpl
import com.sap.gateway.ip.core.customdev.util.Message
import cpi.utils.CPIScriptEnhancer
import spock.lang.Shared
import spock.lang.Specification

class XmlToCsvTest extends Specification{

    @Shared
    Script script
    private Message msg
    @Shared
    private classLoader = new GroovyClassLoader()

    def setupSpec() {
        // Load Groovy Script by its package and class name
        Class scriptClass = classLoader.loadClass("cpi.scripts.data_transformation.xml.XmlToCsv")

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
}