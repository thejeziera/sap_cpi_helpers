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

class PayloadDecryptorTest extends Specification {

    @Shared Script script
    @Shared private GroovyClassLoader classLoader = new GroovyClassLoader()
    private MessageImpl msg

    def setupSpec() {
        Class scriptClass = classLoader.loadClass("cpi.scripts.security.PayloadDecryptor")
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

    // Helper: encrypt a plaintext with zero key and zero IV, return Base64(IV||ciphertext)
    private String encryptWithZeroKeyAndIv(String plaintext) {
        def testKeyBytes = new byte[16]   // 16 zero bytes
        def testIv = new byte[16]         // 16 zero bytes (fixed, deterministic)
        def secretKey = new SecretKeySpec(testKeyBytes, "AES")
        def cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(testIv))
        def ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"))
        def combined = new byte[16 + ciphertext.length]
        System.arraycopy(testIv, 0, combined, 0, 16)
        System.arraycopy(ciphertext, 0, combined, 16, ciphertext.length)
        return Base64.getEncoder().encodeToString(combined)
    }

    // -----------------------------------------------------------------------
    // Test 1: Happy path — valid Base64(IV||ciphertext) → body set to decrypted plaintext
    // -----------------------------------------------------------------------

    def "Happy path: valid Base64(IV||ciphertext) is decrypted and body set to original plaintext"() {
        given:
        def testPlaintext = "Hello, World!"
        def b64Payload = encryptWithZeroKeyAndIv(testPlaintext)
        mockStoreWithZeroKey("testAlias")
        msg.setBody(b64Payload)
        msg.setProperty("enc_credentialAlias", "testAlias")

        when:
        script.processData(msg)

        then:
        msg.getBody() == testPlaintext
    }

    // -----------------------------------------------------------------------
    // Test 2: Missing enc_credentialAlias (null) → IllegalArgumentException
    // -----------------------------------------------------------------------

    def "Missing enc_credentialAlias (null) throws IllegalArgumentException"() {
        given:
        msg.setBody("someBase64Body")
        // enc_credentialAlias property not set

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "enc_credentialAlias property is required but was not set"
    }

    // -----------------------------------------------------------------------
    // Test 3: Blank enc_credentialAlias (empty string) → IllegalArgumentException
    // -----------------------------------------------------------------------

    def "Blank enc_credentialAlias (empty string) throws IllegalArgumentException"() {
        given:
        msg.setBody("someBase64Body")
        msg.setProperty("enc_credentialAlias", "")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "enc_credentialAlias property is required but was not set"
    }

    // -----------------------------------------------------------------------
    // Test 4: SecureStoreService unavailable → IllegalStateException
    // -----------------------------------------------------------------------

    def "SecureStoreService unavailable (ITApiFactory returns null) throws IllegalStateException"() {
        given:
        ITApiFactory.metaClass.static.getApi = { Class c, Object cfg -> null }
        msg.setBody("someBase64Body")
        msg.setProperty("enc_credentialAlias", "testAlias")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "SecureStoreService is not available"
    }

    // -----------------------------------------------------------------------
    // Test 5: Credential not found for alias → IllegalStateException
    // -----------------------------------------------------------------------

    def "Credential not found for alias throws IllegalStateException containing the alias"() {
        given:
        def mockService = Stub(SecureStoreService) {
            getUserCredential("unknownAlias") >> null
        }
        ITApiFactory.metaClass.static.getApi = { Class c, Object cfg -> mockService }
        msg.setBody("someBase64Body")
        msg.setProperty("enc_credentialAlias", "unknownAlias")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "No credential found for alias: unknownAlias"
    }

    // -----------------------------------------------------------------------
    // Test 6: Null body → IllegalArgumentException
    // -----------------------------------------------------------------------

    def "Null body throws IllegalArgumentException with message 'Encrypted payload is missing or blank'"() {
        given:
        mockStoreWithZeroKey("testAlias")
        msg.setBody(null)
        msg.setProperty("enc_credentialAlias", "testAlias")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Encrypted payload is missing or blank"
    }

    // -----------------------------------------------------------------------
    // Test 7: Blank body → IllegalArgumentException
    // -----------------------------------------------------------------------

    def "Blank body throws IllegalArgumentException with message 'Encrypted payload is missing or blank'"() {
        given:
        mockStoreWithZeroKey("testAlias")
        msg.setBody("")
        msg.setProperty("enc_credentialAlias", "testAlias")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Encrypted payload is missing or blank"
    }

    // -----------------------------------------------------------------------
    // Test 8: Decoded bytes exactly 16 (IV only, no ciphertext) → IllegalArgumentException
    // -----------------------------------------------------------------------

    def "Decoded bytes exactly 16 (only IV, no ciphertext) throws IllegalArgumentException with 'too short'"() {
        given:
        mockStoreWithZeroKey("testAlias")
        // 16 bytes Base64-encoded → decoded.length == 16, which is NOT > 16
        msg.setBody(Base64.getEncoder().encodeToString(new byte[16]))
        msg.setProperty("enc_credentialAlias", "testAlias")

        when:
        script.processData(msg)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Encrypted payload is too short to contain an IV"
    }

    // -----------------------------------------------------------------------
    // Test 9: logMode=INFO — enc_credentialAlias and enc_encryptedLength in MPL
    // -----------------------------------------------------------------------

    def "logMode=INFO: enc_credentialAlias and enc_encryptedLength are logged as MPL custom header properties"() {
        given:
        def testPlaintext = "Hello, World!"
        def b64Payload = encryptWithZeroKeyAndIv(testPlaintext)
        mockStoreWithZeroKey("testAlias")
        msg.setBody(b64Payload)
        msg.setProperty("enc_credentialAlias", "testAlias")
        msg.setProperty("logMode", "INFO")
        msg.setMessageLog(script.messageLogFactory.getMessageLog("PayloadDecryptor"))

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog != null
        mplLog.customHeaderPropertiesMap.containsKey("enc_credentialAlias")
        mplLog.customHeaderPropertiesMap.get("enc_credentialAlias") == "testAlias"
        mplLog.customHeaderPropertiesMap.containsKey("enc_encryptedLength")
        // decoded bytes = 16 (IV) + 16 (one AES block for "Hello, World!") = 32
        mplLog.customHeaderPropertiesMap.get("enc_encryptedLength") == "32"
    }

    // -----------------------------------------------------------------------
    // Test 10: logMode=NONE (default) — no MPL custom header properties added
    // -----------------------------------------------------------------------

    def "logMode=NONE (default): no MPL custom header properties added"() {
        given:
        def b64Payload = encryptWithZeroKeyAndIv("Hello, World!")
        mockStoreWithZeroKey("testAlias")
        msg.setBody(b64Payload)
        msg.setProperty("enc_credentialAlias", "testAlias")
        // logMode deliberately omitted — defaults to NONE
        msg.setMessageLog(script.messageLogFactory.getMessageLog("PayloadDecryptor"))

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog != null
        mplLog.customHeaderPropertiesMap.isEmpty()
    }

    // -----------------------------------------------------------------------
    // Test 11: Scope-creep guard — no headers set, no extra properties, body changed to plaintext
    // -----------------------------------------------------------------------

    def "Scope-creep guard: no headers set, no properties added beyond inputs, body changed to plaintext"() {
        given:
        def testPlaintext = "Hello, World!"
        def b64Payload = encryptWithZeroKeyAndIv(testPlaintext)
        mockStoreWithZeroKey("testAlias")
        msg.setBody(b64Payload)
        msg.setProperty("enc_credentialAlias", "testAlias")
        msg.setProperty("logMode", "NONE")

        def headersBefore = new LinkedHashMap(msg.getHeaders())
        def propsBefore   = new LinkedHashMap(msg.getProperties())

        when:
        script.processData(msg)

        then: "body is changed to the decrypted plaintext"
        msg.getBody() == testPlaintext

        and: "no headers were set by the script"
        msg.getHeaders() == headersBefore

        and: "no properties were added beyond what was already set"
        msg.getProperties() == propsBefore
    }
}
