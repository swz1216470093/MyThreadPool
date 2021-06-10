package org.example.mythreadpool;

import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
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
    private int coreSize;

    /**
     * 获取任务超时时间
     */
    private long timeout;
    /**
     * 时间单位
     */
    private TimeUnit timeUnit;

    /**
     * 拒绝策略
     */
    private final RejectPolicy rejectPolicy;

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
                    log.debug("正在执行{}",task);
                    task.run();
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    task = null;
                }
            }
            synchronized (workers){
                log.debug("移除worker{}",this);
                workers.remove(this);
            }
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
//                log.debug("放入任务队列暂存 {}",task);
//                blockingQueue.put(task);
//                死等
//                带超时的等待
//                放弃任务
//                抛出异常
//                让调用者自己执行
                blockingQueue.tryPut(rejectPolicy,task);

            }
        }
    }


}
