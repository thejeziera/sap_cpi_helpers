package cpi.utils

import org.slf4j.LoggerFactory

trait CPITrait {
    def messageLogFactory = LoggerFactory.getLogger("CPI")

    def logMessage(String message) {
        messageLogFactory.info(message)
    }
}
