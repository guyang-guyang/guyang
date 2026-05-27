# 🌳 顾阳软件分发平台 - 全量代码逻辑树

> ⚠️ **安全警告**：您在聊天中泄露了服务器 root 密码 `hzy522625HZY`。请立即登录服务器执行 `passwd root` 更换密码！此密码可能已被记录到 AI 服务端日志中。
>
> 另：服务器 `47.108.209.71` 硬编码在客户端源码中，任何人反编译APK即可获得此IP。建议使用域名 + Cloudflare CDN隐藏真实IP。

---

## 一、项目整体架构

```
顾阳软件分发平台源码/
│
├── 📱 顾阳分发平台/          ← 用户端 Android App (com.guyang.app)
│   ├── build.gradle           ← 根项目构建(AGP 8.4.0)
│   ├── settings.gradle        ← 模块包含
│   └── app/
│       ├── build.gradle       ← compileSdk 34, minSdk 21, targetSdk 33
│       ├── debug.keystore     ← 硬编码密码 "android" 的签名证书
│       ├── proguard-rules.pro ← 空文件，未配置任何规则
│       └── src/main/
│           ├── AndroidManifest.xml
│           ├── java/com/guyang/app/
│           │   ├── ApiClient.java              ← 网络层核心
│           │   ├── AuthHelper.java             ← 认证门面(空壳)
│           │   ├── MainActivity.java           ← 启动页(版本检查)
│           │   ├── LoginActivity.java          ← 登录页
│           │   ├── RegisterActivity.java       ← 注册页
│           │   ├── HomeActivity.java           ← 主页(4Tab导航)
│           │   ├── AppBaseAdapter.java         ← 应用列表适配器
│           │   ├── AppDetailActivity.java      ← 应用详情
│           │   ├── InfoBaseAdapter.java        ← 资讯列表适配器
│           │   ├── InfoDetailActivity.java     ← 资讯详情(含购买)
│           │   ├── InAppDownloadManager.java   ← 下载引擎(断点续传)
│           │   ├── InAppDownloadActivity.java  ← 下载页面UI
│           │   ├── DownloadListActivity.java   ← 下载列表(空壳)
│           │   └── DownloadListAdapter.java    ← 下载列表适配器
│           └── res/                            ← 布局/资源文件
│
├── 🖥️ 顾阳管理后台/          ← 管理员端 Android App (com.guyang.admin)
│   ├── build.gradle           ← 根项目构建(AGP 7.4.2)
│   ├── settings.gradle
│   └── app/
│       ├── build.gradle       ← compileSdk 30, minSdk 21, targetSdk 30
│       └── src/main/
│           └── java/com/guyang/admin/
│               ├── ApiClient.java            ← 网络层核心(含文件上传)
│               ├── LoginActivity.java        ← 管理员登录
│               ├── DashboardActivity.java    ← 首页看板
│               ├── AppsActivity.java         ← 应用管理(CRUD)
│               ├── UploadActivity.java       ← 通用文件上传
│               ├── ApkUploadActivity.java    ← APK发布(上传+创建)
│               ├── UsersActivity.java        ← 用户管理(积分调整)
│               ├── CardsActivity.java        ← 卡密管理(批量生成)
│               ├── InfoActivity.java         ← 资讯管理(CRUD)
│               ├── BannersActivity.java      ← 轮播图设置
│               ├── ImagesActivity.java       ← 图片库(复制直链)
│               ├── SettingsActivity.java     ← 系统配置(含APK上传)
│               ├── PasswordActivity.java     ← 修改管理员密码
│               └── AboutActivity.java        ← 关于页面
│
└── 🌐 后端服务器 (47.108.209.71:80)
    └── /backend/api/          ← PHP后端接口
        ├── /user/
        │   ├── login.php       ← 登录认证
        │   ├── register.php    ← 用户注册
        │   ├── info.php        ← 用户信息
        │   └── checkin.php     ← 每日签到
        ├── /app/
        │   ├── apps.php         ← 应用列表
        │   ├── detail.php       ← 应用详情
        │   ├── download.php     ← 下载记录
        │   ├── update.php       ← 版本更新检查
        │   └── banners.php      ← 轮播图
        ├── /info/
        │   ├── list.php         ← 资讯列表
        │   ├── detail.php       ← 资讯详情
        │   └── buy.php          ← 购买资讯
        ├── /points/
        │   ├── log.php          ← 积分记录
        │   └── redeem.php       ← 卡密兑换
        ├── /admin/
        │   ├── login.php        ← 管理员登录
        │   ├── password.php     ← 修改密码
        │   ├── stats.php        ← 统计看板
        │   ├── apps.php         ← 应用CRUD
        │   ├── users.php        ← 用户管理
        │   ├── cards.php        ← 卡密CRUD
        │   ├── info.php         ← 资讯CRUD
        │   ├── banners.php      ← 轮播设置
        │   ├── images.php       ← 图片列表
        │   ├── upload.php       ← 文件上传
        │   └── system_config.php ← 系统配置
        └── /uploads/            ← 上传文件存储目录
```

---

## 二、数据流图 (Data Flow)

```
┌─────────────────────────────────────────────────────────────┐
│                    用户端 App 启动流程                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  App启动 → MainActivity                                     │
│      │                                                      │
│      ├─① 读取 SharedPreferences("app_prefs").token          │
│      │   ├─ token为空 → LoginActivity                       │
│      │   └─ token存在 → checkUpdate()                       │
│      │                                                      │
│      └─② checkUpdate() [在线程中]                            │
│             │                                                │
│             ├─ GET /app/update.php?version=X&version_code=Y │
│             │                                                │
│             ├─ has_update=true → 弹窗提示                     │
│             │   ├─ force=true → 必须更新(不可取消)            │
│             │   └─ force=false → 可选择"稍后再说"             │
│             │                                                │
│             └─ 无论结果 → goHome()                           │
│                    └─ token为空? LoginActivity : HomeActivity│
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    登录/注册数据流                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  LoginActivity                                              │
│       │                                                     │
│       ├─ 用户输入 username + password                        │
│       │                                                     │
│       ├─ ApiClient.login(un, pw)                            │
│       │   └─ POST http://47.108.209.71/backend/api/         │
│       │      user/login.php  (明文JSON{username,password})   │
│       │                                                     │
│       └─ 响应 {code:0, data:{token, user:{avatar}}}        │
│              ├─ 存储 token → SharedPreferences("app_prefs") │
│              ├─ 下载头像 → getFilesDir()/avatar.png         │
│              └─ 跳转 → HomeActivity                         │
│                                                             │
│  RegisterActivity                                           │
│       │                                                     │
│       ├─ 收集: username, password, qq, invite_code          │
│       │           device_model, device_sdk                  │
│       │                                                     │
│       ├─ 客户端校验:                                         │
│       │   • 用户名 2-20位                                    │
│       │   • 密码 ≥6位                                        │
│       │   • QQ号 5-11位纯数字                                │
│       │                                                     │
│       └─ ApiClient.register(...)                            │
│              └─ POST .../user/register.php                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                 主页(HomeActivity) 数据流                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  HomeActivity.onCreate()                                    │
│       │                                                     │
│       ├─① ApiClient.setToken(token)  ← 设置全局Authorization│
│       │                                                     │
│       ├─② buildHomePage()   → 首页Tab                       │
│       │     └─ 分类: 软件|工具|网赚|视频  + 搜索栏            │
│       │                                                     │
│       ├─③ buildAppsPage()   → 应用Tab                       │
│       ├─④ buildInfoPage()   → 资讯Tab                       │
│       ├─⑤ buildMePage()     → 我的Tab                       │
│       │     ├─ 每日签到 → POST /user/checkin.php            │
│       │     ├─ 积分记录 → GET /points/log.php                │
│       │     ├─ 下载管理 → DownloadListActivity               │
│       │     ├─ 卡密兑换 → POST /points/redeem.php           │
│       │     ├─ 联系客服 → mqqapi:// (QQ群 854934959)         │
│       │     └─ 退出登录 → 清除token + 跳转登录               │
│       │                                                     │
│       ├─⑥ loadSystemConfig() → GET /admin/system_config.php │
│       │     └─ 获取 recharge_url (充值链接)                  │
│       │                                                     │
│       ├─⑦ loadBanners() → GET /app/banners.php              │
│       │     └─ 轮播4秒自动切换，点击跳转资讯详情               │
│       │                                                     │
│       ├─⑧ loadApps() → GET /app/apps.php?category=X         │
│       │     └─ 填充首页列表 + 应用Tab列表                     │
│       │                                                     │
│       └─⑨ loadInfos() → GET /info/list.php                  │
│             └─ 填充资讯Tab列表                               │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   下载系统数据流                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  用户点击"下载"                                              │
│       │                                                     │
│       ├─ AppBaseAdapter.getItemView()                       │
│       │   └─ Intent → InAppDownloadActivity                 │
│       │     携带: appName, appId, fileName, downloadUrl      │
│       │                                                     │
│       └─ InAppDownloadActivity.onCreate()                   │
│              │                                              │
│              ├─ InAppDownloadManager.startDownload()         │
│              │   └─ ExecutorService(3线程) 执行下载           │
│              │                                              │
│              ├─ executeDownload(task)                        │
│              │   │                                          │
│              │   ├─ 检查 .part 文件 → 断点续传               │
│              │   ├─ Range: bytes=已下载字节数-               │
│              │   │                                          │
│              │   ├─ 循环读取 InputStream                     │
│              │   │   ├─ cancelled? → cleanup() 退出          │
│              │   │   ├─ paused?    → 保存.part, 状态=2       │
│              │   │   └─ 正常 → 写入 .part 文件               │
│              │   │                                          │
│              │   └─ 完成后: .part → .apk 重命名              │
│              │             回调 onComplete(localPath)       │
│              │                                              │
│              └─ 安装: FileProvider.getUriForFile()           │
│                     └─ Intent.ACTION_VIEW → 系统安装器       │
│                                                             │
│  状态机: 0=等待 → 1=下载中 ⇄ 2=暂停 → 3=完成 → 4=错误       │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  资讯详情购买流程                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  InfoDetailActivity.loadFromServer()                        │
│       │                                                     │
│       ├─ GET /info/detail.php?id=X&token=Y                  │
│       │   ⚠️ Token通过URL参数传递!                           │
│       │                                                     │
│       └─ 响应 data: {unlocked, pan_url, price, content}      │
│              │                                              │
│              ├─ price=0 (免费)                               │
│              │   ├─ "获取教程" → Intent.ACTION_VIEW(pan_url) │
│              │   └─ "复制链接" → ClipboardManager            │
│              │                                              │
│              ├─ !unlocked (未购买)                            │
│              │   └─ "购买(X积分)" → POST /info/buy.php       │
│              │       └─ 成功 → reload → unlocked=true        │
│              │                                              │
│              └─ unlocked (已购买)                             │
│                  ├─ "打开教程" → Intent.ACTION_VIEW(pan_url) │
│                  └─ "复制链接" → ClipboardManager            │
│                                                             │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                 管理后台完整操作流                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  LoginActivity (管理员登录)                                  │
│       │                                                     │
│       ├─ 读取 SharedPreferences("admin")                     │
│       │   ├─ token存在 → 直接进入 DashboardActivity          │
│       │   └─ token不存在 → 显示登录表单                      │
│       │                                                     │
│       └─ POST /admin/login.php {username, password}         │
│              └─ 响应 {token, ...}                            │
│                  └─ 存储 token + last_user                   │
│                                                             │
│  DashboardActivity (看板)                                    │
│       │                                                     │
│       └─ GET /admin/stats.php                               │
│              ├─ overview: total_users, new_users_today,      │
│              │            online_apps, online_info           │
│              └─ traffic: total_downloads, downloads_today,   │
│                          total_info_views                    │
│                                                             │
│  AppsActivity (应用管理)                                     │
│       │                                                     │
│       ├─ GET /admin/apps.php → 获取应用列表                  │
│       │                                                     │
│       ├─ 点击 → showEditDialog(JSONObject)                   │
│       │   ├─ 编辑: name, version, size, category, price,     │
│       │   │        desc, browser_url, icon(上传)             │
│       │   └─ PUT /admin/apps.php {id, name, ...}            │
│       │       ⚠️ 结果判断用 r.contains("\"code\":0")         │
│       │                                                     │
│       ├─ 长按 → confirmDelete()                             │
│       │   └─ GET /admin/apps.php?id=X&_method=DELETE         │
│       │       ⚠️ DELETE通过GET+查询参数伪装                   │
│       │                                                     │
│       └─ 上下架 → PUT {id, status:"online"/"offline"}       │
│                                                             │
│  ApkUploadActivity (发布新应用)                              │
│       │                                                     │
│       ├─① 选择APK文件 (ACTION_OPEN_DOCUMENT)                 │
│       ├─② 可选: 选择图标                                     │
│       ├─③ 填写: name, version, category, price, desc, pan   │
│       │                                                     │
│       └─④ uploadApk() [多步骤上传]                           │
│              ├─ 步骤1: 上传图标 → POST /admin/upload.php     │
│              ├─ 步骤2: 上传APK → POST /admin/upload.php      │
│              │   └─ 手动构造 multipart/form-data             │
│              │      ⚠️ boundary固定字符串 "---BOUNDARY"      │
│              └─ 步骤3: 创建应用 → POST /admin/apps.php       │
│                       └─ {name, version, size, file_path,    │
│                           icon, category, price, desc, ...}  │
│                                                             │
│  UsersActivity (用户管理)                                    │
│       │                                                     │
│       ├─ GET /admin/users.php → 用户列表                     │
│       │                                                     │
│       └─ 点击 → 调整积分弹窗                                  │
│              ├─ +N / -N → POST /admin/users.php              │
│              │            {id, points: +N, action:"adjust"}  │
│              └─ 设为0  → POST /admin/users.php               │
│                           {id, points: 0, action:"reset"}    │
│                                                             │
│  CardsActivity (卡密管理)                                    │
│       │                                                     │
│       ├─ GET /admin/cards.php → 卡密列表                     │
│       │                                                     │
│       ├─ 批量生成 → {count, points, expire_days}             │
│       │   └─ POST /admin/cards.php                          │
│       │                                                     │
│       └─ 复制卡密 → ClipboardManager.putString(code)         │
│                                                             │
│  InfoActivity (资讯管理)                                     │
│       │                                                     │
│       ├─ GET /admin/info.php → 资讯列表                      │
│       │                                                     │
│       ├─ 新建/编辑 → {title, cover_img, summary, content,    │
│       │               category, price, pan_url}              │
│       │   ├─ 新建 → POST /admin/info.php                     │
│       │   └─ 编辑 → PUT /admin/info.php                      │
│       │                                                     │
│       └─ 删除 → GET /admin/info.php?id=X&_method=DELETE      │
│                                                             │
│  BannersActivity (轮播设置)                                  │
│       │                                                     │
│       ├─ GET /admin/banners.php → 可用资讯列表 + 当前轮播     │
│       │                                                     │
│       └─ 保存 → POST /admin/banners.php {carousel_ids: [...]}│
│              └─ 最多5条资讯作为轮播                           │
│                                                             │
│  SettingsActivity (系统配置)                                 │
│       │                                                     │
│       ├─ GET /admin/system_config.php → 当前配置             │
│       │   └─ app_name, notice, recharge_url,                 │
│       │      app_version, app_version_code,                  │
│       │      app_apk_url, app_apk_size,                      │
│       │      app_update_log, app_force_update                │
│       │                                                     │
│       ├─ 选择APK → 上传到服务器 → 获取URL填入etApkUrl          │
│       │                                                     │
│       └─ 保存 → POST /admin/system_config.php {全部配置}      │
│                                                             │
│  PasswordActivity (修改密码)                                 │
│       │                                                     │
│       └─ POST /admin/password.php {old_password, new_password} │
│                                                             │
│  ImagesActivity (图片库)                                     │
│       │                                                     │
│       ├─ GET /admin/images.php → 图片列表                    │
│       └─ 点击 → 复制直链到剪贴板                              │
│                                                             │
│  UploadActivity (通用上传)                                   │
│       │                                                     │
│       ├─ 选择任意文件                                         │
│       └─ POST /admin/upload.php → 返回URL                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、组件关系图 (Component Dependencies)

```
┌──────────────────────────────────────────────────────────────┐
│                    用户端组件依赖关系                          │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ApiClient (单例静态类) ←────────── 所有Activity依赖          │
│    │ static String S = "http://47.108.209.71"                │
│    │ static String token                                     │
│    │                                                         │
│    ├── req(method, url, body)     ← HTTP核心                  │
│    │   └── Authorization: Bearer {token}                     │
│    │                                                         │
│    ├── get(url) / post(url, json) ← 公开方法                 │
│    ├── is401(resp, ctx)           ← 401处理                  │
│    │   └── Toast + 清除token + 跳转Login                     │
│    │                                                         │
│    ├── login/register/checkin/userInfo    ← 用户认证          │
│    ├── apps/appDetail/downloadApp/banners  ← 应用相关         │
│    ├── infoList/infoDetail/infoBuy         ← 资讯相关         │
│    ├── redeemCard/update                   ← 积分/更新        │
│    │                                                         │
│    └── ⚠️ infoDetail() 使用URL传递token                      │
│                                                              │
│  AuthHelper (冗余门面)                                       │
│    └── 仅转发调用到 ApiClient，无附加逻辑                     │
│                                                              │
│  InAppDownloadManager (单例，线程池)                          │
│    │ ExecutorService(3线程)                                  │
│    │ ConcurrentHashMap<String, DownloadTask>                 │
│    │ ConcurrentHashMap<String, DownloadListener>             │
│    │                                                         │
│    ├── startDownload(url,name,appName,appId) → taskId        │
│    ├── pauseDownload(taskId) / resumeDownload(taskId)        │
│    ├── cancelDownload(taskId)                                │
│    ├── installApk(taskId) → FileProvider                    │
│    │                                                         │
│    └── DownloadListener(接口)                                │
│         ├── onProgress(taskId, downloaded, total, percent)   │
│         ├── onComplete(taskId, localPath)                    │
│         ├── onError(taskId, error)                           │
│         └── onPaused(taskId)                                 │
│                                                              │
│  页面依赖树:                                                  │
│                                                              │
│  MainActivity (入口)                                         │
│    ├──→ LoginActivity                                        │
│    ├──→ HomeActivity                                         │
│    └──→ InAppDownloadActivity (版本更新下载)                  │
│                                                              │
│  LoginActivity                                               │
│    └──→ HomeActivity (成功) / RegisterActivity (注册按钮)      │
│                                                              │
│  RegisterActivity                                            │
│    └──→ HomeActivity (成功) / LoginActivity (返回登录)        │
│                                                              │
│  HomeActivity (4个Tab页面)                                   │
│    ├── Tab0: pageHome (首页)                                 │
│    │   ├──→ AppDetailActivity (点击应用)                      │
│    │   ├──→ InfoDetailActivity (点击轮播)                    │
│    │   └──→ 分类过滤 apps(category)                          │
│    │                                                         │
│    ├── Tab1: pageApps (应用列表)                              │
│    │   └──→ AppDetailActivity                                │
│    │                                                         │
│    ├── Tab2: pageInfo (资讯列表)                              │
│    │   └──→ InfoDetailActivity                               │
│    │                                                         │
│    ├── Tab3: pageMe (个人中心)                                │
│    │   ├──→ DownloadListActivity (下载管理)                   │
│    │   ├──→ 卡密兑换弹窗                                      │
│    │   ├──→ 积分记录弹窗                                      │
│    │   ├──→ QQ客服 (mqqapi://)                               │
│    │   └──→ LoginActivity (退出登录)                          │
│    │                                                         │
│    └── 数据加载:                                              │
│        ├── AppBaseAdapter  ← apps→ListView                   │
│        └── InfoBaseAdapter ← infos→ListView                  │
│                                                              │
│  AppDetailActivity                                           │
│    └──→ InAppDownloadActivity (下载按钮)                      │
│                                                              │
│  InfoDetailActivity                                          │
│    ├──→ Intent.ACTION_VIEW(pan_url) (获取教程)                │
│    └──→ POST buy.php (购买)                                  │
│                                                              │
│  InAppDownloadActivity                                       │
│    └── 实现 DownloadListener，连接 InAppDownloadManager       │
│                                                              │
│  DownloadListActivity (空壳，tasks永远为空)                    │
│                                                              │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                   管理后台组件依赖关系                          │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ApiClient (单例静态类)                                       │
│    │ static String S = "http://47.108.209.71"                │
│    │                                                         │
│    ├── req(method, path, body)                                │
│    │   └── 所有请求路径相对于 A = S+"/backend/api"            │
│    │   └── Authorization: Bearer {token}                     │
│    │                                                         │
│    ├── get/post/put 公开方法                                  │
│    │                                                         │
│    ├── uploadFile(path) ← 手动构造multipart/form-data         │
│    │   ⚠️ 固定boundary "---BOUNDARY"，未过滤文件名            │
│    │                                                         │
│    └── 各业务API方法映射:                                     │
│        ├── login / changePassword                            │
│        ├── getStats                                          │
│        ├── getApps / createApp / updateApp / deleteApp       │
│        ├── getUsers / updateUser                             │
│        ├── getCards / createCard                             │
│        ├── getInfoList / createInfo / updateInfo             │
│        ├── getBanners / createBanner / updateBanner          │
│        ├── getSystemConfig / updateSystemConfig              │
│        ├── getImages                                         │
│        └── uploadFile                                        │
│                                                              │
│  页面依赖树:                                                  │
│                                                              │
│  LoginActivity                                               │
│    └──→ DashboardActivity (成功)                              │
│                                                              │
│  DashboardActivity (看板，9个功能入口)                        │
│    ├──→ AppsActivity (添加应用/应用管理)                      │
│    ├──→ CardsActivity (卡密管理)                              │
│    ├──→ InfoActivity (发布资讯/资讯管理)                       │
│    ├──→ UsersActivity (用户管理)                              │
│    ├──→ BannersActivity (轮播设置)                             │
│    ├──→ ImagesActivity (图片库)                               │
│    ├──→ UploadActivity (文件上传)                              │
│    ├──→ SettingsActivity (系统配置)                            │
│    ├──→ PasswordActivity (修改密码)                             │
│    └──→ AboutActivity (关于)                                   │
│                                                              │
│  AppsActivity                                                │
│    ├──→ ApkUploadActivity (发布新应用)                         │
│    └── 弹窗编辑: icon上传 → uploadImageSync → uploadFile      │
│                                                              │
│  ApkUploadActivity                                           │
│    └── 三步上传: 图标→APK→创建应用记录                         │
│                                                              │
│  所有Activity共享特征:                                        │
│    • extends Activity (而非 AppCompatActivity)                │
│    • new Thread() 手动线程管理                                │
│    • r.contains("\"code\":0") 字符串匹配验证                  │
│    • catch(Exception) 宽泛异常捕获                            │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## 四、API 接口完整清单

### 4.1 用户端API (通过分发平台App调用)

| 方法 | 端点 | 认证 | 参数 | 用途 |
|------|------|------|------|------|
| POST | `/user/login.php` | 无 | username, password | 用户登录 |
| POST | `/user/register.php` | 无 | username, password, qq, invite_code, device_model, device_sdk | 用户注册 |
| POST | `/user/checkin.php` | Bearer | (空body) | 每日签到 |
| GET | `/user/info.php` | Bearer | - | 获取用户信息 |
| GET | `/app/apps.php` | 可选 | ?category=X | 应用列表 |
| GET | `/app/detail.php` | 可选 | ?id=X | 应用详情 |
| POST | `/app/download.php` | Bearer | app_id | 记录下载(返回下载URL) |
| GET | `/app/banners.php` | 无 | - | 轮播图列表 |
| GET | `/info/list.php` | 无 | - | 资讯列表 |
| GET | `/info/detail.php` | ⚠️URL | ?id=X&token=Y | 资讯详情 |
| POST | `/info/buy.php` | Bearer | info_id | 购买资讯 |
| GET | `/app/update.php` | 无 | ?version=X&version_code=Y | 版本更新检查 |
| POST | `/points/redeem.php` | Bearer | code | 卡密兑换 |
| GET | `/points/log.php` | Bearer | - | 积分记录 |
| GET | `/admin/system_config.php` | 无 | - | 获取系统配置(充值链接) |

### 4.2 管理后台API

| 方法 | 端点 | 认证 | 参数 | 用途 |
|------|------|------|------|------|
| POST | `/admin/login.php` | 无 | username, password | 管理员登录 |
| POST | `/admin/password.php` | Bearer | old_password, new_password | 修改密码 |
| GET | `/admin/stats.php` | Bearer | - | 统计数据 |
| GET | `/admin/apps.php` | Bearer | - | 应用列表 |
| POST | `/admin/apps.php` | Bearer | name, version, size, file_path, icon, category, price, desc, browser_url | 创建应用 |
| PUT | `/admin/apps.php` | Bearer | id, name, version, ... | 更新应用 |
| GET | `/admin/apps.php` | Bearer | ?id=X&_method=DELETE | 删除应用 |
| GET | `/admin/users.php` | Bearer | - | 用户列表 |
| POST | `/admin/users.php` | Bearer | id, points, action | 调整用户积分 |
| GET | `/admin/cards.php` | Bearer | - | 卡密列表 |
| POST | `/admin/cards.php` | Bearer | count, points, expire_days | 批量生成卡密 |
| GET | `/admin/info.php` | Bearer | - | 资讯列表 |
| POST | `/admin/info.php` | Bearer | title, cover_img, summary, content, category, price, pan_url | 创建资讯 |
| PUT | `/admin/info.php` | Bearer | id, title, ... | 更新资讯 |
| GET | `/admin/info.php` | Bearer | ?id=X&_method=DELETE | 删除资讯 |
| GET | `/admin/banners.php` | Bearer | - | 轮播配置 |
| POST | `/admin/banners.php` | Bearer | carousel_ids | 设置轮播 |
| GET | `/admin/system_config.php` | Bearer | - | 获取系统配置 |
| POST | `/admin/system_config.php` | Bearer | app_name, notice, recharge_url, app_version, app_version_code, app_apk_url, app_apk_size, app_update_log, app_force_update, app_update_time | 更新系统配置 |
| POST | `/admin/upload.php` | Bearer | multipart: file | 文件上传 |
| GET | `/admin/images.php` | Bearer | - | 图片列表 |

---

## 五、安全风险结构图

```
                        顾阳软件分发平台 - 安全风险全景
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
   ┌────▼────┐          ┌────▼────┐          ┌─────▼─────┐
   │ 传输层  │          │ 存储层  │          │  代码层   │
   │ 安全    │          │ 安全    │          │  安全     │
   └────┬────┘          └────┬────┘          └─────┬─────┘
        │                     │                     │
   ┌────▼──────────┐    ┌────▼──────────┐    ┌─────▼──────────┐
   │• HTTP明文     │    │• Token明文SP  │    │• 服务器IP硬编码│
   │• 无证书固定   │    │• debug.keystore│   │• 密钥硬编码    │
   │• URL传Token   │    │• allowBackup  │    │• 无ProGuard    │
   │• 密码明文传输 │    │     =true     │    │• 宽泛异常处理  │
   └───────────────┘    └───────────────┘    │• 字符串匹配验证│
                                              │• SQL注入风险   │
                                              └────────────────┘
        ┌─────────────────────────────────────────┐
        │          攻击者可能的攻击链               │
        ├─────────────────────────────────────────┤
        │                                         │
        │  1. 反编译APK → 获取 47.108.209.71      │
        │          ↓                               │
        │  2. 端口扫描 → 发现 80/3306/22等端口     │
        │          ↓                               │
        │  3. SSH爆破 或 Web漏洞 → 获取服务器权限  │
        │          ↓                               │
        │  4. 读取 /backend/api/*.php → 获取DB密码 │
        │          ↓                               │
        │  5. 全量导出数据库 → 所有用户数据泄露    │
        │                                         │
        │  或者:                                   │
        │                                         │
        │  1. 中间人攻击 HTTP → 截获登录凭证       │
        │          ↓                               │
        │  2. 截获管理员Token → 伪造请求           │
        │          ↓                               │
        │  3. 上传恶意APK → 所有用户下载安装       │
        │          ↓                               │
        │  4. 全量用户设备被控                     │
        │                                         │
        └─────────────────────────────────────────┘
```

---

## 六、业务功能完整性评估

| 功能模块 | 用户端 | 管理后台 | 状态 |
|----------|--------|----------|------|
| 用户注册/登录 | ✅ | - | 正常 |
| Token认证 | ✅ | ✅ | ⚠️明文+URL泄露 |
| 应用浏览/搜索 | ✅ | - | 正常 |
| 应用分类过滤 | ✅ | ✅ | 正常 |
| 应用发布/编辑/删除 | - | ✅ | 正常 |
| 应用上下架 | - | ✅ | 正常 |
| APK上传 | - | ✅ | ⚠️无类型过滤 |
| 应用内下载(断点续传) | ✅ | - | 正常 |
| 版本更新检查 | ✅ | ✅(配置) | ⚠️可被绕过 |
| 资讯浏览/详情 | ✅ | - | 正常 |
| 资讯发布/编辑/删除 | - | ✅ | 正常 |
| 资讯购买(积分) | ✅ | - | 正常 |
| 轮播图管理 | ✅(展示) | ✅(配置) | 正常 |
| 每日签到 | ✅ | - | 正常 |
| 卡密生成 | - | ✅ | 正常 |
| 卡密兑换 | ✅ | - | 正常 |
| 积分调整 | - | ✅ | 正常 |
| 用户管理 | - | ✅ | 正常 |
| 修改密码 | - | ✅ | 正常 |
| 图片库 | - | ✅ | 正常 |
| 文件上传 | - | ✅ | ⚠️手动multipart |
| 系统配置 | ✅(读取) | ✅(写入) | 正常 |
| 下载管理 | ⚠️空壳 | - | ❌DownloadListActivity无数据 |
| 图片加载 | ⚠️无缓存 | - | ❌无Glide/Coil |
| QQ客服 | ✅ | - | 正常(硬编码群号) |

---

## 七、依赖清单

### 分发平台依赖
```
androidx.appcompat:appcompat:1.6.1
com.google.android.material:material:1.9.0
androidx.constraintlayout:constraintlayout:2.1.4
androidx.swiperefreshlayout:swiperefreshlayout:1.1.0
(AGP 8.4.0, compileSdk 34, targetSdk 33, minSdk 21)
```

### 管理后台依赖
```
com.squareup.okhttp3:okhttp:4.9.3    ← 声明了但代码中未使用(用HttpURLConnection)
(AGP 7.4.2, compileSdk 30, targetSdk 30, minSdk 21)
```

### 仓库配置
```
分发平台: google, mavenCentral, jitpack, jcenter(⚠️已关闭)
管理后台: google, mavenCentral
```

---

## 八、攻击面总结

| 攻击向量 | 入口点 | 严重程度 | 可利用性 |
|----------|--------|----------|----------|
| 反编译获取源码 | APK无混淆 | 🔴严重 | 极高(拖放即可) |
| 获取服务器IP | ApiClient.S 常量 | 🔴严重 | 极高(明文) |
| 拦截HTTP流量 | 无HTTPS | 🔴严重 | 高(同WiFi) |
| 窃取Token | SharedPreferences | 🔴严重 | 中(需Root) |
| 伪造APK分发 | debug.keystore泄露 | 🔴严重 | 高 |
| 上传恶意APK | 管理后台无签名验证 | 🟠高危 | 中(需管理员权限) |
| SQL注入 | URL拼接参数 | 🟠高危 | 中(取决于后端) |
| 中间人篡改下载 | 无URL白名单/哈希 | 🟠高危 | 高 |
| 重放攻击 | 无nonce/时间戳 | 🟠高危 | 中 |
| 路径遍历 | 文件名未过滤 | 🟠高危 | 中 |
| 供应链投毒 | jcenter已关闭 | 🟡中危 | 低 |
| 数据备份泄露 | allowBackup=true | 🟡中危 | 低 |

---

*逻辑树生成完毕。该文档完整覆盖了项目架构、数据流、组件关系、API清单、安全风险拓扑。*