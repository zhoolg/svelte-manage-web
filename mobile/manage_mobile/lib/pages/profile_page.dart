import 'package:flutter/material.dart';

import '../core/app_theme.dart';
import '../core/session_controller.dart';
import '../widgets/async_state.dart';
import '../widgets/info_card.dart';
import '../widgets/polished_surface.dart';

class ProfilePage extends StatelessWidget {
  const ProfilePage({super.key, required this.controller});

  final SessionController controller;

  @override
  Widget build(BuildContext context) {
    final user = controller.user;
    return ListView(
      padding: const EdgeInsets.only(bottom: 24),
      children: [
        AppPanel(
          margin: const EdgeInsets.fromLTRB(16, 8, 16, 2),
          padding: const EdgeInsets.all(16),
          prominent: true,
          borderColor: AppTheme.accent.withValues(alpha: 0.18),
          child: Row(
            children: [
              const SurfaceIcon(
                icon: Icons.person_outline,
                size: 48,
                color: AppTheme.indigo,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      user?.name ?? '-',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                    const SizedBox(height: 4),
                    Text(
                      user?.username ?? '-',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 10, 16, 0),
          child: InsightStrip(
            items: [
              InsightItem(
                label: '角色',
                value: user?.roles.length.toString() ?? '0',
                icon: Icons.admin_panel_settings_outlined,
                color: AppTheme.indigo,
              ),
              InsightItem(
                label: '会话',
                value: '已登录',
                icon: Icons.lock_outline,
                color: AppTheme.success,
              ),
            ],
          ),
        ),
        InfoCard(
          title: '账号信息',
          icon: Icons.badge_outlined,
          children: [
            InfoRow(label: '姓名', value: user?.name ?? '-'),
            InfoRow(label: '账号', value: user?.username ?? '-'),
            InfoRow(label: '角色', value: user?.roles.join(', ') ?? '-'),
            InfoRow(label: '服务器', value: controller.serverUrl),
          ],
        ),
        Padding(
          padding: const EdgeInsets.all(16),
          child: FilledButton.icon(
            style: FilledButton.styleFrom(backgroundColor: AppTheme.danger),
            onPressed: () async {
              try {
                await controller.logout();
              } catch (_) {
                if (context.mounted) {
                  showMessage(context, '已清理本地登录状态');
                }
              }
            },
            icon: const Icon(Icons.logout),
            label: const Text('退出登录'),
          ),
        ),
      ],
    );
  }
}
