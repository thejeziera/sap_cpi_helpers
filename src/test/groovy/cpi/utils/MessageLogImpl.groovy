package cpi.utils

import com.sap.it.api.msglog.MessageLog

class MessageLogImpl implements MessageLog {
    def customHeaderPropertiesMap = new HashMap<String, String>()
    def messageLogPropertyMap = new HashMap<String, Object>()
    def attachmentMap = new HashMap<String, String>()
    def scriptName = "null"

    MessageLogImpl() {

    }

    MessageLogImpl(String name) {
        this.scriptName = name
    }

    void printMessageLogContent() {
        println "=======MessageLog description:========="
        println "Script: $scriptName"
        println "===========Custom Headers:============="
        customHeaderPropertiesMap.each {
            it ->
                println "Name: $it.key, Value: $it.value"
        }
        println "========Log Content Properties:========"
        customHeaderPropertiesMap.each {
            it ->
                println "Name: $it.key, Value: $it.value"
        }
        println "=============Attachments:=============="
        attachmentMap.each {
            it ->
                {
                    def name = it.key.split(";_;")[0]
                    def value = it.value
                    def type = it.key.split(";_;")[1]
                    println "Name: $name, Type: $type, Value: $value"
                }
        }
    }

    @Override
    void setStringProperty(String name, String value) {
        println "MessageLog - add Custom Propert - Name: $name, Value: $value"
        messageLogPropertyMap.put(name, value)
    }

    @Override
    void setIntegerProperty(String name, Integer value) {
        println "MessageLog - add Custom CPI Log Header - Name: $name, Value: $value"
        messageLogPropertyMap.put(name, value)
    }

    @Override
    void setLongProperty(String name, Long value) {
        println "MessageLog - add Custom CPI Log Header - Name: $name, Value: $value"
        messageLogPropertyMap.put(name, value)
    }

    @Override
    void setBooleanProperty(String name, Boolean value) {
        println "MessageLog - add Custom CPI Log Header - Name: $name, Value: $value"
        messageLogPropertyMap.put(name, value)
    }

    @Override
    void setFloatProperty(String name, Float value) {
        println "MessageLog - add Custom CPI Log Header - Name: $name, Value: $value"
        messageLogPropertyMap.put(name, value)
    }

    @Override
    void setDoubleProperty(String name, Double value) {
        println "MessageLog - add Custom CPI Log Header - Name: $name, Value: $value"
        messageLogPropertyMap.put(name, value)
    }

    @Override
    void setDateProperty(String name, Date value) {
        println "MessageLog - add Custom CPI Log Header - Name: $name, Value: $value"
        messageLogPropertyMap.put(name, value)
    }

    @Override
    void addAttachmentAsString(String name, String content, String contentType) {
        println "MessageLog - add Attachment - Name: $name, Content: $content, ContentType: $contentType"
        def mapKey = "$name;_;$contentType" as String
        attachmentMap.put(mapKey, content)
    }

    @Override
    void addCustomHeaderProperty(String name, String value) {
        println "MessageLog - add Custom CPI Log Header - Name: $name, Value: $value"
        customHeaderPropertiesMap.put(name, value)
    }
}
