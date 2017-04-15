/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.filewatch;

import org.gradle.api.Action;
import org.gradle.api.internal.file.FileSystemSubset;
import org.gradle.initialization.BuildCancellationToken;
import org.gradle.internal.UncheckedException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultFileSystemChangeWaiterFactory implements FileSystemChangeWaiterFactory {
    private final FileWatcherFactory fileWatcherFactory;
    private final long quietPeriodMillis;

    public DefaultFileSystemChangeWaiterFactory(FileWatcherFactory fileWatcherFactory) {
        this(fileWatcherFactory, 250L);
    }

    public DefaultFileSystemChangeWaiterFactory(FileWatcherFactory fileWatcherFactory, long quietPeriodMillis) {
        this.fileWatcherFactory = fileWatcherFactory;
        this.quietPeriodMillis = quietPeriodMillis;
    }

    @Override
    public FileSystemChangeWaiter createChangeWaiter(BuildCancellationToken cancellationToken) {
        return new ChangeWaiter(fileWatcherFactory, quietPeriodMillis, cancellationToken);
    }

    private static class ChangeWaiter implements FileSystemChangeWaiter {
        private final long quietPeriodMillis;
        private final BuildCancellationToken cancellationToken;
        private final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        private final Lock lock = new ReentrantLock();
        private final Condition condition = lock.newCondition();
        private final AtomicLong lastChangeAt = new AtomicLong(0);
        private final FileWatcher watcher;
        private final Action<Throwable> onError;
        private boolean watching;
        private volatile FileWatcherEventListener eventListener;

        private ChangeWaiter(FileWatcherFactory fileWatcherFactory, long quietPeriodMillis, BuildCancellationToken cancellationToken) {
            this.quietPeriodMillis = quietPeriodMillis;
            this.cancellationToken = cancellationToken;
            this.onError = new Action<Throwable>() {
                @Override
                public void execute(Throwable throwable) {
                    error.set(throwable);
                    signal(lock, condition);
                }
            };
            watcher = fileWatcherFactory.watch(
                onError,
                new FileWatcherListener() {
                    @Override
                    public void onChange(final FileWatcher watcher, FileWatcherEvent event) {
                        if (!(event.getType() == FileWatcherEvent.Type.MODIFY && event.getFile().isDirectory())) {
                            FileWatcherEventListener listener = eventListener;
                            if (listener != null) {
                                listener.onChange(event);
                            }
                            signal(lock, condition, new Runnable() {
                                @Override
                                public void run() {
                                    lastChangeAt.set(monotonicClockMillis());
                                }
                            });
                        }
                    }
                }
            );
        }

        @Override
        public void watch(FileSystemSubset fileSystemSubset) {
            try {
                if (!fileSystemSubset.isEmpty()) {
                    watching = true;
                    watcher.watch(fileSystemSubset);
                }
            } catch (IOException e) {
                onError.execute(e);
            }
        }

        public void wait(Runnable notifier, FileWatcherEventListener eventListener) {
            Runnable cancellationHandler = new Runnable() {
                @Override
                public void run() {
                    signal(lock, condition);
                }
            };
            try {
                this.eventListener = eventListener;
                if (cancellationToken.isCancellationRequested()) {
                    return;
                }
                cancellationToken.addCallback(cancellationHandler);
                notifier.run();
                lock.lock();
                try {
                    long lastChangeAtValue = lastChangeAt.get();
                    while (!cancellationToken.isCancellationRequested() && error.get() == null && shouldKeepWaitingForQuietPeriod(lastChangeAtValue)) {
                        condition.await(quietPeriodMillis, TimeUnit.MILLISECONDS);
                        lastChangeAtValue = lastChangeAt.get();
                    }
                } finally {
                    lock.unlock();
                }
                Throwable throwable = error.get();
                if (throwable != null) {
                    throw throwable;
                }
            } catch (Throwable e) {
                throw UncheckedException.throwAsUncheckedException(e);
            } finally {
                this.eventListener = null;
                cancellationToken.removeCallback(cancellationHandler);
                watcher.stop();
            }
        }

        private boolean shouldKeepWaitingForQuietPeriod(long lastChangeAtValue) {
            long now = monotonicClockMillis();
            return lastChangeAtValue == 0   // no changes yet
                || now < lastChangeAtValue  // handle case where monotic clock isn't monotonic
                || now - lastChangeAtValue < quietPeriodMillis;
        }

        @Override
        public boolean isWatching() {
            return watching;
        }

        @Override
        public void stop() {
            watcher.stop();
        }
    }

    private static long monotonicClockMillis() {
        return System.nanoTime() / 1000000L;
    }

    private static void signal(Lock lock, Condition condition, Runnable runnable) {
        boolean interrupted = Thread.interrupted();
        lock.lock();
        try {
            runnable.run();
            condition.signal();
        } finally {
            lock.unlock();
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void signal(Lock lock, Condition condition) {
        signal(lock, condition, new Runnable() {
            @Override
            public void run() {

            }
        });
    }

}
