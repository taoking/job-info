# 7. 并发基础与JMM

## 知识点详解

### 7.1 线程创建方式
Thread继承方式适合简单场景，但耦合高；Runnable适合任务与线程分离；Callable可返回结果并抛出检查异常；Future用于获取执行结果和取消任务。实际工程中常通过线程池提交Callable或Runnable，避免频繁创建线程。

### 7.2 synchronized锁升级
synchronized基于对象监视器实现，会经历无锁、偏向锁、轻量级锁、重量级锁的升级过程，以降低无竞争时的开销。竞争激烈时会膨胀为重量级锁，线程阻塞带来上下文切换成本。不同JDK版本对偏向锁策略有所调整，但总体目标是降低锁开销。

### 7.3 volatile语义
volatile保证写入对其他线程可见，并禁止特定重排序，从而保证有序性。但它不保证复合操作原子性，例如 `i++` 仍可能丢失更新。适合做状态标记、配置刷新等轻量场景，不适合计数累加。

### 7.4 JMM与happens-before
JMM定义了线程之间可见性的规则，happens-before描述操作的先后约束。典型规则包括程序次序、监视器锁释放与获取、volatile写与读、线程启动与终止等。理解这些规则有助于定位重排序导致的并发Bug。

### 7.5 线程状态与生命周期
线程状态包括NEW、RUNNABLE、BLOCKED、WAITING、TIMED_WAITING、TERMINATED。RUNNABLE并不代表正在占用CPU，只表示可被调度。理解状态转换可以帮助快速定位死锁、饥饿或阻塞问题。

## 面试点与参考回答

### Q1 volatile能保证哪些性质
volatile保证可见性和有序性，通过内存屏障禁止重排序；不保证原子性，复合操作仍需加锁或使用原子类。

### Q2 synchronized与Lock的区别
synchronized是JVM层面锁，语法简单、自动释放，但不可中断、不可超时、仅支持单一条件队列；Lock提供可中断、可超时、公平锁、多Condition等能力，但需要手动释放。

### Q3 happens-before的典型例子
volatile写-读、解锁-加锁、线程start与join、程序顺序规则都是典型例子。可以举双重检查锁中“volatile写保证构造完成后再发布”的例子。

## 掌握评估

### 自测1 能解释指令重排导致的错误
构造一个“发布未初始化对象”的示例，说明如果没有volatile或锁，可能出现读到半初始化对象的情况，并解释重排序原因。

### 自测2 能写出双重检查锁的正确写法
写出带 `volatile` 的DCL单例，并解释为什么必须加volatile以防止指令重排。

```java
class Singleton {
    private static volatile Singleton INSTANCE;
    static Singleton get() {
        if (INSTANCE == null) {
            synchronized (Singleton.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Singleton();
                }
            }
        }
        return INSTANCE;
    }
}
```
