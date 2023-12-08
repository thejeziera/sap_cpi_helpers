package cpi.scripts.protocol_adapters.odata

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.transform.ToString

class ODataOperations {
    static String filter(Closure<?> criteria) {
        def builder = new FilterBuilder()
        criteria.delegate = builder
        criteria()
        return builder.build()
    }

    @ToString(includeNames = true, includeFields = true)
    static class FilterBuilder {
        private String filterString = ''

        def eq(String field, value) {
            addToFilter("$field eq '$value'")
        }

        def ne(String field, value) {
            addToFilter("$field ne '$value'")
        }

        // TODO Add more OData operations (like gt, lt, and, or, etc.)

        private void addToFilter(String expression) {
            if (!filterString.isEmpty()) {
                filterString += ' and '
            }
            filterString += expression
        }

        String build() {
            return filterString
        }
    }
}

def Message processData(Message message) {
    // Example usage of ODataOperations
    def odataFilter = ODataOperations.filter {
        eq 'Name', 'John Doe'
        ne 'Status', 'Inactive'
        // Additional criteria can be added here
    }

    // Now odataFilter contains an OData filter query string
    // You can use this in your OData operations

    return message
}
