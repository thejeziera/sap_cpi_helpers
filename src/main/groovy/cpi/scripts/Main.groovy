package cpi.scripts

import com.sap.gateway.ip.core.customdev.processor.MessageImpl
import com.sap.gateway.ip.core.customdev.util.Message
//import cpi.scripts.data_transformation.xml.FilterNodes

static void main(String[] args) {
    println "Hello world!"
    def Message testMessage = new MessageImpl()
    testMessage.setBody("test body")
    def headersMap = new HashMap<String, Object>()
    def propertiesMap = new HashMap<String, Object>()
    headersMap.put("Test Header", "test value")
    propertiesMap.put("p_test", "test value 2")
    testMessage.setHeaders(headersMap)
    testMessage.setProperties(propertiesMap)
//    FilterNodes.processData(testMessage)
}