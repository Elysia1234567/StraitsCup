# Repository Guidelines

## 项目结构与模块划分
`BackEnd/` 是当前主要代码目录，后端基于 Spring Boot 3，核心代码位于 `src/main/java/com/omnisource`，测试位于 `src/test/java`，运行配置位于 `src/main/resources`。开发时保持分层清晰：`controller` 负责接口入口，`service` 与 `service/impl` 负责业务逻辑，`mapper` 负责数据访问，公共类型放在 `dto`、`entity`、`enums`、`utils`。

`Util/` 存放数据抓取、清洗和导入脚本，例如 `SearchList.py`、`import_standard_list.py`。`FrontEnd/` 目前只有占位内容，不应默认视为可直接运行的前端工程。`mywork/` 属于个人工作区，不应作为正式代码提交依据。

## 构建、测试与开发命令
后端命令在 `BackEnd/` 目录执行：

- `mvn spring-boot:run`：启动后端服务，默认端口为 `8081`。
- `mvn test`：运行 JUnit 与 Spring Boot 测试。
- `mvn clean package`：清理并打包应用。

工具脚本通常在仓库根目录执行：

- `pip install -r requirements.txt`：安装 Python 抓取与处理依赖。
- `python Util/import_standard_list.py`：导入标准清单数据。

## 代码风格与命名约定
Java 代码使用 4 个空格缩进，遵循 Java 17 常规风格。包名一律小写，类名使用 `PascalCase`，方法与字段使用 `camelCase`，常量使用 `UPPER_SNAKE_CASE`。保持控制器轻量，避免把业务逻辑直接写进 `controller`。DTO 命名应直接表达用途，例如 `LoginRequest`、`UserInfoResponse`。

## 测试规范
测试代码位于 `BackEnd/src/test/java`，当前使用 Spring Boot Test 与 JUnit 5。测试类命名统一使用 `*Test` 结尾，并尽量与被测包结构对应，例如 `exception/GlobalExceptionHandlerTest.java`。涉及接口流程、鉴权、异常处理或数据库行为的修改，应同步补充或更新测试。

## 提交与 Pull Request 规范
现有提交历史以简短、任务导向的提交信息为主，如 `完善RAG`、`changeIP`、`rag`。继续保持单次提交只解决一个问题，提交信息可以用中文或英文，但必须能直接说明改动目的。提交 PR 时应附带变更摘要、影响模块、测试结果；如果改动影响接口返回或页面表现，补充示例请求或截图。

## 安全与配置提示
`BackEnd/src/main/resources/application.yml` 当前包含数据库、Redis、JWT 以及模型相关配置。提交代码前不要继续扩散真实密钥、口令或固定服务器地址。新改动应优先改为环境变量或本地覆盖配置，避免将敏感信息直接写入仓库。
