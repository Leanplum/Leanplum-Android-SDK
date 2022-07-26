/*
 * Copyright 2021, Leanplum, Inc. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.leanplum;

import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;
import com.clevertap.android.sdk.CleverTapAPI;
import com.huawei.agconnect.AGConnectOptionsBuilder;
import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hmf.tasks.OnCompleteListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.aaid.entity.AAIDResult;
import com.huawei.hms.push.HmsMessaging;
import com.huawei.hms.utils.Util;
import com.leanplum.internal.Constants.Defaults;
import com.leanplum.internal.Log;

/**
 * Leanplum provider for Huawei Push Kit.
 * Class is instantiated by reflection using default constructor.
 */
class LeanplumHmsProvider extends LeanplumCloudMessagingProvider {

  /**
   * Constructor called by reflection.
   */
  public LeanplumHmsProvider() {
  }

  @Override
  protected String getSharedPrefsPropertyName() {
    return Defaults.PROPERTY_HMS_TOKEN_ID;
  }

  @Override
  public PushProviderType getType() {
    return PushProviderType.HMS;
  }

  @Override
  public void unregister() {
    // no implementation
  }

  @Override
  public void updateRegistrationId() {
    Context context = Leanplum.getContext();
    if (context == null) {
      return;
    }

    String regId = null;
    try {
      // Read from agconnect-services.json
      String appId = new AGConnectOptionsBuilder().build(context).getString("client/app_id");
      regId = HmsInstanceId.getInstance(context).getToken(appId, "HCM");
    } catch (Throwable e) {
      Log.e("HMS getToken failed:\n" + Log.getStackTraceString(e));
    }

    if (!TextUtils.isEmpty(regId)) {
      setRegistrationId(regId);

      CleverTapAPI ctApi = CleverTapAPI.getDefaultInstance(context.getApplicationContext());
      if (ctApi != null) {
        ctApi.pushHuaweiRegistrationId(regId, true);
      } else {
        Log.e("HMS - ctApi is null");
      }
    }
  }
}
