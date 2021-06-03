package com.leanplum;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.leanplum.__setup.AbstractTest;
import com.leanplum._whitebox.utilities.ResponseHelper;
import com.leanplum.internal.VarCache;
import org.junit.Test;

public class SecuredVarsTest extends AbstractTest {

  @Test
  public void testVarsAndSignature() {
    ResponseHelper.seedResponse("/responses/secured_vars_response.json");

    Leanplum.start(mContext);
    assertTrue(Leanplum.hasStarted());

    SecuredVars securedVars = VarCache.getSecuredVars();

    assertTrue(securedVars.getJson().contains("intVariable"));
    assertTrue(securedVars.getJson().contains("stringVariable"));

    assertEquals(securedVars.getSignature(), "sign_of_vars");
  }

  @Test
  public void testVarsNoSignature() {
    ResponseHelper.seedResponse("/responses/secured_vars_no_sign_response.json");

    Leanplum.start(mContext);
    assertTrue(Leanplum.hasStarted());

    SecuredVars securedVars = VarCache.getSecuredVars();
    assertNull(securedVars);
  }

  @Test
  public void testEmptyVarsNoSignature() {
    ResponseHelper.seedResponse("/responses/secured_vars_empty_response.json");

    Leanplum.start(mContext);
    assertTrue(Leanplum.hasStarted());

    SecuredVars securedVars = VarCache.getSecuredVars();
    assertNull(securedVars);
  }

}
