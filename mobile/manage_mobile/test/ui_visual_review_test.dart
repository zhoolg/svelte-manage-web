import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:manage_mobile/core/api_client.dart';
import 'package:manage_mobile/core/app_theme.dart';
import 'package:manage_mobile/core/session_controller.dart';
import 'package:manage_mobile/models/api_models.dart';
import 'package:manage_mobile/models/module_models.dart';
import 'package:manage_mobile/pages/home_shell.dart';
import 'package:manage_mobile/pages/login_page.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUpAll(_loadReviewFonts);

  testWidgets('视觉审查：登录页 390x844', (tester) async {
    SharedPreferences.setMockInitialValues({});
    await tester.binding.setSurfaceSize(const Size(390, 844));
    addTearDown(() => tester.binding.setSurfaceSize(null));

    await tester.pumpWidget(
      _VisualReviewFrame(
        child: LoginPage(controller: SessionController(api: _FakeApiClient())),
      ),
    );
    await tester.pumpAndSettle();

    await expectLater(
      find.byType(LoginPage),
      matchesGoldenFile('goldens/login_390x844.png'),
    );
  });

  testWidgets('视觉审查：登录页 360x780', (tester) async {
    SharedPreferences.setMockInitialValues({});
    await tester.binding.setSurfaceSize(const Size(360, 780));
    addTearDown(() => tester.binding.setSurfaceSize(null));

    await tester.pumpWidget(
      _VisualReviewFrame(
        child: LoginPage(controller: SessionController(api: _FakeApiClient())),
      ),
    );
    await tester.pumpAndSettle();

    await expectLater(
      find.byType(LoginPage),
      matchesGoldenFile('goldens/login_360x780.png'),
    );
  });

  testWidgets('视觉审查：工作台 390x844', (tester) async {
    SharedPreferences.setMockInitialValues({});
    await tester.binding.setSurfaceSize(const Size(390, 844));
    addTearDown(() => tester.binding.setSurfaceSize(null));

    final controller = SessionController(api: _FakeApiClient())
      ..booting = false
      ..serverUrl = 'https://manage.example.com'
      ..user = UserInfo(
        id: 1,
        username: 'admin',
        name: '平台管理员',
        roles: const ['SUPER_ADMIN', 'OPERATOR'],
      );

    await tester.pumpWidget(
      _VisualReviewFrame(child: HomeShell(controller: controller)),
    );
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 260));
    await tester.pumpAndSettle();

    await expectLater(
      find.byType(HomeShell),
      matchesGoldenFile('goldens/dashboard_390x844.png'),
    );
  });

  testWidgets('视觉审查：工作台 360x780', (tester) async {
    SharedPreferences.setMockInitialValues({});
    await tester.binding.setSurfaceSize(const Size(360, 780));
    addTearDown(() => tester.binding.setSurfaceSize(null));

    final controller = SessionController(api: _FakeApiClient())
      ..booting = false
      ..serverUrl = 'https://manage.example.com'
      ..user = UserInfo(
        id: 1,
        username: 'admin',
        name: '平台管理员',
        roles: const ['SUPER_ADMIN', 'OPERATOR'],
      );

    await tester.pumpWidget(
      _VisualReviewFrame(child: HomeShell(controller: controller)),
    );
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 260));
    await tester.pumpAndSettle();

    await expectLater(
      find.byType(HomeShell),
      matchesGoldenFile('goldens/dashboard_360x780.png'),
    );
  });
}

class _VisualReviewFrame extends StatelessWidget {
  const _VisualReviewFrame({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    final theme = AppTheme.light();
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      theme: theme.copyWith(
        textTheme: theme.textTheme.apply(fontFamily: 'Microsoft YaHei'),
      ),
      home: child,
    );
  }
}

Future<void> _loadReviewFonts() async {
  final chineseFont = File('C:/Windows/Fonts/msyh.ttc');
  if (chineseFont.existsSync()) {
    final loader = FontLoader('Microsoft YaHei')
      ..addFont(
        chineseFont.readAsBytes().then((bytes) => ByteData.sublistView(bytes)),
      );
    await loader.load();
  }

  final iconFont = File(
    'D:/flutter/bin/cache/artifacts/material_fonts/materialicons-regular.otf',
  );
  if (iconFont.existsSync()) {
    final loader = FontLoader('MaterialIcons')
      ..addFont(
        iconFont.readAsBytes().then((bytes) => ByteData.sublistView(bytes)),
      );
    await loader.load();
  }
}

class _FakeApiClient extends ApiClient {
  @override
  Future<SystemStatus> systemStatus() async {
    return SystemStatus(
      status: 'UP',
      checkedAt: '2026-06-05 12:00:00',
      runtime: const {
        'application': 'Svelte Manage',
        'environment': 'production',
        'uptimeSeconds': 186420,
      },
      resources: const {'heapUsedBytes': 572522496, 'heapMaxBytes': 2147483648},
      database: const {'status': 'UP', 'message': 'ready'},
      redis: const {'status': 'UP', 'message': 'ready'},
      aiFactory: const {'status': 'UP', 'message': 'ready'},
      security: const {'activeSessions': 18},
      checks: const [],
    );
  }

  @override
  Future<LicenseStatus> licenseStatus() async {
    return LicenseStatus(
      allowed: true,
      requiresLicense: true,
      code: 'VALID',
      machineId: 'MOBILE-REVIEW-01',
      message: '商业授权有效',
      issuedTo: 'Zhoolg Studio',
      edition: 'Enterprise',
      expiresAt: '2027-12-31',
      integrityTrusted: true,
      integrityCode: 'TRUSTED',
      integrityMessage: '运行环境可信',
      integrityCheckedAt: '2026-06-05 12:00:00',
    );
  }

  @override
  Future<List<AppModule>> modules() async {
    return [
      AppModule(
        id: 'applications',
        label: '应用申请',
        icon: 'clipboard-check',
        path: '/applications',
        crud: _crud('应用申请'),
      ),
      AppModule(
        id: 'admins',
        label: '管理员',
        icon: 'users',
        path: '/admins',
        crud: _crud('管理员'),
      ),
      AppModule(
        id: 'logs',
        label: '审计日志',
        icon: 'file-text',
        path: '/logs',
        crud: _crud('审计日志'),
      ),
      AppModule(
        id: 'system-status',
        label: '系统状态',
        icon: 'activity',
        path: '/system-status',
        customPage: 'SystemStatus',
      ),
      AppModule(
        id: 'ai-modules',
        label: 'AI 模块',
        icon: 'sparkles',
        path: '/ai-modules',
        customPage: 'AiModules',
      ),
      AppModule(
        id: 'settings',
        label: '系统设置',
        icon: 'settings',
        path: '/settings',
        crud: _crud('系统设置'),
      ),
    ];
  }

  CrudConfig _crud(String title) {
    return CrudConfig(
      title: title,
      apiBase: '/admin/review',
      api: CrudApi(list: '/admin/review/list'),
      columns: const [],
      search: const [],
      form: const [],
      showAdd: false,
      showExport: false,
    );
  }
}
