import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrencyLesson {

    public static void main(String[] args) throws Exception {
        System.out.println("===== Lesson 1: ReentrantLock =====");
        demoReentrantLock();

        System.out.println("\n===== Lesson 2: Condition =====");
        demoCondition();

        System.out.println("\n===== Lesson 3: CAS / Atomic / LongAdder =====");
        demoAtomicAndCas();

        System.out.println("\n===== Lesson 4: CountDownLatch / CyclicBarrier / Semaphore / Phaser =====");
        demoTools();

        System.out.println("\nAll demos finished.");
    }

    // -------------------------
    // Lesson 1: ReentrantLock
    // -------------------------
    static void demoReentrantLock() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock(true); // true = fair lock
        AtomicInteger tickets = new AtomicInteger(5);

        Runnable seller = () -> {
            while (true) {
                lock.lock();
                try {
                    if (tickets.get() <= 0) {
                        return;
                    }
                    int left = tickets.getAndDecrement();
                    System.out.println(Thread.currentThread().getName() + " sold 1 ticket, left=" + (left - 1));
                } finally {
                    // 必须在 finally 解锁
                    lock.unlock();
                }

                sleepSilently(80);
            }
        };

        Thread t1 = new Thread(seller, "seller-A");
        Thread t2 = new Thread(seller, "seller-B");
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // tryLock + timeout 示例：避免无限等待
        ReentrantLock lock2 = new ReentrantLock();
        Thread holder = new Thread(() -> {
            lock2.lock();
            try {
                System.out.println("holder got lock2 for 2s");
                sleepSilently(2000);
            } finally {
                lock2.unlock();
            }
        });

        Thread waiter = new Thread(() -> {
            try {
                if (lock2.tryLock(500, TimeUnit.MILLISECONDS)) {
                    try {
                        System.out.println("waiter got lock2");
                    } finally {
                        lock2.unlock();
                    }
                } else {
                    System.out.println("waiter timeout, do fallback logic");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("waiter interrupted while waiting lock2");
            }
        });

        holder.start();
        sleepSilently(100);
        waiter.start();
        holder.join();
        waiter.join();
    }

    // -------------------------
    // Lesson 2: Condition
    // -------------------------
    static void demoCondition() throws InterruptedException {
        class BoundedBuffer {
            private final Queue<Integer> queue = new ArrayDeque<>();
            private final int capacity;
            private final ReentrantLock lock = new ReentrantLock();
            private final Condition notFull = lock.newCondition();
            private final Condition notEmpty = lock.newCondition();

            BoundedBuffer(int capacity) {
                this.capacity = capacity;
            }

            public void put(int v) throws InterruptedException {
                lock.lock();
                try {
                    while (queue.size() == capacity) {
                        notFull.await();
                    }
                    queue.offer(v);
                    System.out.println("produce " + v + ", size=" + queue.size());
                    notEmpty.signal();
                } finally {
                    lock.unlock();
                }
            }

            public int take() throws InterruptedException {
                lock.lock();
                try {
                    while (queue.isEmpty()) {
                        notEmpty.await();
                    }
                    int v = queue.poll();
                    System.out.println("consume " + v + ", size=" + queue.size());
                    notFull.signal();
                    return v;
                } finally {
                    lock.unlock();
                }
            }
        }

        BoundedBuffer buffer = new BoundedBuffer(2);

        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                try {
                    buffer.put(i);
                    sleepSilently(80);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "producer");

        Thread consumer = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                try {
                    buffer.take();
                    sleepSilently(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
    }

    // -------------------------
    // Lesson 3: CAS / Atomic / LongAdder
    // -------------------------
    static void demoAtomicAndCas() throws InterruptedException {
        // 1) AtomicInteger: 原子自增
        AtomicInteger atomicCounter = new AtomicInteger(0);
        runThreads(10, 1000, () -> atomicCounter.incrementAndGet());
        System.out.println("AtomicInteger result=" + atomicCounter.get() + " (expect 10000)");

        // 2) LongAdder: 高并发计数更友好
        LongAdder adder = new LongAdder();
        runThreads(10, 1000, adder::increment);
        System.out.println("LongAdder sum=" + adder.sum() + " (expect 10000)");

        // 3) ABA 与版本号（AtomicStampedReference）
        AtomicStampedReference<Integer> ref = new AtomicStampedReference<>(1, 0);

        Thread t1 = new Thread(() -> {
            int s1 = ref.getStamp();
            ref.compareAndSet(1, 2, s1, s1 + 1); // A->B
            int s2 = ref.getStamp();
            ref.compareAndSet(2, 1, s2, s2 + 1); // B->A
            System.out.println("t1 changed 1->2->1, stamp=" + ref.getStamp());
        });

        Thread t2 = new Thread(() -> {
            int oldStamp = ref.getStamp();
            sleepSilently(200);
            boolean ok = ref.compareAndSet(1, 9, oldStamp, oldStamp + 1);
            System.out.println("t2 CAS with old stamp success? " + ok + ", value=" + ref.getReference() + ", stamp=" + ref.getStamp());
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    // -------------------------
    // Lesson 4: Concurrency Utilities
    // -------------------------
    static void demoTools() throws Exception {
        demoCountDownLatch();
        demoCyclicBarrier();
        demoSemaphore();
        demoPhaser();
    }

    static void demoCountDownLatch() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        Runnable worker = () -> {
            sleepSilently(200);
            System.out.println(Thread.currentThread().getName() + " finished init");
            latch.countDown();
        };

        new Thread(worker, "init-1").start();
        new Thread(worker, "init-2").start();
        new Thread(worker, "init-3").start();

        latch.await();
        System.out.println("main: all init tasks done, continue startup");
    }

    static void demoCyclicBarrier() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(3, () -> System.out.println("barrier action: all arrived, go next round"));
        Runnable runner = () -> {
            for (int round = 1; round <= 2; round++) {
                sleepSilently(120);
                System.out.println(Thread.currentThread().getName() + " ready round " + round);
                try {
                    barrier.await();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        Thread r1 = new Thread(runner, "r1");
        Thread r2 = new Thread(runner, "r2");
        Thread r3 = new Thread(runner, "r3");
        r1.start();
        r2.start();
        r3.start();
        r1.join();
        r2.join();
        r3.join();
    }

    static void demoSemaphore() throws InterruptedException {
        Semaphore semaphore = new Semaphore(2); // 最多2个并发

        Runnable task = () -> {
            try {
                semaphore.acquire();
                System.out.println(Thread.currentThread().getName() + " acquired permit");
                sleepSilently(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                semaphore.release();
                System.out.println(Thread.currentThread().getName() + " released permit");
            }
        };

        Thread a = new Thread(task, "job-A");
        Thread b = new Thread(task, "job-B");
        Thread c = new Thread(task, "job-C");
        a.start();
        b.start();
        c.start();
        a.join();
        b.join();
        c.join();
    }

    static void demoPhaser() throws InterruptedException {
        Phaser phaser = new Phaser(1); // main 先注册

        Runnable student = () -> {
            phaser.register();
            System.out.println(Thread.currentThread().getName() + " phase-1: arrive classroom");
            phaser.arriveAndAwaitAdvance();

            System.out.println(Thread.currentThread().getName() + " phase-2: take exam");
            sleepSilently(150);
            phaser.arriveAndAwaitAdvance();

            System.out.println(Thread.currentThread().getName() + " phase-3: leave");
            phaser.arriveAndDeregister();
        };

        Thread s1 = new Thread(student, "s1");
        Thread s2 = new Thread(student, "s2");
        s1.start();
        s2.start();

        // main 也参与每个阶段
        phaser.arriveAndAwaitAdvance();
        System.out.println("teacher: all students arrived");

        phaser.arriveAndAwaitAdvance();
        System.out.println("teacher: exam finished");

        phaser.arriveAndDeregister();

        s1.join();
        s2.join();
    }

    // -------------------------
    // Helpers
    // -------------------------
    static void runThreads(int threadNum, int loop, Runnable action) throws InterruptedException {
        Thread[] ts = new Thread[threadNum];
        for (int i = 0; i < threadNum; i++) {
            ts[i] = new Thread(() -> {
                for (int j = 0; j < loop; j++) {
                    action.run();
                }
            });
            ts[i].start();
        }
        for (Thread t : ts) {
            t.join();
        }
    }

    static void sleepSilently(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
