# FcmCommon

FcmCommon 是一个面向 HyperOS / MIUI 场景的 LibXposed 模块配置应用，用来集中管理 FCM 相关修复、托管应用列表和模块状态诊断。

## 技术栈

- Kotlin 2.3.20
- Jetpack Compose + MIUIX 0.9.0
- Navigation 3
- Android SDK 37（最低 Android 12 / API 31）
- LibXposed API 101

## 主要能力

- 统一管理推送修复开关，包括广播解限、GMS 重连调优、电量策略旁路等。
- 扫描设备上的应用，识别可能使用 FCM 的候选应用。
- 维护托管应用，让目标应用进入 FCM 修复链路。
- 查看模块连接状态、作用域状态、最近连接事件和诊断信息。
- 支持二级 / 三级页面切换和模块状态诊断。

## 页面说明

- `总览`：查看模块服务、配置同步状态，以及功能、托管应用和扫描指标。
- `应用`：搜索应用、查看 FCM 候选、管理托管应用；筛选规则集中在“设置 > 应用偏好”。
- `设置`：切换跟随系统、浅色或深色主题，调整应用列表偏好，并进入功能配置和模块状态页面。
- `模块状态`：查看当前配置、作用域状态、连接事件和模块日志入口。

## 构建

1. 安装 Android Studio 最新稳定版，使用 JDK 17 或更高版本。
2. 如需签名发布包，在项目根目录创建 `keystore.properties`。
3. 执行：

```powershell
.\gradlew.bat :app:assembleDebug
```

调试 APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

构建正式版 APK：

```powershell
.\gradlew.bat :app:assembleRelease
```

正式版默认启用 R8 代码压缩和资源裁剪。配置 `keystore.properties` 后，APK 会使用发布密钥签名。

如需运行单元测试：

```powershell
.\gradlew.bat :app:testDebugUnitTest
```
