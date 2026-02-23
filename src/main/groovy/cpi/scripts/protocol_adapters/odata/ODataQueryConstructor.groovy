package cpi.scripts.protocol_adapters.odata

import com.sap.gateway.ip.core.customdev.util.Message

def Message processData(Message message) {
    def properties = message.getProperties()

    // Read logMode — default is NONE
    def logMode = properties.get("logMode") ?: "NONE"

    // Obtain message log (null-safe; only used when logMode == INFO)
    def messageLog = messageLogFactory.getMessageLog(message)

    // Read and trim each optional OData query option
    def filter = (properties.get("odata_filter") as String)?.trim()
    def select = (properties.get("odata_select") as String)?.trim()
    def expand = (properties.get("odata_expand") as String)?.trim()

    // Validate: at least one option must be provided
    if (!filter && !select && !expand) {
        throw new IllegalArgumentException(
            "At least one of odata_filter, odata_select, or odata_expand must be provided"
        )
    }

    // Assemble query string in fixed order: filter → select → expand
    def sb = new StringBuilder()
    if (filter) {
        sb.append('$filter=').append(filter)
    }
    if (select) {
        if (sb.length() > 0) sb.append('&')
        sb.append('$select=').append(select)
    }
    if (expand) {
        if (sb.length() > 0) sb.append('&')
        sb.append('$expand=').append(expand)
    }
    def queryString = sb.toString()

    // Write assembled query string to the output property
    message.setProperty("odata_queryString", queryString)

    // Log provided options and final query string when logMode is INFO
    if (logMode == "INFO") {
        if (filter) messageLog?.addCustomHeaderProperty("odata_filter", filter)
        if (select) messageLog?.addCustomHeaderProperty("odata_select", select)
        if (expand) messageLog?.addCustomHeaderProperty("odata_expand", expand)
        messageLog?.addCustomHeaderProperty("odata_queryString", queryString)
    }

    return message
}