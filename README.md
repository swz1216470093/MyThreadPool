# 一个简单线程池

### 简述：

​	当任务数小于核心线程数时，由worker对象执行，当任务数超过核心线程数 放入阻塞队列中，如果阻塞队列已满 执行拒绝策略

### 类描述：

1. ThreadPool 线程池类 定义一个线程池 

   核心方法：

   ```java
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
   //                等待
   //                blockingQueue.put(task);
   //                带超时的等待
   //                blockingQueue.offer(task,timeout,timeunit);
   //                放弃任务
   //                什么也不做
   //                抛出异常
   //                throw new RuntimeException();  
   //                让调用者自己执行
   //                task.run();
                   blockingQueue.tryPut(rejectPolicy,task);
   
               }
           }
       }
   ```

2. Worker 线程包装类  负责执行任务

   核心方法：

   ```java
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
   ```

   3.BlockingQueue 阻塞队列  使用ReentrantLock实现的阻塞队列

   ```java
   /**
        * 阻塞获取
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
   ```

   
