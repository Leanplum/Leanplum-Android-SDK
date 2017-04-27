//Copyright 2016, Leanplum, Inc.

package com.leanplum.callbacks;

import com.leanplum.LeanplumActivityHelper;

/**
 * Action callback that will not be executed for activity classes that are ignored via
 * {@link LeanplumActivityHelper#deferMessagesForActivities(Class[])}
 *
 * @author Ben Marten
 */
public abstract class PostponableAction implements Runnable {
}
