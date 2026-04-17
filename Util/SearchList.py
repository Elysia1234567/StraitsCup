#!/usr/bin/env python3
"""
SearchList.py

使用网站的 AJAX 接口 `/getProject.html` 抓取国家级项目清单（JSON），并导出为 Excel。

接口分析来源：页面内的 `load_data` JS 函数，GET 参数包含：province, rx_time, type, cate, keywords, category_id=16, limit=10, p=页码

输出列（按页面显示顺序）：
 - 序号
 - 项目序号
 - 编号
 - 名称
 - 类别
 - 公布时间
 - 类型
 - 申报地区或单位
 - 保护单位

用法：
	python Util/SearchList.py --output projects.xlsx
"""

import argparse
import logging
import math
import time

import pandas as pd
import requests


HEADERS = {
	"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36",
	"Accept": "application/json, text/javascript, */*; q=0.01",
}


def fetch_json(url, params=None, session=None, retries=3, backoff=1.0):
	session = session or requests.Session()
	for i in range(1, retries + 1):
		try:
			r = session.get(url, headers=HEADERS, params=params, timeout=20)
			r.raise_for_status()
			return r.json()
		except Exception as e:
			logging.warning("请求 JSON 失败（尝试 %d/%d）：%s - %s", i, retries, e, url)
			if i == retries:
				raise
			time.sleep(backoff * i)


def scrape_api(base_url="https://www.ihchina.cn/getProject.html", output="projects.xlsx", max_pages=None, delay=(0.5, 1.2)):
	session = requests.Session()
	page = 1
	limit = 10
	all_items = []

	# 首次请求以获取总数
	params = {
		"province": "",
		"rx_time": "",
		"type": "",
		"cate": "",
		"keywords": "",
		"category_id": "16",
		"limit": str(limit),
		"p": str(page),
	}

	data = fetch_json(base_url, params=params, session=session)
	if not data or "list" not in data or not data.get("list"):
		logging.info("首次请求未返回数据，响应：%s", data)
		return

	total_count = int(data.get("count", 0) or 0)
	total_pages = math.ceil(total_count / limit) if total_count else 1
	logging.info("总条数: %d, 每页: %d, 总页数: %d", total_count, limit, total_pages)

	# 如果用户限制 max_pages，则取较小值
	if max_pages:
		total_pages = min(total_pages, max_pages)

	# 处理第一页数据
	all_items.extend(data.get("list", []))

	for page in range(2, total_pages + 1):
		params["p"] = str(page)
		time.sleep((delay[0] + delay[1]) / 2)
		logging.info("抓取第 %d/%d 页", page, total_pages)
		d = fetch_json(base_url, params=params, session=session)
		if not d:
			logging.warning("第 %d 页返回空，停止。", page)
			break
		if not d.get("list"):
			logging.info("第 %d 页无数据，停止。", page)
			break
		all_items.extend(d.get("list", []))

	# 将 items 转为表格行
	rows = []
	idx = 1
	for item in all_items:
		# item 字段示例见页面 JS: auto_id, id, num, title, cate, rx_time, type, province, protect_unit
		rows.append({
			"序号": idx,
			"项目序号": item.get("auto_id", ""),
			"编号": item.get("num", ""),
			"名称": item.get("title", ""),
			"类别": item.get("type", ""),
			"公布时间": item.get("rx_time", ""),
			"类型": item.get("cate", ""),
			"申报地区或单位": item.get("province", ""),
			"保护单位": item.get("protect_unit", ""),
		})
		idx += 1

	df = pd.DataFrame(rows, columns=["序号", "项目序号", "编号", "名称", "类别", "公布时间", "类型", "申报地区或单位", "保护单位"])
	df.to_excel(output, index=False)
	logging.info("完成：共写入 %d 行 到 %s", len(df), output)


def main():
	parser = argparse.ArgumentParser(description="通过站点 AJAX 接口抓取国家级非遗项目并导出 Excel。")
	parser.add_argument("--output", default="projects.xlsx", help="输出文件名（Excel）")
	parser.add_argument("--max-pages", type=int, default=None, help="最多抓取多少页（每页 10 条），默认抓取全部")
	parser.add_argument("--verbose", action="store_true")
	args = parser.parse_args()

	logging.basicConfig(level=logging.DEBUG if args.verbose else logging.INFO, format='%(levelname)s: %(message)s')
	try:
		scrape_api(output=args.output, max_pages=args.max_pages)
	except Exception as e:
		logging.exception("抓取失败：%s", e)


if __name__ == "__main__":
	main()
