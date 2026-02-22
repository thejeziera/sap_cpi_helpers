package cpi.scripts.utilities

import com.sap.gateway.ip.core.customdev.util.Message

import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.Arrays
import java.util.Base64

def Message processData(Message message) {
    // --- Step 1: Read logMode and inputEncoding properties ---
    def properties = message.getProperties()
    def logMode = (properties.get("logMode") ?: "NONE") as String
    def inputEncoding = properties.get("inputEncoding") as String

    // --- Step 2: Retrieve messageLog (null-safe) ---
    def messageLog = messageLogFactory.getMessageLog(message)

    // --- Step 3: Resolve rawBytes from the message body ---
    def body = message.getBody()
    def rawBytes

    if (body instanceof byte[]) {
        // Body is already a byte array — use directly
        rawBytes = body as byte[]
    } else if ("base64" == inputEncoding) {
        // ASSUMPTION: Standard Base64 decoder (not URL-safe) as specified
        rawBytes = Base64.getDecoder().decode(body as String)
    } else {
        // Treat body as a raw UTF-8 string
        rawBytes = (body as String).getBytes("UTF-8")
    }

    // --- Step 4: Extract magic-byte window (first up to 16 bytes) ---
    def windowSize = Math.min(16, rawBytes.length)
    def window = Arrays.copyOf(rawBytes, windowSize)

    // --- Step 5 & 6: Detect content type by magic bytes, then text heuristics ---
    def detectedType

    if (rawBytes.length == 0) {
        // Empty body treated as plain text per spec
        detectedType = "Text"
    } else if ((window[0] & 0xFF) == 0xFF && (window[1] & 0xFF) == 0xD8 && (window[2] & 0xFF) == 0xFF) {
        // JPEG: FF D8 FF
        detectedType = "JPEG"
    } else if ((window[0] & 0xFF) == 0x89 && window[1] == 0x50 && window[2] == 0x4E && window[3] == 0x47) {
        // PNG: 89 50 4E 47 (.PNG)
        detectedType = "PNG"
    } else if (window[0] == 0x47 && window[1] == 0x49 && window[2] == 0x46 && window[3] == 0x38) {
        // GIF: 47 49 46 38 ("GIF8")
        detectedType = "GIF"
    } else if (window[0] == 0x25 && window[1] == 0x50 && window[2] == 0x44 && window[3] == 0x46) {
        // PDF: 25 50 44 46 ("%PDF")
        detectedType = "PDF"
    } else if (window[0] == 0x50 && window[1] == 0x4B && window[2] == 0x03 && window[3] == 0x04) {
        // ZIP: 50 4B 03 04 ("PK\x03\x04")
        detectedType = "ZIP"
    } else {
        // No magic bytes matched — attempt strict UTF-8 text heuristic.
        // Use a strict decoder (CodingErrorAction.REPORT) so invalid byte sequences
        // throw CharacterCodingException rather than silently replacing them.
        try {
            def decoder = Charset.forName("UTF-8").newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
            def textHint = decoder.decode(java.nio.ByteBuffer.wrap(rawBytes)).toString().trim()
            if (textHint.startsWith("{") || textHint.startsWith("[")) {
                detectedType = "JSON"
            } else if (textHint.startsWith("<")) {
                detectedType = "XML"
            } else {
                detectedType = "Text"
            }
        } catch (Exception ignored) {
            // Strict UTF-8 decode failed on a non-empty body → Unknown binary
            detectedType = "Unknown"
        }
    }

    // --- Step 7 & 8: Map detected type to output values ---
    def mimeType
    def disposition
    def fileExtension
    def fileName

    switch (detectedType) {
        case "JPEG":
            mimeType      = "image/jpeg"
            disposition   = 'attachment; filename="payload.jpg"'
            fileExtension = "jpg"
            fileName      = "payload.jpg"
            break
        case "PNG":
            mimeType      = "image/png"
            disposition   = 'attachment; filename="payload.png"'
            fileExtension = "png"
            fileName      = "payload.png"
            break
        case "GIF":
            mimeType      = "image/gif"
            disposition   = 'attachment; filename="payload.gif"'
            fileExtension = "gif"
            fileName      = "payload.gif"
            break
        case "PDF":
            mimeType      = "application/pdf"
            disposition   = 'attachment; filename="payload.pdf"'
            fileExtension = "pdf"
            fileName      = "payload.pdf"
            break
        case "ZIP":
            mimeType      = "application/zip"
            disposition   = 'attachment; filename="payload.zip"'
            fileExtension = "zip"
            fileName      = "payload.zip"
            break
        case "JSON":
            mimeType      = "application/json"
            disposition   = "inline"
            fileExtension = "json"
            fileName      = ""
            break
        case "XML":
            mimeType      = "application/xml"
            disposition   = "inline"
            fileExtension = "xml"
            fileName      = ""
            break
        case "Text":
            mimeType      = "text/plain"
            disposition   = "inline"
            fileExtension = "txt"
            fileName      = ""
            break
        default:
            // Unknown: non-text binary that failed UTF-8 decode and matched no magic bytes
            mimeType      = "application/octet-stream"
            disposition   = 'attachment; filename="payload.bin"'
            fileExtension = "bin"
            fileName      = "payload.bin"
    }

    // --- Steps 9–12: Set headers and properties ---
    message.setHeader("Content-Type", mimeType)
    message.setHeader("Content-Disposition", disposition)
    message.setProperty("fileName", fileName)
    message.setProperty("fileExtension", fileExtension)

    // --- Step 13: Log detected MIME type if logMode is INFO ---
    if ("INFO" == logMode) {
        messageLog?.addCustomHeaderProperty("detectedMimeType", mimeType)
    }

    // --- Step 14: Return message; body is NOT modified ---
    return message
}
