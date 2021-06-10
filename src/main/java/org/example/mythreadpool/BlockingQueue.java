package org.example.mythreadpool;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author 向前走不回头
 * @date 2021/6/9
 */
@Slf4j(topic = "BlockingQueue")
public class BlockingQueue<T> {
    /**
     * 任务队列
     */
    private final Deque<T> queue = new ArrayDeque<>();
    /**
     * 锁
     */
    private final ReentrantLock lock = new ReentrantLock();
    /**
     * 生产者条件变量
     */
    private final Condition fullWaitSet = lock.newCondition();
    /**
     * 消费者条件变量
     */
    private final Condition emptyWaitSet = lock.newCondition();
    /**
     * 容量
     */
    private final int capacity;

    public BlockingQueue(int capacity) {
        this.capacity = capacity;
    }

    /**
     * 阻塞获取
     *
     * @return 消费的元素
     */
    public T take() {
        T result = null;
        lock.lock();
        try {
            try {
                while (queue.isEmpty()) {
                    emptyWaitSet.await();
                }
                result = queue.removeFirst();
                fullWaitSet.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } finally {
            lock.unlock();
        }
        return result;
    }

    /**
     * 阻塞添加
     *
     * @param element 生产的元素
     */
    public void put(T element) {
        lock.lock();
        try {
            try {
                while (queue.size() == capacity) {
                    log.debug("等待加入任务队列{}",element);
                    fullWaitSet.await();
                }
                queue.addLast(element);
                emptyWaitSet.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 带超时等待的取队列元素
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return
     */
    public T poll(long timeout, TimeUnit unit) {
        T result = null;
        lock.lock();
        try {
            try {
                long nanosTimeout = unit.toNanos(timeout);
                while (queue.isEmpty()) {
//                    使用awaitNanos解决虚假唤醒问题
//         awaitNanos会返回nanosTimeout值减去等待从该方法返回所花费的时间的估计值
//         一个正值可以用作随后调用此方法以完成等待所需时间的参数。 小于或等于零的值表示没有剩余时间
                    if (nanosTimeout <= 0) {
                        return null;
                    }
                    nanosTimeout = emptyWaitSet.awaitNanos(nanosTimeout);
                }
                result = queue.removeFirst();
                fullWaitSet.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            lock.unlock();
        }
        return result;
    }

    /**
     * 带超时的阻塞添加
     * @param element
     */
    public boolean offer(T element,long timeout,TimeUnit timeUnit){
        lock.lock();
        try {
            try {
                long nanosTimeout = timeUnit.toNanos(timeout);
                while (queue.size() == capacity){
                    if (nanosTimeout <= 0){
                        return false;
                    }
                    log.debug("等待加入任务队列,{}",element);
                    nanosTimeout = fullWaitSet.awaitNanos(nanosTimeout);
                }
                log.debug("加入任务队列,{}",element);
                queue.addLast(element);
                emptyWaitSet.signalAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }finally {
            lock.unlock();
        }
        return true;
    }

    /**
     * 使用拒绝策略添加
     * @param rejectPolicy
     * @param task
     */
    public void tryPut(RejectPolicy<T> rejectPolicy, T task) {
        lock.lock();
        try {
            if (queue.size() == capacity){
                rejectPolicy.reject(this,task);
            }else {
                queue.addLast(task);
                emptyWaitSet.signalAll();
            }
        }finally {
            lock.unlock();
        }
    }
}
