package cpi.scripts.data_transformation.xml

import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import spock.lang.Shared
import spock.lang.Specification

class XmlToCsvTest extends Specification {

    @Shared Script script
    @Shared private GroovyClassLoader classLoader = new GroovyClassLoader()
    private MessageImpl msg

    def setupSpec() {
        Class scriptClass = classLoader.loadClass("cpi.scripts.data_transformation.xml.XmlToCsv")
        script = scriptClass.getDeclaredConstructor().newInstance() as Script
        CPIScriptEnhancer.enhanceScript(script)
    }

    def setup() {
        msg = new MessageImpl()
    }

    // ---------------------------------------------------------------------------
    // Happy path
    // ---------------------------------------------------------------------------

    def "Happy path: multi-record XML produces header row and one data row per record"() {
        given:
        msg.setBody("<records><record><name>Alice</name><age>30</age></record><record><name>Bob</name><age>25</age></record></records>")

        and: "scope-creep snapshot"
        def headersBefore = new LinkedHashMap(msg.getHeaders())
        def propsBefore   = new LinkedHashMap(msg.getProperties())

        when:
        script.processData(msg)

        then:
        msg.getBody() == "name,age\nAlice,30\nBob,25"

        and: "no trailing newline"
        !(msg.getBody() as String).endsWith('\n')

        and: "no-scope-creep: no headers written"
        msg.getHeaders() == headersBefore

        and: "no-scope-creep: no properties written"
        msg.getProperties() == propsBefore
    }

    def "Happy path: single-record XML produces header row and one data row"() {
        given:
        msg.setBody("<root><record><id>1</id><value>hello</value></record></root>")

        when:
        script.processData(msg)

        then:
        msg.getBody() == "id,value\n1,hello"
    }

    // ---------------------------------------------------------------------------
    // Error handling
    // ---------------------------------------------------------------------------

    def "Null body throws IllegalArgumentException with missing or blank message"() {
        given:
        msg.setBody(null)

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "XML body is missing or blank"
    }

    def "Blank body throws IllegalArgumentException with missing or blank message"() {
        given:
        msg.setBody("   ")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "XML body is missing or blank"
    }

    def "XML with no record elements throws IllegalArgumentException with no record elements message"() {
        given:
        msg.setBody("<root><item><a>1</a></item></root>")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "No record elements found in XML"
    }

    // ---------------------------------------------------------------------------
    // Boundary / structural cases
    // ---------------------------------------------------------------------------

    def "Record element missing a field produces empty string for that column"() {
        given: "second record is missing the <age> element"
        msg.setBody("<records><record><name>Alice</name><age>30</age></record><record><name>Bob</name></record></records>")

        when:
        script.processData(msg)

        then:
        msg.getBody() == "name,age\nAlice,30\nBob,"
    }

    def "Alternative root element name is supported — records found anywhere in the tree"() {
        given: "records wrapped in a deeply nested root"
        msg.setBody("<envelope><payload><data><record><x>1</x><y>2</y></record><record><x>3</x><y>4</y></record></data></payload></envelope>")

        when:
        script.processData(msg)

        then:
        msg.getBody() == "x,y\n1,2\n3,4"
    }

    // ---------------------------------------------------------------------------
    // logMode behaviour
    // ---------------------------------------------------------------------------

    def "logMode=INFO logs xml_rowCount as MPL custom header property"() {
        given:
        msg.setBody("<records><record><col>A</col></record><record><col>B</col></record></records>")
        msg.setProperty("logMode", "INFO")

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog.customHeaderPropertiesMap["xml_rowCount"] == "2"
    }

    def "logMode=NONE (default): no MPL custom header properties added"() {
        given:
        msg.setBody("<records><record><col>A</col></record></records>")
        // logMode not set — defaults to NONE

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
        msg.setBody("<r><record><field>value</field></record></r>")
        msg.setHeader("existingHeader", "headerVal")
        msg.setProperty("existingProp", "propVal")

        and: "snapshot before execution"
        def headersBefore = new LinkedHashMap(msg.getHeaders())
        def propsBefore   = new LinkedHashMap(msg.getProperties())

        when:
        script.processData(msg)

        then: "body has been transformed"
        msg.getBody() == "field\nvalue"

        and: "headers unchanged"
        msg.getHeaders() == headersBefore

        and: "properties unchanged (logMode was not set, so no write occurs)"
        msg.getProperties() == propsBefore
    }
}
