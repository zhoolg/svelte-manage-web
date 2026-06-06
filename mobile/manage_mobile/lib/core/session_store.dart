import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import '../models/api_models.dart';

class StoredSession {
  StoredSession({
    required this.serverUrl,
    required this.cookies,
    required this.user,
  });

  final String serverUrl;
  final Map<String, String> cookies;
  final UserInfo? user;
}

class SessionStore {
  static const _serverUrlKey = 'manage.serverUrl';
  static const _cookiesKey = 'manage.cookies';
  static const _userKey = 'manage.user';

  Future<StoredSession> load() async {
    final prefs = await SharedPreferences.getInstance();
    final rawCookies = prefs.getString(_cookiesKey);
    final rawUser = prefs.getString(_userKey);
    return StoredSession(
      serverUrl: prefs.getString(_serverUrlKey) ?? '',
      cookies: _decodeStringMap(rawCookies),
      user: rawUser == null || rawUser.isEmpty
          ? null
          : UserInfo.fromJson(decodeObject(rawUser)),
    );
  }

  Future<void> saveServerUrl(String serverUrl) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_serverUrlKey, serverUrl);
  }

  Future<void> saveCookies(Map<String, String> cookies) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_cookiesKey, jsonEncode(cookies));
  }

  Future<void> saveUser(UserInfo user) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_userKey, jsonEncode(user.toJson()));
  }

  Future<void> clearUserAndCookies() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_cookiesKey);
    await prefs.remove(_userKey);
  }

  Map<String, String> _decodeStringMap(String? raw) {
    if (raw == null || raw.isEmpty) {
      return {};
    }
    try {
      final decoded = jsonDecode(raw);
      if (decoded is Map) {
        return decoded.map(
          (key, value) => MapEntry(key.toString(), value.toString()),
        );
      }
    } catch (_) {
      return {};
    }
    return {};
  }
}
