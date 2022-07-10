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
    ActionManager.getInstance().definitions.actionDefinitions.clear()
    ActionManager.getInstance().scheduler = ActionScheduler()
    LeanplumActions.setDismissOnPushOpened(true)
  }

  private fun createMessage(
    name: String,
    messageId: String,
    onPresent: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null): ActionContext {
    val message = ActionContext(name, mapOf(), messageId)

    // Register template
    val messageTemplate = object : MessageTemplate {
      override fun getName(): String = name
      override fun createActionArgs(context: Context): ActionArgs = ActionArgs()
      override fun present(context: ActionContext): Boolean {
        onPresent?.invoke()
        return true
      }
      override fun dismiss(context: ActionContext): Boolean {
        onDismiss?.invoke()
        context.actionDismissed()
        return true
      }
    }

    MessageTemplates.registerTemplate(messageTemplate, mContext)
    return message
  }

  /**
   * Tests whether [ActionManager.dismissCurrentAction] dismisses current executing action.
   */
  @Test
  fun testDismissCurrentAction() {
    var dismissRequested = false

    val name = "message1"
    val message = createMessage(name, "messageId") {
      dismissRequested = true
    }

    // Set client MessageDisplayListener
    var messageDisplayed = false
    ActionManager.getInstance().messageDisplayListener = object : MessageDisplayListener {
      override fun onMessageDisplayed(action: ActionContext) {
        assertEquals(message, action)
        messageDisplayed = true
      }
      override fun onMessageDismissed(action: ActionContext) {}
      override fun onActionExecuted(name: String, action: ActionContext) {}
    }

    // Trigger message
    ActionManager.getInstance().trigger(message)
    val queue = ActionManager.getInstance().queue.queue

    assertTrue(queue.isEmpty())
    assertTrue(messageDisplayed)
    assertEquals(message, ActionManager.getInstance().currentAction.context)

    // Dismiss message
    ActionManager.getInstance().dismissCurrentAction()
    assertTrue(dismissRequested)
    assertNull(ActionManager.getInstance().currentAction)
  }

  /**
   * Tests whether push notification's open action is triggered with higher priority and dismisses
   * current action when [LeanplumActions.setDismissOnPushOpened] is set to `true` (default case).
   */
  @Test
  fun testPrioritizePushNotificationAction() {
    val name1 = "message1"
    var message1Dismissed = false
    val message1 = createMessage(name1, "messageId1") {
      message1Dismissed = true
    }

    val pushOpenActionName = "push notification message"
    val pushOpenAction = createMessage(pushOpenActionName, "messageId2").apply {
      this.parentContext = ActionContext("__Push Notification", mapOf(), "messageId_push")
    }

    // Trigger first message
    ActionManager.getInstance().trigger(message1)
    assertEquals(message1, ActionManager.getInstance().currentAction.context)

    // Trigger push notification message
    ActionManager.getInstance().trigger(pushOpenAction)
    assertEquals(pushOpenAction, ActionManager.getInstance().currentAction.context)

    // Assert first message was dismissed
    val queue = ActionManager.getInstance().queue.queue
    assertTrue(queue.isEmpty())
    assertTrue(message1Dismissed)
  }

  /**
   * Tests whether push notification's open action is not causing current executing action to be
   * dismissed when [LeanplumActions.setDismissOnPushOpened] is set to `false`.
   */
  @Test
  fun testDoNotPrioritizePushNotificationAction() {
    LeanplumActions.setDismissOnPushOpened(false)

    val name1 = "message1"
    var message1Dismissed = false
    val message1 = createMessage(name1, "messageId1") {
      message1Dismissed = true
    }

    val pushOpenActionName = "push notification message"
    val pushOpenAction = createMessage(pushOpenActionName, "messageId2").apply {
      this.parentContext = ActionContext("__Push Notification", mapOf(), "messageId_push")
    }

    // Trigger first message
    ActionManager.getInstance().trigger(message1)
    assertEquals(message1, ActionManager.getInstance().currentAction.context)

    // Trigger push notification message
    ActionManager.getInstance().trigger(pushOpenAction)
    assertEquals(message1, ActionManager.getInstance().currentAction.context)

    // Assert first message was not dismissed
    val queue = ActionManager.getInstance().queue.queue
    assertEquals(1, queue.size)
    assertFalse(message1Dismissed)
  }

  /**
   * Tests show/delay/discard/discardIndefinitely results for
   * MessageDisplayController.shouldDisplayMessage(...).
   */
  @Test
  fun testShowDelayDiscardMessages() {
    class MessageData(
      val name: String,
      var messagePresented: Boolean = false,
      var messageDismissed: Boolean = false,
    ) {
      val message = createMessage(
        name,
        "${name}Id",
        { messagePresented = true },
        { messageDismissed = true })
    }

    // Define test messages
    val messageToShow = MessageData("messageToShow")
    val messageToDiscard = MessageData("messageToDiscard")
    val messageToDelayIndefinitely = MessageData("messageToDelayIndefinitely")
    val messageToDelay = MessageData("messageToDelay")

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
    ActionManager.getInstance().trigger(messageToShow.message)
    assertEquals(messageToShow.message, ActionManager.getInstance().currentAction.context)
    ActionManager.getInstance().currentAction = null // reset message

    // Trigger messageToDiscard
    ActionManager.getInstance().trigger(messageToDiscard.message)
    assertNull(ActionManager.getInstance().currentAction)

    // Trigger messageToDelayIndefinitely
    ActionManager.getInstance().trigger(messageToDelayIndefinitely.message)
    assertNull(ActionManager.getInstance().currentAction)

    // Trigger messageToDelay
    val scheduler = Mockito.mock(ActionScheduler::class.java)
    val action = Action(context = messageToDelay.message)

    Mockito.doNothing().`when`(scheduler).schedule(action, 5)
    ActionManager.getInstance().scheduler = scheduler

    ActionManager.getInstance().trigger(messageToDelay.message)
    Mockito.verify(scheduler, Mockito.times(1)).schedule(action, 5)
  }
}
