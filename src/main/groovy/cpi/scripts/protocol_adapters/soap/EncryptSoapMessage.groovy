package cpi.scripts.protocol_adapters.soap

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.ITApiFactory
import com.sap.it.api.keystore.KeystoreService

import javax.crypto.SecretKey
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.io.StringWriter
import java.io.ByteArrayInputStream
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.apache.xml.security.encryption.XMLCipher
import org.apache.xml.security.encryption.EncryptedData
import org.apache.xml.security.encryption.EncryptedKey
import org.apache.xml.security.encryption.XMLEncryptionException
import org.apache.xml.security.utils.EncryptionConstants
import org.apache.xml.security.utils.XMLUtils
import org.apache.xml.security.keys.KeyInfoFactory

// Initialize Apache XML Security library
org.apache.xml.security.Init.init()

def Message processData(Message message) {
    // Retrieve the key alias and keystore entry name from message headers
    String keyAlias = message.getHeader("keyAlias", String)
    String keystoreEntryName = message.getHeader("keystoreEntryName", String)

    // Retrieve the certificate from the keystore
    KeystoreService keystoreService = ITApiFactory.getApi(KeystoreService.class, null)
    KeyStore.PrivateKeyEntry keyEntry = keystoreService.getPrivateKeyEntry(keyAlias, keystoreEntryName)
    X509Certificate cert = keyEntry.getCertificate()

    // Parse the existing SOAP message into a DOM Document
    def dbFactory = DocumentBuilderFactory.newInstance()
    dbFactory.isNamespaceAware = true
    def docBuilder = dbFactory.newDocumentBuilder()
    Document document = docBuilder.parse(new ByteArrayInputStream(message.getBody(String).bytes))

    // Encrypt the SOAP Body
    try {
        XMLCipher keyCipher = XMLCipher.getInstance(XMLCipher.RSA_v1dot5)
        keyCipher.init(XMLCipher.WRAP_MODE, cert.getPublicKey())
        SecretKey symmetricKey = KeyGenerator.getInstance("AES").generateKey()
        EncryptedKey encryptedKey = keyCipher.encryptKey(document, symmetricKey)

        XMLCipher xmlCipher = XMLCipher.getInstance(XMLCipher.AES_128)
        xmlCipher.init(XMLCipher.ENCRYPT_MODE, symmetricKey)

        // Encrypt the SOAP Body
        Element bodyElement = (Element) document.getElementsByTagNameNS(EncryptionConstants.URI_SOAP11_ENV, "Body").item(0)
        xmlCipher.doFinal(document, bodyElement, true) // "true" indicates replacing the element with its encrypted form

        // Add EncryptedKey to the Security header
        Element securityHeader = createSecurityHeader(document)
        EncryptedData encryptedData = xmlCipher.getEncryptedData()
        KeyInfo keyInfo = new KeyInfo(document)
        keyInfo.addKeyName("Encrypted Key")
        keyInfo.add(encryptedKey)
        encryptedData.setKeyInfo(keyInfo)
        securityHeader.appendChild(xmlCipher.martial(document, encryptedData))

        // Convert back to String and set as message body
        TransformerFactory transformerFactory = TransformerFactory.newInstance()
        StringWriter writer = new StringWriter()
        transformerFactory.newTransformer().transform(new DOMSource(document), new StreamResult(writer))
        message.setBody(writer.toString())

    } catch (Exception e) {
        throw new RuntimeException("Error encrypting SOAP message", e)
    }

    return message
}

Element createSecurityHeader(Document document) {
    Element header = (Element) document.getElementsByTagNameNS(EncryptionConstants.URI_SOAP11_ENV, "Header").item(0)
    if (header == null) {
        header = document.createElementNS(EncryptionConstants.URI_SOAP11_ENV, "soapenv:Header")
        document.getDocumentElement().insertBefore(header, document.getDocumentElement().getFirstChild())
    }
    Element security = document.createElementNS(EncryptionConstants.URI_WSS10, "wsse:Security")
    header.appendChild(security)
    return security
}
