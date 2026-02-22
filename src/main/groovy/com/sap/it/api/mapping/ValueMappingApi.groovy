package com.sap.it.api.mapping

/**
 * Stub interface for com.sap.it.api.mapping.ValueMappingApi.
 *
 * The real class exists only in the SAP CPI runtime. This stub provides
 * the compile-time type needed by ReadValueMapping.groovy and allows
 * Spock to create Stub(ValueMappingApi) instances in local tests.
 *
 * Only the method signature used by ReadValueMapping is declared here.
 */
interface ValueMappingApi {
    String getMappedValue(String srcAgency, String srcScheme, String srcValue,
                          String tgtAgency, String tgtScheme)
}
