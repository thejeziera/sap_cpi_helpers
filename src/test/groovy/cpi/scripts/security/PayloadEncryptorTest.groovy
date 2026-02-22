package cpi.scripts.security

import com.sap.it.api.ITApiFactory
import com.sap.it.api.securestore.SecureStoreService
import com.sap.it.api.securestore.UserCredential
import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import spock.lang.Shared
import spock.lang.Specification

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.Arrays
import java.util.Base64

class PayloadEncryptorTest extends Specification {

    @Shared Script script
    @Shared private GroovyClassLoader classLoader = new GroovyClassLoader()
    private MessageImpl msg

    def setupSpec() {
        Class scriptClass = classLoader.loadClass("cpi.scripts.security.PayloadEncryptor")
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

    // Helper: wire a mock SecureStoreService returning a 16-zero-byte AES key ("AAAAAAAAAAAAAAAAAAAAAA==")
    private void mockStoreWithZeroKey(String alias) {
        def mockCredential = Stub(UserCredential) {
            getPassword() >> "AAAAAAAAAAAAAAAAAAAAAA=="
        }
        def mockService = Stub(SecureStoreService) {
            getUserCredential(alias) >> mockCredential
        }
        ITApiFactory.metaClass.static.getApi = { Class c, Object cfg -> mockService }
    }

    // -----------------------------------------------------------------------
    // Test 1: Happy path — encrypted body is valid Base64; decoded length == 16 + 16
    // -----------------------------------------------------------------------

    def "Happy path: plaintext encrypted → body is valid Base64, decoded length is IV(16) plus padded ciphertext"() {
        given:
        mockStoreWithZeroKey("testAlias")
        msg.setBody("Hello!")
        msg.setProperty("enc_credentialAlias", "testAlias")

        when:
        script.processData(msg)

        then: "body is not the original plaintext"
        msg.getBody() != "Hello!"

        and: "body is valid Base64"
        def decoded = Base64.getDecoder().decode(msg.getBody() as String)
        // "Hello!" (6 bytes) pads to one 16-byte AES block; plus 16-byte IV = 32 bytes total
        decoded.length == 32
    }

    // -----------------------------------------------------------------------
    // Test 2: Round-trip — JCE decrypt of encrypted body yields original plaintext
    // -----------------------------------------------------------------------

    def "Round-trip verification: JCE decrypt of encrypted body yields original plaintext"() {
        given:
        def originalText = "Hello, World!"
        mockStoreWithZeroKey("testAlias")
        msg.setBody(originalText)
        msg.setProperty("enc_credentialAlias", "testAlias")

        when:
        script.processData(msg)

        then: "decrypting the output with the same key and extracted IV recovers the original text"
        def decoded = Base64.getDecoder().decode(msg.getBody() as String)
        def extractedIv = Arrays.copyOf(decoded, 16)
        def extractedCiphertext = Arrays.copyOfRange(decoded, 16, decoded.length)

        def verifyKey = new SecretKeySpec(new byte[16], "AES")
        def verifyCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        verifyCipher.init(Cipher.DECRYPT_MODE, verifyKey, new IvParameterSpec(extractedIv))
        def plaintext = new String(verifyCipher.doFinal(extractedCiphertext), "UTF-8")
        plaintext == originalText
    }

    // -----------------------------------------------------------------------
    // Test 3: Missing enc_credentialAlias (null) → IllegalArgumentException
    // -----------------------------------------------------------------------

    def "Missing enc_credentialAlias (null) throws IllegalArgumentException"() {
        given:
        msg.setBody("some plaintext")
        // enc_credentialAlias property not set

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "enc_credentialAlias property is required but was not set"
    }

    // -----------------------------------------------------------------------
    // Test 4: Blank enc_credentialAlias (empty string) → IllegalArgumentException
    // -----------------------------------------------------------------------

    def "Blank enc_credentialAlias (empty string) throws IllegalArgumentException"() {
        given:
        msg.setBody("some plaintext")
        msg.setProperty("enc_credentialAlias", "")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "enc_credentialAlias property is required but was not set"
    }

    // -----------------------------------------------------------------------
    // Test 5: SecureStoreService unavailable → IllegalStateException
    // -----------------------------------------------------------------------

    def "SecureStoreService unavailable (ITApiFactory returns null) throws IllegalStateException"() {
        given:
        ITApiFactory.metaClass.static.getApi = { Class c, Object cfg -> null }
        msg.setBody("some plaintext")
        msg.setProperty("enc_credentialAlias", "testAlias")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "SecureStoreService is not available"
    }

    // -----------------------------------------------------------------------
    // Test 6: Credential not found for alias → IllegalStateException
    // -----------------------------------------------------------------------

    def "Credential not found for alias throws IllegalStateException containing the alias"() {
        given:
        def mockService = Stub(SecureStoreService) {
            getUserCredential("unknownAlias") >> null
        }
        ITApiFactory.metaClass.static.getApi = { Class c, Object cfg -> mockService }
        msg.setBody("some plaintext")
        msg.setProperty("enc_credentialAlias", "unknownAlias")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "No credential found for alias: unknownAlias"
    }

    // -----------------------------------------------------------------------
    // Test 7: logMode=INFO — enc_credentialAlias and enc_payloadLength in MPL
    // -----------------------------------------------------------------------

    def "logMode=INFO: enc_credentialAlias and enc_payloadLength are logged as MPL custom header properties"() {
        given:
        mockStoreWithZeroKey("testAlias")
        msg.setBody("Hello!")
        msg.setProperty("enc_credentialAlias", "testAlias")
        msg.setProperty("logMode", "INFO")
        msg.setMessageLog(script.messageLogFactory.getMessageLog("PayloadEncryptor"))

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog != null
        mplLog.customHeaderPropertiesMap.containsKey("enc_credentialAlias")
        mplLog.customHeaderPropertiesMap.get("enc_credentialAlias") == "testAlias"
        mplLog.customHeaderPropertiesMap.containsKey("enc_payloadLength")
        // "Hello!" in UTF-8 is 6 bytes
        mplLog.customHeaderPropertiesMap.get("enc_payloadLength") == "6"
    }

    // -----------------------------------------------------------------------
    // Test 8: logMode=NONE (default) — no MPL custom header properties added
    // -----------------------------------------------------------------------

    def "logMode=NONE (default): no MPL custom header properties added"() {
        given:
        mockStoreWithZeroKey("testAlias")
        msg.setBody("Hello!")
        msg.setProperty("enc_credentialAlias", "testAlias")
        // logMode deliberately omitted — defaults to NONE
        msg.setMessageLog(script.messageLogFactory.getMessageLog("PayloadEncryptor"))

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog != null
        mplLog.customHeaderPropertiesMap.isEmpty()
    }

    // -----------------------------------------------------------------------
    // Test 9: Scope-creep guard — no headers set, no extra properties, body is changed
    // -----------------------------------------------------------------------

    def "Scope-creep guard: no headers set, no properties added beyond inputs, body is changed"() {
        given:
        mockStoreWithZeroKey("testAlias")
        msg.setBody("original plaintext")
        msg.setProperty("enc_credentialAlias", "testAlias")
        msg.setProperty("logMode", "NONE")

        def headersBefore = new LinkedHashMap(msg.getHeaders())
        def propsBefore   = new LinkedHashMap(msg.getProperties())

        when:
        script.processData(msg)

        then: "body is changed (encrypted)"
        msg.getBody() != "original plaintext"

        and: "no headers were set by the script"
        msg.getHeaders() == headersBefore

        and: "no properties were added beyond what was already set"
        msg.getProperties() == propsBefore
    }
}
