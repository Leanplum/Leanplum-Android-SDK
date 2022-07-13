package com.leanplum.actions.internal

import com.leanplum.ActionContext
import com.leanplum.__setup.AbstractTest
import com.leanplum.actions.MessageDisplayChoice
import com.leanplum.actions.MessageDisplayController
import com.leanplum.internal.ActionManager
import com.leanplum.internal.VarCache
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(value = [VarCache::class])
class ActionManagerTriggeringTest : AbstractTest() {

  lateinit var actionContexts: List<ActionContext>

  @Before
  override fun before() {
    super.before()
    actionContexts = listOf(
      Mockito.mock(ActionContext::class.java),
      Mockito.mock(ActionContext::class.java)
    )
  }

  @After
  override fun after() {
    super.after()
    ActionManager.getInstance().isPaused = false
    ActionManager.getInstance().queue.queue = mutableListOf()
    ActionManager.getInstance().messageDisplayController = null
  }

  /**
   * When several actions get triggered on the same event only the first one should be added to the
   * queue. This is the old behaviour and we want to preserve it. When setting the controller
   * instance the client might decide what actions to add to queue.
   */
  @Test
  fun testDefaultTriggerWhenControllerIsMissing() {
    ActionManager.getInstance().isPaused = true
    ActionManager.getInstance().trigger(actionContexts)

    val queue = ActionManager.getInstance().queue.queue

    assertEquals(1, queue.size)
    assertEquals(actionContexts[0], queue[0].context)
  }

  /**
   * Tests whether all actions are triggered when using custom [MessageDisplayController].
   */
  @Test
  fun testTriggerWithController() {
    ActionManager.getInstance().messageDisplayController = object : MessageDisplayController {
      override fun shouldDisplayMessage(action: ActionContext) = MessageDisplayChoice.show()
      override fun prioritizeMessages(
        actions: List<ActionContext>,
        trigger: ActionsTrigger?) = actions.reversed()
    }

    ActionManager.getInstance().isPaused = true
    ActionManager.getInstance().trigger(actionContexts)

    val queue = ActionManager.getInstance().queue.queue

    assertEquals(2, queue.size)
    // list is intentionally reversed in prioritizeMessages
    assertEquals(actionContexts[0], queue[1].context)
    assertEquals(actionContexts[1], queue[0].context)
  }

  /**
   * Tests whether action goes in front of queue when using high priority.
   */
  @Test
  fun testTriggerWithPriorityHigh() {
    ActionManager.getInstance().isPaused = true
    ActionManager.getInstance().trigger(actionContexts[0])
    ActionManager.getInstance().trigger(actionContexts[1], Priority.HIGH)

    val queue = ActionManager.getInstance().queue.queue

    assertEquals(2, queue.size)
    // list is reversed because of priority
    assertEquals(actionContexts[0], queue[1].context)
    assertEquals(actionContexts[1], queue[0].context)
  }

  /**
   * Tests [ActionManager.triggerDelayedMessages] by triggering indefinitely delayed actions.
   */
  @Test
  fun testTriggerDelayedMessages() {
    // hasReceivedDiffs would stop performActions from executing if not true
    PowerMockito.mockStatic(VarCache::class.java)
    PowerMockito.`when`<Boolean>(VarCache::class.java, "hasReceivedDiffs").thenReturn(true)
    assertTrue(VarCache.hasReceivedDiffs())

    ActionManager.getInstance().messageDisplayController = object : MessageDisplayController {
      override fun shouldDisplayMessage(action: ActionContext): MessageDisplayChoice {
        return MessageDisplayChoice.delayIndefinitely()
      }
      override fun prioritizeMessages(
        actions: List<ActionContext>,
        trigger: ActionsTrigger?) = actions
    }

    val queue = ActionManager.getInstance().queue.queue
    val delayedQueue = ActionManager.getInstance().delayedQueue.queue

    ActionManager.getInstance().trigger(actionContexts)
    assertTrue(queue.isEmpty())
    assertEquals(2, delayedQueue.size)

    ActionManager.getInstance().isPaused = true
    ActionManager.getInstance().triggerDelayedMessages()

    assertEquals(2, queue.size)
    assertTrue(delayedQueue.isEmpty())
    assertEquals(actionContexts[0], queue[0].context)
    assertEquals(actionContexts[1], queue[1].context)
  }
}
