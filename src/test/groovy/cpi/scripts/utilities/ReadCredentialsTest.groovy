package cpi.scripts.utilities

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.ITApiFactory
import com.sap.it.api.securestore.SecureStoreService
import com.sap.it.api.securestore.UserCredential
import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import spock.lang.Shared
import spock.lang.Specification

class ReadCredentialsTest extends Specification {

    @Shared Script script
    @Shared private GroovyClassLoader classLoader = new GroovyClassLoader()
    private MessageImpl msg

    def setupSpec() {
        Class scriptClass = classLoader.loadClass("cpi.scripts.utilities.ReadCredentials")
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

    // -----------------------------------------------------------------------
    // Test 1: Happy path — valid credentialAlias resolves username and password
    // -----------------------------------------------------------------------

    def "Happy path: valid credentialAlias results in username and password set as properties"() {
        given:
        def mockCredential = Stub(UserCredential) {
            getUsername() >> "testUser"
            getPassword() >> "testPass"
        }
        def mockService = Stub(SecureStoreService) {
            getUserCredential("myAlias") >> mockCredential
        }
        ITApiFactory.metaClass.static.getApi = { Class c, Object cfg -> mockService }

        msg.setBody("unchanged-body")
        msg.setProperty("credentialAlias", "myAlias")

        when:
        script.processData(msg)

        then:
        msg.getProperties().get("username") == "testUser"
        msg.getProperties().get("password") == "testPass"

        and: "body is unchanged"
        msg.getBody() == "unchanged-body"
    }

    // -----------------------------------------------------------------------
    // Test 2: Missing credentialAlias property (null) → IllegalArgumentException
    // -----------------------------------------------------------------------

    def "Missing credentialAlias property (null) throws IllegalArgumentException"() {
        given:
        // credentialAlias property is not set — getProperties().get("credentialAlias") returns null
        msg.setBody("some-body")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "credentialAlias property is required but was not set"
    }

    // -----------------------------------------------------------------------
    // Test 3: Blank credentialAlias property (empty string) → IllegalArgumentException
    // -----------------------------------------------------------------------

    def "Blank credentialAlias property (empty string) throws IllegalArgumentException"() {
        given:
        msg.setProperty("credentialAlias", "")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "credentialAlias property is required but was not set"
    }

    // -----------------------------------------------------------------------
    // Test 4: SecureStoreService unavailable (ITApiFactory returns null) → IllegalStateException
    // -----------------------------------------------------------------------

    def "SecureStoreService unavailable (ITApiFactory returns null) throws IllegalStateException"() {
        given:
        ITApiFactory.metaClass.static.getApi = { Class c, Object cfg -> null }
        msg.setProperty("credentialAlias", "myAlias")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "SecureStoreService is not available"
    }

    // -----------------------------------------------------------------------
    // Test 5: Credential not found for alias → IllegalStateException with alias name
    // -----------------------------------------------------------------------

    def "Credential not found for alias throws IllegalStateException containing the alias"() {
        given:
        def mockService = Stub(SecureStoreService) {
            getUserCredential("unknownAlias") >> null
        }
        ITApiFactory.metaClass.static.getApi = { Class c, Object cfg -> mockService }
        msg.setProperty("credentialAlias", "unknownAlias")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "No credential found for alias: unknownAlias"
    }

    // -----------------------------------------------------------------------
    // Test 6: logMode=INFO → credentialAlias logged as MPL custom header property
    // -----------------------------------------------------------------------

    def "logMode=INFO: credentialAlias key is present in MPL custom header properties"() {
        given:
        def mockCredential = Stub(UserCredential) {
            getUsername() >> "testUser"
            getPassword() >> "testPass"
        }
        def mockService = Stub(SecureStoreService) {
            getUserCredential("myAlias") >> mockCredential
        }
        ITApiFactory.metaClass.static.getApi = { Class c, Object cfg -> mockService }

        msg.setProperty("credentialAlias", "myAlias")
        msg.setProperty("logMode", "INFO")
        // Prime the factory so we can retrieve the log instance after processData
        msg.setMessageLog(script.messageLogFactory.getMessageLog("ReadCredentials"))

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog != null
        mplLog.customHeaderPropertiesMap.containsKey("credentialAlias")
        mplLog.customHeaderPropertiesMap.get("credentialAlias") == "myAlias"
    }

    // -----------------------------------------------------------------------
    // Test 7: logMode=NONE (default) → credentialAlias NOT present in MPL
    // -----------------------------------------------------------------------

    def "logMode=NONE (default): credentialAlias is NOT added to MPL custom header properties"() {
        given:
        def mockCredential = Stub(UserCredential) {
            getUsername() >> "testUser"
            getPassword() >> "testPass"
        }
        def mockService = Stub(SecureStoreService) {
            getUserCredential("myAlias") >> mockCredential
        }
        ITApiFactory.metaClass.static.getApi = { Class c, Object cfg -> mockService }

        msg.setProperty("credentialAlias", "myAlias")
        // logMode property deliberately omitted — should default to "NONE"
        msg.setMessageLog(script.messageLogFactory.getMessageLog("ReadCredentials"))

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog != null
        !mplLog.customHeaderPropertiesMap.containsKey("credentialAlias")
    }

    // -----------------------------------------------------------------------
    // Test 8: Scope-creep guard
    //   - body is unchanged
    //   - no headers are set by the script
    //   - properties contain exactly username + password beyond the test inputs
    // -----------------------------------------------------------------------

    def "Scope-creep guard: body unchanged, no headers set, no extra properties beyond username and password"() {
        given:
        def mockCredential = Stub(UserCredential) {
            getUsername() >> "testUser"
            getPassword() >> "testPass"
        }
        def mockService = Stub(SecureStoreService) {
            getUserCredential("myAlias") >> mockCredential
        }
        ITApiFactory.metaClass.static.getApi = { Class c, Object cfg -> mockService }

        msg.setBody("original-body")
        msg.setProperty("credentialAlias", "myAlias")
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

        and: "the only new properties are username and password — nothing else added"
        def propsAfter = new LinkedHashMap(msg.getProperties())
        def _ = propsAfter.remove("username")
        def __ = propsAfter.remove("password")
        propsAfter == propsBefore
    }
}