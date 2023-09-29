package cpi.utils

class CPIScriptEnhancer {
    static void enhanceScript(Script script) {
        // Add properties to the script's metaClass
        script.metaClass.messageLogFactory = new MessageLogFactoryImpl()

        // Add utility methods to the script's metaClass
        script.metaClass.logMessage = { String message ->
            script.messageLogFactory.info(message)
        }
    }
}
