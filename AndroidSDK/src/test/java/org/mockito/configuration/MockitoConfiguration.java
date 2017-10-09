// Copyright 2016, Leanplum Inc.
package org.mockito.configuration;

/**
 * @author Milos Jakovljevic
 */
public class MockitoConfiguration extends DefaultMockitoConfiguration {
  @Override
  public boolean enableClassCache() {
    return false;
  }
}
