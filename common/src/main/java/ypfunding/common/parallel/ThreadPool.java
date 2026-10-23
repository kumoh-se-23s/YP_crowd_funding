package ypfunding.common.parallel;

import ypfunding.common.util.LogUtil;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPool {
    private final Thread[] pool;
    private final Queue<Runnable> tasks;
    private final AtomicInteger runningTaskCount = new AtomicInteger(0);

    public ThreadPool(int size) {
        this.pool = new Thread[size];
        this.tasks = new LinkedList<>();
        start();
    }

    private void start() {
        for (int i = 0; i < pool.length; i++) {
            pool[i] = new Thread(() -> {
                Runnable r;
                while (true) {
                    try {
                        synchronized (ThreadPool.this) {
                            while ((r = poll()) == null) {
                                wait();
                            }
                        }
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                        return;
                    }

                    try {
                        runningTaskCount.getAndIncrement();
                        r.run();
                    } catch (RuntimeException e) {
                        LogUtil.logError(e);
                    } finally {
                        runningTaskCount.getAndDecrement();
                        synchronized (ThreadPool.this) {
                            ThreadPool.this.notifyAll();
                        }
                    }
                }
            });
            pool[i].start();
        }
    }

    public synchronized void waitIdle() {
        while (!tasks.isEmpty() || runningTaskCount.get() > 0) {
            try {
                wait();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public synchronized void queue(Runnable task) {
        tasks.offer(task);
        notifyAll();
    }

    private synchronized Runnable poll() {
        if (tasks.isEmpty()) return null;
        return tasks.poll();
    }
}
