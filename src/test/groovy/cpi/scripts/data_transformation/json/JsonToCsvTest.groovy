package cpi.scripts.data_transformation.json

import com.sap.gateway.ip.core.customdev.util.Message
import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class JsonToCsvTest extends Specification {

    @Shared Script script
    @Shared private GroovyClassLoader classLoader = new GroovyClassLoader()
    private Message msg

    def setupSpec() {
        Class scriptClass = classLoader.loadClass("cpi.scripts.data_transformation.json.JsonToCsv")
        script = scriptClass.getDeclaredConstructor().newInstance() as Script
        CPIScriptEnhancer.enhanceScript(script)
    }

    def setup() {
        msg = new MessageImpl()
    }

    // ---------------------------------------------------------------------------
    // Happy path
    // ---------------------------------------------------------------------------

    def "Happy path: two-record JSON array produces header row and two data rows"() {
        given:
        def jsonBody = '[{"col1":"a","col2":"b"},{"col1":"c","col2":"d"}]'
        msg.setBody(jsonBody)

        and: "snapshot for scope-creep guard"
        def headersBefore = new LinkedHashMap(msg.getHeaders())
        def propsBefore   = new LinkedHashMap(msg.getProperties())

        when:
        script.processData(msg)

        then:
        msg.getBody() == "col1,col2\na,b\nc,d"

        and: "no-scope-creep: no headers or properties written"
        msg.getHeaders() == headersBefore
        msg.getProperties() == propsBefore
    }

    def "Happy path: single-record JSON array produces header row and one data row"() {
        given:
        def jsonBody = '[{"id":"1","name":"Alice"}]'
        msg.setBody(jsonBody)

        when:
        script.processData(msg)

        then:
        msg.getBody() == "id,name\n1,Alice"
    }

    // ---------------------------------------------------------------------------
    // Error handling
    // ---------------------------------------------------------------------------

    @Unroll
    def "Null or blank body throws IllegalArgumentException with missing or blank message — body=#label"() {
        given:
        msg.setBody(bodyValue)

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "JSON body is missing or blank"

        where:
        label   | bodyValue
        "null"  | null
        "empty" | ""
        "blank" | "   "
    }

    def "Non-array JSON throws IllegalArgumentException with must be a JSON array message"() {
        given:
        msg.setBody('{"not":"an array"}')

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "JSON body must be a JSON array"
    }

    def "Empty JSON array throws IllegalArgumentException with must contain at least one record"() {
        given:
        msg.setBody('[]')

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "JSON array must contain at least one record"
    }

    // ---------------------------------------------------------------------------
    // Boundary / transformation correctness
    // ---------------------------------------------------------------------------

    def "Record with missing field produces empty string for that column"() {
        given:
        // Second record is missing col2; col3 (extra key) must be ignored
        def jsonBody = '[{"col1":"v1","col2":"v2"},{"col1":"v3","col3":"v5"}]'
        msg.setBody(jsonBody)

        when:
        script.processData(msg)

        then:
        // col2 is missing in second record -> empty string; col3 is extra -> ignored
        msg.getBody() == "col1,col2\nv1,v2\nv3,"
    }

    def "Header order is determined by first record key order"() {
        given:
        // Groovy JsonSlurper preserves insertion order for LinkedHashMap
        def jsonBody = '[{"z":"zv","a":"av","m":"mv"}]'
        msg.setBody(jsonBody)

        when:
        script.processData(msg)

        then:
        def lines = (msg.getBody() as String).split('\n')
        lines[0] == "z,a,m"
        lines[1] == "zv,av,mv"
    }

    // ---------------------------------------------------------------------------
    // Logging behaviour
    // ---------------------------------------------------------------------------

    def "logMode=INFO logs json_rowCount as MPL custom header property"() {
        given:
        def jsonBody = '[{"x":"1"},{"x":"2"},{"x":"3"}]'
        msg.setBody(jsonBody)
        msg.setProperty("logMode", "INFO")

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog.customHeaderPropertiesMap["json_rowCount"] == "3"
    }

    def "logMode=NONE (default): no MPL custom header properties added"() {
        given:
        def jsonBody = '[{"x":"1"},{"x":"2"}]'
        msg.setBody(jsonBody)
        // logMode property intentionally not set — default NONE

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog.customHeaderPropertiesMap.isEmpty()
    }

    // ---------------------------------------------------------------------------
    // Scope-creep guard
    // ---------------------------------------------------------------------------

    def "Scope-creep guard: no headers set, no properties written, only body changed to CSV"() {
        given:
        msg.setBody('[{"k":"v"}]')
        msg.setHeader("existingHeader", "hval")
        msg.setProperty("existingProp", "pval")

        and: "snapshot before"
        def headersBefore = new LinkedHashMap(msg.getHeaders())
        def propsBefore   = new LinkedHashMap(msg.getProperties())

        when:
        script.processData(msg)

        then:
        // Only body changed
        msg.getBody() == "k\nv"

        and: "headers untouched"
        msg.getHeaders() == headersBefore

        and: "properties untouched (logMode was never set so none added)"
        msg.getProperties() == propsBefore
    }
}
