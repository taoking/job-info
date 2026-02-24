import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SchedulingAndCompletableFutureDemo {

    public static void main(String[] args) throws Exception {
        // 1) 定时触发器：ScheduledExecutorService（实现类用 ScheduledThreadPoolExecutor）
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(
                1,
                namedFactory("scheduler-"),
                new ThreadPoolExecutor.AbortPolicy()
        );
        scheduler.setRemoveOnCancelPolicy(true);

        // 2) 业务线程池：显式参数 + 有界队列 + 拒绝策略
        ThreadPoolExecutor bizPool = new ThreadPoolExecutor(
                2,                             // corePoolSize
                4,                             // maximumPoolSize
                30, TimeUnit.SECONDS,          // keepAliveTime
                new ArrayBlockingQueue<>(100), // 有界队列
                namedFactory("biz-"),
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );

        try {
            demoScheduledTask(scheduler, bizPool);
            demoCompletableFutureOrchestration(bizPool);
        } finally {
            gracefulShutdown(scheduler, "scheduler");
            gracefulShutdown(bizPool, "bizPool");
        }
    }

    // 示例A：ScheduledExecutorService 定时任务
    static void demoScheduledTask(ScheduledExecutorService scheduler, ThreadPoolExecutor bizPool)
            throws InterruptedException {

        Runnable trigger = () -> {
            try {
                bizPool.execute(() -> {
                    try {
                        for (int i = 0; i < 3; i++) {
                            // 任务响应中断（方式1：主动检查）
                            if (Thread.currentThread().isInterrupted()) {
                                System.out.println("[scheduled-job] interrupted(flag)");
                                return;
                            }
                            TimeUnit.MILLISECONDS.sleep(200);
                        }
                        System.out.println("[scheduled-job] run on " + Thread.currentThread().getName());
                    } catch (InterruptedException e) {
                        // 任务响应中断（方式2：捕获 InterruptedException）
                        Thread.currentThread().interrupt();
                        System.out.println("[scheduled-job] interrupted(exception)");
                    }
                });
            } catch (RejectedExecutionException e) {
                System.err.println("[scheduled-job] rejected by bizPool: " + e.getMessage());
            }
        };

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                trigger, 0, 1, TimeUnit.SECONDS
        );

        TimeUnit.SECONDS.sleep(4);
        future.cancel(true);
    }

    // 示例B：CompletableFuture 多任务编排
    static void demoCompletableFutureOrchestration(ThreadPoolExecutor bizPool) {
        CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(() -> fetchUser("u1001"), bizPool);
        CompletableFuture<List<String>> orderFuture = CompletableFuture.supplyAsync(() -> fetchOrders("u1001"), bizPool);

        CompletableFuture<String> resultFuture = userFuture
                .thenCombineAsync(orderFuture,
                        (user, orders) -> "user=" + user + ", orders=" + orders,
                        bizPool)
                .orTimeout(2, TimeUnit.SECONDS)
                .exceptionally(ex -> "fallback: " + ex.getClass().getSimpleName());

        System.out.println("[completable] " + resultFuture.join());
    }

    static String fetchUser(String userId) {
        simulateWork(300);
        return "Tom(" + userId + ")";
    }

    static List<String> fetchOrders(String userId) {
        simulateWork(500);
        return List.of("O-100", "O-101");
    }

    static void simulateWork(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CancellationException("task interrupted");
        }
    }

    static ThreadFactory namedFactory(String prefix) {
        AtomicInteger idx = new AtomicInteger(1);
        return r -> {
            Thread t = new Thread(r, prefix + idx.getAndIncrement());
            t.setDaemon(false);
            return t;
        };
    }

    // 3) 优雅关闭线程池
    static void gracefulShutdown(ExecutorService pool, String poolName) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("[" + poolName + "] force shutdown...");
                pool.shutdownNow();
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("[" + poolName + "] still not terminated");
                }
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
