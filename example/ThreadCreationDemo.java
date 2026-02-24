import java.util.concurrent.*;

public class ThreadCreationDemo {

    public static void main(String[] args) throws Exception {
        byExtendingThread();
        byRunnable();
        byCallableWithFutureTask();
        byThreadPool();
        byFutureCancel();
    }

    // 1) 继承 Thread
    static void byExtendingThread() throws InterruptedException {
        class MyThread extends Thread {
            MyThread(String name) {
                super(name);
            }

            @Override
            public void run() {
                System.out.println("[Thread] " + getName() + " running");
            }
        }

        Thread t = new MyThread("t-extend-thread");
        t.start();
        t.join();
    }

    // 2) 实现 Runnable（任务与线程解耦）
    static void byRunnable() throws InterruptedException {
        Runnable task = () -> System.out.println("[Runnable] " + Thread.currentThread().getName() + " running");

        Thread t = new Thread(task, "t-runnable");
        t.start();
        t.join();
    }

    // 3) Callable + FutureTask（可返回结果、可抛检查异常）
    static void byCallableWithFutureTask() throws Exception {
        Callable<Integer> callable = () -> {
            TimeUnit.MILLISECONDS.sleep(200);
            return 42;
        };

        FutureTask<Integer> futureTask = new FutureTask<>(callable);
        Thread t = new Thread(futureTask, "t-callable");
        t.start();

        Integer result = futureTask.get();
        System.out.println("[Callable+FutureTask] result = " + result);
    }

    // 4) 线程池 + Future（工程里最常用）
    static void byThreadPool() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // Runnable: submit 后也会返回 Future<?>，get() 返回 null
            Future<?> f1 = pool.submit(() ->
                    System.out.println("[Pool-Runnable] " + Thread.currentThread().getName() + " running")
            );

            // Callable: submit 后 Future<T> 可拿到结果
            Future<String> f2 = pool.submit(() -> {
                TimeUnit.MILLISECONDS.sleep(100);
                return "OK-from-Callable";
            });

            f1.get();
            System.out.println("[Pool-Callable] result = " + f2.get());
        } finally {
            pool.shutdown();
        }
    }

    // 5) Future 取消任务（cancel）
    static void byFutureCancel() throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> future = pool.submit(() -> {
                try {
                    for (int i = 1; i <= 10; i++) {
                        TimeUnit.SECONDS.sleep(1);
                        System.out.println("[CancelableTask] step " + i);
                    }
                    return 1;
                } catch (InterruptedException e) {
                    // 收到中断后及时结束，并恢复中断标记
                    Thread.currentThread().interrupt();
                    System.out.println("[CancelableTask] interrupted");
                    return -1;
                }
            });

            TimeUnit.SECONDS.sleep(2);
            boolean canceled = future.cancel(true); // true: 尝试中断执行线程

            System.out.println("[Future-cancel] canceled = " + canceled
                    + ", isDone = " + future.isDone()
                    + ", isCancelled = " + future.isCancelled());
        } finally {
            pool.shutdownNow();
        }
    }
}
