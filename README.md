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
