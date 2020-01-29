package com.leanplum.internal;

public class ShadowOperationQueue extends OperationQueue {

    public ShadowOperationQueue() {
        super();
    }

    @Override
    public boolean addOperation(Runnable operation) {
        operation.run();
        return true;
    }

    @Override
    public void addParallelOperation(Runnable operation) {
        operation.run();
    }
}
