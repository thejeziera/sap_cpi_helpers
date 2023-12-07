package cpi.scripts.protocol_adapters.soap

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.ITApiFactory
import com.sap.it.api.securestore.SecureStoreService
import com.sap.it.api.securestore.UserCredential
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

def Message processData(Message message) {
    // Retrieve the name of the security material from a message header
    String securityMaterialName = message.getHeader("securityMaterialName", String)

    // Get the SecureStoreService API to access credentials
    SecureStoreService secureStoreService = ITApiFactory.getApi(SecureStoreService.class, null)
    if (secureStoreService == null) {
        throw new IllegalStateException("SecureStoreService is not available")
    }

    // Retrieve user credentials from the CPI Secure Store
    UserCredential credentials = secureStoreService.getUserCredential(securityMaterialName)
    if (credentials == null) {
        throw new IllegalStateException("User credentials not found for: " + securityMaterialName)
    }

    String username = credentials.getUsername()
    String password = credentials.getPassword()

    // Retrieve the SOAP message from the message body
    def soapMessage = message.getBody(String)

    // Parse the existing SOAP message into a DOM Document
    def dbFactory = DocumentBuilderFactory.newInstance()
    dbFactory.isNamespaceAware = true
    def docBuilder = dbFactory.newDocumentBuilder()
    def document = docBuilder.parse(new ByteArrayInputStream(soapMessage.bytes))

    // Namespaces
    def wsseNs = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
    def wsuNs = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"

    // Create WSSE credentials elements
    def securityElem = document.createElementNS(wsseNs, "wsse:Security")
    securityElem.setAttribute("soapenv:mustUnderstand", "1")

    def usernameTokenElem = document.createElementNS(wsseNs, "wsse:UsernameToken")
    usernameTokenElem.setAttributeNS(wsuNs, "wsu:Id", "UsernameToken-1")

    def usernameElem = document.createElementNS(wsseNs, "wsse:Username")
    usernameElem.setTextContent(username)

    def passwordElem = document.createElementNS(wsseNs, "wsse:Password")
    passwordElem.setTextContent(password)
    passwordElem.setAttribute("Type", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText")

    // Append elements
    usernameTokenElem.appendChild(usernameElem)
    usernameTokenElem.appendChild(passwordElem)
    securityElem.appendChild(usernameTokenElem)

    // Insert the security element into the SOAP header
    def headerElem = document.documentElement.getElementsByTagName("soapenv:Header").item(0)
    if (headerElem == null) {
        headerElem = document.createElement("soapenv:Header")
        document.documentElement.insertBefore(headerElem, document.documentElement.firstChild)
    }
    headerElem.appendChild(securityElem)

    // Convert back to String and set as message body
    def transformerFactory = TransformerFactory.newInstance()
    def transformer = transformerFactory.newTransformer()
    def result = new StreamResult(new StringWriter())
    def source = new DOMSource(document)
    transformer.transform(source, result)
    message.setBody(result.writer.toString())

    return message
}
