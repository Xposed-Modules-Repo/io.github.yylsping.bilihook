# bili hook

面向哔哩哔哩 Android `7.4.0`（versionCode `7040300`）的 LSPosed 画质解锁与去广告模块，基于 libxposed Modern API 102。

## 功能

- 登录后解锁高画质选项。
- 去除普通开屏广告，同时保留生日开屏。
- 去除首页、搜索结果、视频相关列表和播放器下方的广告。
- 仅处理 `tv.danmaku.bili` 主进程；目标版本不匹配时不会安装业务 Hook。
- 不修改哔哩哔哩 APK，不提供后台服务、网络代理或常驻轮询。

## 兼容性

| 项目 | 要求 |
| --- | --- |
| 目标应用 | 哔哩哔哩 Android 7.4.0（7040300） |
| Android | 8.0（API 26）及以上 |
| 框架 | 支持 libxposed Modern API 102 的 LSPosed |
| 模块版本 | 1.5.0（versionCode 10） |

该模块依赖目标应用的内部类与方法签名，不保证兼容其他哔哩哔哩版本。

## 安装

1. 从 GitHub Releases 下载并安装 APK。
2. 在 LSPosed 中启用模块，作用域只选择“哔哩哔哩”。
3. 强制停止哔哩哔哩后重新打开。

升级模块后通常无需重启设备。需要回滚时，在 LSPosed 中停用模块并重启目标应用，或直接卸载模块。

## 构建

需要 JDK 17、Android SDK 36，并可从 Maven Central 获取 `io.github.libxposed:api:102.0.0`。

```powershell
.\gradlew.bat clean assembleRelease
```

维护者本地的正式构建使用本项目专属发布密钥：

- 密钥：`signing-private/release.p12`
- 凭据：`signing-private/signing.properties`

这两个文件均已被 `.gitignore` 排除，不会提交到 GitHub。存在本地签名配置时，`assembleRelease` 会直接生成正式签名 APK；没有私钥的仓库克隆仍可使用 `assembleDebug` 生成测试包。

发布密钥决定 Android 能否覆盖升级。请加密备份整个 `signing-private/` 目录，切勿删除、重新生成或提交其中内容。由于本项目此前的 APK 使用测试签名，首次切换到该发布密钥时需要先卸载旧版；此后的正式版本可以直接覆盖升级。

## 相关项目

- [BiliPartFix](https://github.com/yylsping/bili-part-fix)：同样面向哔哩哔哩 7.4.0，负责旧客户端的分 P 播放、动态评论、图文评论、EVA3/Opus 专栏和小站图文等兼容性修复。
`bili hook` 与 `BiliPartFix` 可以分别安装：前者侧重画质解锁与去广告，后者侧重旧版客户端兼容修复。两者均严格限定哔哩哔哩 7.4.0。

## 许可证

本项目采用 [MIT License](LICENSE)。

## 免责声明

本项目仅供学习、研究和个人设备使用，与哔哩哔哩及 LSPosed 项目无隶属或认可关系。使用前请确认符合当地法律及相关服务条款。
