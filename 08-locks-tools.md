# 8. Lock体系与并发工具

## 知识点详解

### 8.1 ReentrantLock
ReentrantLock是可重入显式锁，支持公平/非公平策略、可中断、可超时与 `tryLock`。它能提供比synchronized更灵活的控制，但必须在 `finally` 中释放锁以避免死锁。

### 8.2 Condition
Condition是与Lock配套的条件队列，可实现多个等待队列，适合复杂的生产者-消费者场景。与 `wait/notify` 相比，Condition更灵活、语义更清晰，但必须在持有锁的情况下调用 `await/signal`。

### 8.3 原子类与CAS
CAS是乐观并发原语，通过比较并交换实现无锁更新，但在高竞争下可能自旋过多浪费CPU，并存在ABA问题。ABA可通过版本号解决，例如 `AtomicStampedReference`。LongAdder使用分段累加减少热点竞争，适合高并发计数。

### 8.4 并发工具类
CountDownLatch用于等待一组事件完成，不能复用；CyclicBarrier用于一组线程相互等待，可以复用；Semaphore控制并发许可数量，常用于限流；Phaser支持多阶段任务协作，适合复杂同步场景。

## 面试点与参考回答

### Q1 CAS的优缺点与ABA问题解决方案
优点是无锁、低延迟；缺点是高竞争下自旋浪费CPU，并有ABA问题。解决ABA可以使用带版本号的原子引用或使用不可变对象设计。

### Q2 CountDownLatch与CyclicBarrier区别
CountDownLatch是一次性倒计时，适合“等待若干任务完成”；CyclicBarrier是可复用的屏障，适合“多线程阶段性对齐”。

## 掌握评估

### 自测1 能给出使用场景与代码示例
分别写出 CountDownLatch 用于等待多个子任务完成、CyclicBarrier 用于分阶段批处理的示例，说明为何选择它们。

### 自测2 能解释锁竞争下性能差异
比较synchronized与ReentrantLock在低竞争与高竞争下的性能表现，说明自旋、阻塞、上下文切换对延迟的影响。
