package cpi.utils

import com.sap.gateway.ip.core.customdev.processor.SoapHeaders
import com.sap.gateway.ip.core.customdev.util.AttachmentWrapper
import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.gateway.ip.core.customdev.util.SoapHeader
import com.sap.it.api.msg.ExchangePropertyProvider
import com.sap.it.api.msg.MessageSizeInformation
import com.sap.it.api.msglog.MessageLog
import com.sap.it.script.logging.ILogger
import com.sap.it.script.logging.impl.ScriptLogger
import org.apache.camel.Attachment
import org.apache.camel.Exchange
import org.apache.camel.TypeConversionException

import javax.activation.DataHandler

class MessageImpl implements ExchangePropertyProvider, Message {
    private static final String SAP_SOAP_HEADER_WRITE = "SAP_SoapHeaderWrite";
    private static final String SAP_SOAP_HEADER_READ = "SAP_SoapHeaderRead";
    private Object payload;
    private Map<String, Object> headers;
    private Map<String, Object> properties;
    private Exchange exchange;
    private Map<String, DataHandler> attachments;
    private Map<String, AttachmentWrapper> attachmentWrapperObjects;
    private Map<String, Attachment> attachmentObjects;
    private static final ILogger log = ScriptLogger.getCategoryLogger(com.sap.gateway.ip.core.customdev.processor.MessageImpl.class);
    private MessageLog messageLog

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
        } catch (TypeConversionException var3) {
            log.logErrors(new Object[]{var3.getMessage()});
            throw var3;
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
        MessageSizeInformation info = (MessageSizeInformation)this.getBody(MessageSizeInformation.class);
        return info == null ? -1L : info.getBodySize();
    }

    public long getAttachmentsSize() {
        MessageSizeInformation info = (MessageSizeInformation)this.getBody(MessageSizeInformation.class);
        return info == null ? -1L : info.getAttachmentsSize();
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

    public Map<String, AttachmentWrapper> getAttachmentWrapperObjects() {
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
        SoapHeaders headers = new SoapHeaders((List)this.exchange.getProperty("SAP_SoapHeaderRead"), (List)this.exchange.getIn().getHeader(Header.HEADER_LIST, List.class));
        return headers.getSoapHeaders();
    }

    public void setSoapHeaders(List<SoapHeader> headers) {
        SoapHeaders headersObj = new SoapHeaders((List)this.exchange.getProperty("SAP_SoapHeaderRead"), (List)this.exchange.getIn().getHeader(Header.HEADER_LIST, List.class));
//        List<org.apache.cxf.binding.soap.SoapHeader> cxfSoapHeaders = headersObj.setSoapHeaders(headers, Direction.DIRECTION_OUT);
        List<Object> cxfSoapHeaders = headersObj.setSoapHeaders(headers, Direction.DIRECTION_OUT);
//        List<org.apache.cxf.binding.soap.SoapHeader> oldHeaders = (List)this.exchange.getProperty("SAP_SoapHeaderWrite");
        List<Object> oldHeaders = (List)this.exchange.getProperty("SAP_SoapHeaderWrite");
        if (oldHeaders == null) {
            this.exchange.setProperty("SAP_SoapHeaderWrite", cxfSoapHeaders);
        } else {
            oldHeaders.addAll(cxfSoapHeaders);
        }

    }

    public void clearSoapHeaders() {
        SoapHeaders headersObj = new SoapHeaders((List)this.exchange.getProperty("SAP_SoapHeaderRead"), (List)this.exchange.getIn().getHeader(Header.HEADER_LIST, List.class));
        headersObj.clearSoapHeaders();
    }

    public void setMessageLog(MessageLog messageLog){
        this.messageLog = messageLog
    }

    public MessageLog getMessageLog() {
        return messageLog
    }
}