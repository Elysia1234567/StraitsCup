# OmniSource 数据工具说明

`Util/` 用于维护非遗知识库数据，当前核心产物是 `standardList.jsonl`，后端 `RagServiceImpl` 会默认读取它作为本地 RAG 数据源。

## 文件角色

- `standardList.jsonl`：当前 RAG 知识库，每行一条 JSON 文档。
- `SearchList.py`：从公开非遗项目列表页面抓取项目数据，导出 Excel。
- `import_standard_list.py`：将整理后的标准表转换为后端可读取的 JSONL。
- `requirements.txt`：Python 脚本依赖。

## 安装依赖

```bash
python -m pip install -r requirements.txt
```

## 抓取公开项目列表

```bash
python Util/SearchList.py --output projects.xlsx
```

可选参数：

- `--start`：起始 URL，默认 `https://www.ihchina.cn/project.html#target1`。
- `--output`：输出文件名，默认 `projects.xlsx`。
- `--max-pages`：最多抓取页数，默认抓取全部。
- `--verbose`：显示调试日志。

## 导入 JSONL

```bash
python Util/import_standard_list.py
```

生成或更新后的 `standardList.jsonl` 会被后端用于：

- `/api/rag/retrieve` 检索调试。
- `/api/rag/prompt` Prompt 上下文预览。
- Agent 聊天中的 RAG 事实增强。

## 注意事项

- 抓取脚本优先解析 `<table>` 结构，若目标站改为大量客户端渲染，需要改用接口分析或浏览器自动化。
- 请尊重目标网站的 robots.txt、访问频率和版权边界。
- 提交前检查 JSONL 编码为 UTF-8，且每行都是合法 JSON。
- Milvus 只是可选向量增强；本地 JSONL 数据质量仍直接影响问答效果。
