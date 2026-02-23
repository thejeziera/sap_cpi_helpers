package cpi.scripts.protocol_adapters.odata

import com.sap.gateway.ip.core.customdev.util.Message
import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class ODataQueryConstructorTest extends Specification {

    @Shared Script script
    @Shared private GroovyClassLoader classLoader = new GroovyClassLoader()
    private Message msg

    def setupSpec() {
        Class scriptClass = classLoader.loadClass("cpi.scripts.protocol_adapters.odata.ODataQueryConstructor")
        script = scriptClass.getDeclaredConstructor().newInstance() as Script
        CPIScriptEnhancer.enhanceScript(script)
    }

    def setup() {
        msg = new MessageImpl()
    }

    def "Happy path — filter only: odata_queryString contains dollar-filter and no other clauses"() {
        given:
        msg.setProperty("odata_filter", "City eq 'Berlin'")

        when:
        script.processData(msg)

        then:
        msg.getProperties().get("odata_queryString") == '$filter=City eq \'Berlin\''
    }

    def "Happy path — select only: odata_queryString contains dollar-select and no other clauses"() {
        given:
        msg.setProperty("odata_select", "Name,City,Phone")

        when:
        script.processData(msg)

        then:
        msg.getProperties().get("odata_queryString") == '$select=Name,City,Phone'
    }

    def "Happy path — expand only: odata_queryString contains dollar-expand and no other clauses"() {
        given:
        msg.setProperty("odata_expand", "Orders")

        when:
        script.processData(msg)

        then:
        msg.getProperties().get("odata_queryString") == '$expand=Orders'
    }

    def "Happy path — all three options produce correctly ordered and delimited query string"() {
        given:
        msg.setProperty("odata_filter", "Status eq 'A'")
        msg.setProperty("odata_select", "Name,Status")
        msg.setProperty("odata_expand", "Items")

        when:
        script.processData(msg)

        then:
        msg.getProperties().get("odata_queryString") == '$filter=Status eq \'A\'&$select=Name,Status&$expand=Items'
    }

    @Unroll
    def "Two-option combination with #combo produces correctly joined query string"() {
        given:
        if (filterVal != null) msg.setProperty("odata_filter", filterVal)
        if (selectVal != null) msg.setProperty("odata_select", selectVal)
        if (expandVal != null) msg.setProperty("odata_expand", expandVal)

        when:
        script.processData(msg)

        then:
        msg.getProperties().get("odata_queryString") == expectedQuery

        where:
        combo            | filterVal   | selectVal | expandVal | expectedQuery
        "filter+select"  | "F eq 'X'"  | "Name"    | null      | '$filter=F eq \'X\'&$select=Name'
        "filter+expand"  | "F eq 'X'"  | null      | "Nav"     | '$filter=F eq \'X\'&$expand=Nav'
        "select+expand"  | null        | "Name"    | "Nav"     | '$select=Name&$expand=Nav'
    }

    def "Whitespace in property values is trimmed before assembly"() {
        given:
        msg.setProperty("odata_filter", "  Status eq 'A'  ")

        when:
        script.processData(msg)

        then:
        msg.getProperties().get("odata_queryString") == '$filter=Status eq \'A\''
    }

    def "No options provided (all null) throws IllegalArgumentException"() {
        given: "no odata_* properties set"
        // no setup needed — msg has empty properties by default

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "At least one of odata_filter, odata_select, or odata_expand must be provided"
    }

    def "logMode=INFO — all provided options and odata_queryString logged as MPL custom header properties"() {
        given:
        msg.setProperty("odata_filter", "Status eq 'A'")
        msg.setProperty("odata_select", "Name,Status")
        msg.setProperty("odata_expand", "Items")
        msg.setProperty("logMode", "INFO")
        msg.setMessageLog(script.messageLogFactory.getMessageLog("ODataQueryConstructor"))

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog.customHeaderPropertiesMap.get("odata_filter")      == "Status eq 'A'"
        mplLog.customHeaderPropertiesMap.get("odata_select")      == "Name,Status"
        mplLog.customHeaderPropertiesMap.get("odata_expand")      == "Items"
        mplLog.customHeaderPropertiesMap.get("odata_queryString") == '$filter=Status eq \'A\'&$select=Name,Status&$expand=Items'
    }

    def "logMode=INFO — only provided options are logged (absent options not in MPL map)"() {
        given:
        msg.setProperty("odata_filter", "City eq 'B'")
        msg.setProperty("logMode", "INFO")
        msg.setMessageLog(script.messageLogFactory.getMessageLog("ODataQueryConstructor"))

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog.customHeaderPropertiesMap.containsKey("odata_filter")      == true
        mplLog.customHeaderPropertiesMap.containsKey("odata_queryString") == true
        mplLog.customHeaderPropertiesMap.containsKey("odata_select")      == false
        mplLog.customHeaderPropertiesMap.containsKey("odata_expand")      == false
    }

    def "logMode=NONE (default): no MPL custom header properties added"() {
        given:
        msg.setProperty("odata_filter", "City eq 'Berlin'")
        msg.setProperty("odata_select", "Name,City")
        msg.setProperty("odata_expand", "Orders")
        // logMode not set — defaults to NONE
        msg.setMessageLog(script.messageLogFactory.getMessageLog("ODataQueryConstructor"))

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog.customHeaderPropertiesMap.isEmpty()
    }

    def "Scope-creep guard: body unchanged, no headers set, only odata_queryString property added"() {
        given:
        msg.setBody("ORIGINAL_BODY")
        msg.setProperty("odata_filter", "Name eq 'Test'")

        def headersBefore = new LinkedHashMap<>(msg.getHeaders())
        def propsBefore   = new LinkedHashMap<>(msg.getProperties())

        when:
        script.processData(msg)

        then: "body is untouched"
        msg.getBody() == "ORIGINAL_BODY"

        and: "no headers were added or changed"
        msg.getHeaders() == headersBefore

        and: "exactly odata_queryString was added to properties; all prior properties remain"
        def propsAfter = msg.getProperties()
        propsAfter.get("odata_queryString") == '$filter=Name eq \'Test\''
        propsBefore.every { k, v -> propsAfter.get(k) == v }
        propsAfter.keySet() - propsBefore.keySet() == ["odata_queryString"] as Set
    }
}