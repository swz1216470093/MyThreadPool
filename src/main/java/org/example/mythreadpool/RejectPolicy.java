package org.example.mythreadpool;

/**
 * @author 向前走不回头
 * @date 2021/6/9
 */
@FunctionalInterface
public interface RejectPolicy<T>{
    /**
     * 拒绝策略
     * @param queue 队列
     * @param task 任务
     */
    void reject(BlockingQueue<T> queue, T task);
}
