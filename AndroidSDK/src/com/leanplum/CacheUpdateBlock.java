// Copyright 2017, Leanplum, Inc.
package com.leanplum;

/**
 * Update block that will be triggered on new content.
 *
 * @author Ben Marten
 */
public interface CacheUpdateBlock {
  void updateCache();
}
