package com.leanplum.callbacks;

import com.leanplum.models.MessageArchiveData;

/**
 * Message Closed Callback
 * Nice to know when a Message is dismissed
 * @author Santiago Castaneda Munoz - Tilting Point
 */

public abstract class MessageClosedCallback implements Runnable {

    private MessageArchiveData messageArchiveData;

    public void setMessageArchiveData(MessageArchiveData messageArchiveData) {
        this.messageArchiveData = messageArchiveData;
    }

    public void run() {
        this.messageClosed(messageArchiveData);
    }

    public abstract void messageClosed(MessageArchiveData messageArchiveData);
}