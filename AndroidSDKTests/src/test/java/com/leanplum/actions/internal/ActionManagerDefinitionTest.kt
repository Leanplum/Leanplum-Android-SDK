package com.leanplum.actions.internal

import android.content.Context
import com.leanplum.ActionArgs
import com.leanplum.ActionContext
import com.leanplum.Leanplum
import com.leanplum.__setup.AbstractTest
import com.leanplum.callbacks.ActionCallback
import com.leanplum.internal.ActionManager
import com.leanplum.messagetemplates.MessageTemplate
import com.leanplum.messagetemplates.MessageTemplates
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito

class ActionManagerDefinitionTest : AbstractTest() {

  /**
   * Tests whether Leanplum.defineAction(...) works as intended.
   */
  @Test
  fun testLeanplumDefine() {
    val presentHandler = object : ActionCallback() {
      override fun onResponse(context: ActionContext?): Boolean = true
    }
    val dismissHandler = object : ActionCallback() {
      override fun onResponse(context: ActionContext?): Boolean = true
    }
    val name = "action1"
    val args = ActionArgs()

    Leanplum.defineAction(name, 1, args, presentHandler, dismissHandler)

    val ad = ActionManager.getInstance().definitions.findDefinition(name)

    assertEquals(ad?.name, name)
    assertEquals(ad?.kind, 1)
    assertEquals(ad?.args, args)
    assertEquals(ad?.presentHandler, presentHandler)
    assertEquals(ad?.dismissHandler, dismissHandler)
  }

  /**
   * Tests whether MessageTemplates.register(...) works as intended.
   */
  @Test
  fun testRegisterMessageTemplate() {
    val name = "action2"
    val args = ActionArgs()
    var presentHandlerInvoked = false
    var dismissHandlerInvoked = false
    val actionContext = Mockito.mock(ActionContext::class.java)

    val messageTemplate = object : MessageTemplate {
      override fun getName(): String = name
      override fun createActionArgs(context: Context): ActionArgs = args
      override fun present(context: ActionContext): Boolean {
        presentHandlerInvoked = true
        return true
      }
      override fun dismiss(context: ActionContext): Boolean {
        dismissHandlerInvoked = true
        return true
      }
    }

    MessageTemplates.registerTemplate(messageTemplate, mContext)

    val actionDefinition = ActionManager.getInstance().definitions.findDefinition(name)

    assertEquals(actionDefinition?.name, name)
    assertEquals(actionDefinition?.kind, 3)
    assertEquals(actionDefinition?.args, args)

    actionDefinition?.presentHandler?.onResponse(actionContext)
    assertTrue(presentHandlerInvoked)

    actionDefinition?.dismissHandler?.onResponse(actionContext)
    assertTrue(dismissHandlerInvoked)
  }

}
