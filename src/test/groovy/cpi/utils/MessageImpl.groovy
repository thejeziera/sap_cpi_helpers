package cpi.utils

import com.sap.gateway.ip.core.customdev.util.AttachmentWrapper
import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.gateway.ip.core.customdev.util.SoapHeader

import org.apache.camel.Attachment
import org.apache.camel.Exchange
import org.apache.camel.TypeConversionException
import org.apache.camel.impl.DefaultAttachment

import javax.activation.DataHandler

class MessageImpl implements Message {
    private static final String SAP_SOAP_HEADER_WRITE = "SAP_SoapHeaderWrite";
    private static final String SAP_SOAP_HEADER_READ = "SAP_SoapHeaderRead";
    private Object payload;
    private Map<String, Object> headers;
    private Map<String, Object> properties;
    private Exchange exchange;
    private Map<String, DataHandler> attachments;
    private Map<String, DefaultAttachment> attachmentWrapperObjects;
    private Map<String, Attachment> attachmentObjects;
    private static final ScriptLogger log = ScriptLogger.getCategoryLogger(MessageImpl.class);
    private MessageLogImpl messageLog
    private List<SoapHeader> soapHeaderList = new ArrayList<>()

    public MessageImpl() {
    }

    public MessageImpl(Exchange exchange) {
        this.exchange = exchange;
    }

    public <T> T getBody(Class<T> type) throws TypeConversionException {
        try {
            T obj = this.exchange.getIn().getBody(type);
            if (obj == null) {
                log.logErrors(new Object[]{"Body cannot be casted to type: " + type});
                return null;
            } else {
                return obj;
            }
        } catch (TypeConversionException exception) {
            log.logErrors(new Object[]{exception.getMessage()});
            throw exception;
        }
    }

    public Object getBody() {
        return this.payload;
    }

    public void setBody(Object exchangeBody) {
        this.payload = exchangeBody;
    }

    public Map<String, DataHandler> getAttachments() {
        return this.attachments;
    }

    public void setAttachments(Map<String, DataHandler> attachments) {
        this.attachments = attachments;
    }

    public Map<String, Object> getHeaders() {
        return this.headers;
    }

    public <T> T getHeader(String headerName, Class<T> headerType) throws TypeConversionException {
        try {
            if (this.exchange.getIn().getHeader(headerName) == null) {
                log.logErrors(new Object[]{"Header '" + headerName + "' does not exist"});
                return null;
            } else {
                T obj = this.exchange.getIn().getHeader(headerName, headerType);
                if (obj == null) {
                    log.logErrors(new Object[]{"Error during type conversion of header: '" + headerName + "' to the required type: '" + headerType + "'"});
                    return null;
                } else {
                    return obj;
                }
            }
        } catch (TypeConversionException var4) {
            log.logErrors(new Object[]{var4.getMessage()});
            throw var4;
        }
    }

    public void setHeaders(Map<String, Object> exchangeHeaders) {
        this.headers = exchangeHeaders;
    }

    public void setHeader(String name, Object value) {
        if (this.headers == null) {
            this.headers = new HashMap();
        }

        this.headers.put(name, value);
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }

    public void setProperties(Map<String, Object> exchangeProperties) {
        this.properties = exchangeProperties;
    }

    public void setProperty(String name, Object value) {
        if (this.properties == null) {
            this.properties = new HashMap();
        }

        this.properties.put(name, value);
    }

    public Object getProperty(String name) {
        return this.properties != null ? this.properties.get(name) : null;
    }

    public long getBodySize() {
        return body == null ? -1L : ((body as String).length() + 0L);
    }

    public long getAttachmentsSize() {
        return attachments == null ? -1L : ((attachments as String).length() + 0L);
    }

    public void addAttachmentHeader(String headerName, String headerValue, AttachmentWrapper attachment) {
        attachment.addHeader(headerName, headerValue);
    }

    public void setAttachmentHeader(String headerName, String headerValue, AttachmentWrapper attachment) {
        attachment.setHeader(headerName, headerValue);
    }

    public String getAttachmentHeader(String headerName, AttachmentWrapper attachment) {
        return attachment.getHeader(headerName);
    }

    public void removeAttachmentHeader(String headerName, AttachmentWrapper attachment) {
        attachment.removeHeader(headerName);
    }

    public Map<String, DefaultAttachment> getAttachmentWrapperObjects() {
        return this.attachmentWrapperObjects;
    }

    public void setAttachmentWrapperObjects(Map<String, AttachmentWrapper> attachmentObjects) {
        this.attachmentWrapperObjects = attachmentObjects;
    }

    public void addAttachmentObject(String id, AttachmentWrapper content) {
        this.attachmentWrapperObjects.put(id, content);
    }

    /** @deprecated */
    @Deprecated
    public void addAttachmentHeader(String headerName, String headerValue, Attachment attachment) {
        attachment.addHeader(headerName, headerValue);
    }

    /** @deprecated */
    @Deprecated
    public void setAttachmentHeader(String headerName, String headerValue, Attachment attachment) {
        attachment.setHeader(headerName, headerValue);
    }

    /** @deprecated */
    @Deprecated
    public String getAttachmentHeader(String headerName, Attachment attachment) {
        return attachment.getHeader(headerName);
    }

    /** @deprecated */
    @Deprecated
    public void removeAttachmentHeader(String headerName, Attachment attachment) {
        attachment.removeHeader(headerName);
    }

    /** @deprecated */
    @Deprecated
    public Map<String, Attachment> getAttachmentObjects() {
        return this.attachmentObjects;
    }

    /** @deprecated */
    @Deprecated
    public void setAttachmentObjects(Map<String, Attachment> attachmentObjects) {
        this.attachmentObjects = attachmentObjects;
    }

    /** @deprecated */
    @Deprecated
    public void addAttachmentObject(String id, Attachment content) {
        this.attachmentObjects.put(id, content);
    }

    public List<SoapHeader> getSoapHeaders() {
        return soapHeaderList
    }

    public void setSoapHeaders(List<SoapHeader> headers) {
        soapHeaderList = headers
    }

    public void clearSoapHeaders() {
        soapHeaderList.clear()
    }

    public void setMessageLog(MessageLogImpl messageLog){
        this.messageLog = messageLog
    }

    public MessageLogImpl getMessageLog() {
        return messageLog
    }
}