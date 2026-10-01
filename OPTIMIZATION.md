# 优化进度

## 已落地（本轮）

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
- [ ] 拆分 PlayFragment / ApiConfig
- [ ] 统一协程 + 单一 OkHttp，逐步移除 OkGo
- [ ] Hawk → DataStore/Room

### 工程
- [ ] AGP 8 + Kotlin 1.9 + OkHttp 4
- [ ] targetSdk 34 + 分区存储/通知权限完整适配
- [ ] 去除仍依赖的 tv-recyclerview（需改 Live/Detail/Controller 布局）

### 功能
- [ ] 站点检测改为轻量 homeContent 探测
- [ ] DLNA Stop/Seek 与品牌 protocolInfo 表
- [ ] 模块化 feature 拆分与 UI 测试
