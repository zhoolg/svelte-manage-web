import 'dart:ui';

import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:manage_mobile/core/session_controller.dart';
import 'package:manage_mobile/main.dart';

void main() {
  testWidgets('显示移动端登录页', (tester) async {
    SharedPreferences.setMockInitialValues({});
    await tester.binding.setSurfaceSize(const Size(390, 844));
    addTearDown(() => tester.binding.setSurfaceSize(null));

    await tester.pumpWidget(ManageMobileApp(controller: SessionController()));
    await tester.pumpAndSettle();

    expect(find.text('云管理控制台'), findsOneWidget);
    expect(find.text('登录控制台'), findsOneWidget);
    expect(find.text('服务器地址'), findsOneWidget);
    expect(find.text('账号'), findsOneWidget);
    expect(find.text('密码'), findsOneWidget);
    expect(find.text('验证码'), findsOneWidget);
  });
}
