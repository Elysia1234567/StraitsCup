# ihchina 项目列表爬虫

这是一个简单的 Python 爬虫，用于抓取 https://www.ihchina.cn/project.html#target1 上的项目列表并导出为 Excel (`projects.xlsx`)。

依赖
 - Python 3.8+
 - 见 `requirements.txt`（requests, beautifulsoup4, lxml, pandas, openpyxl）

安装依赖
```bash
python -m pip install -r requirements.txt
```

运行
```bash
python Util/SearchList.py --output projects.xlsx
```

可选参数
 - `--start` 起始 URL（默认 https://www.ihchina.cn/project.html#target1）
 - `--output` 输出文件名（默认 projects.xlsx）
 - `--max-pages` 最多抓取多少页（默认抓取全部）
 - `--verbose` 显示调试日志

注意事项
 - 本脚本尝试解析 <table> 结构优先，如页面采用 div/ul 布局则使用备选解析方法。
 - 如果目标网站使用大量客户端渲染（AJAX），可能需要改用 Selenium 或分析 AJAX 接口。
 - 请尊重目标站点的 robots.txt 与爬虫策略，合理设置速率与重试策略。
