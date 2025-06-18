# KBuilderTDD

[![Kotlin CI with Gradle](https://github.com/iptton-ai/kbuildercli/actions/workflows/unit-tests.yml/badge.svg)](https://github.com/iptton-ai/kbuildercli/actions/workflows/unit-tests.yml)
[![Code Coverage](https://img.shields.io/badge/Code%20Coverage-60.18%25-success.svg)](https://github.com/iptton-ai/KBuilderTDD/actions/workflows/unit-tests.yml)
[![Branch Coverage](https://img.shields.io/badge/Branch%20Coverage-40.50%25-yellow.svg)](https://github.com/iptton-ai/KBuilderTDD/actions/workflows/unit-tests.yml)

一个使用 TDD（测试驱动开发）方法构建的 Kotlin 项目。

## 持续集成

本项目使用 GitHub Actions 进行持续集成，自动执行以下任务：

- 在每次代码推送或 Pull Request 时构建项目
- 执行所有单元测试
- 生成 JaCoCo 测试覆盖率报告
- 验证覆盖率是否满足最低要求（50%）
- 将测试结果和覆盖率报告作为构建产物保存

同时，我们还配置了智能 Issue 分析功能：

- 使用 AutoDev Remote Agent 自动分析新建和更新的 Issues
- 基于 DeepSeek API 提供代码上下文智能分析
- 自动添加相关标签并发表分析评论
- 提供详细的代码文件分析和建议

### 测试覆盖率

当前项目的测试覆盖率状况：

- 指令（代码行）覆盖率：60.18%
- 分支覆盖率：40.50%

## 如何运行测试

本地执行测试和生成覆盖率报告：

```bash
# 运行测试
./gradlew test

# 生成覆盖率报告
./gradlew jacocoTestReport

# 验证覆盖率是否达到要求
./gradlew jacocoTestCoverageVerification
```

覆盖率报告将生成在 `build/jacocoHtml/` 目录下。

## CI 配置指南

### GitHub Actions 设置

1. **设置必要的 API Token**

   项目 CI 需要配置以下 API Token：

   - **DeepSeek API Token**（用于 Issue 智能分析）：
     - 访问 GitHub 仓库的 "Settings" > "Secrets and variables" > "Actions"
     - 点击 "New repository secret"
     - 名称设置为：`DEEPSEEK_TOKEN`
     - 值设置为你的 DeepSeek API 令牌
     - 点击 "Add secret" 保存
     
   - **Codecov Token**（用于覆盖率报告发布）：
     - 访问 [Codecov](https://codecov.io/) 并注册/登录
     - 关联你的 GitHub 仓库
     - 获取 Codecov token
     - 在 GitHub 仓库添加名为 `CODECOV_TOKEN` 的仓库密钥

2. **工作流文件**

   本项目包含两个主要的 GitHub Actions 工作流：
   - `unit-tests.yml` - 用于代码构建和测试
   - `advanced-issue-analysis.yml` - 用于 Issue 智能分析

   这些工作流大部分情况下会自动运行，也支持手动触发。

3. **Issue 智能分析**

   项目使用 AutoDev Remote Agent 提供 Issue 智能分析功能：
   
   - **自动分析**：新建或更新 Issue 时会自动触发分析
   - **手动分析**：在 Actions 页面选择 "Advanced Issue Analysis" 工作流，输入 Issue 编号
   - **分析深度**：可选择 shallow（浅）、medium（中，默认）、deep（深）三种深度
   
   分析结果将作为评论添加到 Issue 中，包含以下内容：
   
   - 问题分析和建议
   - 相关代码引用
   - 处理步骤指导
   - 文件过滤和分析过程信息
