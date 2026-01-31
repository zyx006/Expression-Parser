# 表达式解析器 | Expression-Parser

## 概述
表达式解析器是一个基于**递归下降解析**技术的 Java 项目，支持标量、向量、矩阵的数学表达式解析与计算。项目还提供了一个交互式 REPL（Read-Eval-Print Loop）终端，方便用户实时输入和计算表达式。

## 实现原理

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

### 1. 词法分析 (Lexical Analysis)

词法分析器（[Lexer](src/main/java/cn/czyx007/expression_parser/lexer/Lexer.java)）将输入字符串分割成一个个**Token**（词法单元）：

| Token 类型 | 示例 | 说明 |
|-----------|------|------|
| `NUMBER` | `3.14`, `1e-5` | 数字（支持浮点数、科学计数法） |
| `IDENTIFIER` | `sin`, `PI`, `x` | 标识符（函数名、常量、变量） |
| `PLUS/MINUS/MULTIPLY/DIVIDE` | `+`, `-`, `*`, `/` | 四则运算符 |
| `POWER` | `^` | 幂运算符 |
| `LPAREN/RPAREN` | `(`, `)` | 括号 |

**示例**：输入 `"3 + 4 * 2"` 被分割为：`[NUMBER:3] [PLUS] [NUMBER:4] [MULTIPLY] [NUMBER:2] [EOF]`

### 2. 语法分析 (Syntax Analysis)

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

### 3. 抽象语法树 (AST)

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

### 4. 表达式求值 (Evaluation)

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

## 功能特点

| 特性 | 说明 |
|------|------|
| **数学运算** | 加减乘除、幂运算、取模、阶乘 |
| **常量** | `PI`（圆周率）、`E`（自然常数） |
| **函数** | 三角函数、对数函数、统计函数等 |
| **变量** | 支持变量赋值和持久化 |
| **数组** | 数组字面量和数组操作 |
| **矩阵** | 转置、行列式、矩阵乘法等 |
| **REPL** | 交互式命令行，支持历史结果引用 |

## 快速开始

### 环境要求
- Java 8 或更高版本
- Maven（用于构建项目）

### 构建项目
在项目根目录下运行 Maven 构建命令：
```bash
mvn clean package
```
构建完成后，会在 `target/` 目录下生成可执行的 JAR 文件。

### 启动 REPL
构建成功后，运行以下命令启动交互式 REPL：
```bash
java -jar target/Expression-Parser-1.2.2.jar
```
启动后将显示欢迎界面，输入 `help` 可查看支持的完整功能列表。

### 在项目中使用
除了 REPL 交互模式，你也可以将本库作为依赖集成到自己的 Java 项目中：
```java
import cn.czyx007.expression_parser.lexer.Lexer;
import cn.czyx007.expression_parser.parser.Parser;
import cn.czyx007.expression_parser.ast.ExprNode;
import cn.czyx007.expression_parser.ast.Value;

// 创建词法分析器和语法分析器
Lexer lexer = new Lexer("x = 10; y = 2 * x; x + y");
Parser parser = new Parser(lexer);

// 解析表达式并构建抽象语法树
ExprNode root = parser.parse();

// 创建执行上下文并求值
Map<String, Object> context = new HashMap<>();
Value result = root.evalValue(context);

System.out.println(result); // 输出: 30
```

## REPL 支持的功能

REPL（Read-Eval-Print Loop）支持以下功能，方便用户实时输入和计算表达式：

### 基本运算
| 运算符 | 说明 |
|--------|------|
| `+`, `-`, `*`, `/` | 加减乘除 |
| `%` | 取模 |
| `^` | 幂运算（右结合，如 `2^3^2 = 512`） |
| `!` | 阶乘（如 `5! = 120`） |

### 常量
| 常量 | 说明 |
|------|------|
| `PI` | 圆周率 π ≈ 3.14159... |
| `E` | 自然常数 e ≈ 2.71828... |

### 数学函数

#### 三角函数与反三角函数
`sin`, `cos`, `tan`, `asin`, `acos`, `atan`

#### 双曲函数
`sinh`, `cosh`, `tanh`

#### 指数与对数
`exp`, `ln`, `log`, `log10`

#### 根号与绝对值
`sqrt`, `cbrt`, `abs`

#### 取整与符号函数
`ceil`, `floor`, `round`, `signum`, `sign`

#### 角度转换与其他
`degrees`, `radians`, `atan2(y,x)`, `hypot(x,y)`, `pow(base,exponent)`

### 统计函数（向量操作）

对于向量 `X = [...]; Y = [...];`（也可手动按序展开输入）：

#### 基础统计
| 函数 | 说明 | 参数要求 |
|------|------|----------|
| `max(X)`, `min(X)` | 极值 | 2+ 个 |
| `sum(X)` | 求和 | 1+ 个 |
| `count(X)` | 参数计数 | - |
| `avg(X)` | 平均值 | 1+ 个 |
| `median(X)` | 中位数 | 1+ 个 |
| `prod(X)`, `product(X)` | 乘积 | 1+ 个 |

#### 方差与标准差
| 函数 | 说明 | 参数要求 |
|------|------|----------|
| `var(X)`, `variance(X)` | 样本方差 | ≥2 |
| `std(X)`, `stddev(X)` | 样本标准差 | ≥2 |
| `varp(X)`, `variancep(X)` | 总体方差 | ≥1 |
| `stdp(X)`, `stddevp(X)` | 总体标准差 | ≥1 |

#### 分布与距离统计
| 函数 | 说明 | 参数要求 |
|------|------|----------|
| `gcd(X)`, `lcm(X)` | 最大公约数/最小公倍数 | 整数，2+ 个 |
| `range(X)` | 极差 | 1+ 个 |
| `geomean(X)` | 几何平均数 | 1+ 个正数 |
| `norm1(X)`, `sumabs(X)` | 绝对值和 (L1) | - |
| `norm2(X)`, `rms(X)` | 欧几里得范数 (L2)/均方根 | - |
| `percentile(p,X)`, `pctl(p,X)` | 百分位数 | p ∈ [0,100] |

#### 协方差与相关
| 函数 | 说明 |
|------|------|
| `cov(X,Y)`, `covariance(X,Y)` | 样本协方差 |
| `covp(X,Y)`, `covariancep(X,Y)` | 总体协方差 |
| `corr(X,Y)`, `correlation(X,Y)` | 相关系数 |

### 向量与矩阵操作

#### 向量操作
| 函数 | 说明 |
|------|------|
| `dot(X,Y)`, `dotprod(X,Y)` | 向量点积 |
| `dist(X,Y)`, `distance(X,Y)`, `euclidean(X,Y)` | 欧几里得距离 |
| `manhattan(X,Y)`, `taxicab(X,Y)` | 曼哈顿距离 |

#### 矩阵操作
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

#### 组合数学
| 函数 | 说明 |
|------|------|
| `C(n,k)`, `comb(n,k)` | 组合数 |
| `P(n,k)`, `perm(n,k)` | 排列数 |

### 变量与数组

#### 变量赋值
- `x = 10` - 赋值
- `x = 10; y = 2x` - 多语句（分号分隔）
- `ans` - 上一次计算结果

#### 数组
- `[1, 2, 3]` - 数组字面量
- `[[1,2], [3,4]]` - 二维数组（矩阵）
- `scores = [1, 2, 3]` - 数组变量赋值
- `avg(scores)` - 数组作为函数参数

### 隐式乘法
- `2PI` - 数字与常量
- `3(4+5)` - 数字与括号
- `2sqrt(4)` - 数字与函数

### REPL 命令
| 命令 | 说明 |
|------|------|
| `help`, `?` | 显示帮助 |
| `vars` | 显示已定义的变量 |
| `clear` | 清除所有变量 |
| `exit`, `quit`, `q` | 退出 |

## 测试
项目包含全面的单元测试。运行以下命令执行测试：
```bash
mvn test
```

## 项目结构
- **src/main/java**：包含主代码，包括词法分析器、语法分析器和 REPL。
- **src/test/java**：包含各组件的单元测试。
- **target/**：生成的文件和编译后的类。

## 许可证
此项目基于 MIT 许可证开源。详情请参阅 [LICENSE](LICENSE) 文件。
