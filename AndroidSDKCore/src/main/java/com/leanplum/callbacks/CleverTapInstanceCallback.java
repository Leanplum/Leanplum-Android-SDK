package com.leanplum.callbacks;

import androidx.annotation.NonNull;
import com.clevertap.android.sdk.CleverTapAPI;

public interface CleverTapInstanceCallback {
  void onInstance(@NonNull CleverTapAPI cleverTapInstance);
}
