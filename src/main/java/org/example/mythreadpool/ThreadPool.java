package org.example.mythreadpool;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author 向前走不回头
 * @date 2021/6/9
 */
@Slf4j(topic = "ThreadPool")
public class ThreadPool {
    /**
     * 任务队列
     */
    private final BlockingQueue<Runnable> blockingQueue;
    /**
     * 线程集合
     */
    private final HashSet<Worker> workers;

    /**
     * 核心线程数
     */
    private final int coreSize;

    /**
     * 获取任务超时时间
     */
    private final long timeout;
    /**
     * 时间单位
     */
    private final TimeUnit timeUnit;

    /**
     * 拒绝策略
     */
    private final RejectPolicy<Runnable> rejectPolicy;

    public ThreadPool(int coreSize, long timeout, TimeUnit timeUnit,int queueCapacity,RejectPolicy<Runnable> rejectPolicy) {
        this.coreSize = coreSize;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.rejectPolicy = rejectPolicy;
        workers = new HashSet<>();
        blockingQueue = new BlockingQueue<>(queueCapacity);
    }

    /**
     * 线程包装类
     */
    private final class Worker extends Thread {
        Runnable task;

        public Worker(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
//            执行任务
//            task不为空 执行任务
//            task执行完毕  从任务队列获取任务并执行
//            while (task != null || (task = blockingQueue.take()) != null){
            while (task != null || (task = blockingQueue.poll(timeout,timeUnit)) != null){
                try {
                    log.debug("正在执行{}", task);
                    task.run();
                }finally {
                    task = null;
                }
            }
            synchronized (workers){
                log.debug("移除worker{}",this);
                workers.remove(this);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Worker worker = (Worker) o;
            return Objects.equals(task, worker.task);
        }

        @Override
        public int hashCode() {
            return Objects.hash(task);
        }
    }

    /**
     * 一直等待
     */
    public static class Wait implements RejectPolicy<Runnable>{
        @Override
        public void reject(BlockingQueue<Runnable> queue, Runnable task) {
            queue.put(task);
        }
    }

    /**
     * 超时等待
     */
    public static class WaitTimeout implements RejectPolicy<Runnable> {
        private final int timeout;
        private final TimeUnit timeUnit;
        public WaitTimeout(int timeout, TimeUnit timeUnit) {
            this.timeout = timeout;
            this.timeUnit = timeUnit;
        }
        @Override
        public void reject(BlockingQueue<Runnable> queue, Runnable task) {
            queue.offer(task,timeout,timeUnit);
        }
    }
    /**
     * 放弃任务
     */
    public static class Discard implements RejectPolicy<Runnable>{

        @Override
        public void reject(BlockingQueue<Runnable> queue, Runnable task) {
//            什么也不做
        }
    }
    /**
     * 调用者自己执行
     */
    public static class CallerRun implements RejectPolicy<Runnable>{

        @Override
        public void reject(BlockingQueue<Runnable> queue, Runnable task) {
            task.run();
        }
    }
    /**
     * 抛出异常
     */
    public static class Abort implements RejectPolicy<Runnable>{

        @Override
        public void reject(BlockingQueue<Runnable> queue, Runnable task) {
            throw new RuntimeException("Task " + task.toString() +
                    " rejected from " +
                    this);
        }
    }
    /**
     * 执行任务
     */
    public void execute(Runnable task){
        //当任务数没有超过核心线程数时，直接交给worker对象执行
        synchronized (workers){
            if (workers.size() < coreSize){
                log.debug("新增 worker{}, task{}", workers,task);
                Worker worker = new Worker(task);
                workers.add(worker);
                worker.start();
            }else {
                log.debug("核心线程数已满");
//        如果任务数超过核心线程数，放入阻塞队列中暂存
                blockingQueue.tryPut(rejectPolicy,task);

            }
        }
    }

    @Override
    public String toString() {
        return "ThreadPool{}";
    }
}
