package cpi.scripts.monitoring

import com.sap.gateway.ip.core.customdev.util.Message
import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import spock.lang.Shared
import spock.lang.Specification

class SetXMessageIDTest extends Specification {

    @Shared Script script
    @Shared private GroovyClassLoader classLoader = new GroovyClassLoader()
    private Message msg

    def setupSpec() {
        Class scriptClass = classLoader.loadClass("cpi.scripts.monitoring.SetXMessageID")
        script = scriptClass.getDeclaredConstructor().newInstance() as Script
        CPIScriptEnhancer.enhanceScript(script)
    }

    def setup() {
        msg = new MessageImpl()
    }

    // -------------------------------------------------------------------------
    // 1. Happy path — X-Message-ID already present: value retained, not overwritten
    // -------------------------------------------------------------------------
    def "Happy path — X-Message-ID already present: header value is retained and not overwritten"() {
        given:
        msg.setHeader("X-Message-ID", "existing-id")
        msg.setHeader("SAP_MessageProcessingLogID", "sap-id")

        when:
        script.processData(msg)

        then:
        msg.getHeaders().get("X-Message-ID") == "existing-id"
    }

    // -------------------------------------------------------------------------
    // 2. Happy path — X-Message-ID absent; SAP_MessageProcessingLogID used as fallback
    // -------------------------------------------------------------------------
    def "Happy path — X-Message-ID absent, SAP_MessageProcessingLogID used as fallback"() {
        given:
        msg.setHeader("SAP_MessageProcessingLogID", "sap-mpl-id-001")

        when:
        script.processData(msg)

        then:
        msg.getHeaders().get("X-Message-ID") == "sap-mpl-id-001"
    }

    // -------------------------------------------------------------------------
    // 3. Blank X-Message-ID treated as absent
    // -------------------------------------------------------------------------
    def "Blank X-Message-ID is treated as absent and SAP_MessageProcessingLogID is used"() {
        given:
        msg.setHeader("X-Message-ID", "")
        msg.setHeader("SAP_MessageProcessingLogID", "sap-mpl-id-002")

        when:
        script.processData(msg)

        then:
        msg.getHeaders().get("X-Message-ID") == "sap-mpl-id-002"
    }

    // -------------------------------------------------------------------------
    // 4. Both headers absent — IllegalStateException with exact message
    // -------------------------------------------------------------------------
    def "Both headers absent throws IllegalStateException with expected message"() {
        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "Could not determine X-Message-ID: neither X-Message-ID nor SAP_MessageProcessingLogID header is present"
    }

    // -------------------------------------------------------------------------
    // 5. logMode=INFO, X-Message-ID already present: MPL contains the existing ID
    // -------------------------------------------------------------------------
    def "logMode=INFO and X-Message-ID already set: MPL contains the existing ID value"() {
        given:
        msg.setHeader("X-Message-ID", "trace-001")
        msg.setProperty("logMode", "INFO")
        msg.setMessageLog(script.messageLogFactory.getMessageLog("SetXMessageID"))

        when:
        script.processData(msg)

        then:
        script.messageLogFactory.messageLog.customHeaderPropertiesMap.get("X-Message-ID") == "trace-001"
    }

    // -------------------------------------------------------------------------
    // 6. logMode=INFO, X-Message-ID absent: MPL contains the SAP MPL log ID
    // -------------------------------------------------------------------------
    def "logMode=INFO and X-Message-ID absent: MPL contains the SAP MPL log ID value"() {
        given:
        msg.setHeader("SAP_MessageProcessingLogID", "sap-trace-007")
        msg.setProperty("logMode", "INFO")
        msg.setMessageLog(script.messageLogFactory.getMessageLog("SetXMessageID"))

        when:
        script.processData(msg)

        then:
        script.messageLogFactory.messageLog.customHeaderPropertiesMap.get("X-Message-ID") == "sap-trace-007"
    }

    // -------------------------------------------------------------------------
    // 7. logMode=NONE (default): no MPL custom header properties added
    // -------------------------------------------------------------------------
    def "logMode=NONE (default): no MPL custom header properties added"() {
        given:
        msg.setHeader("SAP_MessageProcessingLogID", "some-id")
        // logMode not set — defaults to NONE
        msg.setMessageLog(script.messageLogFactory.getMessageLog("SetXMessageID"))

        when:
        script.processData(msg)

        then:
        script.messageLogFactory.messageLog.customHeaderPropertiesMap.isEmpty()
    }

    // -------------------------------------------------------------------------
    // 8. Scope-creep guard — X-Message-ID already present: body/headers/properties unchanged
    // -------------------------------------------------------------------------
    def "Scope-creep guard — X-Message-ID already present: body unchanged, headers unchanged, no properties written"() {
        given:
        msg.setBody("BODY")
        msg.setHeader("X-Message-ID", "pre-set-id")

        def headersBefore = new LinkedHashMap(msg.getHeaders())
        def propsBefore   = new LinkedHashMap(msg.getProperties())

        when:
        script.processData(msg)

        then:
        msg.getBody() == "BODY"
        msg.getHeaders() == headersBefore
        msg.getProperties() == propsBefore
    }

    // -------------------------------------------------------------------------
    // 9. Scope-creep guard — X-Message-ID absent: only X-Message-ID header added,
    //    body unchanged, no properties written
    // -------------------------------------------------------------------------
    def "Scope-creep guard — X-Message-ID absent: body unchanged, only X-Message-ID header added, no properties written"() {
        given:
        msg.setBody("BODY")
        msg.setHeader("SAP_MessageProcessingLogID", "sap-id-999")

        def headersBefore = new LinkedHashMap(msg.getHeaders())
        def propsBefore   = new LinkedHashMap(msg.getProperties())

        when:
        script.processData(msg)

        then:
        msg.getBody() == "BODY"
        msg.getProperties() == propsBefore
        msg.getHeaders().get("X-Message-ID") == "sap-id-999"
        // Only X-Message-ID was added — no other headers appeared
        msg.getHeaders().keySet() - headersBefore.keySet() == ["X-Message-ID"] as Set
    }
}
