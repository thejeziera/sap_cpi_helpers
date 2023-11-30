package cpi.scripts.utilities

import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    def headers = message.getHeaders()
    def SAP_Sender = headers.get("SAP_Sender")

    final valueMapApi = ITApiFactory.getService(ValueMappingApi.class, null)
    def receiverURL = valueMapApi.getMappedValue('package.VM.Generic',
                                                    'package.VM.SenderSystem', SAP_Sender,
                                                    'package.VM.SAP',
                                                    'package.VM.ReceiverSystemURL')

    if (receiverURL == null || receiverURL.isEmpty()) {
        throw new IllegalStateException("No SAP system URL value found for: " + SAP_Sender + " in package.VM")
    }

    message.setHeader("ReceiverURL", receiverURL)

    return message
}