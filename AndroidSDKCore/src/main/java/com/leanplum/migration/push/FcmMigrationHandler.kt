package com.leanplum.migration.push

import com.clevertap.android.sdk.pushnotification.fcm.CTFcmMessageHandler

class FcmMigrationHandler internal constructor() : CTFcmMessageHandler()

// TODO trigger sending of tokens after migration status is fetched?
