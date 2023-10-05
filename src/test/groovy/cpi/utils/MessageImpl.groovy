package cpi.utils

import com.sap.gateway.ip.core.customdev.util.AttachmentWrapper
import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.gateway.ip.core.customdev.util.SoapHeader

import org.apache.camel.Attachment
import org.apache.camel.Exchange
import org.apache.camel.TypeConversionException
import org.apache.camel.impl.DefaultAttachment

import javax.activation.DataHandler
import javax.xml.namespace.QName

class MessageImpl implements Message {
    private static final String SAP_SOAP_HEADER_WRITE = "SAP_SoapHeaderWrite"
    private static final String SAP_SOAP_HEADER_READ = "SAP_SoapHeaderRead"
    private Object payload = ""
    private Map<String, Object> headers = new HashMap<String, Object>()
    private Map<String, Object> properties = new HashMap<String, Object>()
    private Exchange exchange
    private Map<String, DataHandler> attachments = new HashMap<String, DataHandler>()
    private Map<String, DefaultAttachment> attachmentWrapperObjects = new HashMap<String, DefaultAttachment>()
    private Map<String, Attachment> attachmentObjects = new HashMap<String, Attachment>()
    private static final ScriptLogger log = ScriptLogger.getCategoryLogger(MessageImpl.class)
    private MessageLogImpl messageLog
    private List<SoapHeader> soapHeaderList = new ArrayList<>()

    MessageImpl() {
        soapHeaderList.add(new SoapHeader(new QName("uri:namespace"), null, false, SAP_SOAP_HEADER_READ))
        soapHeaderList.add(new SoapHeader(new QName("uri:namespace"), null, false, SAP_SOAP_HEADER_WRITE))
    }

    MessageImpl(Exchange exchange) {
        this.exchange = exchange
        this.headers = exchange.getIn().getHeaders()
        this.attachments = exchange.getIn().getAttachments()
        this.attachmentObjects = exchange.getIn().getAttachmentObjects()
        this.payload = exchange.getIn().getBody()
    }

    <T> T getBody(Class<T> type) throws TypeConversionException {
        try {
            T obj = this.exchange.getIn().getBody(type)
            if (obj == null) {
                log.logErrors(new Object[]{"Body cannot be casted to type: " + type})
                return null
            } else {
                return obj
            }
        } catch (TypeConversionException exception) {
            log.logErrors(new Object[]{exception.getMessage()})
            throw exception
        }
    }

    Object getBody() {
        return this.payload
    }

    void setBody(Object exchangeBody) {
        this.payload = exchangeBody
    }

    Map<String, DataHandler> getAttachments() {
        return this.attachments
    }

    void setAttachments(Map<String, DataHandler> attachments) {
        this.attachments = attachments
    }

    Map<String, Object> getHeaders() {
        return this.headers
    }

    <T> T getHeader(String headerName, Class<T> headerType) throws TypeConversionException {
        try {
            if (this.exchange.getIn().getHeader(headerName) == null) {
                log.logErrors(new Object[]{"Header '" + headerName + "' does not exist"})
                return null
            } else {
                T obj = this.exchange.getIn().getHeader(headerName, headerType)
                if (obj == null) {
                    log.logErrors(new Object[]{"Error during type conversion of header: '" + headerName + "' to the required type: '" + headerType + "'"})
                    return null
                } else {
                    return obj
                }
            }
        } catch (TypeConversionException error) {
            log.logErrors(new Object[]{error.getMessage()})
            throw error
        }
    }

    void setHeaders(Map<String, Object> exchangeHeaders) {
        this.headers = exchangeHeaders
    }

    void setHeader(String name, Object value) {
        this.headers.put(name, value)
        if (this.exchange != null && this.exchange.getIn() != null) {
            this.exchange.getIn().setHeader(name, value)
        }
    }

    Map<String, Object> getProperties() {
        return this.properties
    }

    void setProperties(Map<String, Object> exchangeProperties) {
        this.properties = exchangeProperties
    }

    void setProperty(String name, Object value) {
        this.properties.put(name, value)
    }

    Object getProperty(String name) {
        return this.properties.get(name)
    }

    long getBodySize() {
        return (this.body as String).length() + 0L
    }

    long getAttachmentsSize() {
        return (this.attachments as String).length() + 0L
    }

    void addAttachmentHeader(String headerName, String headerValue, AttachmentWrapper attachment) {
        attachment.addHeader(headerName, headerValue)
    }

    void setAttachmentHeader(String headerName, String headerValue, AttachmentWrapper attachment) {
        attachment.setHeader(headerName, headerValue)
    }

    String getAttachmentHeader(String headerName, AttachmentWrapper attachment) {
        return attachment.getHeader(headerName)
    }

    void removeAttachmentHeader(String headerName, AttachmentWrapper attachment) {
        attachment.removeHeader(headerName)
    }

    Map<String, DefaultAttachment> getAttachmentWrapperObjects() {
        return this.attachmentWrapperObjects
    }

    void setAttachmentWrapperObjects(Map<String, AttachmentWrapper> attachmentObjects) {
        this.attachmentWrapperObjects = attachmentObjects
    }

    void addAttachmentObject(String id, AttachmentWrapper content) {
        this.attachmentWrapperObjects.put(id, content)
    }

    /** @deprecated */
    @Deprecated
    void addAttachmentHeader(String headerName, String headerValue, Attachment attachment) {
        attachment.addHeader(headerName, headerValue)
    }

    /** @deprecated */
    @Deprecated
    void setAttachmentHeader(String headerName, String headerValue, Attachment attachment) {
        attachment.setHeader(headerName, headerValue)
    }

    /** @deprecated */
    @Deprecated
    String getAttachmentHeader(String headerName, Attachment attachment) {
        return attachment.getHeader(headerName)
    }

    /** @deprecated */
    @Deprecated
    void removeAttachmentHeader(String headerName, Attachment attachment) {
        attachment.removeHeader(headerName)
    }

    /** @deprecated */
    @Deprecated
    Map<String, Attachment> getAttachmentObjects() {
        return this.attachmentObjects
    }

    /** @deprecated */
    @Deprecated
    void setAttachmentObjects(Map<String, Attachment> attachmentObjects) {
        this.attachmentObjects = attachmentObjects
    }

    /** @deprecated */
    @Deprecated
    void addAttachmentObject(String id, Attachment content) {
        this.attachmentObjects.put(id, content)
    }

    List<SoapHeader> getSoapHeaders() {
        return this.soapHeaderList
    }

    void setSoapHeaders(List<SoapHeader> headers) {
        this.soapHeaderList = headers
    }

    void clearSoapHeaders() {
        this.soapHeaderList.clear()
    }

    void setMessageLog(MessageLogImpl messageLog){
        this.messageLog = messageLog
    }

    MessageLogImpl getMessageLog() {
        return this.messageLog
    }
}