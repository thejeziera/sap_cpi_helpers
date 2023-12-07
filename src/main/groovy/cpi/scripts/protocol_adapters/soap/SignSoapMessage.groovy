package cpi.scripts.protocol_adapters.soap

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.ITApiFactory
import com.sap.it.api.keystore.KeystoreService
import javax.xml.crypto.dsig.*
import javax.xml.crypto.dsig.dom.DOMSignContext
import javax.xml.crypto.dsig.keyinfo.KeyInfo
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory
import javax.xml.crypto.dsig.keyinfo.X509Data
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec
import javax.xml.crypto.dsig.spec.TransformParameterSpec
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document

import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Collections

def Message processData(Message message) {
    // Retrieve the SOAP message from the message body
    def soapMessage = message.getBody(String)
    def dbFactory = DocumentBuilderFactory.newInstance()
    dbFactory.isNamespaceAware = true
    def docBuilder = dbFactory.newDocumentBuilder()
    Document doc = docBuilder.parse(new ByteArrayInputStream(soapMessage.getBytes("UTF-8")))

    // Get keystore service
    KeystoreService keystoreService = ITApiFactory.getApi(KeystoreService.class, null)
    KeyStore.PrivateKeyEntry keyEntry = keystoreService.getPrivateKeyEntry("yourKeyAlias", "yourKeystoreEntryName")
    PrivateKey privateKey = keyEntry.getPrivateKey()
    X509Certificate cert = keyEntry.getCertificate()

    // Create a DOM XMLSignatureFactory
    XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM")

    // Create a Reference to the enveloped document
    Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA256, null),
            Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null)

    // Create the SignedInfo
    SignedInfo si = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE,
            (C14NMethodParameterSpec) null), fac.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
            Collections.singletonList(ref))

    // Create a KeyInfo and add the X509Data to it
    KeyInfoFactory kif = fac.getKeyInfoFactory()
    X509Data xd = kif.newX509Data(Collections.singletonList(cert))
    KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd))

    // Create a DOMSignContext and specify the RSA PrivateKey and location of the resulting XMLSignature's parent element
    DOMSignContext dsc = new DOMSignContext(privateKey, doc.getDocumentElement())

    // Create the XMLSignature, but don't sign it yet
    XMLSignature signature = fac.newXMLSignature(si, ki)

    // Marshal, generate, and sign the enveloped signature
    signature.sign(dsc)

    // Output the resulting document
    ByteArrayOutputStream os = new ByteArrayOutputStream()
    TransformerFactory tf = TransformerFactory.newInstance()
    Transformer trans = tf.newTransformer()
    trans.transform(new DOMSource(doc), new StreamResult(os))

    // Set the signed SOAP message as the body
    message.setBody(new String(os.toByteArray(), "UTF-8"))

    return message
}
