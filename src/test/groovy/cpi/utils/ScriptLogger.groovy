package cpi.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ScriptLogger{
    private Logger internalLogger;

    private ScriptLogger(Logger logger) {
        this.internalLogger = logger;
    }

    static ScriptLogger getCategoryLogger(Class<?> requesterClass) {
        if(requesterClass as Object == null)
            throw new Exception("Requester Class for Log should not be null")
        return new ScriptLogger(LoggerFactory.getLogger(requesterClass));
    }

    void logErrors(Object... objects) {
        objects.each {
            it ->
                internalLogger.error("Internal logger: logErrors - $it")
        }
    }

    void logWarnings(Object... objects) {
        objects.each {
            it ->
                internalLogger.error("Internal logger: logWarnings - $it")
        }
    }

    void logInfo(Object... objects) {
        objects.each {
            it ->
                internalLogger.error("Internal logger: logInfo - $it")
        }
    }
}
