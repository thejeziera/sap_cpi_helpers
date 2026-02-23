package cpi.scripts.protocol_adapters.http

import com.sap.gateway.ip.core.customdev.util.Message
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.ContentType

def Message processData(Message message) {
    def properties = message.getProperties()

    // Read logMode; default NONE
    def logMode = properties.get("logMode") ?: "NONE"

    // Read optional text-part configuration; defaults applied where not set
    def textPartName = properties.get("mp_textPartName") ?: "text"
    def textMimeType = properties.get("mp_textMimeType") ?: "application/json"

    // Read required text content; validate
    def textContent = properties.get("mp_textContent")
    if (!textContent) {
        throw new IllegalArgumentException("mp_textContent property is required but was not set")
    }

    // Read optional image-part name; default "image"
    def imagePartName = properties.get("mp_imagePartName") ?: "image"

    // Read required image name; validate
    def imageName = properties.get("mp_imageName")
    if (!imageName) {
        throw new IllegalArgumentException("mp_imageName property is required but was not set")
    }

    // Read required image MIME type; validate
    def imageMimeType = properties.get("mp_imageMimeType")
    if (!imageMimeType) {
        throw new IllegalArgumentException("mp_imageMimeType property is required but was not set")
    }

    // Obtain MPL message log (null-safe usage below)
    def messageLog = messageLogFactory.getMessageLog(message)

    // Read binary image body and validate not null/empty
    def imageBytes = message.getBody() as byte[]
    if (imageBytes == null || imageBytes.length == 0) {
        throw new IllegalArgumentException("Image body is missing or empty")
    }

    // Build multipart/form-data entity
    def builder = MultipartEntityBuilder.create()

    // Add text part using configurable part name and MIME type
    builder.addTextBody(textPartName as String, textContent as String, ContentType.create(textMimeType as String))

    // Add binary image part using configurable part name, MIME type and filename
    builder.addBinaryBody(imagePartName as String, imageBytes, ContentType.create(imageMimeType as String), imageName as String)

    def multipartEntity = builder.build()

    // Write full Content-Type (including generated boundary) to the Content-Type header
    message.setHeader("Content-Type", multipartEntity.getContentType().getValue())

    // Serialize multipart entity to byte[]
    def out = new ByteArrayOutputStream()
    multipartEntity.writeTo(out)
    def multipartBinaryData = out.toByteArray()

    message.setBody(multipartBinaryData)

    // INFO logging: image metadata diagnostics
    if ("INFO" == logMode) {
        messageLog?.addCustomHeaderProperty("mp_imageName",    imageName as String)
        messageLog?.addCustomHeaderProperty("mp_imageMimeType", imageMimeType as String)
        messageLog?.addCustomHeaderProperty("mp_imageBytes",   String.valueOf(imageBytes.length))
    }

    return message
}