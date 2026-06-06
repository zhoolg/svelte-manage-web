import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'core/app_theme.dart';
import 'core/session_controller.dart';
import 'pages/home_shell.dart';
import 'pages/login_page.dart';
import 'widgets/polished_surface.dart';

void main() {
  runApp(ManageMobileApp(controller: SessionController()));
}

class ManageMobileApp extends StatefulWidget {
  const ManageMobileApp({super.key, required this.controller});

  final SessionController controller;

  @override
  State<ManageMobileApp> createState() => _ManageMobileAppState();
}

class _ManageMobileAppState extends State<ManageMobileApp> {
  @override
  void initState() {
    super.initState();
    widget.controller.restore();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Svelte Manage',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      themeAnimationDuration: const Duration(milliseconds: 220),
      builder: (context, child) {
        return AnnotatedRegion<SystemUiOverlayStyle>(
          value: const SystemUiOverlayStyle(
            statusBarColor: AppTheme.background,
            statusBarIconBrightness: Brightness.dark,
            systemNavigationBarColor: AppTheme.surface,
            systemNavigationBarIconBrightness: Brightness.dark,
          ),
          child: ScrollConfiguration(
            behavior: const _AppScrollBehavior(),
            child: child ?? const SizedBox.shrink(),
          ),
        );
      },
      home: AnimatedBuilder(
        animation: widget.controller,
        builder: (context, _) {
          if (widget.controller.booting) {
            return const _BootPage();
          }
          if (!widget.controller.authenticated) {
            return LoginPage(controller: widget.controller);
          }
          return HomeShell(controller: widget.controller);
        },
      ),
    );
  }
}

class _AppScrollBehavior extends MaterialScrollBehavior {
  const _AppScrollBehavior();

  @override
  ScrollPhysics getScrollPhysics(BuildContext context) {
    return const BouncingScrollPhysics(parent: AlwaysScrollableScrollPhysics());
  }
}

class _BootPage extends StatelessWidget {
  const _BootPage();

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            BrandMark(size: 64),
            SizedBox(height: 18),
            SizedBox(
              width: 26,
              height: 26,
              child: CircularProgressIndicator(strokeWidth: 2.4),
            ),
          ],
        ),
      ),
    );
  }
}
