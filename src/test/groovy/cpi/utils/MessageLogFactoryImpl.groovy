package cpi.utils

import com.sap.it.api.msglog.MessageLog
import com.sap.it.api.msglog.MessageLogFactory

class MessageLogFactoryImpl implements MessageLogFactory {
    def messageLog = new MessageLogImpl()

    @Override
    MessageLog getMessageLog(Object o) {
        if(o != null)
            messageLog = new MessageLogImpl(o.toString())
        return messageLog
    }

}
