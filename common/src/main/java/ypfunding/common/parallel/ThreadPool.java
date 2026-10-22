package ypfunding.common.parallel;

import ypfunding.common.util.LogUtil;

import java.util.LinkedList;
import java.util.Queue;

public class ThreadPool {
    private final Thread[] threadPool;
    private final Queue<Runnable> tasks;

    public ThreadPool(int size) {
        this.threadPool = new Thread[size];
        this.tasks = new LinkedList<>();
    }

    public void start() {
        for (int i = 0; i < threadPool.length; i++) {
            threadPool[i] = new Thread(() -> {
                Runnable r;
                while (true) {
                    try {
                        synchronized (ThreadPool.this) {
                            while ((r = poll()) == null) {
                                wait();
                            }
                        }
                        r.run();
                    }  catch (RuntimeException e){
                        LogUtil.logError(e);
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

            });
            threadPool[i].start();
        }
    }

    public synchronized void queue(Runnable task) {
        tasks.add(task);
        notifyAll();
    }

    private synchronized Runnable poll() {
        if (tasks.isEmpty()) return null;
        return tasks.poll();
    }
}
