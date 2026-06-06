import 'dart:convert';

class ApiException implements Exception {
  ApiException(this.message, this.code);

  final String message;
  final int code;

  @override
  String toString() => message;
}

class ApiEnvelope<T> {
  ApiEnvelope({required this.code, required this.msg, required this.data});

  final int code;
  final String msg;
  final T? data;
}

class CaptchaData {
  CaptchaData({required this.captchaId, required this.image});

  final String captchaId;
  final String image;

  factory CaptchaData.fromJson(Map<String, dynamic> json) {
    return CaptchaData(
      captchaId: stringValue(json['captchaId']),
      image: stringValue(json['image']),
    );
  }
}

class UserInfo {
  UserInfo({
    required this.id,
    required this.username,
    required this.name,
    this.avatar,
    this.roles = const [],
    this.usingDefaultPassword = false,
  });

  final int id;
  final String username;
  final String name;
  final String? avatar;
  final List<String> roles;
  final bool usingDefaultPassword;

  factory UserInfo.fromJson(Map<String, dynamic> json) {
    return UserInfo(
      id: intValue(json['id']),
      username: stringValue(json['username']),
      name: stringValue(json['name'], fallback: stringValue(json['username'])),
      avatar: nullableString(json['avatar']),
      roles: listValue(json['roles']).map((item) => item.toString()).toList(),
      usingDefaultPassword: boolValue(json['usingDefaultPassword']),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'username': username,
      'name': name,
      'avatar': avatar,
      'roles': roles,
      'usingDefaultPassword': usingDefaultPassword,
    };
  }
}

class LoginResult {
  LoginResult({
    required this.user,
    required this.permissions,
    required this.isAdmin,
    required this.usingDefaultPassword,
  });

  final UserInfo user;
  final List<String> permissions;
  final bool isAdmin;
  final bool usingDefaultPassword;

  factory LoginResult.fromJson(Map<String, dynamic> json) {
    return LoginResult(
      user: UserInfo.fromJson(mapValue(json['user'])),
      permissions: listValue(
        json['permissions'],
      ).map((item) => item.toString()).toList(),
      isAdmin: boolValue(json['isAdmin']),
      usingDefaultPassword: boolValue(json['usingDefaultPassword']),
    );
  }
}

class SystemStatus {
  SystemStatus({
    required this.status,
    required this.checkedAt,
    required this.runtime,
    required this.resources,
    required this.database,
    required this.redis,
    required this.aiFactory,
    required this.security,
    required this.checks,
  });

  final String status;
  final String checkedAt;
  final Map<String, dynamic> runtime;
  final Map<String, dynamic> resources;
  final Map<String, dynamic> database;
  final Map<String, dynamic> redis;
  final Map<String, dynamic> aiFactory;
  final Map<String, dynamic> security;
  final List<Map<String, dynamic>> checks;

  factory SystemStatus.fromJson(Map<String, dynamic> json) {
    return SystemStatus(
      status: stringValue(json['status'], fallback: 'UNKNOWN'),
      checkedAt: stringValue(json['checkedAt']),
      runtime: mapValue(json['runtime']),
      resources: mapValue(json['resources']),
      database: mapValue(json['database']),
      redis: mapValue(json['redis']),
      aiFactory: mapValue(json['aiFactory']),
      security: mapValue(json['security']),
      checks: listValue(
        json['checks'],
      ).whereType<Map>().map((item) => item.cast<String, dynamic>()).toList(),
    );
  }
}

class LicenseStatus {
  LicenseStatus({
    required this.allowed,
    required this.requiresLicense,
    required this.code,
    required this.machineId,
    required this.message,
    required this.issuedTo,
    required this.edition,
    required this.expiresAt,
    required this.integrityTrusted,
    required this.integrityCode,
    required this.integrityMessage,
    required this.integrityCheckedAt,
  });

  final bool allowed;
  final bool requiresLicense;
  final String code;
  final String machineId;
  final String message;
  final String issuedTo;
  final String edition;
  final String expiresAt;
  final bool integrityTrusted;
  final String integrityCode;
  final String integrityMessage;
  final String integrityCheckedAt;

  factory LicenseStatus.fromJson(Map<String, dynamic> json) {
    return LicenseStatus(
      allowed: boolValue(json['allowed']),
      requiresLicense: boolValue(json['requiresLicense']),
      code: stringValue(json['code']),
      machineId: stringValue(json['machineId']),
      message: stringValue(json['message']),
      issuedTo: stringValue(json['issuedTo']),
      edition: stringValue(json['edition']),
      expiresAt: stringValue(json['expiresAt']),
      integrityTrusted: boolValue(json['integrityTrusted']),
      integrityCode: stringValue(json['integrityCode']),
      integrityMessage: stringValue(json['integrityMessage']),
      integrityCheckedAt: stringValue(json['integrityCheckedAt']),
    );
  }
}

class PageResult {
  PageResult({required this.list, required this.total});

  final List<Map<String, dynamic>> list;
  final int total;

  factory PageResult.fromJson(Map<String, dynamic> json) {
    return PageResult(
      list: listValue(
        json['list'],
      ).whereType<Map>().map((item) => item.cast<String, dynamic>()).toList(),
      total: intValue(json['total']),
    );
  }
}

Map<String, dynamic> decodeObject(String text) {
  final decoded = jsonDecode(text);
  if (decoded is Map<String, dynamic>) {
    return decoded;
  }
  if (decoded is Map) {
    return decoded.cast<String, dynamic>();
  }
  return {};
}

Map<String, dynamic> mapValue(Object? value) {
  if (value is Map<String, dynamic>) {
    return value;
  }
  if (value is Map) {
    return value.cast<String, dynamic>();
  }
  return {};
}

List<dynamic> listValue(Object? value) {
  if (value is List) {
    return value;
  }
  return const [];
}

String stringValue(Object? value, {String fallback = ''}) {
  if (value == null) {
    return fallback;
  }
  final text = value.toString();
  return text.isEmpty ? fallback : text;
}

String? nullableString(Object? value) {
  if (value == null) {
    return null;
  }
  final text = value.toString();
  return text.isEmpty ? null : text;
}

int intValue(Object? value, {int fallback = 0}) {
  if (value is int) {
    return value;
  }
  if (value is num) {
    return value.toInt();
  }
  return int.tryParse(value?.toString() ?? '') ?? fallback;
}

bool boolValue(Object? value) {
  if (value is bool) {
    return value;
  }
  if (value is num) {
    return value != 0;
  }
  final text = value?.toString().toLowerCase();
  return text == 'true' || text == '1' || text == 'yes';
}
