# SDD实战，OpenSpec在家庭理财系统中的使用

1. 简单介绍OpenSpec，如何安装等
    
    ### **第一步：安装OpenSpec CLI**
    
    ```bash
    npm install -g @fission-ai/openspec@latest
    ```
    
    ### **验证安装**
    
    ```bash
    openspec --version
    ```
    
    ### **第二步：在项目中初始化OpenSpec**
    
    ```bash
    cd your-project-directory
    openspec init
    ```
    
2. 主要的三个skills
    
    # **三阶段工作流（Draft → Review → Implement → Archive）**
    
    OpenSpec 的核心工作流分为草案、审查/对齐、实施与归档三个阶段。下图展示了这一流程：
    
    ![图 1: OpenSpec 工作流](https://assets.jimmysong.io/images/book/ai-handbook/sdd/openspec/7c37d798b7d7b46d27f13dbc37ca0c10.svg)
    
    图 1: OpenSpec 工作流
    
    图 1: OpenSpec 工作流
    
    在实际操作中，用户在 `openspec/changes/<change-id>/` 下创建或由 AI 生成以下文件：
    
    - `proposal.md`：说明为何需要改动、影响范围及验收标准。
    - `tasks.md`：分解后的实现任务清单。
    - `specs/` 子目录：仅包含 delta（新增/修改/移除/重命名）的规范片段。
    
    经审查并批准后，AI 按 `tasks.md` 顺序执行实现，并在完成后通过 `Archive` 命令将 delta 应用回 `openspec/specs/`，同时将 change 移入归档目录，保证 `specs/` 始终代表当前事实。
    
    核心三个command
    
    1. /opsx:propose - 草案
    2. /opsx: apply - 实施
    3. /opsx: achieve - 归档
3. 实战三个features
    1. feature 1， runway caculator
        1. /opsx:propose
        
         I want to add a new function, runway analysis, I have one example - C:\Users\lorra\projects\personal\nextplan\finance\runway-calculation, please use the same structure,
        you can get the future month expense from system, and you can also get the liquid asset from system. please have a proposal under requirement
        
        27个tasks，但是只有一个backend的manual task（要求开发者自己用Swager UI来测试）
        
        b. /opsx: apply
        
        犯了一个致命错误，没考虑货币和汇率，直接简单的相加
        
        指出错误后，修正是没有考虑performance，对每笔记录，每次都在数据库查询汇率，导致report生成速度很慢
        
        之后有增强了需求，allow to exclude some liquid assets and adjust the expense for particular items.
        
        c. /opsx: achieve
        

refine，先重新整理了config.yaml, 这个是openspec的Claude.md文件

please help me update the config.yaml under openspec folder, use the format as Example in the file, and refer to [CLAUDE.md](http://claude.md/) under root folder to get the
tech stack, convetions, styles guides etc. you can see the [README.md](http://readme.md/) to get domain knowledge too.

针对测试比较弱，专门对config.yaml做了测试加强

please add testing strategies, it is a full stack app, testing should include backend API testing and frontend UI testing

同时将change 1当时犯得错误也写进config.yaml

when use openspec to develop runway feature, it made a mistake - it does not consider the currency for each asset and expense account. and later, it
tried to query curreny rate everytime in DB (in fact, there are controllers cached the change rate data), please update such mistakes in config.yaml, so
such mistakes could be avoided in future changes development.

b. feature 2, runway report persistence 
now runway analysis feature is ready, however it is a snapshoot, every time, when you open this page, it will calculate the result based on current asset and expense.  please help me  add a new feature, that I can save current snapshoot (after I uncheck some accounts and adjust the expense for some accounts) into a file. so I can review it later.

修改需求

with JSON file, it is easy to load for system, however it is not friendly for me to review it offline, I do want to have a PDF file which include the
whole runway report. could you provide it?

修改需求

I change my mind, I do not want to store into JSON and export to local, how about persist the JSON data into backend database table, and show as
runway-{date}-report. it should have a new page to list all reports and user can click one to show that day runway report.

因为需求的修改，从单纯前端修改的change变成了包含前后端和测试，一共11类，34个tasks。完美重新生成，以前有出现过，在半成品的需求下，修改反而AI一直达不到想要的效果，OpenSpec看到需求大幅改变，果断删除重新生成

apply，很顺利，因为新引入了mock测试，开始失败，根据错误信息修改，最后也修复了。

完成后尝试使用功能，一开始“保存报告”功能有问题，后让AI诊断是API routine不对，这个也可以写入config.yaml，避免下次发生
然后PDF不支持中文，这个是AI使用的library的问题，改用了其他实现也成功了。

AI产生的UI和PDF的呈现都问题不大，做了一个小修改，要求加入Family的信息

但是最大的问题是UI测试是manual的，有一个optional的task，需要自动引入UI测试的Task

安装 Vitest + Vue Test Utils（组件测试），配置 vite.config.js

achieve，成功将第二个change归档

     c. feature 3, property investment caclator

    propose：I added an excel under requirement folder - named The Brutal Calculator.xlsx), please read the sheet and convert as a new feature Property Investment Calculator. I may add a new group （投资） in sidebar - 管理，分析，投资，设置。

花了不少时间去安装读取excel文件的tool，这次没有backend change，所有task都在前端，增加了vue test相关的task，一共8个group，22个task。

apply顺利完成，包含ui test，但是有几个公式计算错误，提示后修改成功

之后对UI布局做了一些调整，比如从2列布局改成了三列布局，调整了label的显示等

之后顺利achieve和git commit push

新增2000+代码，一个小时内完成

1. 三个features的总结 - 代码数，tasks数（复杂度），犯错，开发时间
    
    
    |  | 代码 | tasks | 复杂度 | 测试 | 犯错 | 开发时间 |
    | --- | --- | --- | --- | --- | --- | --- |
    | runway analysis | ~1900 | 27 | 前后端，但是不需要有新的数据库表 | 只有manual的后端测试 | 一个致命错误，一个performance的错误 | 2h |
    | runway report/export | ~1800 | 34 | 已有需求的增强，修改后需求包含前后端，引入新的数据库表 | 后端测试+manual的前端测试 | UI一个致命错误，routine不对，Save Report不工作
    PDF不支持中文 | 38m |
    | property  | ~2300 | 22 | 新需求，只有前端 | 前端后端都自动测试了 | 读取Excel，对公式理解哟问题，计算逻辑不对
    其他实现没有致命错误 | 49m |
    
2. OpenSpec和SpecKit和Superpowers的比较

https://jimmysong.io/zh/book/ai-handbook/sdd/openspec/

[https://juejin.cn/post/7605494530017165352](https://juejin.cn/post/7605494530017165352) - 这个文章很好，很详细，帮我简单的提炼下OpenSpec/SpecKit/Superpowers的比较

[https://intent-driven.dev/knowledge/spec-kit-vs-openspec/](https://intent-driven.dev/knowledge/spec-kit-vs-openspec/)

1. 结论

我在personal finance management系统中用了OpenSpec，和之前的Vibe Code比较，生产率和代码质量得到了很大的提高，相比SpecKit比较重量级的spec driven的工具，我更倾向于使用OpenSpec。同时我在另一个项目，个人blog项目中用了superpowers，效果也不错，我会进一步探讨如何将两者结合起来。