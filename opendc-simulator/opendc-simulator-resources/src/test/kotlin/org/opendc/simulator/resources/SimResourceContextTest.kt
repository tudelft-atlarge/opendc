/*
 * Copyright (c) 2021 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.opendc.simulator.resources

import io.mockk.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.opendc.simulator.core.runBlockingSimulation

/**
 * A test suite for the [SimAbstractResourceContext] class.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SimResourceContextTest {
    @Test
    fun testFlushWithoutCommand() = runBlockingSimulation {
        val scheduler = SimResourceSchedulerTrampoline(coroutineContext, clock)
        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any()) } returns SimResourceCommand.Consume(10.0, 1.0) andThen SimResourceCommand.Exit

        val context = object : SimAbstractResourceContext(4200.0, scheduler, consumer) {
            override fun onIdle(deadline: Long) {}
            override fun onConsume(work: Double, limit: Double, deadline: Long) {}
            override fun onFinish() {}
        }

        context.flush(isIntermediate = false)
    }

    @Test
    fun testIntermediateFlush() = runBlockingSimulation {
        val scheduler = SimResourceSchedulerTrampoline(coroutineContext, clock)
        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any()) } returns SimResourceCommand.Consume(10.0, 1.0) andThen SimResourceCommand.Exit

        val context = spyk(object : SimAbstractResourceContext(4200.0, scheduler, consumer) {
            override fun onIdle(deadline: Long) {}
            override fun onFinish() {}
            override fun onConsume(work: Double, limit: Double, deadline: Long) {}
        })

        context.start()
        delay(1) // Delay 1 ms to prevent hitting the fast path
        context.flush(isIntermediate = true)

        verify(exactly = 2) { context.onConsume(any(), any(), any()) }
    }

    @Test
    fun testIntermediateFlushIdle() = runBlockingSimulation {
        val scheduler = SimResourceSchedulerTrampoline(coroutineContext, clock)
        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any()) } returns SimResourceCommand.Idle(10) andThen SimResourceCommand.Exit

        val context = spyk(object : SimAbstractResourceContext(4200.0, scheduler, consumer) {
            override fun onIdle(deadline: Long) {}
            override fun onFinish() {}
            override fun onConsume(work: Double, limit: Double, deadline: Long) {}
        })

        context.start()
        delay(5)
        context.flush(isIntermediate = true)
        delay(5)
        context.flush(isIntermediate = true)

        assertAll(
            { verify(exactly = 2) { context.onIdle(any()) } },
            { verify(exactly = 1) { context.onFinish() } }
        )
    }

    @Test
    fun testDoubleStart() = runBlockingSimulation {
        val scheduler = SimResourceSchedulerTrampoline(coroutineContext, clock)
        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any()) } returns SimResourceCommand.Idle(10) andThen SimResourceCommand.Exit

        val context = object : SimAbstractResourceContext(4200.0, scheduler, consumer) {
            override fun onIdle(deadline: Long) {}
            override fun onFinish() {}
            override fun onConsume(work: Double, limit: Double, deadline: Long) {}
        }

        context.start()
        assertThrows<IllegalStateException> { context.start() }
    }

    @Test
    fun testIdempodentCapacityChange() = runBlockingSimulation {
        val scheduler = SimResourceSchedulerTrampoline(coroutineContext, clock)
        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any()) } returns SimResourceCommand.Consume(10.0, 1.0) andThen SimResourceCommand.Exit

        val context = object : SimAbstractResourceContext(4200.0, scheduler, consumer) {
            override fun onIdle(deadline: Long) {}
            override fun onConsume(work: Double, limit: Double, deadline: Long) {}
            override fun onFinish() {}
        }

        context.start()
        context.capacity = 4200.0

        verify(exactly = 0) { consumer.onEvent(any(), SimResourceEvent.Capacity) }
    }

    @Test
    fun testFailureNoInfiniteLoop() = runBlockingSimulation {
        val scheduler = SimResourceSchedulerTrampoline(coroutineContext, clock)
        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any()) } returns SimResourceCommand.Exit
        every { consumer.onEvent(any(), SimResourceEvent.Exit) } throws IllegalStateException("onEvent")
        every { consumer.onFailure(any(), any()) } throws IllegalStateException("onFailure")

        val context = object : SimAbstractResourceContext(4200.0, scheduler, consumer) {
            override fun onIdle(deadline: Long) {}
            override fun onConsume(work: Double, limit: Double, deadline: Long) {}
            override fun onFinish() {}
        }

        context.start()

        delay(1)

        verify(exactly = 1) { consumer.onFailure(any(), any()) }
    }
}
