package cpi.utils

import com.sap.gateway.ip.core.customdev.util.Message

class MessageLogFactoryImpl {

    def getMessageLog(Message message) {
        return new MessageLogImpl()
    }

    class MessageLogImpl {
        void addAttachmentAsString(String name, String content, String contentType) {
            println "Attachment Name: $name, Content: $content, ContentType: $contentType"
        }
    }
}
