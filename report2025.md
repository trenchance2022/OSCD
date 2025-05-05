# 《操作系统课程设计》实验报告

[TOC]



------

## 课程设计题目

操作系统模拟程序的设计与实现。





## 课程设计目标和要求

### 目标

设计并实现一个具有操作系统基本功能的软件，具有操作系统的基本功能：

1.  进程管理功能。进程创建（new）、进程调度（scheduling）、进程阻塞（block）、 进程唤醒（wakeup）、进程同步（synchronous）等； 
2. 内存管理功能。进程存储空间的分配和回收，空闲空间的管理等；
3. 文件系统。目录/文件的创建和删除、空间分配和回收 ；
4. 设备管理。设备的申请、分配、使用、释放等；
5. UI界面；
6. 中断机制。

### 基本要求

要求完成的最小功能集合 ：

1. 进程管理和调度 ；
2. 内存管理（存储分配与回收，进程交换） ；
3. 时钟管理：timer ；
4. 中断处理：中断响应、中断处理 ；
5. GUI，图形界面展示多道程序并发执行的过程（系统快照）。





## 需求分析

### 引言

#### 编写目的

本需求分析文档旨在系统、准确地描述操作系统模拟程序的功能与运行需求，为后续的系统设计、开发、测试与维护提供明确依据。文档面向本项目的开发者、测试人员以及指导教师，作为课程设计过程中的重要技术参考材料，确保系统功能设计满足实验教学目标，系统行为与用户预期保持一致。

#### 项目背景

本项目以“操作系统模拟程序的设计与实现”为课题，模拟实现简化版操作系统内核功能，并通过前端图形界面进行动态展示和交互。系统以多进程调度为核心，集成内存管理、文件操作、设备请求与中断处理机制，帮助我们通过自己动手实践，以可视化方式深入理解操作系统的基本运行原理与模块协作机制。



### 总体描述

#### 系统目标

本项目旨在开发一个基于 Web 的操作系统模拟程序，集成进程管理、内存管理、文件操作、设备请求与中断处理机制等模块。系统通过模拟真实操作系统中的关键组件与行为，达成操作系统内核的仿真实现。

用户可以在浏览器进入操作系统界面，通过界面的Shell与操作系统进行交互，并实时展示系统快照。

操作系统提供丰富广泛的指令可供用户使用，容纳了各种文件操作，程序编写，程序并发执行，程序互斥读写文件，外设使用，内存分配，虚拟内存扩展等功能。

实现目标如下：

1. 功能完整：实现操作系统核心模块的功能，包括进程状态管理、逻辑地址映射等完整的操作系统功能。

2. 结构清晰：采用模块化架构，后端以 Spring Boot 框架搭建，前端以原生 JavaScript 实现，逻辑层次分明。

3. 交互直观：通过图形界面展示系统快照，包括内存、磁盘等实时状态，结合模拟终端进行指令输入与控制。

4. 模拟真实环境：支持多核并发执行等机制，尽可能还原真实操作系统的执行过程。

    

#### 用户特征

本系统面向计算机及相关专业的本科生与教师，主要用于操作系统功能的演示与学生自我实践。用户具有一定的编程基础，能够理解命令行操作、以及操作系统的基本原理。



#### 系统功能概要

系统划分为七个核心功能模块，分别为：

1. Shell交互功能：提供类 Unix 命令行界面，支持文件管理、目录切换、程序执行、进程终止等指令，通过终端与后端交互。
2. 进程管理模块：实现进程的创建、调度、阻塞、唤醒与终止；支持多种调度策略；支持多 CPU 并发模拟执行；支持用户更换调度策略或者更换处理器个数。
3. 内存管理模块：实现页式管理机制，支持逻辑地址到物理地址的转换、缺页中断、TLB缓存、页置换算法等。
4. 文件管理模块：支持目录树结构、文件读写与编辑、磁盘块分配与释放，文件内容映射为进程执行指令集。
5. 设备管理模块：模拟打印机、USB、扫描仪等设备，支持进程发起设备请求，设备完成后触发中断唤醒阻塞进程。
6. 中断管理模块：处理时钟中断、I/O中断、文件锁唤醒等事件，实现多进程抢占与异步事件响应。
7. 系统快照展示模块：自动采集系统状态，包括运行进程、内存分配、磁盘使用、目录结构、设备状态等，通过前端页面可视化展示。



#### 运行环境与约束

系统采用前后端分离架构，整体运行环境要求如下：

后端运行环境：

- Java 17，Spring Boot 3.4.4 框架
- 每次启动重置操作系统环境，不依赖数据库

前端环境：

- 原生 HTML + CSS + JavaScript
- 支持现代浏览器（Chrome、Edge）



### 详细功能需求

#### Shell交互功能

Shell 交互模块为用户提供统一的命令输入入口，用于操作系统核心功能的访问与控制。用户可通过模拟终端界面输入指令，与文件系统、进程调度器、内存管理器、设备管理器等模块进行交互。所有操作过程中的提示、反馈与系统状态变化均实时推送至前端终端输出区域。

Shell模块接收用户输入的字符串命令，对其进行语义解析和参数提取（实现一个简易Parser），根据命令类型，调用对应的模块进行功能执行。

Shell支持通过命令 `vi <filename>` 打开图形化文件编辑窗口，前端提供编辑界面，编辑完成后可提交保存。若文件不存在则新建文件且自动提示并创建，已存在文件直接载入编辑内容覆盖原本文件。用户可以通过命令 `vi <filename>` 编写自定义程序并通过 Shell 提交任务使其执行。

Shell支持根据当前所在目录动态更新终端提示符（如 `/root/dir1> instruction xxx`）。

以下是一个简单的顺序图，部分操作可能和实际实现有略微差异，该图仅用来理解数据流向与调用逻辑。

<img src="./report2025.assets/image-20250502160136945.png" alt="image-20250502160136945" style="zoom: 67%;" />



#### 进程管理功能

进程管理模块是操作系统模拟程序的核心，负责实现进程的五状态管理与调度控制。该模块协调多个 CPU、调度策略、时间片、状态转换与中断响应等机制，确保系统能稳定支持多道程序并发执行。

具体功能列表：

1. 进程创建

    1. 系统支持通过伪代码源文件创建进程。
    2. 每个进程分配一个唯一的进程标识符（PID），并初始化其进程控制块（PCB）。

2. 进程控制块（PCB）管理

    1. 系统为每个进程维护一个 PCB，记录进程的基本属性
    2. PCB 与内存页表、文件资源、设备请求等信息绑定。

3. 调度器策略

    1. 系统调度器支持多种经典调度算法，包括：

        1. FCFS（先来先服务）

        2. SJF（短作业优先）

        3. RR（时间片轮转）

        4. PRIORITY（优先级调度）

        5. 
            PRIORITY_PREEMPTIVE（抢占）

        6. MLFQ（多级反馈队列）

    2. 每种策略对应不同的就绪队列管理方式，用户可以在系统启动前切换调度策略。

4. 多核调度与执行
   1. 系统支持多个模拟 CPU 核心同时执行进程。
   2. 每个 CPU 为一个独立线程，持续从调度器拉取就绪进程并执行其指令。
   3. 指令支持模拟计算、内存访问、设备 I/O、文件读写、进程结束等操作。

5. 进程状态转换
   1. 系统支持标准五态模型：`NEW` → `READY` → `RUNNING` → `WAITING` / `TERMINATED`。
   2. 进程可因阻塞（如设备占用、锁等待）、时间片耗尽、中断等原因发生状态切换。

6. 进程终止与资源回收
   1. 当进程正常结束或被用户终止，系统自动回收资源。




#### 内存管理功能

内存管理模块是操作系统核心组成部分之一，负责为进程提供隔离、高效的主存使用环境。内存管理模块负责管理进程的内存分配、释放、读取和写入操作，它协调 CPU 执行过程中的地址转换与页表映射，支持分页、缺页中断与页面置换机制，并与进程管理、文件系统和中断机制紧密协作。

具体功能列表：

1. 分页内存管理
    1. 系统采用固定分区的页式管理方式，将内存划分为固定大小的页框（页帧），每页大小为 1024 字节。
    2. 系统总共支持 64 个物理内存页，物理页以数组方式管理，每页可供一个进程使用。
    3. 每个进程申请内存时，按页为单位进行分配，不支持跨页连续物理分配。

2. 虚拟内存支持
    1. 系统支持虚拟内存，通过逻辑页号访问用户地址空间，并由页表完成地址映射。
    2. 每个进程拥有独立虚拟地址空间，访问的逻辑地址不依赖于实际物理地址。
    3. 未在物理内存中的页将触发缺页中断。

3. 地址转换机制
    1.  MMU（内存管理单元）用于执行逻辑地址到物理地址的转换。
    2. 页未在内存中，将触发缺页中断，由缺页处理器调页。

4. 页表与页表项管理
    1. 系统为每个进程建立独立的页表，并存放在页表区域中以模拟内核空间中的页表寄存区。
    2. 每个页表包含若干个页表项，每个页表项记录该逻辑页的物理块号、有效位、访问位、修改位、磁盘块号等信息。
    3. 页表支持随进程运行动态增长与释放，进程终止时页表一并释放。

5. 快表 TLB 机制

    1. 加速虚拟地址到物理地址的映射。 

    1. 用Clock算法替换快表项。

6. 页面置换算法
    1. 使用二次机会算法选择牺牲页面。
2. 内存读写
    1. 系统支持跨页的逻辑地址访问，在读写过程中自动处理页边界，确保逻辑连续访问正确映射到多个物理块。
    2. 内存读写基于逻辑地址提供数据操作。
    3. 所有进程间的内存空间互相隔离，不支持跨进程访问。
3. 内存可视化支持
    1. 系统支持对所有物理内存页状态进行实时采集和展示，标明每个页所属进程及页号。
4. 内存释放机制
    1. 当进程终止时，系统自动释放其占用的所有物理页和页表。
    2. 所有调出的磁盘页面、页表项、缓存等信息均被清理，确保资源可重用。



#### 文件管理功能

文件管理模块负责模拟操作系统中的树状目录结构、磁盘存储模拟、块读写与分配释放管理、创建删除切换目录、文件创建编辑与读写、文件锁机制等关键功能。该模块与进程管理模块深度集成，支持文件驱动的程序执行，以及设备请求与文件资源同步控制。所有文件数据均存放在模拟磁盘中，通过对模拟磁盘读写进行文件读写，支持动态块分配、文件可视化、并发访问控制等操作。

具体功能列表：

1. 目录结构管理

    1. 支持目录树结构，具有根目录 `/root`。
    2. 支持目录的创建、删除、切换和展示。

2. 文件创建与删除

    1. 支持以给定名称和大小创建文件。
    2. 自动分配磁盘块，在磁盘中分配空间存储文件数据，支持非连续块索引。
    3. 支持删除单个文件或所在目录下所有内容，由操作系统回收空间。

3. 文件编辑与读写

    1. 支持通过图形化编辑界面修改文件内容。
    2. 自定义程序支持文件读写操作指令。
    3. 提供按名称查看文件内容、占用块等信息的功能。

4. 磁盘模拟与空间管理

    1. 提供了1MB的模拟磁盘空间可供使用，磁盘空间可真实存放数据。
    2. 由操作系统管理磁盘块分配状态。
    3. 每个文件由多个块组成，支持分配、回收与读取。
    4. 支持磁盘占用率查看，可视化磁盘块使用情况。

5. 文件锁管理

    | 类型 | 是否支持共享 | 是否互斥 |
    | ---- | ------------ | -------- |
    | 读锁 | 是           | 对写互斥 |
    | 写锁 | 否           | 是       |

    1. 申请读锁  若无写锁 → 立即持有，否则 → 进程阻塞 。
    2. 申请写锁 若无读写锁 → 立即持有，否则 → 进程阻塞 。
    3. 自动唤醒策略 ，优先唤醒等待写锁的进程，其次批量唤醒可并发执行的读锁进程 。
    4. 进程终止时释放其持有的所有锁 。



#### 设备管理功能

设备管理模块用于模拟操作系统对外围设备的控制，包括设备的创建、删除、分配、使用、释放、进程阻塞与中断唤醒等全过程管理。模块支持多设备、进程阻塞与设备调度，并与中断机制协同工作，实现设备使用过程的完整生命周期管理。

1. 设备增添与删除管理

    1. 系统支持多种自定义的模拟设备，包括 USB、打印机、扫描仪等。
    2. 每类设备支持多个编号实例（如 USB 0、USB 1），每个设备实例独立运行。
    3. 所有设备信息由设备管理器集中维护，支持运行时用户添加与删除设备。

2. 设备请求机制

    1. 进程在执行 I/O 指令时向设备管理器发起请求。
    2. 如果设备当前空闲，请求立即开始；否则进程被加入设备的等待队列。

3. 设备运行与阻塞管理

    1. 当设备正在服务某一进程时，该进程进入阻塞状态（`WAITING`），CPU 释放。
    2. 被阻塞进程在其设备请求完成后由中断处理器唤醒并返回就绪状态。

4. 设备队列调度与进程唤醒

    1. 每个设备维护一个先进先出（FIFO）等待队列。
    2. 当前设备完成任务后，从队首取出下一个等待请求并开始执行。
    3. 同时通过中断处理器将对应进程状态设为 `READY`，交由调度器重新调度。

5. 设备可视化状态展示

    1. 所有设备运行状态、当前服务进程、等待队列信息将推送至前端快照界面。
    2. 可视化面板展示每个设备实例。

    

#### 中断管理功能

中断管理模块负责协调处理操作系统运行过程中发生的异步事件，包括时钟中断、I/O设备完成中断和文件锁释放中断。通过中断机制，系统能够实现进程抢占、阻塞唤醒、资源释放等动态行为，从而保证多进程系统的实时性与响应性。

1. 中断类型支持

    1. 系统支持三种主要中断类型：

        时钟中断（Timer Interrupt）

        设备完成中断（I/O Interrupt）

        文件锁释放中断（File Lock Wakeup）

    2. 各类中断通过统一的中断处理器（`InterruptHandler`）进行分发与处理。

2. 时钟中断管理

    1. 每个 CPU 在每个时钟周期后自动触发时钟中断，模拟硬件定时器功能。
    2. 该机制保证了如 RR、MLFQ 等策略的时间片调度功能。

3. 设备完成中断

    1. 当进程发起 I/O 请求后，会被挂起并加入目标设备的等待队列。
    2. 设备服务进程运行完成后，自动调用 `InterruptHandler` 触发设备完成中断。
    3. 中断处理器将相关进程状态设置为 `READY`，并通知调度器将其加入就绪队列。

4. 文件锁释放中断

    1. 当某一文件锁（读锁/写锁）被释放后，文件锁管理器会通过中断机制通知中断处理器。
    2. 中断处理器根据等待队列状态，唤醒等待进程并按调度策略将其重新调度。

5. 中断处理流程统一化

    1. 所有中断请求通过 `InterruptHandler.handleInterrupt(InterruptType, params)` 接口处理，形成统一事件分发机制。
    2. 中断处理过程中自动同步更新 PCB 状态、系统日志与快照数据，确保各模块一致性。

6. 中断影响的状态转换

    - `RUNNING` → `READY`（时间片耗尽）
    - `WAITING` → `READY`（设备完成/锁释放）
    - `READY` → `RUNNING`（进程重新被调度执行）



#### 系统快照展示功能

系统快照展示功能用于实时展示操作系统当前的系统快照，包括进程状态、内存使用情况、设备状态、文件系统等。通过系统快照，用户可以直观地看到操作系统的运行状况、资源分配与使用情况，有助于调试程序和监控系统的运行。

1. **进程状态展示**
    1. 系统展示出每个CPU上的进程状态，便于分析多核调度的正确性。
    2. 系统展示 `RUNNING`、`READY`、`WAITING` 队列的进程内容。
    3. 系统显示每个运行进程的 PID、优先级、指令、剩余时间等信息。
2. **内存状态展示**
    1. 显示所有物理内存页的当前状态，包括是否被占用、属于哪个进程、页号等。
3. **设备状态展示**
    1. 支持实时展示所有设备（如打印机、USB 设备）的当前状态（空闲、正在使用等）。
    2. 展示当前被服务的进程以及等待队列中的进程。
4. **文件系统状态展示**
    1. 展示文件系统中的目录结构信息。
    2. 展示磁盘块的占用情况信息。
5. **实时数据更新**
    1. 快照界面能够实时更新操作系统的状态信息（每100ms刷新一次），确保用户看到的是最新的状态。





## 开发环境

本操作系统模拟器项目基于 **Java 17 + Spring Boot 3.4.4** 构建，系统前后端分离。

### 后端环境

|                | 工具说明                                                     |
| -------------- | ------------------------------------------------------------ |
| **开发语言**   | Java 17                                                      |
| **框架**       | Spring Boot 3.4.4                                            |
| **构建工具**   | Maven（版本兼容 JDK 17）                                     |
| **主要依赖**   | spring-boot-starter-web、thymeleaf、lombok、javax.annotation |
| **开发工具**   | IntelliJ IDEA                                                |
| **日志输出**   | Server-Sent Events（SSE） 实时推送                           |
| **并发控制**   | 多线程 + BlockingQueue + 自定义调度器                        |
| **热部署支持** | Spring DevTools                                              |

### 前端环境

| 项目         | 配置 / 工具说明                                              |
| ------------ | ------------------------------------------------------------ |
| **页面结构** | HTML5 + 原生 JavaScript                                      |
| **样式框架** | CSS                                                          |
| **交互机制** | SSE（用于日志输出与快照更新）Fetch API（用于命令提交与 vi 编辑） |
| **调试工具** | Chrome 开发者工具、浏览器控制台                              |

### 项目目录结构

```elixir
src/
├── main/
│   ├── java/
│   │   └── org/example/oscdspring/
│   │       ├── controller/              # 控制器
│   │       ├── device_management/       # 设备管理
│   │       ├── file_disk_management/    # 文件磁盘管理
│   │       ├── interrupt_management/     # 中断管理
│   │       ├── main/                     # 系统全局
│   │       ├── memory_management/        # 内存管理
│   │       ├── process_management/       # 进程管理
│   │       ├── snapshot/                  # 快照
│   │       ├── util/                      # 工具
│   │       └── OscdSpringApplication.java	# 启动类
│   ├── resources/
│   │   ├── application.properties
│   │   ├── static/
│   │   │   ├── index.html
│   │   │   ├── css/
│   │   │   └── js/
│   │   └── templates/
└── test/
```





## 总体设计

### 系统架构设计

#### 技术选型

本项目采用Java 语言实现后端核心逻辑，基于Spring Boot框架构建Web服务接口，并使用HTML+CSS+JavaScript构建前端界面。

选择Java是因为其丰富的多线程支持和稳定的内存管理机制，便于模拟操作系统的并发和内存分页等底层功能。同时，Spring Boot提供开箱即用的REST API和Server-Sent Events (SSE)支持，方便实现前后端实时通信（如日志流和状态快照推送）。前端采用原生HTML/JS，无需引入额外框架，利用浏览器即可直观呈现操作系统各模块状态。

开发过程中使用PlantUML辅助设计架构图。

#### 模块划分

#### 系统架构图（这是最重要的地方）

（通过图示的方式展示系统的整体架构，包含各个模块、组件及其相互关系，使用plantuml代码，图中内容尽可能详细且正确）



### 模块设计

（模块功能，模块关系，模块接口，模块间通信等）



### 数据结构设计（如果方便写的话）

### 用户接口设计

（API，前后端交互等）

### 用户界面设计





## 详细设计

### Shell交互实现

```cmd
未完成未完成未完成未完成未完成未完成未完成未完成未完成未完成未完成未完成未完成未完成未完成未完成
```

```cmd
mkdir <directory>		#创建目录
mkf <filename> <size>	#创建指定字节数的随机内容文本文件
cd <directory>		#切换目录（能够实现绝对路径解析）
cd..				#返回上一级
cat <filename>		#展示文件内容
ls					#展示当前目录下文件与目录
rm <filename>		#删除文件
rmdir <directory>		#删除空目录
rmrdir <directory>	#递归删除该目录下所有内容
shf <filename>		#查看该文件的磁盘块占用
vi <filename>		#编辑文件内容，若文件不存在则创建
exec <filename1> <priority1> [<filename2> <priority2> ...]	#运行指定文件并指定优先级
addevc <deviceName> <deviceId>				#增加外设
rmdevc <deviceName> <deviceId>				#删除外设
kill <pid>			#杀死进程
info dir			#查看目录树
info disk			#查看磁盘位图
Info memory			#查看内存
```



### 进程管理实现

```cmd
未完成未完成未完成未完成未完成未完成未完成未完成未完成未完成未完成未完成未完成未完成未完成未完成
```

```cmd
C <time># 								#模拟占用CPU时间，单位ms
R/W <filename> <readtime># 			#不会往里面写真实的内容，重点在于读写互斥性
D <deviceName> <deviceID> <IOtime># 		#外设ID
M <byte># 								#申请使用的内存大小
MW/MR <logicAddress> <Bytes># 			#写入指定长度随机数据或者读取指定长度数据
Q# 									#进程结束，释放资源
```



### 内存管理实现



### 文件管理实现



### 设备管理实现



### 中断管理实现



### 系统快照展示实现







## 程序清单

### 代码结构

```elixir
src/
├── main/
│   ├── java/
│   │   └── org/example/oscdspring/
│   │       ├── controller/              # 控制器
│   │       │   ├── ShellController.java
│   │       │   ├── ShellSseController.java
│   │       │   └── SnapshotController.java
│   │       ├── device_management/       # 设备管理
│   │       │   ├── DeviceManager.java
│   │       │   ├── IODevice.java
│   │       │   └── IORequest.java
│   │       ├── file_disk_management/    # 文件磁盘管理
│   │       │   ├── Bitmap.java
│   │       │   ├── Directory.java
│   │       │   ├── Disk.java
│   │       │   ├── FileDiskManagement.java
│   │       │   ├── FileLockManager.java
│   │       │   ├── FileSystemImpl.java
│   │       │   └── Inode.java
│   │       ├── interrupt_management/     # 中断管理
│   │       │   └── InterruptHandler.java
│   │       ├── main/                     # 主要类
│   │       │   ├── Constants.java
│   │       │   ├── Library.java
│   │       │   ├── Shell.java
│   │       │   └── StartupInitializer.java
│   │       ├── memory_management/        # 内存管理
│   │       │   ├── Memory.java
│   │       │   ├── MemoryManagement.java
│   │       │   ├── MemoryManagementImpl.java
│   │       │   ├── MMU.java
│   │       │   ├── PageTable.java
│   │       │   ├── PageTableArea.java
│   │       │   └── PageTableEntry.java
│   │       ├── process_management/       # 进程管理
│   │       │   ├── CPU.java
│   │       │   ├── PCB.java
│   │       │   ├── PIDBitmap.java
│   │       │   ├── ProcessState.java
│   │       │   └── Scheduler.java
│   │       ├── snapshot/                  # 快照
│   │       │   └── SystemSnapshot.java
│   │       ├── util/                      # 工具类
│   │       │   ├── LogEmitterService.java
│   │       │   └── SnapshotEmitterService.java
│   │       └── OscdSpringApplication.java  # 应用程序入口
│   └── resources/
│       ├── application.properties
│       ├── static/
│       │   ├── index.html
│       │   ├── css/
│       │   │   └── style.css
│       │   └── js/
│       │       ├── device.js
│       │       ├── disk.js
│       │       ├── filesystem.js
│       │       ├── memory.js
│       │       ├── process.js
│       │       ├── shell.js
│       │       └── snapshotManager.js
│       └── templates/
└── test/
```

代码结构为常见的 Spring 项目结构，程序从 `OscdSpringApplication.java` 为入口启动。

操作系统核心逻辑按照功能划分为多个模块。每个模块负责不同的操作系统功能，并通过类和接口实现。

控制器模块负责处理前端发出的请求；快照模块用于从各个模块采集操作系统的状态快照，填充展示给前端的json，并调用工具类把信息推送给前端；工具类负责消息与快照的前端推送服务。

前端文件位于 `src/main/resources/static/` 目录下，用于动态展示系统的运行状态。

`application.properties` 用于配置 Spring 属性，以及操作系统的部分属性（CPU数，调度算法）。



### 代码说明

```coffeescript
src/main/java/org/example/oscdspring/OscdSpringApplication.java
# Spring Boot 应用的入口类

src/main/java/org/example/oscdspring/controller/ShellController.java
# Spring 控制器，处理前端 Shell 提交的命令。它接收命令并通过 `Shell` 类处理，返回处理结果。包含解析普通shell指令和vi操作的接口

src/main/java/org/example/oscdspring/controller/ShellSseController.java
# 通过 SSE 推送实时消息给前端。包括 Shell 的输出和各模块运行时输出

src/main/java/org/example/oscdspring/controller/SnapshotController.java
# 提供一个 API 接口，允许前端通过 SSE 通道接收操作系统状态的实时快照

src/main/java/org/example/oscdspring/device_management/DeviceManager.java
# 设备管理类

src/main/java/org/example/oscdspring/device_management/IODevice.java
# I/O 设备类

src/main/java/org/example/oscdspring/device_management/IORequest.java
# I/O 请求类

src/main/java/org/example/oscdspring/file_disk_management/Bitmap.java
# 位图类

src/main/java/org/example/oscdspring/file_disk_management/Directory.java
# 目录类

src/main/java/org/example/oscdspring/file_disk_management/Disk.java
# 模拟磁盘类

src/main/java/org/example/oscdspring/file_disk_management/FileDiskManagement.java
# 文件管理的接口类，供其他模块调用基本方法

src/main/java/org/example/oscdspring/file_disk_management/FileLockManager.java
# 文件锁管理类

src/main/java/org/example/oscdspring/file_disk_management/FileSystemImpl.java
# 文件管理接口类的实现

src/main/java/org/example/oscdspring/file_disk_management/Inode.java
# 文件索引节点类

src/main/java/org/example/oscdspring/interrupt_management/InterruptHandler.java
# 中断处理类

src/main/java/org/example/oscdspring/main/Constants.java
# 常数

src/main/java/org/example/oscdspring/main/Library.java
# 管理各模块Manager对象的访问

src/main/java/org/example/oscdspring/main/Shell.java
# Shell命令解析类

src/main/java/org/example/oscdspring/main/StartupInitializer.java
# BIOS类，初始化系统

src/main/java/org/example/oscdspring/memory_management/Memory.java
# 模拟内存类

src/main/java/org/example/oscdspring/memory_management/MemoryManagement.java
# 内存管理接口

src/main/java/org/example/oscdspring/memory_management/MemoryManagementImpl.java
# 内存管理接口的具体实现

src/main/java/org/example/oscdspring/memory_management/MMU.java
# 内存管理单元

src/main/java/org/example/oscdspring/memory_management/PageTable.java
# 页表类

src/main/java/org/example/oscdspring/memory_management/PageTableArea.java
# 单例类，用于模拟内存系统区中存储页表的区域

src/main/java/org/example/oscdspring/memory_management/PageTableEntry.java
# 页表项类

src/main/java/org/example/oscdspring/process_management/CPU.java
# 模拟 CPU，负责执行指令

src/main/java/org/example/oscdspring/process_management/PCB.java
# 进程控制块类

src/main/java/org/example/oscdspring/process_management/PIDBitmap.java
#管理进程 ID 的分配

src/main/java/org/example/oscdspring/process_management/ProcessState.java
# 定义进程五种状态

src/main/java/org/example/oscdspring/process_management/Scheduler.java
# 进程调度器类

src/main/java/org/example/oscdspring/snapshot/SystemSnapshot.java
# 负责采集操作系统的实时状态并生成快照

src/main/java/org/example/oscdspring/util/LogEmitterService.java
# 日志发射服务

src/main/java/org/example/oscdspring/util/SnapshotEmitterService.java
# 快照发射服务

src/main/resources/application.properties
# Spring Boot 应用配置文件，定义系统的基本参数，以及调度算法，CPU数量

src/main/resources/static/index.html
# 前端首页

src/main/resources/static/css/style.css
# 样式

src/main/resources/static/js/device.js
# 设备管理前端交互与展示

src/main/resources/static/js/disk.js
# 磁盘占用前端交互与展示

src/main/resources/static/js/filesystem.js
# 目录结构展示

src/main/resources/static/js/memory.js
# 内存管理前端交互和显示

src/main/resources/static/js/process.js
# 进程管理前端交互和显示

src/main/resources/static/js/shell.js
# Shell前端交互逻辑

src/main/resources/static/js/snapshotManager.js
# 实时接收来自服务器的操作系统状态快照
```



### 源代码

源代码见提交。





## 测试报告

### 测试环境

为保证各模块在模拟环境下的正确性与稳定性，测试工作在以下软硬件和开发配置下进行：

- 操作系统：Windows 11 
- 开发语言：Java 17
- 构建工具：Maven
- 开发环境：IntelliJ IDEA
- 框架平台：Spring Boot 3.4.4
- 测试框架：JUnit 5
- 浏览器环境：Chrome / Edge 最新版本



### 测试计划





### 三级标题：单元测试

这部分是针对某一模块的java代码的某些函数的测试，与整个系统无关，相当于给一个函数搭上脚手架。不需要覆盖很全，意思意思就行。可以让gpt随便跑点。

#### 四级标题：进程管理单元测试

描述被测试的函数，测试代码，执行结果。

#### 四级标题：内存管理单元测试

同上。

#### 四级标题：xxx单元测试……

同上。





### 三级标题：集成测试

####这部分是指在`StartupInitializer.java`中编写测试程序，并在网页运行，在前端观察结果是否符合预期，必要时需要截图，甚至作甘特图等图辅助说明。



#### Shell 测试

##### 错误命令

<img src="./report2025.assets/image-20250502104933111.png" alt="image-20250502104933111" style="zoom:50%;" />

能够提示报错，符合预期。



##### `mkdir`

创建目录前：

<img src="./report2025.assets/屏幕截图 2025-05-02 103903.png" style="zoom:50%;" />

输入指令创建目录：

<img src="./report2025.assets/屏幕截图 2025-05-02 103913.png" alt="屏幕截图 2025-05-02 103913" style="zoom:50%;" />

创建目录后：

<img src="./report2025.assets/屏幕截图 2025-05-02 103920.png" alt="屏幕截图 2025-05-02 103920" style="zoom:50%;" />

出现名为a的文件夹。

<img src="./report2025.assets/image-20250502104708651.png" alt="image-20250502104708651" style="zoom:50%;" />

重名目录会报错，符合预期。



##### `mkf`与`cat`

创建文件前：

<img src="./report2025.assets/image-20250502104058030.png" alt="image-20250502104058030" style="zoom:50%;" />

创建文件：

<img src="./report2025.assets/image-20250502104152075.png" alt="image-20250502104152075" style="zoom:50%;" />

创建文件后：

<img src="./report2025.assets/image-20250502104208695.png" alt="image-20250502104208695" style="zoom:50%;" />

文件占用了10个块，符合预期。

创建另一个10字节文件并查看：

<img src="./report2025.assets/image-20250502104344779.png" alt="image-20250502104344779" style="zoom: 50%;" />

文件确实由10个随机的`char`构成，且显示正常。

<img src="./report2025.assets/image-20250502104754338.png" alt="image-20250502104754338" style="zoom:50%;" />

创建过大文件会提示空间不足。

<img src="./report2025.assets/image-20250502104818310.png" alt="image-20250502104818310" style="zoom:50%;" />

且磁盘块在创建过大文件失败时，保持原来大小不变，而不是占满。



##### `cd`与`cd..`

<img src="./report2025.assets/image-20250502104550666.png" alt="image-20250502104550666" style="zoom:50%;" />

功能均正常。

<img src="./report2025.assets/image-20250502104638028.png" alt="image-20250502104638028" style="zoom:50%;" />

输入错误路径会报错，符合预期。



##### `ls`

在根目录执行：

<img src="./report2025.assets/屏幕截图 2025-05-02 105145.png" style="zoom:50%;" />

符合根目录下内容：

<img src="./report2025.assets/屏幕截图 2025-05-02 105151.png" alt="屏幕截图 2025-05-02 105151" style="zoom: 50%;" />



##### `rm`

删除b：

<img src="./report2025.assets/屏幕截图 2025-05-02 105527.png" style="zoom:50%;" />

相应的回收其空间，符合预期：

<img src="./report2025.assets/屏幕截图 2025-05-02 105534.png" alt="屏幕截图 2025-05-02 105534" style="zoom:50%;" />

这时如果再创建文件：

<img src="./report2025.assets/屏幕截图 2025-05-02 105725.png" style="zoom:50%;" />

文件系统会遍历位图找到合适空间分配：

<img src="./report2025.assets/image-20250502105842321.png" alt="image-20250502105842321" style="zoom:50%;" />

删除不存在的文件会报错，符合预期：

<img src="./report2025.assets/image-20250502105903999.png" alt="image-20250502105903999" style="zoom:50%;" />



##### `rmdir`

删除空文件夹a：

<img src="./report2025.assets/屏幕截图 2025-05-02 110250.png" style="zoom:50%;" />

a被删除：

<img src="./report2025.assets/image-20250502110316091.png" alt="image-20250502110316091" style="zoom:50%;" />



##### `rmrdir`

递归删除文件夹d1：

<img src="./report2025.assets/屏幕截图 2025-05-02 110427.png" style="zoom:50%;" />

空间被回收：

<img src="./report2025.assets/屏幕截图 2025-05-02 110433.png" alt="屏幕截图 2025-05-02 110433" style="zoom:50%;" />

目录中全部内容被删除：

<img src="./report2025.assets/屏幕截图 2025-05-02 110440.png" alt="屏幕截图 2025-05-02 110440" style="zoom:50%;" />

 

##### `shf`

创建一个40960字节的文件。

<img src="./report2025.assets/屏幕截图 2025-05-02 110611.png" style="zoom:50%;" />

创建前磁盘情况：

<img src="./report2025.assets/屏幕截图 2025-05-02 110433-1746155241933-16.png" style="zoom:50%;" />

创建后磁盘情况：

<img src="./report2025.assets/image-20250502110732317.png" alt="image-20250502110732317" style="zoom:50%;" />

符合上面shf的输出结果。



##### `vi`

输入vi指令：

<img src="./report2025.assets/屏幕截图 2025-05-02 110952.png" style="zoom:50%;" />

若文件不存在，系统会自动为我们新建：

<img src="./report2025.assets/屏幕截图 2025-05-02 111001.png" style="zoom:50%;" />

输入文件内容：

<img src="./report2025.assets/屏幕截图 2025-05-02 111051.png" style="zoom:50%;" />

查看文件内容与磁盘块占用，符合预期：

<img src="./report2025.assets/屏幕截图 2025-05-02 111115.png" style="zoom:50%;" />



##### `exec`

创建一个文件，输入一段简易示例程序：

<img src="./report2025.assets/image-20250502111455781.png" alt="image-20250502111455781" style="zoom:50%;" />

输入指令开始执行：

<img src="./report2025.assets/image-20250502111548989.png" alt="image-20250502111548989" style="zoom:50%;" />

<img src="./report2025.assets/image-20250502111524176.png" alt="image-20250502111524176" style="zoom:50%;" />



##### `addevc`与`rmdevc`

<img src="./report2025.assets/image-20250502111832116.png" alt="image-20250502111832116" style="zoom:50%;" />

<img src="./report2025.assets/image-20250502111847973.png" alt="image-20250502111847973" style="zoom: 50%;" />

<img src="./report2025.assets/屏幕截图 2025-05-02 111907.png" style="zoom:50%;" />

<img src="./report2025.assets/屏幕截图 2025-05-02 111913.png" alt="屏幕截图 2025-05-02 111913" style="zoom:50%;" />



##### `kill`

###### kill 运行态进程

kill执行前状态：

<img src="./report2025.assets/屏幕截图 2025-05-04 094304.png" style="zoom:50%;" />

kill 1，杀死正在执行的t1进程，可见其运行被终止，且占用的内存资源被释放。

<img src="./report2025.assets/屏幕截图 2025-05-04 094315.png" alt="屏幕截图 2025-05-04 094315" style="zoom:50%;" />



###### kill 就绪态进程

kill执行前状态：

<img src="./report2025.assets/image-20250504094041872.png" alt="image-20250504094041872" style="zoom:50%;" />

kill 3，杀死在Ready队列的t3，结果如下所示：

<img src="./report2025.assets/image-20250504094121027.png" alt="image-20250504094121027" style="zoom:50%;" />

t3从Ready队列移除，转为Terminate态，释放资源。



###### kill IO进程

kill执行前状态：

![](./report2025.assets/屏幕截图 2025-05-04 094519.png)

t1正在进行设备IO：

![屏幕截图 2025-05-04 094524](./report2025.assets/屏幕截图 2025-05-04 094524.png)

这个时候执行kill操作：

![image-20250504094623038](./report2025.assets/image-20250504094623038.png)

设备释放，程序终止。



###### kill 内存读写进程

执行t8，读内存：

![image-20250504094807894](./report2025.assets/image-20250504094807894.png)

执行kill：

![image-20250504094839116](./report2025.assets/image-20250504094839116.png)

资源释放，读锁释放。



##### `info`

<img src="./report2025.assets/屏幕截图 2025-05-02 112512.png" style="zoom:50%;" />

<img src="./report2025.assets/屏幕截图 2025-05-02 112520.png" alt="屏幕截图 2025-05-02 112520" style="zoom:50%;" />

<img src="./report2025.assets/屏幕截图 2025-05-02 112530.png" alt="屏幕截图 2025-05-02 112530" style="zoom:50%;" />







#### 进程调度测试

测试程序：

```yaml
t9:
C 2000#Q#
t10:
C 3000#Q#
t11:
C 4000#Q#
t12:
C 1000#C 2000#Q#
t13:
C 10000#Q#
t14:
C 2000#Q#
t15:
C 2000#Q#
t16:
C 2000#Q#
```

为便于测试，CPU数量均设置为1

1. FCFS

   测试命令：`exec t9 0 t10 0 t11 0`

   结果甘特图：

   ```mermaid
   gantt
       title Gantt Chart for t9, t10, t11
       dateFormat  HH:mm
       axisFormat  %H:%M
       section t9
       C 2000: 00:00, 00:02
       section t10
       C 3000: 00:02, 00:05
       section t11
       C 4000: 00:05, 00:09
   ```

   结果截图：

   ![](.\report2025.assets\进程调度测试FCFS-1.png)

   ![](.\report2025.assets\进程调度测试FCFS-2.png)

   ![](.\report2025.assets\进程调度测试FCFS-3.png)

2. SJF

   说明：本程序SJF调度算法按程序代码段大小对程序进行排序

   测试命令:`exec t9 0 t12 0 t11 0`

   结果甘特图：

   ```mermaid
   gantt
       title Gantt Chart for t9, t11, t12
       dateFormat  HH:mm
       axisFormat  %H:%M
       section t9
       C 2000: 00:00, 00:02
       section t11
       C 4000: 00:02, 00:06
       section t12
       C 1000: 00:06, 00:07
       C 2000: 00:07, 00:09
   ```

   结果截图：

   ![](.\report2025.assets\进程调度测试SJF-1.png)

   ![](.\report2025.assets\进程调度测试SJF-2.png)

   ![](.\report2025.assets\进程调度测试SJF-3.png)

3. RR

   测试命令：`exec t9 0 t10 0`

   测试结果：CPU交替运行t9、t10。

4. PRIORITY

   测试命令：`exec t9 3 t10 2 t11 1`

   结果甘特图：

   ```mermaid
   gantt
       title Gantt Chart for t9, t10, t11
       dateFormat  HH:mm
       axisFormat  %H:%M
       section t9
       C 2000: 00:00, 00:02
       section t10
       C 3000: 00:06, 00:09
       section t11
       C 4000: 00:02, 00:06
   ```

   结果截图：

   ![](.\report2025.assets\进程调度测试PRIORITY-1.png)

   ![](.\report2025.assets\进程调度测试PRIORITY-2.png)

   ![](.\report2025.assets\进程调度测试PRIORITY-3.png)

5. MLFQ

   测试命令：`exec t13 0 t14 1 t15 2 t16 3`

   测试结果：CPU按t13, t14, t13, t15, t14, t13, t16, t15, t14, t13, t16, t15, t14, t13, t16, t15, t14, t13, t16, t15, t14, t13, t16, t15的顺序依次执行完毕，符合MLFQ调度算法的预期。

6. PRIORITY_Preemptive

   测试命令：`exec t14 3 t15 2 t16 1`

   结果甘特图：
   
   ```mermaid
   gantt
       title Gantt Chart for t14, t15, t16
       dateFormat  ss:SSS
       axisFormat  %S:%L
       section t14
       C 2000: 00:000, 00:100
       C 2000: 04:100, 06:000
       section t15
       C 2000: 00:100, 02:100
       section t16
       C 2000: 02:100, 04:100
   ```
   
   结果截图：
   
   ![](.\report2025.assets\进程调度测试优先级抢占-1.png)
   
   ![](.\report2025.assets\进程调度测试优先级抢占-2.png)
   
   ![](.\report2025.assets\进程调度测试优先级抢占-3.png)
   
   ![](.\report2025.assets\进程调度测试优先级抢占-4.png)

#### 设备调度测试

测试程序：

```yaml
t1:
M 4096#C 5000#C 10000#D Printer 1 10000#Q#
t2:
M 4096#C 10000#C 2000#D Printer 1 10000#Q#
t3:
M 10240#C 5000#C 10000#D USB 3 10000#Q#
```

测试命令：`exec t1 0 t2 0 t3 0`

测试环境：4个CPU，调度算法FCFS。

结果甘特图：

```mermaid
gantt
    title Gantt Chart for t1, t2, t3
    dateFormat  HH:mm
    axisFormat  %H:%M
    section t1
    M 4096: 00:00, 00:00
    C 5000: 00:00, 00:05
    C 10000: 00:05, 00:15
    D Printer 1 10000: 00:22, 00:32
    Q: 00:32, 00:32
    section t2
    M 4096: 00:00, 00:00
    C 10000: 00:00, 00:10
    C 2000: 00:10, 00:12
    D Printer 1 10000: 00:12, 00:22
    Q: 00:22, 00:22
    section t3
    M 10240: 00:00, 00:00
    C 5000: 00:00, 00:05
    C 10000: 00:05, 00:15
    D USB 3 10000: 00:15, 00:25
    Q: 00:25, 00:25



```

结果截图：

<img src="./report2025.assets/image-20250504103441989.png" alt="image-20250504103441989" style="zoom:50%;" />

<img src="./report2025.assets/image-20250504103043263.png" alt="image-20250504103043263" style="zoom:50%;" />

<img src="./report2025.assets/屏幕截图 2025-05-04 103130.png" style="zoom:50%;" />

<img src="./report2025.assets/屏幕截图 2025-05-04 103139.png" alt="屏幕截图 2025-05-04 103139" style="zoom:50%;" />

符合甘特图的描述。





#### 文件系统测试

基本功能（文件创建删除修改，目录创建删除切换，磁盘块管理，磁盘空间分配等）已在Shell测试部分完成测试，下面仅测试文件读写互斥性。

测试程序：

```yaml
t4:
（被读写文件）
t5:
M 4096#W t4 2000#Q#
t6:
M 4096#W t4 2000#Q#
t7:
M 4096#R t4 2000#Q#
t8:
M 4096#R t4 2000#Q#
```

测试命令：`exec t5 0 t6 0 t7 0 t8 0`

测试环境：4个CPU，调度算法FCFS。

结果甘特图：

```mermaid
gantt
    title Gantt Chart for t5, t6, t7, t8
    dateFormat  HH:mm
    axisFormat  %H:%M
    section t5
    M 4096: 00:00, 00:00
    W t4 2000: 00:00, 00:02
    Q: 00:02, 00:02
    section t6
    M 4096: 00:00, 00:00
    W t4 2000: 00:02, 00:04
    Q: 00:04, 00:04
    section t7
    M 4096: 00:00, 00:00
    R t4 2000: 00:04, 00:06
    Q: 00:06, 00:06
    section t8
    M 4096: 00:00, 00:00
    R t4 2000: 00:04, 00:06
    Q: 00:06, 00:06


```

结果截图：

首先执行写进程t5：

<img src="./report2025.assets/屏幕截图 2025-05-04 105631.png" style="zoom:50%;" />

<img src="./report2025.assets/屏幕截图 2025-05-04 105551.png" style="zoom:50%;" />

t5完成后执行写进程t6，图中t5内存块已被释放，代表t5进程已结束执行：

<img src="./report2025.assets/屏幕截图 2025-05-04 105613.png" style="zoom:50%;" />

<img src="./report2025.assets/屏幕截图 2025-05-04 105606-1746327601179-11.png" style="zoom:50%;" />

t6执行后，t7和t8同时进行读操作：

<img src="./report2025.assets/image-20250504110038382.png" alt="image-20250504110038382" style="zoom:50%;" />

<img src="./report2025.assets/屏幕截图 2025-05-04 110031.png" style="zoom:50%;" />

<img src="./report2025.assets/屏幕截图 2025-05-04 110136.png" style="zoom:50%;" />

t7和t8读之后同时释放：

<img src="./report2025.assets/image-20250504110203214.png" alt="image-20250504110203214" style="zoom:50%;" />





#### 四级标题：xxx集成测试……

同上。

#### 四级标题：总体测试

可能需要复杂的涉及全模块功能的测试。逻辑和上面一样，只是程序写的复杂一些。准备等上面写好了再做这个。







## 个人总结与体会

### 任泽高



### 安轶东



### 董佳鑫



### 路遥



### 杨皓岚



### 张志鑫



