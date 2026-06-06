import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models/api_models.dart';
import '../models/module_models.dart';
import 'rsa_oaep.dart';
import 'session_store.dart';

class ApiClient {
  ApiClient({http.Client? httpClient, SessionStore? store})
    : _httpClient = httpClient ?? http.Client(),
      _store = store ?? SessionStore();

  final http.Client _httpClient;
  final SessionStore _store;
  final Map<String, String> _cookies = {};

  String _baseUrl = '';

  String get baseUrl => _baseUrl;

  Map<String, String> get cookies => Map.unmodifiable(_cookies);

  void configure({required String serverUrl, Map<String, String>? cookies}) {
    _baseUrl = normalizeServerUrl(serverUrl);
    _cookies
      ..clear()
      ..addAll(cookies ?? const {});
  }

  Future<void> persistServer() async {
    await _store.saveServerUrl(_baseUrl);
  }

  Future<void> persistCookies() async {
    await _store.saveCookies(_cookies);
  }

  Future<T> get<T>(
    String path, {
    Map<String, Object?>? query,
    required T Function(Object? data) decode,
  }) {
    return _request('GET', path, query: query, decode: decode);
  }

  Future<T> post<T>(
    String path, {
    Object? body,
    Map<String, Object?>? query,
    required T Function(Object? data) decode,
  }) {
    return _request('POST', path, query: query, body: body, decode: decode);
  }

  Future<CaptchaData> captcha(String serverUrl) async {
    configure(serverUrl: serverUrl, cookies: _cookies);
    await persistServer();
    return get<CaptchaData>(
      '/admin/auth/captcha',
      decode: (data) => CaptchaData.fromJson(mapValue(data)),
    );
  }

  Future<LoginResult> login({
    required String serverUrl,
    required String account,
    required String password,
    required String captchaId,
    required String captcha,
  }) async {
    configure(serverUrl: serverUrl, cookies: _cookies);
    await persistServer();

    final publicKey = await get<String>(
      '/admin/auth/public-key',
      decode: (data) => stringValue(mapValue(data)['publicKey']),
    );
    final encryptedAccount = RsaOaep.encryptSpkiBase64(publicKey, account);
    final encryptedPassword = RsaOaep.encryptSpkiBase64(publicKey, password);
    final result = await post<LoginResult>(
      '/admin/auth/login',
      body: {
        'accountNo': encryptedAccount,
        'password': encryptedPassword,
        'captchaId': captchaId,
        'captcha': captcha,
      },
      decode: (data) => LoginResult.fromJson(mapValue(data)),
    );
    await persistCookies();
    return result;
  }

  Future<UserInfo> me() {
    return get<UserInfo>(
      '/admin/auth/me',
      decode: (data) => UserInfo.fromJson(mapValue(data)),
    );
  }

  Future<void> logout() async {
    try {
      await post<void>('/admin/auth/logout', decode: (_) {});
    } finally {
      _cookies.clear();
      await _store.clearUserAndCookies();
    }
  }

  Future<SystemStatus> systemStatus() {
    return get<SystemStatus>(
      '/admin/system/status',
      decode: (data) => SystemStatus.fromJson(mapValue(data)),
    );
  }

  Future<LicenseStatus> licenseStatus() {
    return get<LicenseStatus>(
      '/admin/license/status',
      decode: (data) => LicenseStatus.fromJson(mapValue(data)),
    );
  }

  Future<List<AppModule>> modules() {
    return get<List<AppModule>>(
      '/admin/meta/modules',
      decode: (data) => listValue(data)
          .whereType<Map>()
          .map((item) => AppModule.fromJson(item.cast<String, dynamic>()))
          .toList(),
    );
  }

  Future<PageResult> page(String path, Map<String, Object?> params) {
    return get<PageResult>(
      path,
      query: params,
      decode: (data) => PageResult.fromJson(mapValue(data)),
    );
  }

  Future<void> submit(String path, Map<String, Object?> body) {
    return post<void>(path, body: body, decode: (_) {});
  }

  Future<T> _request<T>(
    String method,
    String path, {
    Map<String, Object?>? query,
    Object? body,
    required T Function(Object? data) decode,
  }) async {
    if (_baseUrl.isEmpty) {
      throw ApiException('请先填写服务器地址', 0);
    }

    final uri = _uri(path, query);
    final headers = <String, String>{
      'Content-Type': 'application/json',
      if (_cookies.isNotEmpty)
        'Cookie': _cookies.entries.map((e) => '${e.key}=${e.value}').join('; '),
    };

    final request = http.Request(method, uri)..headers.addAll(headers);
    if (body != null) {
      request.body = jsonEncode(body);
    }

    late http.Response response;
    try {
      response = await _httpClient
          .send(request)
          .then(http.Response.fromStream)
          .timeout(const Duration(seconds: 18));
    } on TimeoutException {
      throw ApiException('请求超时', 408);
    } catch (_) {
      throw ApiException('无法连接服务器', 0);
    }

    _absorbSetCookie(response.headers['set-cookie']);
    if (response.headers.containsKey('set-cookie')) {
      await persistCookies();
    }

    final text = utf8.decode(response.bodyBytes);
    if (text.isEmpty) {
      if (response.statusCode >= 200 && response.statusCode < 300) {
        return decode(null);
      }
      throw ApiException('服务器响应异常：${response.statusCode}', response.statusCode);
    }

    final json = decodeObject(text);
    final code = intValue(json['code'], fallback: response.statusCode);
    final msg = stringValue(json['msg'], fallback: '请求失败');
    if (code != 0 && code != 200) {
      throw ApiException(msg, code);
    }
    return decode(json['data']);
  }

  Uri _uri(String path, Map<String, Object?>? query) {
    final base = Uri.parse(_baseUrl);
    final normalizedPath = path.startsWith('/') ? path : '/$path';
    final target = base.replace(
      path: '${base.path == '/' ? '' : base.path}$normalizedPath',
      queryParameters: _queryParams(query),
    );
    return target;
  }

  Map<String, String>? _queryParams(Map<String, Object?>? query) {
    final values = <String, String>{};
    for (final entry in (query ?? const <String, Object?>{}).entries) {
      final value = entry.value;
      if (value == null || value.toString().isEmpty) {
        continue;
      }
      values[entry.key] = value.toString();
    }
    return values.isEmpty ? null : values;
  }

  void _absorbSetCookie(String? rawHeader) {
    if (rawHeader == null || rawHeader.isEmpty) {
      return;
    }
    for (final cookie in _splitSetCookie(rawHeader)) {
      final first = cookie.split(';').first.trim();
      final index = first.indexOf('=');
      if (index <= 0) {
        continue;
      }
      final name = first.substring(0, index);
      final value = first.substring(index + 1);
      final expired =
          cookie.toLowerCase().contains('max-age=0') || value.isEmpty;
      if (expired) {
        _cookies.remove(name);
      } else {
        _cookies[name] = value;
      }
    }
  }

  List<String> _splitSetCookie(String rawHeader) {
    return rawHeader
        .split(RegExp(r',(?=\s*[^;,\s]+=)'))
        .map((item) => item.trim())
        .where((item) => item.isNotEmpty)
        .toList();
  }
}

String normalizeServerUrl(String input) {
  var text = input.trim();
  if (text.isEmpty) {
    return '';
  }
  if (!text.startsWith('http://') && !text.startsWith('https://')) {
    text = 'http://$text';
  }
  while (text.endsWith('/')) {
    text = text.substring(0, text.length - 1);
  }
  return text;
}
