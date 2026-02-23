package cpi.scripts.protocol_adapters.http

import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class PrepareMultipartMessageTest extends Specification {

    @Shared Script script
    @Shared private GroovyClassLoader classLoader = new GroovyClassLoader()
    private MessageImpl msg

    def setupSpec() {
        Class scriptClass = classLoader.loadClass("cpi.scripts.protocol_adapters.http.PrepareMultipartMessage")
        script = scriptClass.getDeclaredConstructor().newInstance() as Script
        CPIScriptEnhancer.enhanceScript(script)
    }

    def setup() {
        msg = new MessageImpl()
    }

    // -----------------------------------------------------------------------
    // Helper: sets all required properties; optional ones are left unset
    // so that default values apply inside the script.
    // -----------------------------------------------------------------------
    private void setAllRequiredProperties(MessageImpl m) {
        m.setProperty("mp_textContent",   '{"key":"value"}')
        m.setProperty("mp_imageName",     "photo.png")
        m.setProperty("mp_imageMimeType", "image/png")
    }

    // Deterministic 8-byte PNG magic header used as binary body in tests
    private static final byte[] PNG_MAGIC = [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A] as byte[]

    // -----------------------------------------------------------------------
    // Test 1: happy path — body is byte[], Content-Type header set correctly
    // -----------------------------------------------------------------------
    def "Happy path: binary body and all required properties produce byte[] body with Content-Type header"() {
        given:
        setAllRequiredProperties(msg)
        msg.setBody(PNG_MAGIC)

        when:
        script.processData(msg)

        then:
        msg.getBody() instanceof byte[]
        (msg.getHeaders().get("Content-Type") as String).startsWith("multipart/form-data; boundary=")
    }

    // -----------------------------------------------------------------------
    // Test 2: multipart content includes text payload value and image filename
    // -----------------------------------------------------------------------
    def "Multipart content contains text payload and image filename"() {
        given:
        setAllRequiredProperties(msg)
        msg.setBody(PNG_MAGIC)

        when:
        script.processData(msg)

        then:
        def bodyStr = new String(msg.getBody() as byte[], "UTF-8")
        bodyStr.contains('{"key":"value"}')
        bodyStr.contains("photo.png")
    }

    // -----------------------------------------------------------------------
    // Test 3: required property null → IllegalArgumentException
    // -----------------------------------------------------------------------
    @Unroll
    def "Required property '#propName' is null throws IllegalArgumentException"() {
        given:
        setAllRequiredProperties(msg)
        msg.setBody(PNG_MAGIC)
        // Remove the property under test so it is null when the script reads it
        def removed = msg.getProperties().remove(propName)

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "${propName} property is required but was not set"

        where:
        propName << ["mp_textContent", "mp_imageName", "mp_imageMimeType"]
    }

    // -----------------------------------------------------------------------
    // Test 4: required property blank → IllegalArgumentException
    // -----------------------------------------------------------------------
    @Unroll
    def "Required property '#propName' is blank throws IllegalArgumentException"() {
        given:
        setAllRequiredProperties(msg)
        msg.setBody(PNG_MAGIC)
        // Overwrite the property under test with an empty string
        msg.setProperty(propName, "")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "${propName} property is required but was not set"

        where:
        propName << ["mp_textContent", "mp_imageName", "mp_imageMimeType"]
    }

    // -----------------------------------------------------------------------
    // Test 5: empty byte[] body throws IllegalArgumentException
    // -----------------------------------------------------------------------
    def "Empty byte[] body throws IllegalArgumentException with 'Image body is missing or empty'"() {
        given:
        setAllRequiredProperties(msg)
        msg.setBody(new byte[0])

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Image body is missing or empty"
    }

    // -----------------------------------------------------------------------
    // Test 6: logMode=INFO → mp_imageName, mp_imageMimeType, mp_imageBytes logged
    // -----------------------------------------------------------------------
    def "logMode=INFO logs mp_imageName, mp_imageMimeType and mp_imageBytes as MPL custom header properties"() {
        given:
        setAllRequiredProperties(msg)
        msg.setBody(PNG_MAGIC)
        msg.setProperty("logMode", "INFO")
        msg.setMessageLog(script.messageLogFactory.getMessageLog("PrepareMultipartMessage"))

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog.customHeaderPropertiesMap["mp_imageName"]    == "photo.png"
        mplLog.customHeaderPropertiesMap["mp_imageMimeType"] == "image/png"
        mplLog.customHeaderPropertiesMap["mp_imageBytes"]   == "8"
    }

    // -----------------------------------------------------------------------
    // Test 7: logMode=NONE (default) → no MPL custom header properties added
    // -----------------------------------------------------------------------
    def "logMode=NONE (default): no MPL custom header properties added"() {
        given:
        setAllRequiredProperties(msg)
        msg.setBody(PNG_MAGIC)
        // logMode not set — default NONE applies
        msg.setMessageLog(script.messageLogFactory.getMessageLog("PrepareMultipartMessage"))

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog.customHeaderPropertiesMap.isEmpty()
    }

    // -----------------------------------------------------------------------
    // Test 8: scope-creep guard — only Content-Type header added, no
    // properties written, body type is byte[]
    // -----------------------------------------------------------------------
    def "Scope-creep guard: only Content-Type header added, no properties written, body is byte[]"() {
        given:
        setAllRequiredProperties(msg)
        msg.setBody(PNG_MAGIC)

        and: "snapshot of properties and headers before execution"
        def propsBefore   = new LinkedHashMap(msg.getProperties())

        when:
        script.processData(msg)

        then: "no properties were added or changed"
        msg.getProperties() == propsBefore

        and: "only Content-Type header is present"
        msg.getHeaders().keySet() == ["Content-Type"].toSet()

        and: "body is a byte array"
        msg.getBody() instanceof byte[]
    }
}