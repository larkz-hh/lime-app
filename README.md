# Lime

<div align="center">

![Language](https://img.shields.io/badge/language-Kotlin-blue)
![Android](https://img.shields.io/badge/platform-Android%2026+-green)
![Architecture](https://img.shields.io/badge/architecture-MVVM-purple)
![Version](https://img.shields.io/badge/version-1.0-blue)
![UI](https://img.shields.io/badge/UI-Compose%20%2B%20M3-orange)
![Build](https://img.shields.io/badge/build-Gradle%20KTS-yellow)
![CI](https://img.shields.io/github/actions/workflow/status/larkz-hh/lime-app/ci.yml?branch=main&label=CI)
![Stars](https://img.shields.io/github/stars/larkz-hh/lime-app)
![Forks](https://img.shields.io/github/forks/larkz-hh/lime-app)
![Last Commit](https://img.shields.io/github/last-commit/larkz-hh/lime-app)
![PRs](https://img.shields.io/badge/PRs-welcome-brightgreen)
![License](https://img.shields.io/badge/license-Apache_2.0-blue)

基于 Kotlin + Jetpack Compose 的仿小红书应用，实现双列瀑布流、视频弹幕、AI 对话、即时通讯等。

[快速开始](#-快速开始) • [技术栈](#tech) • [项目展示](#-项目展示) • [贡献](#-贡献指南)

</div>

---

## 📱 项目介绍

**Lime** 是一个 Kotlin + Jetpack Compose 编写的图文/视频社区 Android 应用，集成自建 REST 后端与腾讯云 IM，实现瀑布流信息流、视频弹幕互动、AI 对话与写作等社区功能。项目按 `ui / domain / data` 分层组织，主界面采用单 Activity 导航。

### ✨ 核心特性

- 🏠 **双列瀑布流信息流** - 图文 / 视频统一卡片，Paging3 分页 + 封面滑动预加载
- 📹 **竖屏视频流** - Media3 ExoPlayer 自动连播、全屏 / PiP 画中画、实时弹幕
- 🤖 **AI 对话与写作** - SSE 流式输出 + 增量 Markdown 渲染，支持图片配文、润色、续写
- 🌐 **中英离线翻译** - ML Kit 本地词典，语言包后台预下载
- 💬 **腾讯云 IM** - 文本 / 图片私信、群聊、未读同步
- 🔔 **通知与桌面小组件** - SSE 实时未读、系统通知、桌面小组件（Glance）

---

## 📸 项目展示

| 双列瀑布流首页 | 视频 + 弹幕 | 笔记详情 |
|:---:|:---:|:---:|
| ![首页](docs/screenshots/feed.png) | ![视频](docs/screenshots/video_1.png) | ![详情](docs/screenshots/detail.png) |

| AI 对话 |                  AI 写作                   | 离线翻译 |
|:---:|:----------------------------------------:|:---:|
| ![AI 对话](docs/screenshots/ai_1.png) | ![AI 写作](docs/screenshots/publish_3.png) | ![翻译](docs/screenshots/translate_1.png) |

| 个人主页 | 私信聊天 | 消息与通知 |
|:---:|:---:|:---:|
| ![个人主页](docs/screenshots/profile_1.png) | ![私信](docs/screenshots/im.png) | ![消息通知](docs/screenshots/message_1.png) |

| 深色模式 | 扫码加好友 | 桌面小组件 |
|:---:|:---:|:---:|
| ![深色模式](docs/screenshots/night.png) | ![扫码](docs/screenshots/qr_scan.png) | ![小组件](docs/screenshots/widget.png) |

**视频全屏（横屏沉浸）**

![视频全屏](docs/screenshots/video_3.png)

更多界面？→ [📷 完整截图库](docs/screenshots-gallery.md)

---

## 🗺️ 功能全景

### 🏠 首页与信息流

- 发现 / 关注两个信息流，顶部 Tab 切换
- 双列瀑布流，卡片高度自适应
- 下拉刷新、上拉加载更多（Paging3）
- 滑动时预加载后续封面图

### 📄 笔记详情

- 多图横向轮播，Telephoto 手势全屏缩放预览
- 点赞、收藏、评论，评论可回复
- 选中文字或全文翻译（ML Kit 离线词典 + AI 在线）
- 作者可编辑或删除笔记

### ✍️ 发布

- 发布图文 / 视频笔记
- 图片多选后长按拖拽排序（Reorderable）
- 发布页内置 AI 帮写辅助：配文、润色、续写、精简、起标题
- 草稿保存、续写、删除

### 📹 视频

- 竖屏沉浸式信息流，Media3 ExoPlayer 自动连播（可关闭）
- 全屏观看：左右滑动调亮度 / 音量，进度条拖动，倍速播放
- 清屏播放、画中画小窗、后台继续播放
- 实时弹幕：发送、开关、不透明度调节

### 🔍 搜索

- 搜索笔记与用户
- 输入联想补全、热搜词榜
- 笔记结果可排序（综合 / 最新 / 点赞 / 评论 / 收藏），按类型与时间筛选

### 👥 社区与社交

- 关注 / 取关，查看关注与粉丝列表
- 群聊：创建（邀请好友）、资料编辑、解散 / 退出
- 扫码加好友与二维码名片

### 🤖 AI

- 对话：SSE 流式输出，Markdown 渲染，支持停止、重试、重新生成
- 联网搜索开关，多模型切换
- 可引用笔记、发送图片提问
- 回复可朗读，选取文字时提供剥离 Markdown 的纯文本
- 写作：看图配文、起标题、润色、续写、精简

### 🌐 翻译

- 中英互译：AI 在线优先，离线词典兜底
- 语言包后台预下载，可手动管理

### 💬 即时通讯（腾讯云 IM）

- 私信：文本与图片消息
- 群聊：群消息、群资料编辑
- 会话列表与未读标记

### 🔔 消息与通知

- 点赞、关注、评论等互动通知
- SSE 实时同步未读
- 系统通知与桌面角标（ShortcutBadger）
- 异地登录强制下线提醒

### 👤 个人中心

- 密码 / 验证码登录注册，Token 自动续期
- 资料编辑
- 主页分区：笔记 / 点赞 / 收藏
- 浏览历史、关注与粉丝列表

### 🧩 小组件与设置

- 桌面搜索小组件
- 主题色、深色模式、字体与多语言
- 应用内更新检查

---
<a id="tech"></a>
## 🛠️ 技术栈

| 分类 | 技术 |
| --- | --- |
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 底部导航 | Exyte AnimatedNavigationBar |
| 架构 | MVVM（ViewModel + Repository + Domain 三层） |
| 依赖注入 | Hilt |
| 导航 | Navigation Compose（主界面单 Activity，全屏视频等独立 Activity） |
| 网络 | Retrofit2 + OkHttp3（SSE 流式） |
| 图片加载 | Coil3 |
| 本地数据库 | Room（按账号分库，Paging3 分页） |
| 键值存储 | MMKV |
| 视频播放 | Media3 ExoPlayer（含 PiP） |
| 相机 / ML | CameraX / ML Kit |
| 图片处理 | UCrop 裁剪 / Telephoto 缩放 / Reorderable 拖拽 |
| 动画 | Lottie Compose |
| 即时通讯 | 腾讯云 IM（imsdk-plus） |
| 桌面小组件 | Glance + ShortcutBadger |
| 后台任务 | WorkManager |
| 异步 | Kotlinx Coroutines + Flow |
| 序列化 | Gson（网络 / 本地缓存 / SSE 解析） |
| 权限 | Accompanist Permissions |
| 最低 SDK | 26（Android 8.0） |
| 目标 SDK | 36 |

---

## 🏗️ 架构设计

### 分层与模块

```
app/src/main/java/xyz/larkzhh/lime/
├── data/        # 数据层：Room 分库、网络接口与模型、仓储、IM、通知中心
├── domain/      # 领域层：仓储接口、领域模型、事件总线
├── di/          # Hilt 依赖注入
├── navigation/  # 导航：路由、图、状态
├── ui/          # 页面层：按功能分包，各功能内含 Screen 与 ViewModel
├── util/        # 通用工具：cache / media / system / text
└── work/        # WorkManager 后台任务
```

### 设计要点

- **主界面单 Activity + Navigation Compose**（启动页 `SplashActivity`、全屏视频/PiP `VideoActivity` 等为独立 Activity），代码按 `ui / domain / data` 三层分包：`ui` 页面层（Screen + ViewModel）→ `domain` 领域层（仓储接口 / 领域模型）→ `data` 数据层（仓储实现 / 网络 / 本地库）；
- **按功能分包**：网络接口按功能拆分为独立 `*Api`，数据仓储与本地库按功能对齐；
- **多账号隔离**：Room 各库按当前账号分库，登出保留各账号库文件，登录态切换自动重建界面；
- **性能工程**：独立 `:baselineprofile` 模块生成启动与关键路径 Baseline Profile，随 release 自动打包。

---

## 🚀 快速开始

### 前置要求

1. **Android Studio**（建议最新稳定版）
2. **JDK 17+**（随项目 wrapper 管理）
3. **后端服务**：[lime-server](https://github.com/larkz-hh/lime-server) 部署并启动（接口地址见下）

### 编译步骤

#### 1️⃣ 克隆项目

```bash
git clone https://github.com/larkz-hh/lime-app.git
cd lime-app
```

#### 2️⃣ 配置后端地址

在项目根目录 `local.properties` 中配置（构建时注入 `BuildConfig.API_BASE_URL`，未配置则用默认地址）：

```properties
BASE_URL=http://<your-server>:8080/
```

> 地址在编译时打进 APK，修改后需重新构建。

#### 3️⃣ 编译运行

```bash
# 编译并安装 Debug 测试包
./gradlew :app:installDebug

# 只编译不安装
./gradlew :app:assembleDebug

# 打 release（无正式签名时使用 debug 签名）
# -PslimAbi=true 开启 ABI 裁剪，只出 arm64-v8a（默认全 ABI）
./gradlew :app:assembleRelease -PslimAbi=true
```

或直接用 Android Studio：打开项目 → Run ▶。

> 依赖仓库默认走阿里云镜像；海外网络环境构建可设置环境变量 `LIME_USE_ALIYUN=false`。

### 权限声明

应用需要以下权限（已在 `AndroidManifest.xml` 中声明）：

```xml
<uses-permission android:name="android.permission.INTERNET" />                              <!-- 网络 -->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />                  <!-- 网络状态 -->
<uses-permission android:name="android.permission.CAMERA" />                               <!-- 拍照发布 -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />                         <!-- 录音 -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />                    <!-- 系统通知 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />                   <!-- 前台服务 -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />    <!-- 视频后台播放 -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />             <!-- 应用内更新安装 -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />                    <!-- 相册图片 -->
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />                     <!-- 相册视频 -->
```

---

## 💡 技术亮点

### 1. AI 对话：SSE 流式 + 增量 Markdown 渲染

- 回复经 SSE 逐字推送，增量写入 Room，页面订阅数据库 Flow 渲染
- 按块渲染：已闭合的段落 / 代码块不再重复解析；未闭合内容先纯文本，结束后按 Markdown 格式化

```kotlin
// ChatMessageBubble.kt（节选）
when {
    data.renderMarkdown ->
        StreamingMarkdown(
            content = data.content,
            renderTailAsMarkdown = data.status != ChatBubbleStatus.STREAMING,
        )
}
```

### 2. 弹幕：轨道分配与碰撞检测

- 弹幕按轨道分配，做碰撞检测
- 记录发送时的播放进度，暂停 / 拖动进度条后按播放时钟对齐

### 3. 视频：播放池、后台与小窗

- ExoPlayer 实例由播放池复用，上下滑不重复创建
- 前后台切换自动暂停 / 续播
- 全屏走独立 Activity：画中画小窗、后台音频、清屏播放
- 全屏左右滑动调亮度 / 音量，支持拖进度、切倍速

### 4. 图片管线：缓存 + 视频帧取封面

- 自定义 Coil ImageLoader：内存缓存 25%、磁盘缓存 256MB
- 注册 `VideoFrameDecoder`，封面直接按时间戳取视频帧，无需单独生成封面图

### 5. 多账号：Room 按账号分库

```kotlin
// UserDatabases.kt（节选）
val name = if (uid != null) "${prefix}_$uid" else prefix
val db = builder("$name.db")
```

- 登出保留各账号库文件，切换账号打开对应库，账号间数据互不可见

### 6. 弱网与离线可用

- 断网提示并拦截发送
- 首页 / 详情 / 用户数据本地缓存，先读缓存再后台刷新
- 恢复联网后补齐分页与消息，网络层带自动重试

### 7. 通知链路：SSE → 系统通知 → 角标

- SSE 长连接接收互动增量，驱动站内通知与未读
- 前台服务保活，后台也能收到 IM 与通知
- 异地登录 / 被踢自动登出，ShortcutBadger 同步桌面角标

### 8. 体积与启动优化

- R8 混淆 + 资源压缩（`isMinifyEnabled` + `shrinkResources`）
- ABI 裁剪：`-PslimAbi=true` 仅保留 arm64-v8a
- Baseline Profile 随 release 打包，覆盖冷启动 / 首页 / 详情热路径

---
## 🔀 核心工作流

### 1. 应用启动流程

```
SplashActivity（Lottie 启动动画，可在设置中关闭）
  └─> MainActivity → AppNavGraph
        ├─> 已登录 → Home（首页双信息流）
        └─> 未登录 → 跳转 Login

首页：Paging3 分页拉取信息流 → Room 缓存 → 双列瀑布流
```

### 2. AI 对话流程

```
发送消息
  └─> ChatSendEngine
      ├─> （含图则先上传图片）
      ├─> SSE 订阅 /api/ai/chat
      │    ├─> Delta → 约 120ms 节流写 Room → 页面按块增量渲染
      │    └─> Done → 落库 DONE → 尾巴按 Markdown 格式化
      └─> 可停止（断开 SSE）/ 重新生成（复用前置用户消息）
```

### 3. 发布笔记流程

```
选图 / 拍摄 / 选视频 → 拖拽排序 / 裁剪封面（UCrop）
  ├─> 可选：发布页 AI 帮写（配文 / 润色 / 续写 / 精简 / 标题）
  ├─> 上传图片 / 视频 → 提交后端
  └─> 中途可存草稿，稍后继续编辑
```

---

## 📂 关键类说明

### LimeApplication (`LimeApplication.kt`)
**职责**：应用入口，Hilt 容器

- 自定义 Coil `ImageLoader`：内存缓存 25%、磁盘 256MB，注册 `VideoFrameDecoder` 视频帧解码
- Activity 前后台回调通知播放器暂停 / 续播
- 首启安排翻译语言包预下载、小组件热搜刷新

### ChatSendEngine (`data/repository/ai/ChatSendEngine.kt`)
**职责**：AI 对话发送引擎，收口一次会话的完整状态

- 上传图片 → 订阅 SSE `/api/ai/chat`
- 约 120ms 节流把增量写 Room，完成时写 `DONE`，停止写 `STOPPED`，失败写 `FAILED`
- 界面只订阅 Room Flow，不直接依赖网络流

### UserDatabases (`data/local/UserDatabases.kt`)
**职责**：多账号 Room 分库提供者

- 库名按账号拼：`lime_chat_$uid` 等
- 登出保留各账号库文件，切换账号重新打开对应库

### AppUpdater (`ui/about/AppUpdater.kt`)
**职责**：应用内检查更新与下载安装

- 读取 GitHub 最新 Release，tag 语义化版本比较
- 按 `universal` / `arm64` 资产命名选择下载包
- 接系统 `DownloadManager`，下载完成拉起安装页

### VideoPlayerPool (`ui/video/player/VideoPlayerPool.kt`)
**职责**：ExoPlayer 实例池

- 上下滑复用播放器实例，不重复创建
- 配合前后台回调与独立 `VideoActivity`，支撑连播、PiP 与后台音频

### TtsManager (`util/TtsManager.kt`)
**职责**：AI 回复朗读

- 朗读前剥离 Markdown 符号，全局单例管理播放与停止

---
## 🔀 核心数据流（AI 对话）

```
输入消息
  └─> ChatSendEngine.send()
      ├─> 图片上传（若含图）
      ├─> SSE 订阅 /api/ai/chat
      │    ├─> Delta 事件 → 增量写入 Room（约 120ms 节流）
      │    │      └─> 页面 observe Room Flow → StreamingMarkdown 增量渲染
      │    └─> Done 事件 → 消息落库 DONE → 尾巴格式化 + 操作行
      └─> 支持 Stop（断开 SSE）/ Regenerate（复用前置用户消息重生成）
```

---

## 🎯 使用场景

Lime 是一款图文 / 视频社区 App，展示基于 Kotlin + Jetpack Compose 的内容社区实现。

可应用于以下场景（需按需扩展）：

- 💬 **内容社区参考** - 信息流、发布、视频弹幕、IM、通知的完整链路
- 📱 **工程实践示例** - Compose + 三层分层 + 多账号分库 + 离线可用
- 🤖 **多能力整合** - AI 对话写作、离线翻译与即时通讯在同一客户端落地

---
## 🗺️ Roadmap

- [x] 基础工程与三层架构搭建
- [x] 图文 / 视频发布与信息流
- [x] 视频播放（连播 / 全屏 / PiP / 弹幕 / 倍速）
- [x] 离线翻译 + 语言包预下载
- [x] AI 写作工具箱
- [x] AI 对话（流式 / Markdown / 多模型 / 停止重试 / 联网搜索）
- [x] 桌面小组件
- [x] 弱网离线优化（弱网提示 / 首页、详情、用户数据缓存）
- [x] 腾讯云 IM 私信
- [x] SSE 通知、桌面角标与前台保活
- [x] 多账号分库隔离
- [x] Baseline Profile 启动优化（含 ABI 裁剪）
- [ ] 多模块架构
- [ ] Hero 共享元素转场
- [ ] 封装发布通用组件库
- [ ] 进一步性能优化
- [ ] 单元测试与集成测试覆盖

---

## 🤝 贡献指南

欢迎 Issue、PR 和讨论！

### 提交 Issue
- **Bug 报告**：描述现象、复现步骤、期望结果、实际结果、日志
- **功能建议**：说明使用场景、期望效果、参考方案

### 提交 PR
1. Fork 本仓库
2. 创建特性分支：`git checkout -b feature/YourFeature`
3. 提交更改（Conventional Commits，如 `fix(chat): …`、`feat(video): …`、`perf: …`）
4. 推送并开启 Pull Request，描述改动内容

---

## 📄 许可证

本项目采用 **Apache License 2.0** 开源协议，详见 [LICENSE](./LICENSE) 文件。

```text
Copyright 2026 larkz-hh

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
...
```

---

## 📞 联系方式

- 📧 **Issues**：[GitHub Issues](https://github.com/larkz-hh/lime-app/issues)
- 👤 **作者**：[@larkz-hh](https://github.com/larkz-hh)

---

## 📓 开发日记

记录本项目开发过程中的日志、开发思路与技术选型，见 [`docs/notes/`](docs/notes/)。

---

## 🙏 致谢

- [Jetpack Compose / AndroidX](https://developer.android.com/jetpack) - 现代 UI 与系统组件
- [Coil](https://coil-kt.github.io/coil/) - 图片加载
- [Retrofit / OkHttp](https://square.github.io/okhttp/) - 网络与 SSE 流式
- [Media3 ExoPlayer](https://developer.android.com/media/media3) - 视频播放
- [腾讯云 IM](https://cloud.tencent.com/product/im) - 即时通讯
- [Mikepenz Markdown](https://github.com/mikepenz/markdown) - Compose Markdown 渲染
- [Telephoto](https://github.com/saket/telephoto) / [UCrop](https://github.com/Yalantis/uCrop) - 图片缩放与裁剪
- [Lottie](https://airbnb.design/lottie/) - 矢量动画
- [Exyte AnimatedNavigationBar](https://github.com/exyte/AnimatedNavigationBar) - 底部导航栏动画
- [阿里巴巴矢量图标库 iconfont](https://www.iconfont.cn/) - 界面图标素材
- [LottieFiles](https://lottiefiles.com/) - Lottie 动画素材

---

<div align="center">

[⬆ 回到顶部](#lime)

**⭐ 如果觉得项目有帮助，请给个 Star！**

Made with ❤️ by [larkz-hh](https://github.com/larkz-hh)

</div>
