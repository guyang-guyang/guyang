# 🔍 顾阳软件分发平台 - 源码安全审计报告

**审计日期**: 2026-05-27  
**审计范围**: 顾阳分发平台（用户端）+ 顾阳管理后台（管理员端）  
**项目类型**: Android 原生应用（Java）  
**审计人员**: 自动化代码审计

---

## 📊 风险等级总览

| 等级 | 数量 | 说明 |
|------|------|------|
| 🔴 **严重** | 6 | 需立即修复 |
| 🟠 **高危** | 8 | 应尽快修复 |
| 🟡 **中危** | 10 | 建议修复 |
| 🟢 **低危** | 5 | 可择机修复 |

---

## 🔴 严重风险（Critical）

### 1. 硬编码服务器IP地址（明文暴露后端地址）
**文件**：`顾阳分发平台/app/src/main/java/com/guyang/app/ApiClient.java:8`  
**文件**：`顾阳管理后台/app/src/main/java/com/guyang/admin/ApiClient.java:4`

```java
// 硬编码IP，完全明文暴露服务器地址
public static final String S="http://47.108.209.71";
public static final String A=S+"/backend/api";
```

**风险**：
- 服务器IP `47.108.209.71` 直接硬编码在客户端代码中
- 使用 HTTP 明文协议（非HTTPS），所有通信数据可被中间人截获
- 攻击者反编译APK后可直接获取后端地址，进行针对性攻击
- 服务器IP暴露后，容易遭受DDoS攻击、端口扫描、漏洞探测

**修复建议**：
```java
// 1. 使用HTTPS
public static final String S = "https://api.guyang.com";
// 2. 使用BuildConfig或加密存储
public static final String S = BuildConfig.API_BASE_URL;
// 3. 添加证书固定（Certificate Pinning）
```

---

### 2. 硬编码Debug签名密钥
**文件**：`顾阳分发平台/app/build.gradle:10-15`

```groovy
signingConfigs {
    debug {
        storeFile file("debug.keystore")
        storePassword "android"       // 硬编码密码
        keyAlias "androiddebugkey"
        keyPassword "android"         // 硬编码密码
    }
}
```

**风险**：
- 签名密钥和密码直接写在build.gradle中
- `debug.keystore` 文件可能被提交到版本控制
- 攻击者可使用该密钥签名恶意APK并伪装成官方应用

**修复建议**：
- 从环境变量或 `local.properties`（已在.gitignore中）读取密码
- debug.keystore 加入到 `.gitignore`
- 生产签名务必使用独立的 release keystore

---

### 3. Token 明文存储在 SharedPreferences
**文件**：`顾阳分发平台/.../LoginActivity.java:38`、`RegisterActivity.java:57`  
**文件**：`顾阳管理后台/.../LoginActivity.java:39`

```java
// 分发平台
sp.edit().putString("token", t).apply();  // 明文存储

// 管理后台
getSharedPreferences("admin", MODE_PRIVATE).edit()
    .putString("token", tok).putString("last_user", un).apply();
```

**风险**：
- JWT Token 明文存储在 SharedPreferences（MODE_PRIVATE）
- 在已Root设备上，攻击者可轻易读取该Token
- Token泄露后，攻击者可以完全冒充用户身份
- 管理后台Token泄露更为严重，会导致整个系统被接管

**修复建议**：
```java
// 使用 Android Keystore + EncryptedSharedPreferences
MasterKey masterKey = new MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build();
SharedPreferences sp = EncryptedSharedPreferences.create(
    context, "secure_prefs", masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
);
```

---

### 4. 明文HTTP传输与全局明文流量允许
**文件**：`顾阳分发平台/app/src/main/AndroidManifest.xml:17`  
**文件**：`顾阳分发平台/app/src/main/res/xml/network_security_config.xml:3`

```xml
<!-- manifest -->
android:usesCleartextTraffic="true"

<!-- network_security_config.xml -->
<base-config cleartextTrafficPermitted="true">
    <trust-anchors>
        <certificates src="system" />
    </trust-anchors>
</base-config>
```

**风险**：
- 全局允许HTTP明文通信
- 没有证书固定（Certificate Pinning），可能遭受中间人攻击
- 登录凭证、Token、用户数据全部明文传输
- 管理后台同样通过HTTP访问服务器

**修复建议**：
```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">guyang.com</domain>
        <pin-set>
            <pin digest="SHA-256">base64_encoded_pin=</pin>
        </pin-set>
    </domain-config>
</network-security-config>
```

---

### 5. Token直接通过URL参数传递
**文件**：`顾阳分发平台/.../ApiClient.java:66`

```java
// Token被拼接到URL中，会被中间代理、日志和Referer泄露
public static String infoDetail(String id) {
    return get(A+"/info/detail.php?id="+id+"&token="+token);
}
```

**风险**：
- Token出现在URL参数中，会被浏览器历史、服务器日志、代理服务器记录
- 在Referer头中可能泄露给第三方
- 不符合安全最佳实践，Token应通过Authorization头传递

**修复建议**：
- 已在其他API中正确使用 `Authorization: Bearer` 头，此处遗漏
- 移除URL中的token参数

---

### 6. 管理后台app/build.gradle使用apply plugin旧语法且缺少安全配置
**文件**：`顾阳管理后台/app/build.gradle`

```groovy
apply plugin: 'com.android.application'  // 已过时的DSL
android {
    buildTypes { release { minifyEnabled false } }  // 无混淆
}
```

**风险**：
- Release版本无代码混淆（minifyEnabled false），反编译后源码完全可读
- 缺少ProGuard/R8规则，所有内部逻辑暴露
- 管理后台包含更多敏感操作，无混淆风险更高

---

## 🟠 高危风险（High）

### 7. 用户密码明文传输
**文件**：`顾阳分发平台/.../ApiClient.java:57`、`顾阳管理后台/.../ApiClient.java:23`

```java
// 密码在JSON中明文发送（通过HTTP）
public static String login(String u, String p) {
    JSONObject b = new JSONObject();
    b.put("username", u);
    b.put("password", p);  // 密码明文
    return post(A + "/user/login.php", b);
}
```

**风险**：
- 密码明文通过HTTP传输，可被中间人截获
- 客户端未做任何密码哈希处理（虽然服务端应处理，但客户端预哈希可增加一层防护）

**修复建议**：
- 强制使用HTTPS
- 可在客户端先做一次SHA-256哈希再传输（需服务端配合双重哈希验证）

---

### 8. 异常处理过于宽泛 - 静默吞下所有异常
**文件**：几乎所有Activity和网络请求代码

```java
// 遍及整个项目
} catch (Exception e) {
    e.setText(r.startsWith("ERROR:") ? r.substring(6) : r);
}
// 或
} catch (Exception ignored) {}  // 静默忽略
```

**风险**：
- 异常信息直接展示给用户（可能泄露内部路径或逻辑信息）
- `catch (Exception ignored) {}` 完全吞噬错误，调试困难
- 攻击者可通过错误信息推断后端逻辑

**示例位置**：
- `HomeActivity.java:267、305、357、375、399 等多处`
- `InfoDetailActivity.java:66、94`
- `LoginActivity.java:57`

---

### 9. 下载功能无SSL验证和URL白名单
**文件**：`顾阳分发平台/.../InAppDownloadManager.java:134-136`

```java
URL downloadUrl = new URL(task.url);
task.connection = (HttpURLConnection) downloadUrl.openConnection();
task.connection.setConnectTimeout(30000);
// 无SSL证书验证
// 无URL白名单检查
// 无文件类型校验
```

**风险**：
- 下载URL由服务端返回，但可能被中间人篡改
- 无域名白名单，可能下载任意来源文件
- 无文件哈希校验，文件可能被篡改
- 安装APK前未验证签名

**修复建议**：
```java
// 1. URL白名单
if (!url.startsWith("https://trusted-domain.com/")) throw SecurityException();
// 2. 校验文件哈希
String expectedHash = serverResponse.optString("file_hash");
String actualHash = computeSha256(downloadedFile);
if (!expectedHash.equals(actualHash)) { /* 删除文件 */ }
// 3. 安装前验证APK签名
```

---

### 10. 管理后台不安全的文件上传
**文件**：`顾阳管理后台/.../ApiClient.java:50-63`、`ApkUploadActivity.java`

```java
// 手动构造 multipart 请求，容易出现边界问题
os.write(("-----BOUNDARY\r\nContent-Disposition: form-data; name=\"file\"; 
    filename=\""+f.getName()+"\"\r\n\r\n").getBytes());
```

**风险**：
- 文件名未过滤，可能包含路径遍历字符（如 `../../etc/passwd`）
- 文件上传到临时目录后直接读取内容，无文件类型校验
- 上传APK文件时无签名验证，可能上传恶意APK分发给用户
- Multipart边界是固定常量，易被绕过

---

### 11. 服务端响应使用 `r.contains("\"code\":0")` 字符串匹配
**文件**：`AppsActivity.java:92、123`、`UsersActivity.java:27`、`CardsActivity.java:26`、`InfoActivity.java:43`

```java
// 极不安全的响应验证方式
Toast.makeText(this, r.contains("\"code\":0") ? "保存成功" : "保存失败", ...)
```

**风险**：
- 使用字符串包含判断，极端情况下可能误判
- 错误消息可能被注入特定字符串产生误判
- 例如响应 `{"data":"code\":0 fake"}` 会被误判为成功

---

### 12. SQL注入风险（通过URL参数）
**文件**：`ApiClient.java:62、66`（分发平台）、`ApiClient.java:31`（管理后台）

```java
// ID参数直接拼接到URL，如后端未正确过滤，存在SQL注入风险
return get(A + "/app/detail.php?id=" + id);
return get(A + "/info/detail.php?id=" + id + "&token=" + token);

// 管理后台
return get("/admin/apps.php?id=" + id + "&_method=DELETE");
```

**风险**：
- 虽然后端PHP应该有过滤，但客户端未做任何输入校验
- 即使参数来自服务端JSON响应，如JSON数据被篡改仍可注入
- ID参数未校验格式（应为纯数字或特定格式）

---

### 13. 注册时收集设备信息
**文件**：`顾阳分发平台/.../RegisterActivity.java:31-32`

```java
final String dm = android.os.Build.MODEL;
final String ds = String.valueOf(android.os.Build.VERSION.SDK_INT);
```

**风险**：
- 收集设备型号和SDK版本并上传到服务器
- 可能涉及用户隐私合规问题（需在隐私政策中声明）
- 未获得用户同意即收集

---

### 14. 管理后台无请求重放保护
**文件**：所有Activity

**问题**：
- Token验证通过后，无请求时间戳或nonce机制
- 即使Token泄露被发现，攻击者仍可在有效期内无限次重放请求
- 卡密生成、积分调整等敏感操作无二次确认机制

---

## 🟡 中危风险（Medium）

### 15. allowBackup = true
**文件**：`顾阳分发平台/app/src/main/AndroidManifest.xml:13`

```xml
android:allowBackup="true"
```

**风险**：
- Android自动备份会将SharedPreferences（含Token）备份到Google云端
- 攻击者若获取用户Google账号，可恢复包含Token的应用数据
- Android 12+默认为false，但targetSdkVersion 33应主动设置

**修复建议**：
```xml
android:allowBackup="false"
android:dataExtractionRules="@xml/data_extraction_rules"
```

---

### 16. 使用已淘汰的jcenter仓库
**文件**：`顾阳分发平台/app/repositories.json:14-17`

```json
{
    "name": "jcenter",
    "url": "https://jcenter.bintray.com"
}
```

**风险**：
- JFrog已于2022年关闭JCenter服务
- 依赖可能无法下载或使用过时/有安全漏洞的版本
- 可能遭受域名抢注后的供应链攻击

---

### 17. 主线程网络请求警告（在调试时显现）
**文件**：`MainActivity.java:25-79`、`LoginActivity.java:19-22`

```java
// checkUpdate 在线程中，但如果网络在主线程打开StrictMode会崩溃
new Thread(() -> {
    // 网络请求
}).start();
```

**问题**：
- 使用了 `new Thread()` 而非推荐的线程池或协程
- 线程未受控，多次快速切换页面会创建大量线程
- 未使用 `Executors` 或 `AsyncTask` 等推荐方式

---

### 18. 版本检查使用硬编码版本号
**文件**：`顾阳分发平台/.../MainActivity.java:29-32`

```java
PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
String ver = pi.versionName;
int vc = pi.versionCode;
String s = ApiClient.get(ApiClient.A + "/app/update.php?version=" + ver + "&version_code=" + vc);
```

**风险**：
- versionName/versionCode可被篡改（通过重新打包）
- 重打包者可设置极低的版本号来绕过强制更新

---

### 19. 图片加载无缓存和压缩
**文件**：`InfoBaseAdapter.java:44-56`、`HomeActivity.java:322-339`

```java
// 每次getView都创建新线程下载图片
new Thread(() -> {
    URL u = new URL(url);
    Bitmap b = BitmapFactory.decodeStream(c.getInputStream());
    ctx.runOnUiThread(() -> { if(b != null) iv.setImageBitmap(b); });
}).start();
```

**风险**：
- 没有使用图片加载库（如Glide、Coil），重复下载浪费流量
- 无内存缓存，滑动列表时频繁GC
- 无图片压缩，大图直接加载到ImageView导致OOM
- Bitmap未回收，可能内存泄漏

**修复建议**：
```groovy
// 引入 Glide
implementation 'com.github.bumptech.glide:glide:4.16.0'
```

---

### 20. 本地存储二维码/头像副本
**文件**：`LoginActivity.java:62-75`、`RegisterActivity.java:77-94`

```java
FileOutputStream out = new FileOutputStream(new File(getFilesDir(), "avatar.png"));
```

**问题**：
- 头像文件永久存储在内部存储，无过期清理机制
- 用户更换头像后旧文件仍存在

---

### 21. 网赚类应用分发合规风险
**文件**：`HomeActivity.java:117`

```java
v.findViewById(R.id.cat_netearn).setOnClickListener(cv -> { 
    currentCategory = "网赚"; switchTab(1); 
});
```

**风险**：
- "网赚"类应用在主流应用市场通常被禁止
- 分发此类应用可能违反相关平台政策
- 存在法律合规风险

---

### 22. QQ群/客服跳转硬编码
**文件**：`HomeActivity.java:213-217`

```java
startActivity(new Intent(Intent.ACTION_VIEW,
    Uri.parse("mqqapi://card/show_pslcard?src_type=internal&version=1&
    card_type=group&uin=854934959")));
```

**问题**：
- QQ群号硬编码在代码中
- QQUIN暴露且不可动态修改

---

### 23. 缺少ProGuard/R8混淆 （分发平台Release版本）
**文件**：`顾阳分发平台/app/build.gradle:28`

```groovy
release {
    minifyEnabled false
    proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
}
```

**风险**：
- Release版本无代码混淆，反编译后源码完全可读
- 所有API端点、参数结构、业务逻辑暴露
- proguard-rules.pro 文件为空

---

### 24. settings.json泄露项目结构
**文件**：两个项目的根目录

```json
// 顾阳管理后台/settings.json - 内容未知但可能泄露配置
```

**问题**：
- settings.json可能包含IDE特定配置，未在.gitignore中

---

## 🟢 低危风险（Low）

### 25. 代码风格不统一
- 部分文件使用K&R风格，部分使用Allman风格
- 包级导入与单类导入混用
- 缩进不一致（4空格混用Tab）
- 部分文件所有代码在一行（如ApiClient.java极难阅读）

**建议**：
- 引入代码格式化工具（如 Spotless）
- 统一使用 Checkstyle 规则

---

### 26. 管理后台编译SDK版本较低
**文件**：`顾阳管理后台/app/build.gradle:4`

```groovy
compileSdk 30   // Android 11
targetSdkVersion 30
```

**风险**：
- 未使用最新的API级别保护（Android 14 = SDK 34）
- 缺少Android 12+的隐私增强功能
- 新版本Android的行为变更可能导致兼容问题

---

### 27. 缺少依赖版本管理
**文件**：两个项目的build.gradle

```groovy
// 分发平台 - 使用implementation直接指定版本
implementation 'androidx.appcompat:appcompat:1.6.1'

// 管理后台 - OkHttp使用旧版本
implementation 'com.squareup.okhttp3:okhttp:4.9.3'  // 最新版4.12.0
```

**建议**：
- 使用 Gradle Version Catalog (libs.versions.toml) 统一管理
- 定期检查依赖安全漏洞（使用 `gradle dependencyCheckAnalyze`）

---

### 28. 管理后台直接使用Activity而非AppCompatActivity
**文件**：多处Activity

```java
public class DashboardActivity extends Activity  // 而非 AppCompatActivity
```

**问题**：
- 缺少AppCompatActivity提供的向后兼容特性
- Material Design组件可能需要AppCompatActivity支持

---

### 29. 无输入长度上限限制（可能DoS）
**文件**：各处EditText

```java
// 注册时对QQ号做了校验，但对其他字段（用户名、密码、邀请码等）无长度限制
// 虽然服务端应有校验，但客户端也应做基本限制
```

---

## 📋 修复优先级建议

### 立即修复（本周内）
1. 🔴 **切换为HTTPS** - 配置SSL证书，强制加密通信
2. 🔴 **移除硬编码密码和密钥** - 使用环境变量/密钥管理服务
3. 🔴 **使用 EncryptedSharedPreferences** - 加密存储Token
4. 🔴 **启用R8混淆** - 对两个项目的Release版本启用 `minifyEnabled true`

### 尽快修复（两周内）
5. 🟠 添加证书固定（Certificate Pinning）
6. 🟠 下载文件添加哈希校验和URL白名单
7. 🟠 客户端添加输入校验（SQL注入防护）
8. 🟠 文件上传添加文件类型/大小/名称校验
9. 🟠 修改Token传递方式（URL→Authorization头）
10. 🟠 异常处理规范化

### 计划修复（一个月内）
11. 🟡 引入图片加载库（Glide/Coil）
12. 🟡 更新targetSdkVersion到34
13. 🟡 代码格式化和风格统一
14. 🟡 移除jcenter仓库
15. 🟡 完善ProGuard规则

---

## 📝 总结

本平台存在多个严重的安全漏洞，主要集中在：
1. **网络通信安全**：HTTP明文+无证书固定
2. **敏感数据存储**：Token明文+SharedPreferences
3. **代码保护**：无混淆+硬编码密钥
4. **输入校验**：多处缺失
5. **依赖管理**：过时仓库+未启用安全功能

建议在正式发布前完成上述严重和高危风险的修复。特别是管理后台的安全加固尤为重要，因为管理员权限泄露将导致整个平台被完全控制。

---

*报告生成时间: 2026-05-27 | 审计工具: 人工代码审查*