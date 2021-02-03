package com.leanplum.callbacks;

import com.leanplum.ActionContext;

/**
 * Callback that gets run when any action is triggered.
 *
 * @author Andrew First
 */
public abstract class ActionTriggeredCallback implements Runnable {

    private ActionContext actionContext;

    public void setActionContext(ActionContext actionContext) {
        this.actionContext = actionContext;
    }

    public void run() {
        this.actionTriggered(actionContext);
    }

    public abstract void actionTriggered(ActionContext context);
}
