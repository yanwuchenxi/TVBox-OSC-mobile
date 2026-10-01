# TVBox-OSC-mobile

基于 [TVBoxOS-Mobile](https://github.com/XiaoRanLiu3119) / [q215613905/TVBoxOS](https://github.com/q215613905/TVBoxOS) 的**手机版**维护分支。

## 两种 APK

| 产物 | 构建命令 | 说明 |
|------|----------|------|
| **standard** | `./gradlew assembleStandardDebug` | 默认包，体积较小，**不含** Python 运行时；`.py` 源会安全降级 |
| **python64** | `./gradlew assemblePython64Debug -PenablePython=true` | 内置 **Chaquopy** + pyramid 脚本，可解析 `.py` 爬虫（arm64-v8a） |

CI 会分别上传 `MBox-standard-debug` 与 `MBox-python64-debug` Artifacts。

Release 构建：

```bash
./gradlew assembleStandardRelease
./gradlew assemblePython64Release -PenablePython=true
```

## 已增强能力

- **订阅**：粘贴添加、空状态引导、多线路预览勾选导入、有效性检测与排序（6 小时缓存）
- **站点**：配置加载后抽检 API 可达性并重排（缓存）
- **去广告**：m3u8 切片过滤；设置中「视频净化」在 **关闭 / 标准 / 激进** 三档循环
- **投屏**：SSDP 搜设备 + AVTransport 一键推流（失败可复制链接）
- **Python**：仅 python64 变体完整支持

## 结构

- `MainActivity` + 底部导航（首页 / 我的）
- `HomeFragment` 手机顶栏（站源、搜索、收藏）
- `app/src/python/java` 仅参与 python64 编译

## 说明

仅供学习交流。请勿传播未授权的影视内容。
