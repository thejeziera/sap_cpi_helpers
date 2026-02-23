package cpi.scripts.monitoring

import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    def properties = message.getProperties()
    def headers = message.getHeaders()

    // Read logMode; default to NONE
    def logMode = properties.get("logMode") ?: "NONE"

    // Obtain messageLog — null-safe via ?. in usage
    def messageLog = messageLogFactory.getMessageLog(message)

    // Read X-Message-ID; falsy check covers both null and blank
    def xMessageId = headers.get("X-Message-ID") as String

    if (!xMessageId) {
        // X-Message-ID is absent or blank — fall back to SAP_MessageProcessingLogID
        def sapId = headers.get("SAP_MessageProcessingLogID") as String
        if (!sapId) {
            throw new IllegalStateException(
                "Could not determine X-Message-ID: neither X-Message-ID nor SAP_MessageProcessingLogID header is present"
            )
        }
        xMessageId = sapId
        // Set header unconditionally — NOT gated on messageLog presence
        message.setHeader("X-Message-ID", xMessageId)
    }
    // If xMessageId was already present, the header is left unchanged

    // MPL logging — only when logMode is INFO
    if (logMode == "INFO") {
        messageLog?.addCustomHeaderProperty("X-Message-ID", xMessageId)
    }

    return message
}
