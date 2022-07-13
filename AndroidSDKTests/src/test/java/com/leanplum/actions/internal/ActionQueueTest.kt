package com.leanplum.actions.internal

import com.leanplum.ActionContext
import org.junit.*
import org.mockito.Mockito

/**
 * Unit test for [ActionQueue]
 */
class ActionQueueTest {

  private var actions = listOf(
    Action.create(Mockito.mock(ActionContext::class.java)),
    Action.create(Mockito.mock(ActionContext::class.java)),
    Action.create(Mockito.mock(ActionContext::class.java))
  )

  @Test
  fun testPop() {
    val queue = ActionQueue()
    queue.pushBack(actions[0])
    queue.pushBack(actions[1])
    Assert.assertEquals(queue.pop(), actions[0])
    Assert.assertEquals(queue.pop(), actions[1])
    Assert.assertTrue(queue.empty())
  }

  @Test
  fun testPopAll() {
    val queue = ActionQueue()
    queue.pushBack(actions[0])
    queue.pushBack(actions[1])
    queue.pushBack(actions[2])

    val wantedSequence = listOf(actions[0], actions[1], actions[2])
    Assert.assertEquals(queue.popAll(), wantedSequence)
    Assert.assertTrue(queue.empty())
  }

  @Test
  fun testPushFront() {
    val queue = ActionQueue()
    queue.pushBack(actions[0])
    queue.pushBack(actions[1])
    queue.pushFront(actions[2])

    val wantedSequence = listOf(actions[2], actions[0], actions[1])
    Assert.assertEquals(queue.popAll(), wantedSequence)
  }

  @Test
  fun testPushFrontList() {
    val queue = ActionQueue()
    queue.pushBack(actions[0])
    queue.pushFront(listOf(actions[1], actions[2]))

    val wantedSequence = listOf(actions[1], actions[2], actions[0])
    Assert.assertEquals(queue.popAll(), wantedSequence)
  }
}
