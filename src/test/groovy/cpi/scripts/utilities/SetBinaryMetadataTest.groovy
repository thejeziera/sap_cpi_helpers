package cpi.scripts.utilities

import cpi.utils.CPIScriptEnhancer
import cpi.utils.MessageImpl
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.util.Base64

class SetBinaryMetadataTest extends Specification {

    @Shared Script script
    @Shared private GroovyClassLoader classLoader = new GroovyClassLoader()
    private MessageImpl msg

    def setupSpec() {
        Class scriptClass = classLoader.loadClass("cpi.scripts.utilities.SetBinaryMetadata")
        script = scriptClass.getDeclaredConstructor().newInstance() as Script
        CPIScriptEnhancer.enhanceScript(script)
    }

    def setup() {
        msg = new MessageImpl()
    }

    // -----------------------------------------------------------------------
    // Test scenarios 1–4 + GIF: magic-byte detection for binary types (byte[])
    // -----------------------------------------------------------------------

    @Unroll
    def "Magic bytes (byte[]) for #label → Content-Type=#expectedMime, fileName=#expectedFileName, fileExtension=#expectedExt"() {
        given:
        msg.setBody(magicBytes as byte[])

        when:
        script.processData(msg)

        then:
        msg.getHeaders().get("Content-Type")        == expectedMime
        msg.getHeaders().get("Content-Disposition") == expectedDisposition
        msg.getProperties().get("fileName")         == expectedFileName
        msg.getProperties().get("fileExtension")    == expectedExt

        and: "body is unchanged (no setBody called)"
        msg.getBody() == magicBytes as byte[]

        where:
        label  | magicBytes                                                                | expectedMime        | expectedDisposition                    | expectedExt | expectedFileName
        "PNG"  | [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]                      | "image/png"         | 'attachment; filename="payload.png"'   | "png"       | "payload.png"
        "JPEG" | [0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10]                                   | "image/jpeg"        | 'attachment; filename="payload.jpg"'   | "jpg"       | "payload.jpg"
        "GIF"  | [0x47, 0x49, 0x46, 0x38, 0x39, 0x61]                                   | "image/gif"         | 'attachment; filename="payload.gif"'   | "gif"       | "payload.gif"
        "PDF"  | [0x25, 0x50, 0x44, 0x46, 0x2D, 0x31]                                   | "application/pdf"   | 'attachment; filename="payload.pdf"'   | "pdf"       | "payload.pdf"
        "ZIP"  | [0x50, 0x4B, 0x03, 0x04, 0x14, 0x00]                                   | "application/zip"   | 'attachment; filename="payload.zip"'   | "zip"       | "payload.zip"
    }

    // -----------------------------------------------------------------------
    // Test scenarios 5–7: text heuristic detection (String body)
    // -----------------------------------------------------------------------

    @Unroll
    def "String body '#label' → Content-Type=#expectedMime, Content-Disposition=inline, fileName='', fileExtension=#expectedExt"() {
        given:
        msg.setBody(bodyText)

        when:
        script.processData(msg)

        then:
        msg.getHeaders().get("Content-Type")        == expectedMime
        msg.getHeaders().get("Content-Disposition") == "inline"
        msg.getProperties().get("fileName")         == ""
        msg.getProperties().get("fileExtension")    == expectedExt

        and: "body is unchanged"
        msg.getBody() == bodyText

        where:
        label        | bodyText           | expectedMime       | expectedExt
        "JSON object"| '{"key":"val"}'   | "application/json" | "json"
        "JSON array" | '[1,2,3]'         | "application/json" | "json"
        "XML"        | '<root/>'         | "application/xml"  | "xml"
        "plain text" | 'hello world'     | "text/plain"       | "txt"
    }

    // -----------------------------------------------------------------------
    // Test scenario 8: Base64-encoded PNG (String body + inputEncoding=base64)
    // -----------------------------------------------------------------------

    def "Base64-encoded PNG string with inputEncoding=base64 → detected as image/png"() {
        given:
        def pngMagic = [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A] as byte[]
        def b64String = Base64.getEncoder().encodeToString(pngMagic)
        msg.setBody(b64String)
        msg.setProperty("inputEncoding", "base64")

        when:
        script.processData(msg)

        then:
        msg.getHeaders().get("Content-Type")        == "image/png"
        msg.getHeaders().get("Content-Disposition") == 'attachment; filename="payload.png"'
        msg.getProperties().get("fileName")         == "payload.png"
        msg.getProperties().get("fileExtension")    == "png"

        and: "body (base64 string) is unchanged"
        msg.getBody() == b64String
    }

    // -----------------------------------------------------------------------
    // Test scenario 9: Unknown binary (no magic match + invalid UTF-8)
    // -----------------------------------------------------------------------

    def "Unknown binary bytes (no magic match, invalid UTF-8) → application/octet-stream"() {
        given:
        // First byte 0x00 matches no magic sequence; 0xFF 0xFE are invalid in strict UTF-8
        def unknownBytes = [0x00, 0x01, (byte) 0xFF, (byte) 0xFE, 0x0A, 0x0B] as byte[]
        msg.setBody(unknownBytes)

        when:
        script.processData(msg)

        then:
        msg.getHeaders().get("Content-Type")        == "application/octet-stream"
        msg.getHeaders().get("Content-Disposition") == 'attachment; filename="payload.bin"'
        msg.getProperties().get("fileName")         == "payload.bin"
        msg.getProperties().get("fileExtension")    == "bin"

        and: "body is unchanged"
        msg.getBody() == unknownBytes
    }

    // -----------------------------------------------------------------------
    // Test scenario 10: Empty byte[] body → text/plain
    // -----------------------------------------------------------------------

    def "Empty byte[] body → detected as text/plain (inline)"() {
        given:
        msg.setBody(new byte[0])

        when:
        script.processData(msg)

        then:
        msg.getHeaders().get("Content-Type")        == "text/plain"
        msg.getHeaders().get("Content-Disposition") == "inline"
        msg.getProperties().get("fileName")         == ""
        msg.getProperties().get("fileExtension")    == "txt"
    }

    // -----------------------------------------------------------------------
    // Test scenario 11: Scope-creep guard
    // -----------------------------------------------------------------------

    def "Scope-creep guard: pre-existing headers and properties are unchanged; no extra keys added"() {
        given:
        msg.setBody('{"x":1}')
        msg.setHeader("X-Custom", "keep-me")
        msg.setProperty("p_existing", "keep-me-too")

        def headersBefore = new HashMap(msg.getHeaders())
        def propsBefore   = new HashMap(msg.getProperties())

        when:
        script.processData(msg)

        then: "mandated headers are set"
        msg.getHeaders().get("Content-Type")        == "application/json"
        msg.getHeaders().get("Content-Disposition") == "inline"

        and: "mandated properties are set"
        msg.getProperties().get("fileName")      == ""
        msg.getProperties().get("fileExtension") == "json"

        and: "no additional headers or properties were added beyond the four mandated keys"
        // Build comparison maps using def assignments (not bare expressions, so Spock
        // does NOT treat them as boolean assertions)
        def headersAfter = new HashMap(msg.getHeaders())
        def _ = headersAfter.remove("Content-Type")    // discard return value via assignment
        def __ = headersAfter.remove("Content-Disposition")
        def propsAfter = new HashMap(msg.getProperties())
        def ___ = propsAfter.remove("fileName")
        def ____ = propsAfter.remove("fileExtension")
        headersAfter == headersBefore
        propsAfter   == propsBefore
    }

    // -----------------------------------------------------------------------
    // Test scenario 12: logMode=INFO → detectedMimeType added to MPL
    // -----------------------------------------------------------------------

    def "logMode=INFO: detectedMimeType custom header property is set in MessageLog"() {
        given:
        def pngMagic = [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A] as byte[]
        msg.setBody(pngMagic)
        msg.setProperty("logMode", "INFO")
        // Wire the factory's log instance into the msg so both point to the same object
        msg.setMessageLog(script.messageLogFactory.getMessageLog("SetBinaryMetadata"))

        when:
        script.processData(msg)

        then:
        // The script calls messageLogFactory.getMessageLog(message) during execution,
        // which replaces the factory's stored instance; retrieve the latest one.
        // MessageLogImpl exposes the map as 'customHeaderPropertiesMap' (no getter method).
        def mplLog = script.messageLogFactory.messageLog
        mplLog != null
        mplLog.customHeaderPropertiesMap.containsKey("detectedMimeType")
        mplLog.customHeaderPropertiesMap.get("detectedMimeType") == "image/png"
    }

    // -----------------------------------------------------------------------
    // Test scenario 13: logMode=NONE → no detectedMimeType in MPL
    // -----------------------------------------------------------------------

    def "logMode=NONE: detectedMimeType is NOT added to MessageLog"() {
        given:
        def pngMagic = [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A] as byte[]
        msg.setBody(pngMagic)
        msg.setProperty("logMode", "NONE")
        msg.setMessageLog(script.messageLogFactory.getMessageLog("SetBinaryMetadata"))

        when:
        script.processData(msg)

        then:
        def mplLog = script.messageLogFactory.messageLog
        mplLog != null
        !mplLog.customHeaderPropertiesMap.containsKey("detectedMimeType")
    }
}