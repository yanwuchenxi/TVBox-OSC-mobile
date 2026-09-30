# TVBox-OSC-mobile

基于 [TVBoxOS-Mobile](https://github.com/XiaoRanLiu3119) / [q215613905/TVBoxOS](https://github.com/q215613905/TVBoxOS) 的**手机版**工程。

当前仓库以附件 `TVBoxOS-Mobile-main` 为基底进行维护与优化，不再沿用旧版 TV 壳改造方案。

## 结构

- `MainActivity` + 底部导航（首页 / 我的）
- `HomeFragment` 手机顶栏（站源切换、搜索、收藏）
- 设置、搜索、直播等为手机端布局

## 构建

```bash
./gradlew assembleDebug
```

产物：`app/build/outputs/apk/` 下 `MBox_v*.apk`

也可在 GitHub Actions 中下载构建产物。

## 说明

仅供学习交流。


## 功能说明（本分支增强）

### 去广告（m3u8 切片）
播放 m3u8 时自动过滤：少数路径前缀、过短切片、广告域名/路径关键字。可在设置中关闭「视频净化」类开关（`VIDEO_PURIFY`）。

### 订阅有效性
订阅管理页进入自动检测，并按 **有效 → 未知 → 失效** 排序；当前订阅失效会提示。

### 局域网 DLNA
投屏弹窗会 SSDP 搜索 `MediaRenderer` 等设备，可复制/分享播放地址到电视/投屏应用。

### Python 源
仓库含 `pyramid/` 与 `IPyLoader` 接口。默认 APK **未打包 Chaquopy Python 运行时**（体积大、需本机 Python 构建）。

启用完整 Python 源解析：

1. 根工程引入 Chaquopy 插件（参考上游 TVBoxOS `classpath "com.chaquo.python:gradle:12.0.1"`）
2. `settings.gradle` 增加 `include ':pyramid'`
3. `app` 依赖 `implementation project(':pyramid')` 并用真 `PythonLoader` 替换 `PyLoaderStub`
4. 使用含 `python` flavor 的构建变体

当前 `.py` 源会安全降级为空 Spider，避免崩溃。
