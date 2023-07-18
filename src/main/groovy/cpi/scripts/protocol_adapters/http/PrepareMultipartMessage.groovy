package cpi.scripts.protocol_adapters.http

import com.sap.gateway.ip.core.customdev.util.Message
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.ContentType

class PrepareMultipartMessage {
    static Message processData(Message message) {
        // Extract the fileName and fileType properties
        def fileName = message.getProperty("p_attachment_filename")
        def fileType = message.getProperty("p_attachment_filetype")

        // Allowed file types in SAP Concur
        def allowedFileTypes = ["PNG", "PDF", "TIFF", "JPEG", "JPG"]

        // Check if the file type is allowed
        if (allowedFileTypes.contains(fileType.toUpperCase())) {
            // Check if the file name already has an extension
            def extensionPattern = ~/\..{2,4}$/
//        if (!fileName.matches(extensionPattern)) {
//            // Append the file type as an extension
//            fileName = "${fileName}.${fileType.toLowerCase()}"
//        }
        } else {
            // Handle unsupported file types - log a warning and return the original fileName
            System.err.println("Unsupported file type '${fileType}' for file '${fileName}'.")
        }

        // Set the modified fileName property back to the message
        message.setProperty("p_attachment_filename", fileName)

        // Extract binary data from the message body
        byte[] attachmentBinaryData = message.getBody(byte[].class)

        // Retrieve the Expense body for creating the multipart message
        def concurBody = message.getProperty("p_expense_body")

        // Create the multipart message
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create()

        // Add the Concur JSON payload as a part of the multipart message
        multipartEntityBuilder.addTextBody("quickExpenseRequest", concurBody as String, ContentType.APPLICATION_JSON)

        // Add the binary attachment as a part of the multipart message
        multipartEntityBuilder.addBinaryBody("fileContent", attachmentBinaryData, ContentType.create(fileType as String), fileName as String)

        // Build the multipart entity and set it as the message body
        def multipartEntity = multipartEntityBuilder.build()

        // Ensure correct Content-Type header boundary values
        message.setProperty("p_content_type", "multipart/form-data; boundary=" + multipartEntity.getContentType().getValue().split("boundary=")[1])

        // Convert MultipartEntity object to byte[] data
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        multipartEntity.writeTo(out)
        byte[] multipartBinaryData = out.toByteArray()

        message.setProperty("p_expense_body", multipartBinaryData)

        message.setBody(multipartBinaryData)

        return message
    }
}