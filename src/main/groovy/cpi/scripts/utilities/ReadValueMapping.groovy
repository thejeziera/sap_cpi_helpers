package cpi.scripts.utilities

import com.sap.gateway.ip.core.customdev.util.Message
import com.sap.it.api.ITApiFactory
import com.sap.it.api.mapping.ValueMappingApi

def Message processData(Message message) {
    // --- Step 1: Read logMode from properties; default to "NONE" ---
    def properties = message.getProperties()
    def logMode = (properties.get("logMode") ?: "NONE") as String

    // --- Steps 2–6: Read and validate all five required input properties ---
    def srcAgency = properties.get("vm_srcAgency") as String
    if (!srcAgency) {
        throw new IllegalArgumentException("vm_srcAgency property is required but was not set")
    }

    def srcScheme = properties.get("vm_srcScheme") as String
    if (!srcScheme) {
        throw new IllegalArgumentException("vm_srcScheme property is required but was not set")
    }

    def srcValue = properties.get("vm_srcValue") as String
    if (!srcValue) {
        throw new IllegalArgumentException("vm_srcValue property is required but was not set")
    }

    def tgtAgency = properties.get("vm_tgtAgency") as String
    if (!tgtAgency) {
        throw new IllegalArgumentException("vm_tgtAgency property is required but was not set")
    }

    def tgtScheme = properties.get("vm_tgtScheme") as String
    if (!tgtScheme) {
        throw new IllegalArgumentException("vm_tgtScheme property is required but was not set")
    }

    // --- Step 7: Obtain messageLog (null-safe; standard CPI MPL plumbing) ---
    def messageLog = messageLogFactory.getMessageLog(message)

    // --- Step 8: Retrieve ValueMappingApi via ITApiFactory ---
    def valueMapApi = ITApiFactory.getService(ValueMappingApi.class, null)

    // --- Step 9: Validate ValueMappingApi is not null ---
    if (valueMapApi == null) {
        throw new IllegalStateException("ValueMappingApi is not available")
    }

    // --- Step 10: Look up the mapped value ---
    def mappedValue = valueMapApi.getMappedValue(srcAgency, srcScheme, srcValue, tgtAgency, tgtScheme)

    // --- Step 11: Validate the result is not null or empty ---
    if (!mappedValue) {
        throw new IllegalStateException("No mapped value found for: " + srcValue)
    }

    // --- Step 12: Set result as message property "vm_result" ---
    message.setProperty("vm_result", mappedValue)

    // --- Step 13: Log diagnostics to MPL if logMode is INFO ---
    if ("INFO" == logMode) {
        messageLog?.addCustomHeaderProperty("vm_srcValue", srcValue)
        messageLog?.addCustomHeaderProperty("vm_result", mappedValue)
    }

    // --- Step 14: Return message; payload is not modified ---
    return message
}