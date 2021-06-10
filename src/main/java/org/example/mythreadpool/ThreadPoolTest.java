package org.example.mythreadpool;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author 向前走不回头
 * @date 2021/6/10
 */
@Slf4j(topic = "ThreadPoolTest")
public class ThreadPoolTest {
    public static void main(String[] args) {
        ThreadPool threadPool = new ThreadPool(1, 1, TimeUnit.SECONDS, 1,new ThreadPool.Discard());
        for (int i = 0; i < 4; i++) {
            int j = i;
            threadPool.execute(()->{
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.debug("{}",j);
            });
        }

    }
}
