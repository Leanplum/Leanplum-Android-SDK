package com.leanplum.actions.internal

import android.content.Context
import com.leanplum.ActionArgs
import com.leanplum.ActionContext
import com.leanplum.__setup.AbstractTest
import com.leanplum.actions.LeanplumActions
import com.leanplum.actions.MessageDisplayChoice
import com.leanplum.actions.MessageDisplayController
import com.leanplum.actions.MessageDisplayListener
import com.leanplum.internal.ActionManager
import com.leanplum.internal.VarCache
import com.leanplum.messagetemplates.MessageTemplate
import com.leanplum.messagetemplates.MessageTemplates
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(value = [VarCache::class])
class ActionManagerExecutionTest : AbstractTest() {

  @Before
  override fun before() {
    super.before()
    // hasReceivedDiffs would stop performActions from executing if not true
    PowerMockito.mockStatic(VarCache::class.java)
    PowerMockito.`when`<Boolean>(VarCache::class.java, "hasReceivedDiffs").thenReturn(true)
    assertTrue(VarCache.hasReceivedDiffs())
  }

  @After
  override fun after() {
    super.after()
    ActionManager.getInstance().isPaused = false
    ActionManager.getInstance().queue.queue = mutableListOf()
    ActionManager.getInstance().delayedQueue.queue = mutableListOf()
    ActionManager.getInstance().currentAction = null
    ActionManager.getInstance().messageDisplayController = null
    ActionManager.getInstance().messageDisplayListener = null
    ActionManager.getInstance().definitions.clear()
    ActionManager.getInstance().scheduler = ActionScheduler()
    LeanplumActions.setDismissOnPushOpened(true)
  }

  private fun registerTemplate(name: String, onPresent: () -> Unit, onDismiss: () -> Unit) {
    val messageTemplate = object : MessageTemplate {
      override fun getName(): String = name
      override fun createActionArgs(context: Context): ActionArgs = ActionArgs()
      override fun present(context: ActionContext): Boolean {
        onPresent.invoke()
        return true
      }
      override fun dismiss(context: ActionContext): Boolean {
        onDismiss.invoke()
        context.actionDismissed()
        return true
      }
    }

    MessageTemplates.registerTemplate(messageTemplate, mContext)
  }

  private inner class MessageData(name: String) {
    var messagePresented: Boolean = false
    var messageDismissed: Boolean = false
    val actionContext: ActionContext

    init {
      registerTemplate(
        name = name,
        onPresent = { messagePresented = true },
        onDismiss = { messageDismissed = true },
      )

      actionContext = ActionContext(name, mapOf(), "${name}Id")
    }
  }

  /**
   * Tests whether [ActionManager.dismissCurrentAction] dismisses current executing action.
   */
  @Test
  fun testDismissCurrentAction() {
    val message = MessageData("message")

    // Trigger message
    ActionManager.getInstance().trigger(message.actionContext)
    val queue = ActionManager.getInstance().queue.queue

    assertTrue(queue.isEmpty())
    assertTrue(message.messagePresented)
    assertEquals(message.actionContext, ActionManager.getInstance().currentAction.context)

    // Dismiss message
    ActionManager.getInstance().dismissCurrentAction()
    assertTrue(message.messageDismissed)
    assertNull(ActionManager.getInstance().currentAction)
  }

  /**
   * Tests whether push notification's open action is triggered with higher priority and dismisses
   * current action when [LeanplumActions.setDismissOnPushOpened] is set to `true` (default case).
   */
  @Test
  fun testPrioritizePushNotificationAction() {
    val firstMessage = MessageData("message")

    val pushOpenAction = MessageData("push notification message").apply {
      this.actionContext.parentContext = ActionContext("__Push Notification", mapOf(), "messageId_push")
    }

    // Trigger first message
    ActionManager.getInstance().trigger(firstMessage.actionContext)
    assertEquals(firstMessage.actionContext, ActionManager.getInstance().currentAction.context)

    // Trigger push notification message
    ActionManager.getInstance().trigger(pushOpenAction.actionContext)
    assertEquals(pushOpenAction.actionContext, ActionManager.getInstance().currentAction.context)

    // Assert first message was dismissed
    val queue = ActionManager.getInstance().queue.queue
    assertTrue(queue.isEmpty())
    assertTrue(firstMessage.messageDismissed)
  }

  /**
   * Tests whether push notification's open action is not causing current executing action to be
   * dismissed when [LeanplumActions.setDismissOnPushOpened] is set to `false`.
   */
  @Test
  fun testDoNotPrioritizePushNotificationAction() {
    // Disable push notification's open action priority
    LeanplumActions.setDismissOnPushOpened(false)

    val firstMessage = MessageData("message")

    val pushOpenAction = MessageData("push notification message").apply {
      this.actionContext.parentContext = ActionContext("__Push Notification", mapOf(), "messageId_push")
    }

    // Trigger first message
    ActionManager.getInstance().trigger(firstMessage.actionContext)
    assertEquals(firstMessage.actionContext, ActionManager.getInstance().currentAction.context)

    // Trigger push notification message
    ActionManager.getInstance().trigger(pushOpenAction.actionContext)
    assertEquals(firstMessage.actionContext, ActionManager.getInstance().currentAction.context)

    // Assert first message was not dismissed
    val queue = ActionManager.getInstance().queue.queue
    assertEquals(1, queue.size)
    assertFalse(firstMessage.messageDismissed)
  }

  /**
   * Tests show/delay/discard/discardIndefinitely results for
   * MessageDisplayController.shouldDisplayMessage(...).
   */
  @Test
  fun testShouldDisplayMessage() {
    // Define test messages
    val messageToShow = MessageData("messageToShow")
    val messageToDiscard = MessageData("messageToDiscard")
    val messageToDelayIndefinitely = MessageData("messageToDelayIndefinitely")
    val messageToDelay = MessageData("messageToDelay")

    // Register MessageDisplayController
    LeanplumActions.setMessageDisplayController(object : MessageDisplayController {
      override fun shouldDisplayMessage(action: ActionContext): MessageDisplayChoice? {
        return when (action.actionName()) {
          "messageToShow" -> MessageDisplayChoice.show()
          "messageToDiscard" -> MessageDisplayChoice.discard()
          "messageToDelayIndefinitely" -> MessageDisplayChoice.delayIndefinitely()
          "messageToDelay" -> MessageDisplayChoice.delay(5)
          else -> null
        }
      }
      override fun prioritizeMessages(
        actions: List<ActionContext>,
        trigger: ActionsTrigger?
      ) = actions
    })

    // Trigger messageToShow
    ActionManager.getInstance().trigger(messageToShow.actionContext)
    assertEquals(messageToShow.actionContext, ActionManager.getInstance().currentAction.context)
    ActionManager.getInstance().currentAction = null // reset message

    // Trigger messageToDiscard
    ActionManager.getInstance().trigger(messageToDiscard.actionContext)
    assertNull(ActionManager.getInstance().currentAction)

    // Trigger messageToDelayIndefinitely
    ActionManager.getInstance().trigger(messageToDelayIndefinitely.actionContext)
    assertNull(ActionManager.getInstance().currentAction)

    // Trigger messageToDelay
    val scheduler = Mockito.mock(ActionScheduler::class.java)
    val action = Action(context = messageToDelay.actionContext)

    Mockito.doNothing().`when`(scheduler).schedule(action, 5)
    ActionManager.getInstance().scheduler = scheduler

    ActionManager.getInstance().trigger(messageToDelay.actionContext)
    Mockito.verify(scheduler, Mockito.times(1)).schedule(action, 5)
  }

  /**
   * Tests all methods of [MessageDisplayListener].
   */
  @Test
  fun testMessageDisplayListener() {
    val message = MessageData("message")

    var messageDisplayed = false
    var messageDismissed = false

    val runActionName = "open action"
    var actionExecuted = false

    // Register listener
    ActionManager.getInstance().messageDisplayListener = object : MessageDisplayListener {
      override fun onMessageDisplayed(action: ActionContext) {
        assertEquals(message.actionContext, action)
        messageDisplayed = true
      }
      override fun onMessageDismissed(action: ActionContext) {
        assertEquals(message.actionContext, action)
        messageDismissed = true
      }
      override fun onActionExecuted(name: String, action: ActionContext) {
        assertNotEquals(message.actionContext, action)
        assertEquals(message.actionContext, action.parentContext)
        assertEquals(runActionName, name)
        actionExecuted = true
      }
    }

    // Test displayed
    ActionManager.getInstance().trigger(message.actionContext)
    assertEquals(message.actionContext, ActionManager.getInstance().currentAction.context)
    assertTrue(messageDisplayed)

    // Test action executed - `open action` is not defined in the args and wouldn't execute anything
    message.actionContext.runActionNamed(runActionName)
    assertTrue(actionExecuted)

    // Test dismissed
    ActionManager.getInstance().dismissCurrentAction()
    assertNull(ActionManager.getInstance().currentAction)
    assertTrue(messageDismissed)
  }
}
