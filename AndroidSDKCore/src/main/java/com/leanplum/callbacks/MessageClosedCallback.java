package com.leanplum.callbacks;

/**
 * Message Closed Callback
 * Nice to know when a Message is dismissed
 * @author Santiago Castaneda Munoz - Tilting Point
 */

public abstract class MessageClosedCallback implements Runnable {

    public void run()
    {
        this.messageClosed();
    }

    public abstract void messageClosed();
}