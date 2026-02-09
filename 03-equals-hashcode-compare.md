# 3. equals/hashCode/compareTo与集合行为

## 知识点详解

### 3.1 equals与hashCode契约
`equals` 的自反性、对称性、传递性和一致性是集合正确性的基础。`hashCode` 必须保证：如果两个对象 `equals` 为真，则 `hashCode` 必须相等，否则哈希集合会把“同一个逻辑对象”放到不同桶中导致查找失败。

实现时要基于不可变或稳定字段，避免使用可变字段参与 `hashCode`，否则对象放入 `HashMap` 后再修改字段会“消失”。可使用 `Objects.equals` 和 `Objects.hash`，但要注意性能开销。

### 3.2 compareTo与Comparator
`compareTo` 是自然排序规则，`Comparator` 是外部排序策略。排序需要满足自反性、对称性和传递性，否则会导致排序结果不稳定甚至抛出异常。通常要求 `compareTo(x) == 0` 与 `equals` 一致，否则 `TreeSet`、`TreeMap` 会把不同对象当成重复而覆盖。

当排序与 `equals` 不一致时，需要明确业务语义，例如排序只看部分字段，但 `equals` 看全部字段，此时不应使用 `TreeSet` 保存对象，或者需提供更一致的比较规则。

### 3.3 常见陷阱
只重写 `equals` 不重写 `hashCode` 会导致 `HashMap` 查找失败；`compareTo` 与 `equals` 不一致会导致 `SortedSet` 误判重复；`BigDecimal` 的 `equals` 与 `compareTo` 行为不同也常被忽略。

## 面试点与参考回答

### Q1 HashMap中equals与hashCode如何协作
`hashCode` 用于定位桶的位置，缩小查找范围；若桶内存在多个元素，`equals` 再做精确匹配。两者必须一致才能保证插入与查询逻辑正确。

### Q2 compareTo与equals冲突的后果
在 `TreeSet` / `TreeMap` 中，元素是否重复由比较器决定，若 `compareTo` 返回0但 `equals` 为false，会导致覆盖或丢失元素；反过来则会产生“逻辑重复但不被认为重复”的集合。

## 掌握评估

### 自测1 能写出正确的equals与hashCode实现
选择一个包含多个字段的类，正确处理空值、类型判断、对称性，并确保 `hashCode` 与 `equals` 使用一致字段。

### 自测2 能解释Set和Map中重复判断的依据
区分 `HashSet` 与 `TreeSet` 的重复判定逻辑，说明 `HashSet` 依赖 `hashCode+equals`，`TreeSet` 依赖比较结果。
