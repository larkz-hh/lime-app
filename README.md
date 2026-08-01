# Lime

一款仿小红书风格的图文社区 Android 应用 APP。

后端仓库：[lime-server](https://github.com/larkz-hh/lime-server)

---

## 技术栈

| 分类 | 技术 |
|---|---|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose + Material3 |
| 架构模式 | MVVM（ViewModel + Repository + Domain 层） |
| 依赖注入 | Hilt |
| 页面导航 | Navigation Compose |
| 网络请求 | Retrofit2 + OkHttp3 |
| 图片加载 | Coil3 |
| 本地数据库 | Room（含 Paging 支持） |
| 键值存储 | MMKV |
| 分页加载 | Paging3 |
| 视频播放 | Media3 ExoPlayer |
| 相机 | CameraX |
| 机器学习 | ML Kit |
| 图片裁剪 | UCrop |
| 图片缩放 | Telephoto ZoomableImage |
| 拖拽排序 | Reorderable |
| 动画 | Lottie Compose |
| 后台任务 | WorkManager |
| 协程 | Kotlinx Coroutines |
| 序列化 | Kotlinx Serialization |
| 权限 | Accompanist Permissions |
| 最低 SDK | 26（Android 8.0） |
| 目标 SDK | 36 |

---

## 施工中......🚜🚧🚧🚧

### 一、基础功能

#### 1. 首页信息流
- [x] 关注 / 发现 Tab
- [x] 瀑布流布局
- [x] 笔记卡片：封面图、标题、作者头像 + 昵称、点赞数
- [x] 下拉刷新
- [x] 上拉加载更多
- [x] 点击卡片进入笔记详情页
- [ ] 关注 Tab 内容

#### 2. 笔记详情
- [x] 图片横向轮播
- [x] 图片全屏预览
- [x] 笔记正文展示
- [x] 作者信息栏
- [x] 点赞 / 收藏交互
- [x] 选中文字操作菜单(复制...)
- [ ] 评论列表展示
- [ ] 评论输入功能

#### 3. 发布功能
- [x] 发布图文笔记
- [x] 图片多选
- [x] 已选图片拖拽排序
- [x] 已选图片删除
- [x] 发布后在个人主页查看
- [ ] 发布时图片裁剪预览

#### 4. 搜索功能
- [x] 首页顶部搜索入口
- [ ] 搜索页面及关键词搜索

#### 5. 个人主页
- [x] 登录 / 注册页面
- [x] Token 本地持久化
- [x] 个人信息展示
- [x] 用户发布的笔记列表
- [x] 收藏内容列表
- [x] 浏览历史记录页面
- [x] 个人资料编辑页面
- [x] 头像上传
- [x] 背景图上传
- [ ] 头像上传裁剪

#### 6. AI 辅助功能
- [ ] 翻译
- [ ] AI 对话

---

### 二、进阶功能

#### 视频笔记
- [ ] 视频笔记展示 / 播放页面

#### 社区互动
- [x] 消息通知页面
- [ ] 评论 / 回复功能
- [ ] 关注 / 取关功能
- [ ] 通知推送

#### 发布体验
- [x] 图片多选
- [x] 拖拽排序
- [x] 删除已选图片
- [ ] 发布时图片裁剪

#### 其他
- [ ] 弱网离线体验优化
- [ ] 页面转场动画优化
- [ ] 启动屏优化

仅供参考，梦到什么加什么

---

## 项目结构

```
app/src/main/java/xyz/larkzhh/lime/
├── data/
│   ├── local/          # TokenStorage（MMKV）
│   ├── network/        # ApiService、AuthInterceptor、网络数据模型
│   └── repository/     # Auth / Note / User 仓储实现
├── di/                 # Hilt 模块（网络、仓储）
├── domain/             # Repository 接口、NoteEventBus
├── navigation/         # NavGraph、BottomBar、Screen 路由定义
├── ui/
│   ├── auth/           # 登录、注册页面
│   ├── components/     # 公共组件
│   ├── detail/         # 笔记详情页
│   ├── home/           # 首页信息流
│   ├── message/        # 消息页
│   ├── profile/        # 个人主页、编辑资料、浏览历史
│   ├── publish/        # 发布笔记、图片选择
│   ├── qrscan/         # QR 码扫描
│   ├── theme/          # 颜色、字体、主题
│   └── video/          # 视频页
└── util/               # 工具函数（Toast、Clipboard、图片保存）
```
