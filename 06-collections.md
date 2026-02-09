# 6. 集合框架

## 知识点详解

### 6.1 ArrayList
ArrayList基于动态数组实现，随机访问是O(1)，尾部追加摊销为O(1)。当容量不足时会扩容，一般按1.5倍增长。中间插入或删除需要移动元素，时间复杂度为O(n)，且扩容会带来一次性拷贝开销。

### 6.2 LinkedList
LinkedList是双向链表，适合在头尾频繁插入删除，时间复杂度接近O(1)。缺点是随机访问为O(n)，节点对象占用较多内存，并且链表结构对CPU缓存不友好。LinkedList同时实现了Deque，可用于队列或栈。

### 6.3 HashMap
HashMap使用“数组 + 链表/红黑树”结构。容量为2的幂以便快速定位桶位置；负载因子默认0.75，超过阈值会触发扩容。JDK8中当链表长度超过阈值会树化为红黑树以提升查询性能。

### 6.4 LinkedHashMap
LinkedHashMap在HashMap基础上维护一个双向链表，可以保持插入顺序或访问顺序。配合 `removeEldestEntry` 可以轻松实现LRU缓存。它在空间上稍有额外开销，但换来更稳定的迭代顺序。

### 6.5 TreeMap/TreeSet
TreeMap和TreeSet基于红黑树，元素按排序存储，查找、插入、删除都是O(log n)。排序可以来自自然顺序或自定义Comparator。需要注意排序规则必须满足一致性，否则会破坏集合行为。

### 6.6 Fail-Fast机制
大多数集合的迭代器在迭代过程中检测结构性修改（modCount），如果检测到并发修改会抛出 ConcurrentModificationException。这不是并发安全机制，而是一种“快速失败”的错误提示。

## 面试点与参考回答

### Q1 HashMap在JDK8中的结构变化
JDK8引入链表树化，桶内元素过多时转换为红黑树以提升查询效率；插入采用尾插法减少死循环风险；扩容时迁移逻辑更高效，避免JDK7的环形链表问题。

### Q2 ConcurrentModificationException的触发条件
当迭代过程中集合发生结构性修改，且修改不是通过迭代器自身的 `remove` 进行时，modCount不一致会触发异常。并发环境下即使没有异常也不能保证正确性，因此需要使用并发集合。

### Q3 HashMap与Hashtable、ConcurrentHashMap区别
Hashtable是早期同步容器，方法级同步、性能差且不允许null；HashMap非线程安全、允许null；ConcurrentHashMap在分段/桶级别加锁或CAS，提供更高并发性能，也不允许null。

## 掌握评估

### 自测1 能解释HashMap扩容触发条件与性能成本
说明扩容阈值计算方式，解释扩容时为何性能抖动，并能给出避免频繁扩容的做法（预估容量、调用 `ensureCapacity`）。

### 自测2 能手写一个简易LRU缓存
使用LinkedHashMap实现LRU，说明为什么选择访问顺序模式，并解释淘汰策略在高并发场景下的风险。
