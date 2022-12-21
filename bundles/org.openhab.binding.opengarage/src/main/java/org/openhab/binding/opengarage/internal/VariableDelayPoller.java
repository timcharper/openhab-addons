package org.openhab.binding.opengarage.internal;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Variable poller will schedule a task to run periodically, but will allow the
 * schedule to be modified on-the-fly
 *
 * @author Tim Harper - Initial contribution
 */
public class VariableDelayPoller {
    private final Logger logger = LoggerFactory.getLogger(VariableDelayPoller.class);
    private ScheduledExecutorService scheduler;
    private Supplier<Long> task;
    private long defaultPollSeconds;
    private AtomicLong lastThreadId = new AtomicLong(0);
    private Future<?> currentFuture;

    /**
     * @param scheduler - Reference to the Java Scheduler instance
     * @param task - task to invoke. Task should return a number of seconds indicating when the next task should be
     *            called
     * @param defaultPollSeconds - how long to set the next poll should task fail to return a value due to an uncaught
     *            exception
     */
    public VariableDelayPoller(ScheduledExecutorService scheduler, Supplier<Long> task, long defaultPollSeconds) {
        this.scheduler = scheduler;
        this.defaultPollSeconds = defaultPollSeconds;
        this.task = task;

        startNextPoll(lastThreadId.get(), 1);
    };

    synchronized public void stop(boolean abortIfRunning) {
        lastThreadId.getAndIncrement(); // prevent future tasks from being scheduled
        this.currentFuture.cancel(abortIfRunning);
    }

    private void doPoll(long threadId) {
        var nextPollDuration = this.defaultPollSeconds;
        try {
            nextPollDuration = task.get();
        } catch (Exception e) {
            logger.error("error occurred while invoking periodic task", e);
        }
        startNextPoll(threadId, nextPollDuration);
    }

    /**
     * Cause the next invocation of the task to run at a different time, canceling the scheduled task in the future.
     *
     * @param inSeconds How many seconds from now should we schedule the task?
     * @param abortExistingIfRunning If the task happens to be running now, should we abort it?
     */
    synchronized public void reschedule(long inSeconds, boolean abortExistingIfRunning) {
        logger.debug("Cancelled current future in order to reschedule...");
        this.currentFuture.cancel(abortExistingIfRunning);
        startNextPoll(lastThreadId.get(), inSeconds);
    }

    synchronized private void startNextPoll(long threadId, long inSeconds) {
        var nextThreadId = threadId + 1;
        // we want to be extra-sure that we don't have multiple poll invocations starting parallel poll sequences.
        if (lastThreadId.compareAndSet(threadId, nextThreadId)) {
            logger.debug("Starting next poll in {}", inSeconds);
            this.currentFuture = this.scheduler.schedule(() -> this.doPoll(nextThreadId), inSeconds, TimeUnit.SECONDS);
        }
    }
}
