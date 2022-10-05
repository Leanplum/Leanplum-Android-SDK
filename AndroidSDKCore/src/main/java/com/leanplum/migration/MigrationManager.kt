package com.leanplum.migration

import com.leanplum.callbacks.CleverTapInstanceCallback
import com.leanplum.internal.*
import com.leanplum.migration.model.MigrationConfig
import com.leanplum.migration.model.MigrationState
import com.leanplum.migration.wrapper.IWrapper
import com.leanplum.migration.wrapper.NullWrapper
import com.leanplum.migration.wrapper.StaticMethodsWrapper
import com.leanplum.migration.wrapper.WrapperFactory
import org.json.JSONObject

object MigrationManager {

  @JvmStatic
  fun getState(): MigrationState = MigrationState.from(MigrationConfig.state)

  @JvmStatic
  var wrapper: IWrapper = NullWrapper
    private set

  @JvmStatic
  var cleverTapInstanceCallback: CleverTapInstanceCallback? = null
    set(value) {
      field = value
      wrapper.setInstanceCallback(value)
    }

  @JvmStatic
  fun updateWrapper() {
    if (getState().useCleverTap()
      && (wrapper == NullWrapper || wrapper == StaticMethodsWrapper)
    ) {
      wrapper = WrapperFactory.createWrapper(cleverTapInstanceCallback)
    } else if (wrapper != NullWrapper && !getState().useCleverTap()) {
      wrapper = WrapperFactory.createNullWrapper()
    }
  }

  @JvmStatic
  fun fetchState(callback: (MigrationState) -> Unit) {
    if (getState() != MigrationState.Undefined) {
      updateWrapper() // replaces StaticMethodsWrapper with CTWrapper
      callback.invoke(getState())
    } else {
      fetchStateAsync {
        callback.invoke(getState())
      }
    }
  }

  private fun fetchStateAsync(callback: (Boolean) -> Unit) {
    val request = RequestBuilder
      .withGetMigrateState()
      .andType(Request.RequestType.IMMEDIATE)
      .create()

    request.onError {
      Log.d("Error getting migration state", it)
      callback.invoke(false)
    }

    request.onResponse {
      Log.d("Migration state response: $it")
      val responseData = ResponseHandler().handleMigrateStateContent(it)
      if (responseData != null) {
        val oldState = getState()
        MigrationConfig.update(responseData)
        val newState = getState()
        handleStateTransition(oldState, newState)
      }
      callback.invoke(true)
    }

    RequestSender.getInstance().send(request)
  }

  @JvmStatic
  fun refreshStateMidSession(responseBody: JSONObject): Boolean {
    val newHash = ResponseHandler().handleMigrateState(responseBody) ?: return false
    if (newHash != MigrationConfig.hash) {
      fetchStateAsync { success ->
        if (success) {
          // transition side effects are handled in fetchStateAsync
        } else {
          // TODO guard against continuous failure?
        }
      }
      return true
    }
    return false
  }

  private fun handleStateTransition(oldState: MigrationState, newState: MigrationState) {
    if (oldState.useLeanplum() && !newState.useLeanplum()) {
      OperationQueue.sharedInstance().addOperation {
        // flush all saved data to LP
        RequestSender.getInstance().sendRequests()
        // delete LP data
        VarCache.clearUserContent()
        VarCache.saveDiffs()
      }
    }

    if (!oldState.useCleverTap() && newState.useCleverTap()) {
      OperationQueue.sharedInstance().addOperation {
        // flush all saved data to LP, new data will come with the flag ct=true
        RequestSender.getInstance().sendRequests()
        // create wrapper
        updateWrapper()
      }
    }

    if (oldState.useCleverTap() && !newState.useCleverTap()) {
      // remove wrapper
      updateWrapper()
    }
  }

  fun mapAttributeName(attributeName: String): String {
    val newName = MigrationConfig.attributeMap?.get(attributeName)
    return newName ?: attributeName
  }

  fun mapAttributeName(attribute: Map.Entry<String, Any?>): String {
    val newName = MigrationConfig.attributeMap?.get(attribute.key)
    return newName ?: attribute.key
  }

  @JvmStatic
  fun trackGooglePlayPurchases() = MigrationConfig.trackGooglePlayPurchases

}
