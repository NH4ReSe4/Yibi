# 一笔 · YiBi

一笔是一款面向个人使用的 Android 本地记账应用。界面与语音交互均为简体中文，所有账目统一以欧元记录。

## 功能

- 手动记账与普通话语音记账
- 语音识别由 Android 系统在线语音服务完成，并针对记账数字同音进行纠错
- 中文金额、相对日期、分类和中英文地点文本解析
- 日、周、月报表与分类占比
- 固定开销、订阅、日常消费和投资四级支出结构
- 自定义账单截止日与周期预算
- 固定开销和订阅按到期日自动写入流水，并按月折算联动预算预留
- JSON 完整备份、恢复及 CSV 导出
- 通过 GitHub Release 检查、下载和安装新版 APK

账目、预算和周期模板仅保存在手机本地。应用不会上传账目，也不会保存原始录音；使用语音记账时，录音会交给手机上配置的系统在线语音服务转写。

## 环境要求

- Android Studio 或 JDK 17+
- Android SDK 35
- Android 8.0（API 26）或更高版本
- 已针对 Android 14 与 Pixel 9a 的 393dp 布局验证

## 本地构建

在 Android Studio 中打开项目并运行 `app`，或在 Windows PowerShell 中执行：

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Debug APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 调试

- Compose 预览位于 `app/src/debug/java/com/dubiao/yibi/ui/DebugPreviews.kt`。
- Debug 包的设置页包含开发者工具，可生成演示数据并测试语音文本解析。
- Logcat 可使用 `YiBiDebug` 标签过滤。
- 开发者工具由 `BuildConfig.DEBUG` 控制，不会进入 Release 界面。

## 在线更新

应用不会自动联网检查。只有用户在设置页点击“检查更新”后，才会从以下固定地址读取最新版本信息：

```text
https://github.com/NH4ReSe4/Yibi/releases/latest/download/update.json
```

Release 必须包含 `update.json` 和对应的已签名 APK。应用会比较 `versionCode`、显示更新说明、下载 APK 并校验 SHA-256；最终安装仍由 Android 系统确认。

创建以 `v` 开头的标签会触发 GitHub Actions 发布流程，例如 `v1.2`。仓库需要配置以下 Actions Secrets：

- `YIBI_KEYSTORE_BASE64`
- `YIBI_KEYSTORE_PASSWORD`
- `YIBI_KEY_ALIAS`
- `YIBI_KEY_PASSWORD`

发布签名文件及密码不得提交到仓库。用户从 Debug 签名切换到正式签名时，需要先备份账目、卸载 Debug 版、安装正式版，再恢复备份。

## 许可证

本项目使用 [Apache License 2.0](LICENSE)。
