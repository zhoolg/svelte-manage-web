import 'package:flutter/foundation.dart';

import '../models/api_models.dart';
import 'api_client.dart';
import 'session_store.dart';

class SessionController extends ChangeNotifier {
  SessionController({ApiClient? api, SessionStore? store})
    : api = api ?? ApiClient(),
      _store = store ?? SessionStore();

  final ApiClient api;
  final SessionStore _store;

  bool booting = true;
  UserInfo? user;
  String serverUrl = '';

  bool get authenticated => user != null && serverUrl.isNotEmpty;

  Future<void> restore() async {
    final stored = await _store.load();
    serverUrl = stored.serverUrl;
    user = stored.user;
    api.configure(serverUrl: serverUrl, cookies: stored.cookies);
    booting = false;
    notifyListeners();
    if (authenticated) {
      _refreshCurrentUserSilently();
    }
  }

  Future<CaptchaData> captcha(String nextServerUrl) {
    serverUrl = normalizeServerUrl(nextServerUrl);
    notifyListeners();
    return api.captcha(serverUrl);
  }

  Future<void> login({
    required String nextServerUrl,
    required String account,
    required String password,
    required String captchaId,
    required String captcha,
  }) async {
    final result = await api.login(
      serverUrl: nextServerUrl,
      account: account,
      password: password,
      captchaId: captchaId,
      captcha: captcha,
    );
    serverUrl = api.baseUrl;
    user = result.user;
    await _store.saveUser(result.user);
    notifyListeners();
  }

  Future<void> logout() async {
    await api.logout();
    user = null;
    notifyListeners();
  }

  Future<void> _refreshCurrentUserSilently() async {
    try {
      final current = await api.me();
      user = current;
      await _store.saveUser(current);
      notifyListeners();
    } catch (_) {
      user = null;
      await _store.clearUserAndCookies();
      notifyListeners();
    }
  }
}
