# Manage Mobile

Flutter Android 管理后台客户端。

## 功能

- 登录页支持填写服务器地址、账号、密码和验证码。
- 登录协议与 Web 端一致：先请求 `/admin/auth/public-key`，使用 RSA-OAEP(SHA-256) 加密账号和密码，再提交 `/admin/auth/login`。
- 客户端维护服务端下发的 `httpOnly` 会话 Cookie，后续接口自动携带。
- 首页展示系统状态、授权状态和快捷入口。
- 功能页从 `/admin/meta/modules` 读取后端模块元数据，支持通用 CRUD 列表、新增、编辑、删除和搜索。
- 授权页展示机器码、授权客户、版本、过期时间和运行完整性状态。

## 运行

```bash
cd mobile/manage_mobile
flutter pub get
flutter run
```

Android 模拟器访问本机后端时，服务器地址通常填写：

```text
http://10.0.2.2:8080
```

真机访问时填写电脑在局域网中的地址，例如：

```text
http://192.168.1.10:8080
```

## 验证

```bash
flutter analyze
flutter test
flutter build apk --debug
```

当前代码已通过 `flutter analyze` 和 `flutter test`。本机 `flutter build apk --debug` 需要先让 Flutter 找到 Android SDK：

```bash
flutter config --android-sdk <Android SDK 路径>
```
