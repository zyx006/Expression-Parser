# 表达式解析器 | Expression-Parser

[中文](#中文) | [English](#english)

---

## 中文

### 概述
表达式解析器是一个基于**递归下降解析**技术的 Java 项目，支持标量、向量、矩阵的数学表达式解析与计算。项目还提供了一个交互式 REPL（Read-Eval-Print Loop）终端，方便用户实时输入和计算表达式。

### 实现原理

本项目采用经典的编译器前端架构，将表达式解析分为四个阶段：

```
输入: "3 + 4 * 2"

阶段1: 词法分析 (Lexer)              阶段2: 语法分析 (Parser)
┌───────────────────┐                ┌───────────────────────────────────────┐
│ NUMBER(3)         │                │ expr                                  │
│ PLUS(+)           │                │ └── addExpr                           │
│ NUMBER(4)         │ ────────────▶ │     ├── term                          │
│ MULTIPLY(*)       │   Token序列    │     │   └── unary                     │
│ NUMBER(2)         │                │     │       └── power                 │
│ EOF               │                │     │           └── implicitMul       │
└───────────────────┘                │     │               └── postfix       │
                                     │     │                   └── factor    │
                                     │     │                       └── 3     │
                                     │     ├── PLUS                          │
                                     │     └── term                          │
                                     │         ├── unary                     │
                                     │         │   └── power                 │
                                     │         │       └── implicitMul       │
                                     │         │               └── postfix   │
                                     │         │                   └── factor│
                                     │         │                       └── 4 │
                                     │         ├── MULTIPLY                  │
                                     │         └── unary                     │
                                     │             └── power                 │
                                     │                 └── implicitMul       │
                                     │                         └── postfix   │
                                     │                             └── factor│
                                     │                                 └── 2 │
                                     └───────────────────────────────────────┘

阶段3: 构建 AST                        阶段4: 表达式求值 (Eval)
┌───────────────────┐                ┌─────────────────────────────────────┐
│ BinaryOpNode(+)   │                │ BinaryOpNode(+)                     │
│    /        \     │                │   left = 3.eval() = 3               │
│ Number    BinaryOp│ ────────────▶ │   right = *.eval()                  │
│ Node(3)   Node(*) │                │     left = 4.eval() = 4             │
│          /      \ │                │     right = 2.eval() = 2            │
│    Number   Number│                │     return 4 * 2 = 8                │
│    Node(4) Node(2)│                │   return 3 + 8 = 11                 │
└───────────────────┘                └─────────────────────────────────────┘
```

> **透传说明**：在 `3 + 4 * 2` 例子中，`power`、`implicitMul`、`postfix` 层级均无对应运算符，直接透传给下一层：
> - `power` 无 `^` → 透传给 `implicitMul`
> - `implicitMul` 无隐式乘法（如 `2x`）→ 透传给 `postfix`
> - `postfix` 无阶乘 `!` → 透传给 `factor`
> - `factor` 识别数字 → 返回 `NumberNode`

#### 1. 词法分析 (Lexical Analysis)

词法分析器（[Lexer](src/main/java/cn/czyx007/expression_parser/lexer/Lexer.java)）将输入字符串分割成一个个**Token**（词法单元）：

| Token 类型 | 示例 | 说明 |
|-----------|------|------|
| `NUMBER` | `3.14`, `1e-5` | 数字（支持浮点数、科学计数法） |
| `IDENTIFIER` | `sin`, `PI`, `x` | 标识符（函数名、常量、变量） |
| `PLUS/MINUS/MULTIPLY/DIVIDE` | `+`, `-`, `*`, `/` | 四则运算符 |
| `POWER` | `^` | 幂运算符 |
| `LPAREN/RPAREN` | `(`, `)` | 括号 |

**示例**：输入 `"3 + 4 * 2"` 被分割为：`[NUMBER:3] [PLUS] [NUMBER:4] [MULTIPLY] [NUMBER:2] [EOF]`

#### 2. 语法分析 (Syntax Analysis)

语法分析器（[Parser](src/main/java/cn/czyx007/expression_parser/parser/Parser.java)）使用**递归下降**技术，根据运算符优先级递归解析表达式。

**优先级从高到低**：

| 优先级 | 语法规则 | 说明 |
|-------|---------|------|
| 高 | `factor` | 数字、标识符、括号表达式、数组 |
| ↑ | `postfix` | 阶乘（如 `5!`）|
| ↑ | `implicitMul` | 隐式乘法（如 `2x`、`3(4+5)`）|
| ↑ | `power` | 幂运算（右结合，如 `2^3^2 = 2^(3^2)`）|
| ↑ | `unary` | 一元正负号（如 `-3^2 = -(3^2)`）|
| ↑ | `term` | 乘、除、取模 |
| ↑ | `addExpr` | 加、减 |
| 低 | `expr` | 赋值表达式 |

**语法规则**（BNF 范式：`→` 表示定义为，`|` 表示或，`(...)` 表示分组，`*` 表示零次或多次，`?` 表示零次或一次）：

```
expr        → IDENTIFIER ASSIGN expr | addExpr
addExpr     → term ((PLUS | MINUS) term)*
term        → unary ((MULTIPLY | DIVIDE | MODULO) unary)*
unary       → (PLUS | MINUS) unary | power
power       → implicitMul (POWER power)?
implicitMul → postfix (postfix)*
postfix     → factor FACTORIAL?
factor      → NUMBER | IDENTIFIER | LPAREN expr RPAREN | LBRACKET ... RBRACKET
```

#### 3. 抽象语法树 (AST)

解析完成后生成**抽象语法树**（[ExprNode](src/main/java/cn/czyx007/expression_parser/ast/ExprNode.java)），树节点类型包括：

| 节点类型 | 说明 | 示例 |
|---------|------|------|
| `NumberNode` | 数字字面量 | `3.14` |
| `VariableNode` | 变量引用 | `x` |
| `BinaryOpNode` | 二元运算 | `a + b`, `x * y` |
| `UnaryOpNode` | 一元运算 | `-x`, `+5` |
| `FunctionNode` | 函数调用 | `sin(PI/2)` |
| `AssignNode` | 变量赋值 | `x = 10` |
| `ArrayNode` | 数组字面量 | `[1, 2, 3]` |

**AST 示例**：表达式 `3 + 4 * 2` 的语法树：

```
       (+)
      /   \
    (3)   (*)
         /   \
       (4)   (2)
```

#### 4. 表达式求值 (Evaluation)

每个 AST 节点实现 `eval()` 方法，通过**递归调用**子节点的 `eval()` 完成求值。

以 `BinaryOpNode` 为例（[源码](src/main/java/cn/czyx007/expression_parser/ast/BinaryOpNode.java)）：

```java
@Override
public double eval(Map<String, Double> context) {
    // 1. 递归求值左子树
    double leftVal = left.eval(context);
    // 2. 递归求值右子树
    double rightVal = right.eval(context);
    // 3. 应用运算符
    switch (op.type()) {
        case PLUS:  return leftVal + rightVal;
        case MINUS: return leftVal - rightVal;
        case MULTIPLY: return leftVal * rightVal;
        case DIVIDE:   return leftVal / rightVal;
        case POWER:    return Math.pow(leftVal, rightVal);
        // ...
    }
}
```

`NumberNode` 直接返回存储的数值（[源码](src/main/java/cn/czyx007/expression_parser/ast/NumberNode.java)）：

```java
@Override
public double eval(Map<String, Double> context) {
    return value;
}
```

**求值过程**：对于 `3 + 4 * 2`
1. `BinaryOpNode(+).eval()` → 需要左值和右值
2. 左值：`NumberNode(3).eval()` → `3`
3. 右值：`BinaryOpNode(*).eval()` → 需要左值和右值
4.   左值：`NumberNode(4).eval()` → `4`
5.   右值：`NumberNode(2).eval()` → `2`
6.   结果：`4 * 2 = 8`
7. 最终结果：`3 + 8 = 11`

---

### 功能特点

| 特性 | 说明 |
|------|------|
| **数学运算** | 加减乘除、幂运算、取模、阶乘 |
| **常量** | `PI`（圆周率）、`E`（自然常数） |
| **函数** | 三角函数、对数函数、统计函数等 |
| **变量** | 支持变量赋值和持久化 |
| **数组** | 数组字面量和数组操作 |
| **矩阵** | 转置、行列式、矩阵乘法等 |
| **REPL** | 交互式命令行，支持历史结果引用 |

### 快速开始

#### 环境要求
- **构建**: JDK 11 或更高版本（JDK 8 的 javadoc 不支持 `--release` 参数）
- **运行**: Java 8 或更高版本
- Maven（用于构建项目）

#### 构建项目
在项目根目录下运行 Maven 构建命令：
```bash
mvn clean package
```
构建完成后，会在 `target/` 目录下生成可执行的 JAR 文件。

#### 启动 REPL
构建成功后，运行以下命令启动交互式 REPL：
```bash
java -jar target/Expression-Parser-1.3.1.jar
```
启动后将显示欢迎界面，输入 `help` 可查看支持的完整功能列表。

#### 在项目中使用
除了 REPL 交互模式，你也可以将本库作为依赖集成到自己的 Java 项目中。

**Maven 依赖：**
```xml
<dependency>
    <groupId>cn.czyx007</groupId>
    <artifactId>Expression-Parser</artifactId>
    <version>1.3.1</version>
</dependency>
```

**Gradle 依赖：**
```groovy
implementation 'cn.czyx007:Expression-Parser:1.3.1'
```

**使用示例：**
```java
import cn.czyx007.expression_parser.api.ExpressionEvaluator;
import cn.czyx007.expression_parser.ast.Value;

// 简单计算
Value result = ExpressionEvaluator.eval("1 + 2 * 3");
System.out.println(result); // 输出: 7

// 使用变量上下文（表达式内赋值，变量可复用）
Map<String, Object> context = new HashMap<>();
Value result1 = ExpressionEvaluator.eval("x = 10; y = 20; x + y", context);
System.out.println(result1); // 输出: 30

// 复用 context 中的变量继续计算
Value result2 = ExpressionEvaluator.eval("x * y", context);
System.out.println(result2); // 输出: 200
```

### REPL 支持的功能

REPL（Read-Eval-Print Loop）支持以下功能，方便用户实时输入和计算表达式：

#### 基本运算
| 运算符 | 说明 |
|--------|------|
| `+`, `-`, `*`, `/` | 加减乘除 |
| `%` | 取模 |
| `^` | 幂运算（右结合，如 `2^3^2 = 512`） |
| `!` | 阶乘（如 `5! = 120`） |

#### 常量
| 常量 | 说明 |
|------|------|
| `PI` | 圆周率 π ≈ 3.14159... |
| `E` | 自然常数 e ≈ 2.71828... |

#### 数学函数

##### 三角函数与反三角函数
`sin`, `cos`, `tan`, `asin`, `acos`, `atan`

##### 双曲函数
`sinh`, `cosh`, `tanh`

##### 指数与对数
`exp`, `ln`, `log`, `log10`

##### 根号与绝对值
`sqrt`, `cbrt`, `abs`

##### 取整与符号函数
`ceil`, `floor`, `round`, `signum`, `sign`

##### 角度转换与其他
`degrees`, `radians`, `atan2(y,x)`, `hypot(x,y)`, `pow(base,exponent)`

#### 统计函数（向量操作）

对于向量 `X = [...]; Y = [...];`（也可手动按序展开输入）：

##### 基础统计
| 函数 | 说明 | 参数要求 |
|------|------|----------|
| `max(X)`, `min(X)` | 极值 | 2+ 个 |
| `sum(X)` | 求和 | 1+ 个 |
| `count(X)` | 参数计数 | - |
| `avg(X)` | 平均值 | 1+ 个 |
| `median(X)` | 中位数 | 1+ 个 |
| `prod(X)`, `product(X)` | 乘积 | 1+ 个 |

##### 方差与标准差
| 函数 | 说明 | 参数要求 |
|------|------|----------|
| `var(X)`, `variance(X)` | 样本方差 | ≥2 |
| `std(X)`, `stddev(X)` | 样本标准差 | ≥2 |
| `varp(X)`, `variancep(X)` | 总体方差 | ≥1 |
| `stdp(X)`, `stddevp(X)` | 总体标准差 | ≥1 |

##### 分布与距离统计
| 函数 | 说明 | 参数要求 |
|------|------|----------|
| `gcd(X)`, `lcm(X)` | 最大公约数/最小公倍数 | 整数，2+ 个 |
| `range(X)` | 极差 | 1+ 个 |
| `geomean(X)` | 几何平均数 | 1+ 个正数 |
| `norm1(X)`, `sumabs(X)` | 绝对值和 (L1) | - |
| `norm2(X)`, `rms(X)` | 欧几里得范数 (L2)/均方根 | - |
| `percentile(p,X)`, `pctl(p,X)` | 百分位数 | p ∈ [0,100] |

##### 协方差与相关
| 函数 | 说明 |
|------|------|
| `cov(X,Y)`, `covariance(X,Y)` | 样本协方差 |
| `covp(X,Y)`, `covariancep(X,Y)` | 总体协方差 |
| `corr(X,Y)`, `correlation(X,Y)` | 相关系数 |

#### 向量与矩阵操作

##### 向量操作
| 函数 | 说明 |
|------|------|
| `dot(X,Y)`, `dotprod(X,Y)` | 向量点积 |
| `dist(X,Y)`, `distance(X,Y)`, `euclidean(X,Y)` | 欧几里得距离 |
| `manhattan(X,Y)`, `taxicab(X,Y)` | 曼哈顿距离 |

##### 矩阵操作
| 函数 | 说明 |
|------|------|
| `transpose(matrix)`, `t(matrix)` | 矩阵/向量转置 |
| `det(matrix)`, `determinant(matrix)` | 行列式 |
| `matmul(matrix_A, matrix_B)` | 矩阵乘法 |
| `trace(matrix)` | 矩阵的迹（主对角线之和） |
| `rank(matrix)` | 矩阵的秩 |
| `mean(matrix, axis)` | 矩阵均值（axis=0 列，axis=1 行） |
| `inv(matrix)` | 矩阵求逆 |
| `solve(A, b)` | 解线性方程组 Ax=b |

##### 组合数学
| 函数 | 说明 |
|------|------|
| `C(n,k)`, `comb(n,k)` | 组合数 |
| `P(n,k)`, `perm(n,k)` | 排列数 |

#### 变量与数组

##### 变量赋值
- `x = 10` - 赋值
- `x = 10; y = 2x` - 多语句（分号分隔）
- `ans` - 上一次计算结果

##### 数组
- `[1, 2, 3]` - 数组字面量
- `[[1,2], [3,4]]` - 二维数组（矩阵）
- `scores = [1, 2, 3]` - 数组变量赋值
- `avg(scores)` - 数组作为函数参数

#### 隐式乘法
- `2PI` - 数字与常量
- `3(4+5)` - 数字与括号
- `2sqrt(4)` - 数字与函数

#### REPL 命令
| 命令 | 说明 |
|------|------|
| `help`, `?` | 显示帮助 |
| `vars` | 显示已定义的变量 |
| `clear` | 清除所有变量 |
| `exit`, `quit`, `q` | 退出 |

### 测试
项目包含全面的单元测试。运行以下命令执行测试：
```bash
mvn test
```

### 项目结构
- **src/main/java**：包含主代码，包括词法分析器、语法分析器和 REPL。
- **src/test/java**：包含各组件的单元测试。
- **target/**：生成的文件和编译后的类。

### 许可证
此项目基于 MIT 许可证开源。详情请参阅 [LICENSE](LICENSE) 文件。

---

## English

### Overview
Expression Parser is a Java project based on **recursive descent parsing** technology, supporting mathematical expression parsing and calculation for scalars, vectors, and matrices. The project also provides an interactive REPL (Read-Eval-Print Loop) terminal for users to input and calculate expressions in real-time.

### Implementation Principles

This project adopts a classic compiler frontend architecture, dividing expression parsing into four stages:

```
Input: "3 + 4 * 2"

Stage 1: Lexical Analysis (Lexer)    Stage 2: Syntax Analysis (Parser)
┌───────────────────┐                ┌───────────────────────────────────────┐
│ NUMBER(3)         │                │ expr                                  │
│ PLUS(+)           │                │ └── addExpr                           │
│ NUMBER(4)         │ ────────────▶ │     ├── term                          │
│ MULTIPLY(*)       │   Token Seq    │     │   └── unary                     │
│ NUMBER(2)         │                │     │       └── power                 │
│ EOF               │                │     │           └── implicitMul       │
└───────────────────┘                │     │               └── postfix       │
                                     │     │                   └── factor    │
                                     │     │                       └── 3     │
                                     │     ├── PLUS                          │
                                     │     └── term                          │
                                     │         ├── unary                     │
                                     │         │   └── power                 │
                                     │         │       └── implicitMul       │
                                     │         │               └── postfix   │
                                     │         │                   └── factor│
                                     │         │                       └── 4 │
                                     │         ├── MULTIPLY                  │
                                     │         └── unary                     │
                                     │             └── power                 │
                                     │                 └── implicitMul       │
                                     │                         └── postfix   │
                                     │                             └── factor│
                                     │                                 └── 2 │
                                     └───────────────────────────────────────┘

Stage 3: Build AST                     Stage 4: Expression Evaluation (Eval)
┌───────────────────┐                ┌─────────────────────────────────────┐
│ BinaryOpNode(+)   │                │ BinaryOpNode(+)                     │
│    /        \     │                │   left = 3.eval() = 3               │
│ Number    BinaryOp│ ────────────▶ │   right = *.eval()                  │
│ Node(3)   Node(*) │                │     left = 4.eval() = 4             │
│          /      \ │                │     right = 2.eval() = 2            │
│    Number   Number│                │     return 4 * 2 = 8                │
│    Node(4) Node(2)│                │   return 3 + 8 = 11                 │
└───────────────────┘                └─────────────────────────────────────┘
```

> **Pass-through Note**: In the `3 + 4 * 2` example, the `power`, `implicitMul`, and `postfix` levels have no corresponding operators and directly pass through to the next level:
> - `power` has no `^` → passes to `implicitMul`
> - `implicitMul` has no implicit multiplication (e.g., `2x`) → passes to `postfix`
> - `postfix` has no factorial `!` → passes to `factor`
> - `factor` recognizes the number → returns `NumberNode`

#### 1. Lexical Analysis

The Lexer ([source](src/main/java/cn/czyx007/expression_parser/lexer/Lexer.java)) splits the input string into **Tokens** (lexical units):

| Token Type | Example | Description |
|-----------|------|------|
| `NUMBER` | `3.14`, `1e-5` | Numbers (supports floating-point and scientific notation) |
| `IDENTIFIER` | `sin`, `PI`, `x` | Identifiers (function names, constants, variables) |
| `PLUS/MINUS/MULTIPLY/DIVIDE` | `+`, `-`, `*`, `/` | Arithmetic operators |
| `POWER` | `^` | Power operator |
| `LPAREN/RPAREN` | `(`, `)` | Parentheses |

**Example**: Input `"3 + 4 * 2"` is split into: `[NUMBER:3] [PLUS] [NUMBER:4] [MULTIPLY] [NUMBER:2] [EOF]`

#### 2. Syntax Analysis

The Parser ([source](src/main/java/cn/czyx007/expression_parser/parser/Parser.java)) uses **recursive descent** technology to recursively parse expressions based on operator precedence.

**Precedence from high to low**:

| Precedence | Grammar Rule | Description |
|-------|---------|------|
| High | `factor` | Numbers, identifiers, parenthesized expressions, arrays |
| ↑ | `postfix` | Factorial (e.g., `5!`) |
| ↑ | `implicitMul` | Implicit multiplication (e.g., `2x`, `3(4+5)`) |
| ↑ | `power` | Power operation (right-associative, e.g., `2^3^2 = 2^(3^2)`) |
| ↑ | `unary` | Unary plus/minus signs (e.g., `-3^2 = -(3^2)`) |
| ↑ | `term` | Multiplication, division, modulo |
| ↑ | `addExpr` | Addition, subtraction |
| Low | `expr` | Assignment expressions |

**Grammar Rules** (BNF notation: `→` means defined as, `|` means or, `(...)` means grouping, `*` means zero or more, `?` means zero or one):

```
expr        → IDENTIFIER ASSIGN expr | addExpr
addExpr     → term ((PLUS | MINUS) term)*
term        → unary ((MULTIPLY | DIVIDE | MODULO) unary)*
unary       → (PLUS | MINUS) unary | power
power       → implicitMul (POWER power)?
implicitMul → postfix (postfix)*
postfix     → factor FACTORIAL?
factor      → NUMBER | IDENTIFIER | LPAREN expr RPAREN | LBRACKET ... RBRACKET
```

#### 3. Abstract Syntax Tree (AST)

After parsing, an **Abstract Syntax Tree** ([ExprNode](src/main/java/cn/czyx007/expression_parser/ast/ExprNode.java)) is generated. Tree node types include:

| Node Type | Description | Example |
|---------|------|------|
| `NumberNode` | Numeric literal | `3.14` |
| `VariableNode` | Variable reference | `x` |
| `BinaryOpNode` | Binary operation | `a + b`, `x * y` |
| `UnaryOpNode` | Unary operation | `-x`, `+5` |
| `FunctionNode` | Function call | `sin(PI/2)` |
| `AssignNode` | Variable assignment | `x = 10` |
| `ArrayNode` | Array literal | `[1, 2, 3]` |

**AST Example**: The syntax tree for expression `3 + 4 * 2`:

```
       (+)
      /   \
    (3)   (*)
         /   \
       (4)   (2)
```

#### 4. Expression Evaluation

Each AST node implements the `eval()` method to complete evaluation through **recursive calls** to child nodes' `eval()`.

Taking `BinaryOpNode` as an example ([source](src/main/java/cn/czyx007/expression_parser/ast/BinaryOpNode.java)):

```java
@Override
public double eval(Map<String, Double> context) {
    // 1. Recursively evaluate left subtree
    double leftVal = left.eval(context);
    // 2. Recursively evaluate right subtree
    double rightVal = right.eval(context);
    // 3. Apply operator
    switch (op.type()) {
        case PLUS:  return leftVal + rightVal;
        case MINUS: return leftVal - rightVal;
        case MULTIPLY: return leftVal * rightVal;
        case DIVIDE:   return leftVal / rightVal;
        case POWER:    return Math.pow(leftVal, rightVal);
        // ...
    }
}
```

`NumberNode` directly returns the stored value ([source](src/main/java/cn/czyx007/expression_parser/ast/NumberNode.java)):

```java
@Override
public double eval(Map<String, Double> context) {
    return value;
}
```

**Evaluation Process**: For `3 + 4 * 2`
1. `BinaryOpNode(+).eval()` → needs left and right values
2. Left value: `NumberNode(3).eval()` → `3`
3. Right value: `BinaryOpNode(*).eval()` → needs left and right values
4.   Left value: `NumberNode(4).eval()` → `4`
5.   Right value: `NumberNode(2).eval()` → `2`
6.   Result: `4 * 2 = 8`
7. Final result: `3 + 8 = 11`

---

### Features

| Feature | Description |
|------|------|
| **Mathematical Operations** | Addition, subtraction, multiplication, division, power, modulo, factorial |
| **Constants** | `PI` (pi), `E` (natural constant) |
| **Functions** | Trigonometric, logarithmic, statistical functions, etc. |
| **Variables** | Support variable assignment and persistence |
| **Arrays** | Array literals and array operations |
| **Matrices** | Transpose, determinant, matrix multiplication, etc. |
| **REPL** | Interactive command line with history result reference |

### Quick Start

#### Requirements
- **Build**: JDK 11 or higher (JDK 8's javadoc doesn't support `--release` flag)
- **Runtime**: Java 8 or higher
- Maven (for building the project)

#### Building the Project
Run the Maven build command in the project root directory:
```bash
mvn clean package
```
After building, an executable JAR file will be generated in the `target/` directory.

#### Starting REPL
After successful build, run the following command to start the interactive REPL:
```bash
java -jar target/Expression-Parser-1.3.1.jar
```
After startup, a welcome screen will be displayed. Enter `help` to view the complete list of supported features.

#### Using in Your Project
In addition to REPL interactive mode, you can also integrate this library as a dependency into your own Java project.

**Maven Dependency:**
```xml
<dependency>
    <groupId>cn.czyx007</groupId>
    <artifactId>Expression-Parser</artifactId>
    <version>1.3.1</version>
</dependency>
```

**Gradle Dependency:**
```groovy
implementation 'cn.czyx007:Expression-Parser:1.3.1'
```

**Usage Example:**
```java
import cn.czyx007.expression_parser.api.ExpressionEvaluator;
import cn.czyx007.expression_parser.ast.Value;

// Simple calculation
Value result = ExpressionEvaluator.eval("1 + 2 * 3");
System.out.println(result); // Output: 7

// With variable context (assign in expression, variables reusable)
Map<String, Object> context = new HashMap<>();
Value result1 = ExpressionEvaluator.eval("x = 10; y = 20; x + y", context);
System.out.println(result1); // Output: 30

// Reuse variables from context
Value result2 = ExpressionEvaluator.eval("x * y", context);
System.out.println(result2); // Output: 200
```

### REPL Supported Features

REPL (Read-Eval-Print Loop) supports the following features for users to input and calculate expressions in real-time:

#### Basic Operations
| Operator | Description |
|--------|------|
| `+`, `-`, `*`, `/` | Addition, subtraction, multiplication, division |
| `%` | Modulo |
| `^` | Power operation (right-associative, e.g., `2^3^2 = 512`) |
| `!` | Factorial (e.g., `5! = 120`) |

#### Constants
| Constant | Description |
|------|------|
| `PI` | Pi π ≈ 3.14159... |
| `E` | Natural constant e ≈ 2.71828... |

#### Mathematical Functions

##### Trigonometric and Inverse Trigonometric Functions
`sin`, `cos`, `tan`, `asin`, `acos`, `atan`

##### Hyperbolic Functions
`sinh`, `cosh`, `tanh`

##### Exponential and Logarithmic
`exp`, `ln`, `log`, `log10`

##### Roots and Absolute Value
`sqrt`, `cbrt`, `abs`

##### Rounding and Sign Functions
`ceil`, `floor`, `round`, `signum`, `sign`

##### Angle Conversion and Others
`degrees`, `radians`, `atan2(y,x)`, `hypot(x,y)`, `pow(base,exponent)`

#### Statistical Functions (Vector Operations)

For vectors `X = [...]; Y = [...];` (can also be manually expanded in order):

##### Basic Statistics
| Function | Description | Parameter Requirements |
|------|------|----------|
| `max(X)`, `min(X)` | Extremes | 2+ items |
| `sum(X)` | Sum | 1+ items |
| `count(X)` | Parameter count | - |
| `avg(X)` | Average | 1+ items |
| `median(X)` | Median | 1+ items |
| `prod(X)`, `product(X)` | Product | 1+ items |

##### Variance and Standard Deviation
| Function | Description | Parameter Requirements |
|------|------|----------|
| `var(X)`, `variance(X)` | Sample variance | ≥2 |
| `std(X)`, `stddev(X)` | Sample standard deviation | ≥2 |
| `varp(X)`, `variancep(X)` | Population variance | ≥1 |
| `stdp(X)`, `stddevp(X)` | Population standard deviation | ≥1 |

##### Distribution and Distance Statistics
| Function | Description | Parameter Requirements |
|------|------|----------|
| `gcd(X)`, `lcm(X)` | GCD/LCM | Integers, 2+ items |
| `range(X)` | Range | 1+ items |
| `geomean(X)` | Geometric mean | 1+ positive numbers |
| `norm1(X)`, `sumabs(X)` | Sum of absolute values (L1) | - |
| `norm2(X)`, `rms(X)` | Euclidean norm (L2)/RMS | - |
| `percentile(p,X)`, `pctl(p,X)` | Percentile | p ∈ [0,100] |

##### Covariance and Correlation
| Function | Description |
|------|------|
| `cov(X,Y)`, `covariance(X,Y)` | Sample covariance |
| `covp(X,Y)`, `covariancep(X,Y)` | Population covariance |
| `corr(X,Y)`, `correlation(X,Y)` | Correlation coefficient |

#### Vector and Matrix Operations

##### Vector Operations
| Function | Description |
|------|------|
| `dot(X,Y)`, `dotprod(X,Y)` | Vector dot product |
| `dist(X,Y)`, `distance(X,Y)`, `euclidean(X,Y)` | Euclidean distance |
| `manhattan(X,Y)`, `taxicab(X,Y)` | Manhattan distance |

##### Matrix Operations
| Function | Description |
|------|------|
| `transpose(matrix)`, `t(matrix)` | Matrix/vector transpose |
| `det(matrix)`, `determinant(matrix)` | Determinant |
| `matmul(matrix_A, matrix_B)` | Matrix multiplication |
| `trace(matrix)` | Matrix trace (sum of main diagonal) |
| `rank(matrix)` | Matrix rank |
| `mean(matrix, axis)` | Matrix mean (axis=0 columns, axis=1 rows) |
| `inv(matrix)` | Matrix inverse |
| `solve(A, b)` | Solve linear equations Ax=b |

##### Combinatorics
| Function | Description |
|------|------|
| `C(n,k)`, `comb(n,k)` | Combinations |
| `P(n,k)`, `perm(n,k)` | Permutations |

#### Variables and Arrays

##### Variable Assignment
- `x = 10` - Assignment
- `x = 10; y = 2x` - Multiple statements (semicolon-separated)
- `ans` - Previous calculation result

##### Arrays
- `[1, 2, 3]` - Array literal
- `[[1,2], [3,4]]` - 2D array (matrix)
- `scores = [1, 2, 3]` - Array variable assignment
- `avg(scores)` - Array as function parameter

#### Implicit Multiplication
- `2PI` - Number and constant
- `3(4+5)` - Number and parentheses
- `2sqrt(4)` - Number and function

#### REPL Commands
| Command | Description |
|------|------|
| `help`, `?` | Display help |
| `vars` | Display defined variables |
| `clear` | Clear all variables |
| `exit`, `quit`, `q` | Exit |

### Testing
The project includes comprehensive unit tests. Run the following command to execute tests:
```bash
mvn test
```

### Project Structure
- **src/main/java**: Contains main code, including lexer, parser, and REPL.
- **src/test/java**: Contains unit tests for each component.
- **target/**: Generated files and compiled classes.

### License
This project is open-sourced under the MIT License. See [LICENSE](LICENSE) file for details.
