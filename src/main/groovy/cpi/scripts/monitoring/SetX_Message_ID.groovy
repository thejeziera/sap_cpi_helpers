package cpi.scripts.monitoring

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.commons.logging.slf4j.LoggerFactory

def Message processData(Message message) {
    def headers = message.getHeaders()
    def messageLog = messageLogFactory.getMessageLog(message)
    def X_Message_ID = headers["X-Message-ID"]

    if (messageLog && X_Message_ID == null) {
        X_Message_ID = headers["SAP_MessageProcessingLogID"]
        message.setHeader("X-Message-ID", X_Message_ID)
    }

    messageLog?.addCustomHeaderProperty("X-Message-ID", X_Message_ID)

    return message
}
