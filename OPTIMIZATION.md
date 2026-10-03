# 优化进度

## 已落地（本轮）

- [x] 修复 SelectDialog TvRecyclerView `Invalid target position` 崩溃（safeSelect + try/catch）

- [x] 订阅/站点健康检测 **6 小时缓存**，手动检测可强制刷新
- [x] 切换订阅时失效缓存
- [x] 视频净化 **关闭 / 标准 / 激进** 三档
- [x] 播放失败/过滤广告提示文案
- [x] DLNA 推流失败更明确的错误说明
- [x] Python 变体启动自检（Chaquopy）
- [x] targetSdk **33**，version **1.2.0**
- [x] CI 产出 standard/python64 的 **debug + release**
- [x] README 双 APK 说明

## 后续 backlog（未在本轮完整执行）

### 架构
- [x] 拆分起步：`SpiderFactory`、`M3u8PurifyHelper`
- [x] 继续拆分：`JsonParseUtil`、`ParseBeanResolver`、`PlayRetryHelper`
- [x] `VideoFormatChecker`、`SubtitleCacheKey`
- [x] `SniffWebViewConfig`（嗅探 WebView 设置外移）
- [x] `SniffResourceInterceptor`（广告/视频发现拦截）
- [x] 引入 `NetworkClient` 单一 OkHttp 入口
- [x] Source/Subscription 健康检测改用 NetworkClient
- [x] FileUtils / UserFragment 豆瓣热搜 / SubtitleLoader 改用 NetworkClient
- [x] PlayFragment type=1 JSON 解析改用 NetworkClient
- [x] ApiConfig loadConfig/loadJar 改用 NetworkClient（已无 OkGo）
- [x] SourceViewModel **全部** HTTP（sort/list/detail/search/play）改用 httpGet/NetworkClient，已无 OkGo
- [x] 配置试点：`ConfigStore`（SharedPreferences + Gson，订阅/API；Hawk 双写 + 启动迁移）
- [ ] 其余 Hawk 键逐步迁入 ConfigStore / DataStore

### 工程
- [ ] AGP 8 + Kotlin 1.9 + OkHttp 4
- [ ] targetSdk 34 + 分区存储/通知权限完整适配
- [ ] 去除仍依赖的 tv-recyclerview（需改 Live/Detail/Controller 布局）

### 功能
- [x] 站点检测增强内容特征判断（非仅 HTTP 状态）
- [x] DLNA Stop / Seek
- [ ] 品牌 protocolInfo 表
- [ ] 模块化 feature 拆分与 UI 测试
