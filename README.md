# FcmCommon

FcmCommon 是一个面向 HyperOS / MIUI 场景的 LibXposed 模块配置应用，用来集中管理 FCM 相关修复、托管应用列表和模块状态诊断。

项目当前使用：

- Kotlin 2.2
- Jetpack Compose Material 3
- Android SDK 36
- LibXposed API 101

## 主要能力

- 统一管理推送修复开关，包括广播解限、GMS 重连调优、电量策略旁路等。
- 扫描设备上的应用，识别可能使用 FCM 的候选应用。
- 维护托管应用，让目标应用进入 FCM 修复链路。
- 查看模块连接状态、作用域状态、最近连接事件和诊断信息。
- 支持二级 / 三级页面切换和模块状态诊断。

## 页面说明

- `总览`：查看模块是否已连接、已启用功能数量、托管应用数量和最近一次扫描结果。
- `应用`：搜索应用、查看 FCM 候选、管理托管应用、手动重新扫描。
- `设置`：调整外观、应用列表展示偏好，并进入功能配置和模块状态页面。
- `模块状态`：查看当前配置、作用域状态、连接事件和模块日志入口。

## 构建

1. 安装 Android Studio 最新稳定版，确保本地 JDK 为 17。
2. 如需签名发布包，在项目根目录创建 `keystore.properties`。
3. 执行：

```powershell
.\gradlew.bat assembleDebug
```

构建正式版 APK：

```powershell
.\gradlew.bat assembleRelease
```

如果已经配置好 `keystore.properties`，这里产出的会是已签名的正式版 APK。

如需运行单元测试：

```powershell
.\gradlew.bat testDebugUnitTest
```
