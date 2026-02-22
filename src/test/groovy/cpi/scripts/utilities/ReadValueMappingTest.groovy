package cpi.scripts.utilities

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.ITApiFactory
import com.sap.it.api.mapping.ValueMappingApi
import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class ReadValueMappingTest extends Specification {

    @Shared Script script
    @Shared private GroovyClassLoader classLoader = new GroovyClassLoader()
    private MessageImpl msg

    def setupSpec() {
        Class scriptClass = classLoader.loadClass("cpi.scripts.utilities.ReadValueMapping")
        script = scriptClass.getDeclaredConstructor().newInstance() as Script
        CPIScriptEnhancer.enhanceScript(script)
    }

    def setup() {
        msg = new MessageImpl()
    }

    def cleanup() {
        // Restore ITApiFactory metaClass after every test to prevent test pollution
        GroovySystem.metaClassRegistry.removeMetaClass(ITApiFactory)
    }

    // Helper: sets all five required vm_ properties on msg
    private void setAllRequiredProperties(MessageImpl m) {
        m.setProperty("vm_srcAgency", "SRC_AGENCY")
        m.setProperty("vm_srcScheme", "SRC_SCHEME")
        m.setProperty("vm_srcValue",  "SRC_VALUE")
        m.setProperty("vm_tgtAgency", "TGT_AGENCY")
        m.setProperty("vm_tgtScheme", "TGT_SCHEME")
    }

    // -----------------------------------------------------------------------
    // Test 1: Happy path — all properties set → vm_result contains mapped value
    // -----------------------------------------------------------------------

    def "Happy path: all properties set -> vm_result property contains the mapped value"() {
        given:
        def mockApi = Stub(ValueMappingApi) {
            getMappedValue("SRC_AGENCY", "SRC_SCHEME", "SRC_VALUE", "TGT_AGENCY", "TGT_SCHEME") >> "MAPPED_RESULT"
        }
        ITApiFactory.metaClass.static.getService = { Class c, Object cfg -> mockApi }

        setAllRequiredProperties(msg)
        msg.setBody("original-body")

        when:
        script.processData(msg)

        then:
        msg.getProperties().get("vm_result") == "MAPPED_RESULT"

        and: "body is unchanged"
        msg.getBody() == "original-body"
    }

    // -----------------------------------------------------------------------
    // Test 2: @Unroll — required property missing (null) → IllegalArgumentException
    // -----------------------------------------------------------------------

    @Unroll
    def "Required property '#propName' missing (null) -> throws IllegalArgumentException"() {
        given:
        setAllRequiredProperties(msg)
        // Remove the property under test so it is absent (returns null on get)
        def removed = msg.getProperties().remove(propName)

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "${propName} property is required but was not set"

        where:
        propName << ["vm_srcAgency", "vm_srcScheme", "vm_srcValue", "vm_tgtAgency", "vm_tgtScheme"]
    }

    // -----------------------------------------------------------------------
    // Test 3: @Unroll — required property blank (empty string) → IllegalArgumentException
    // -----------------------------------------------------------------------

    @Unroll
    def "Required property '#propName' blank (empty string) -> throws IllegalArgumentException"() {
        given:
        setAllRequiredProperties(msg)
        // Override the property under test with an empty string
        msg.setProperty(propName, "")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "${propName} property is required but was not set"

        where:
        propName << ["vm_srcAgency", "vm_srcScheme", "vm_srcValue", "vm_tgtAgency", "vm_tgtScheme"]
    }

    // -----------------------------------------------------------------------
    // Test 4: ValueMappingApi unavailable (getService returns null) → IllegalStateException
    // -----------------------------------------------------------------------

    def "ValueMappingApi unavailable (getService returns null) -> throws IllegalStateException"() {
        given:
        ITApiFactory.metaClass.static.getService = { Class c, Object cfg -> null }
        setAllRequiredProperties(msg)

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "ValueMappingApi is not available"
    }

    // -----------------------------------------------------------------------
    // Test 5: getMappedValue returns null → IllegalStateException containing srcValue
    // -----------------------------------------------------------------------

    def "getMappedValue returns null -> throws IllegalStateException containing srcValue"() {
        given:
        def mockApi = Stub(ValueMappingApi) {
            getMappedValue(*_) >> null
        }
        ITApiFactory.metaClass.static.getService = { Class c, Object cfg -> mockApi }
        setAllRequiredProperties(msg)

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "No mapped value found for: SRC_VALUE"
    }

    // -----------------------------------------------------------------------
    // Test 6: getMappedValue returns empty string → IllegalStateException containing srcValue
    // -----------------------------------------------------------------------

    def "getMappedValue returns empty string -> throws IllegalStateException containing srcValue"() {
        given:
        def mockApi = Stub(ValueMappingApi) {
            getMappedValue(*_) >> ""
        }
        ITApiFactory.metaClass.static.getService = { Class c, Object cfg -> mockApi }
        setAllRequiredProperties(msg)

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "No mapped value found for: SRC_VALUE"
    }

    // -----------------------------------------------------------------------
    // Test 7: logMode=INFO → vm_srcValue and vm_result logged as MPL custom header properties
    // -----------------------------------------------------------------------

    def "logMode=INFO: vm_srcValue and vm_result logged as MPL custom header properties"() {
        given:
        def mockApi = Stub(ValueMappingApi) {
            getMappedValue("SRC_AGENCY", "SRC_SCHEME", "SRC_VALUE", "TGT_AGENCY", "TGT_SCHEME") >> "MAPPED_RESULT"
        }
        ITApiFactory.metaClass.static.getService = { Class c, Object cfg -> mockApi }

        setAllRequiredProperties(msg)
        msg.setProperty("logMode", "INFO")
        // Prime the factory so we can retrieve the log instance after processData
        msg.setMessageLog(script.messageLogFactory.getMessageLog("ReadValueMapping"))

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog != null
        mplLog.customHeaderPropertiesMap.get("vm_srcValue") == "SRC_VALUE"
        mplLog.customHeaderPropertiesMap.get("vm_result")   == "MAPPED_RESULT"
    }

    // -----------------------------------------------------------------------
    // Test 8: logMode=NONE (default) → no custom header properties added to MPL
    // -----------------------------------------------------------------------

    def "logMode=NONE (default): no custom header properties added to MPL"() {
        given:
        def mockApi = Stub(ValueMappingApi) {
            getMappedValue("SRC_AGENCY", "SRC_SCHEME", "SRC_VALUE", "TGT_AGENCY", "TGT_SCHEME") >> "MAPPED_RESULT"
        }
        ITApiFactory.metaClass.static.getService = { Class c, Object cfg -> mockApi }

        setAllRequiredProperties(msg)
        // logMode property deliberately omitted — should default to "NONE"
        msg.setMessageLog(script.messageLogFactory.getMessageLog("ReadValueMapping"))

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog != null
        mplLog.customHeaderPropertiesMap.isEmpty()
    }

    // -----------------------------------------------------------------------
    // Test 9: Scope-creep guard
    //   - body is unchanged
    //   - no headers are set by the script
    //   - properties contain exactly vm_result beyond the test inputs
    // -----------------------------------------------------------------------

    def "Scope-creep guard: body unchanged, no headers set, no extra properties beyond vm_result"() {
        given:
        def mockApi = Stub(ValueMappingApi) {
            getMappedValue("SRC_AGENCY", "SRC_SCHEME", "SRC_VALUE", "TGT_AGENCY", "TGT_SCHEME") >> "MAPPED_RESULT"
        }
        ITApiFactory.metaClass.static.getService = { Class c, Object cfg -> mockApi }

        msg.setBody("original-body")
        setAllRequiredProperties(msg)
        msg.setProperty("logMode", "NONE")

        // Snapshot state before execution
        def headersBefore = new LinkedHashMap(msg.getHeaders())
        def propsBefore   = new LinkedHashMap(msg.getProperties())

        when:
        script.processData(msg)

        then: "body is unchanged"
        msg.getBody() == "original-body"

        and: "no headers are set by the script"
        msg.getHeaders() == headersBefore

        and: "the only new property is vm_result — nothing else added"
        def propsAfter = new LinkedHashMap(msg.getProperties())
        def _ = propsAfter.remove("vm_result")
        propsAfter == propsBefore
    }
}