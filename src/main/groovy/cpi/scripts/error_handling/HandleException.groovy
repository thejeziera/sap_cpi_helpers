package cpi.scripts.error_handling

import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    //TODO SPLIT FOR DIFFERENT ADAPTERS

    // Get the MessageLog object
    def messageLog = messageLogFactory.getMessageLog(message)

    // Save payload, headers, and properties as attachments in the message log
    messageLog.addAttachmentAsString("payload", message.getBody() as String, "text/plain")
    messageLog.addAttachmentAsString("headers", message.getHeaders().toString(), "text/plain")
    messageLog.addAttachmentAsString("properties", message.getProperties().toString(), "text/plain")

    // Initialize status, message, and errorCode variables
    String status = "error"
    String messageText = "An error occurred during processing."
    String errorCode = "ERR_CODE_DEFAULT"

    // Get the exception from the CamelExceptionCaught property
    String camelException = message.getProperty("CamelExceptionCaught")

    // Get the current step name from the property
    String currentStep = message.getProperty("p_current_step")

    // Check if the error is due to an HTTP response with status code 4XX or 5XX
    def httpResponseCode = message.getHeaders().get("CamelHttpResponseCode") as Integer
    if (httpResponseCode != null && (httpResponseCode >= 400 && httpResponseCode < 600)) {
        messageText = "HTTP Error ${httpResponseCode} occurred during step '${currentStep}'."
        errorCode = "HTTP_${httpResponseCode}"
    }
    // Check if the error is due to a mapping error (example: missing required property)
    else if (camelException.contains("mapping")) {
        messageText = "A mapping error occurred during step '${currentStep}': ${camelException}"
        errorCode = "MAP_ERROR"
    }
    // If no specific error condition is met, use the exception message as the error message
    else {
        messageText = "An error occurred during step '${currentStep}': ${camelException}"
        errorCode = "CPI_ERROR"
    }

    // Set the properties for status, message, and errorCode
    message.setProperty("p_status", status)
    message.setProperty("p_message", messageText)
    message.setProperty("p_error_code", errorCode)

    return message
}
