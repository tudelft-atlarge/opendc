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

package org.opendc.workflow.service

import io.opentelemetry.api.metrics.Meter
import org.opendc.compute.api.ComputeClient
import org.opendc.workflow.api.Job
import org.opendc.workflow.service.internal.WorkflowServiceImpl
import org.opendc.workflow.service.scheduler.WorkflowSchedulerMode
import org.opendc.workflow.service.scheduler.job.JobAdmissionPolicy
import org.opendc.workflow.service.scheduler.job.JobOrderPolicy
import org.opendc.workflow.service.scheduler.task.TaskEligibilityPolicy
import org.opendc.workflow.service.scheduler.task.TaskOrderPolicy
import java.time.Clock
import kotlin.coroutines.CoroutineContext

/**
 * A service for cloud workflow management.
 *
 * The workflow scheduler is modelled after the Reference Architecture for Topology Scheduling by Andreadis et al.
 */
public interface WorkflowService : AutoCloseable {
    /**
     * Submit the specified [Job] to the workflow service for scheduling.
     */
    public suspend fun submit(job: Job)

    /**
     * Run the specified [Job] and suspend execution until the job is finished.
     */
    public suspend fun run(job: Job)

    /**
     * Terminate the lifecycle of the workflow service, stopping all running workflows.
     */
    public override fun close()

    public companion object {
        /**
         * Construct a new [WorkflowService] implementation.
         *
         * @param context The [CoroutineContext] to use in the service.
         * @param clock The clock instance to use.
         * @param tracer The event tracer to use.
         * @param meter The meter to use.
         * @param compute The compute client to use.
         * @param mode The scheduling mode to use.
         * @param jobAdmissionPolicy The job admission policy to use.
         * @param jobOrderPolicy The job order policy to use.
         * @param taskEligibilityPolicy The task eligibility policy to use.
         * @param taskOrderPolicy The task order policy to use.
         */
        public operator fun invoke(
            context: CoroutineContext,
            clock: Clock,
            meter: Meter,
            compute: ComputeClient,
            mode: WorkflowSchedulerMode,
            jobAdmissionPolicy: JobAdmissionPolicy,
            jobOrderPolicy: JobOrderPolicy,
            taskEligibilityPolicy: TaskEligibilityPolicy,
            taskOrderPolicy: TaskOrderPolicy
        ): WorkflowService {
            return WorkflowServiceImpl(
                context,
                clock,
                meter,
                compute,
                mode,
                jobAdmissionPolicy,
                jobOrderPolicy,
                taskEligibilityPolicy,
                taskOrderPolicy
            )
        }
    }
}
