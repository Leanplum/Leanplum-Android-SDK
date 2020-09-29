package com.leanplum.internal;

public class ShadowOperationQueue extends OperationQueue {

    private Runnable lastDelayedOperation;

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

    @Override
    public void addUiOperation(Runnable operation) {
        operation.run();
    }

    @Override
    public boolean addOperationAfterDelay(Runnable operation, long delayMillis) {
        /**
         * Check if previous operation is the same to avoid stack overflow.
         * Mainly because of the RequestSender timer implementation.
         */
        if (operation.equals(lastDelayedOperation)) {
            lastDelayedOperation = null;
            return true;
        }
        lastDelayedOperation = operation;
        operation.run();
        return true;
    }
}
