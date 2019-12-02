package com.leanplum.internal;

public class ShadowOperationQueue extends OperationQueue {

    public ShadowOperationQueue() {
        super();
    }

    @Override
    boolean addOperation(Runnable operation) {
        operation.run();
        return true;
    }
}
