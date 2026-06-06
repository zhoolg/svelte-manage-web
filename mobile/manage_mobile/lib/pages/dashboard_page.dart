import 'package:flutter/material.dart';

import '../core/app_theme.dart';
import '../core/session_controller.dart';
import '../models/api_models.dart';
import '../models/module_models.dart';
import '../widgets/async_state.dart';
import '../widgets/info_card.dart';
import '../widgets/polished_surface.dart';
import 'modules_page.dart';
import 'system_status_page.dart';

class DashboardPage extends StatefulWidget {
  const DashboardPage({super.key, required this.controller});

  final SessionController controller;

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends State<DashboardPage> {
  bool _loading = true;
  String _error = '';
  SystemStatus? _status;
  LicenseStatus? _license;
  List<AppModule> _modules = [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const LoadingView(message: '正在读取后台状态');
    }
    if (_error.isNotEmpty) {
      return ErrorView(message: _error, onRetry: _load);
    }

    final status = _status;
    final license = _license;
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.only(bottom: 24),
        children: [
          _WelcomePanel(
            userName: widget.controller.user?.name ?? '-',
            account: widget.controller.user?.username ?? '-',
            serverUrl: widget.controller.serverUrl,
            status: status?.status ?? '-',
          ),
          if (status != null || license != null)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 14, 16, 0),
              child: _OperationStats(
                status: status,
                license: license,
                uptime: _duration(intValue(status?.runtime['uptimeSeconds'])),
                memory: status == null ? '-' : _memory(status.resources),
              ),
            ),
          const SectionHeader(title: '常用功能', subtitle: '常用运维入口'),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: _modules.isEmpty
                ? const EmptyState(
                    icon: Icons.apps_outlined,
                    title: '暂无快捷入口',
                    subtitle: '可用功能会显示在这里',
                  )
                : _ShortcutGrid(
                    modules: _modules.take(8).toList(),
                    controller: widget.controller,
                  ),
          ),
          if (status != null)
            InfoCard(
              title: '系统详情',
              icon: Icons.monitor_heart_outlined,
              trailing: StatusPill(text: status.status),
              children: [
                InfoRow(
                  label: '应用',
                  value: stringValue(status.runtime['application']),
                ),
                InfoRow(
                  label: '环境',
                  value: stringValue(status.runtime['environment']),
                ),
                InfoRow(
                  label: '运行时长',
                  value: _duration(intValue(status.runtime['uptimeSeconds'])),
                ),
                InfoRow(label: '堆内存', value: _memory(status.resources)),
              ],
            ),
          if (license != null)
            InfoCard(
              title: '授权信息',
              icon: Icons.verified_outlined,
              color: license.allowed ? AppTheme.success : AppTheme.danger,
              trailing: StatusPill(
                text: license.allowed ? 'ALLOWED' : 'DENIED',
              ),
              children: [
                InfoRow(label: '状态', value: license.message),
                InfoRow(label: '客户', value: license.issuedTo),
                InfoRow(label: '版本', value: license.edition),
                InfoRow(label: '到期', value: license.expiresAt),
              ],
            ),
          const SizedBox(height: 12),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: OutlinedButton.icon(
              onPressed: () => Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) =>
                      SystemStatusPage(controller: widget.controller),
                ),
              ),
              icon: const Icon(Icons.monitor_heart_outlined),
              label: const Text('查看完整系统状态'),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = '';
    });
    try {
      final results = await Future.wait([
        widget.controller.api.systemStatus(),
        widget.controller.api.licenseStatus(),
        widget.controller.api.modules(),
      ]);
      setState(() {
        _status = results[0] as SystemStatus;
        _license = results[1] as LicenseStatus;
        _modules = flattenModules(
          results[2] as List<AppModule>,
        ).where((module) => module.path != '/').toList();
      });
    } on ApiException catch (error) {
      setState(() => _error = error.message);
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  String _duration(int seconds) {
    if (seconds <= 0) {
      return '-';
    }
    final days = seconds ~/ 86400;
    final hours = (seconds % 86400) ~/ 3600;
    final minutes = (seconds % 3600) ~/ 60;
    if (days > 0) {
      return '$days 天 $hours 小时';
    }
    if (hours > 0) {
      return '$hours 小时 $minutes 分钟';
    }
    return '$minutes 分钟';
  }

  String _memory(Map<String, dynamic> resources) {
    final used = intValue(resources['heapUsedBytes']);
    final max = intValue(resources['heapMaxBytes']);
    if (used <= 0 || max <= 0) {
      return '-';
    }
    return '${_bytes(used)} / ${_bytes(max)}';
  }

  String _bytes(int value) {
    if (value > 1024 * 1024 * 1024) {
      return '${(value / 1024 / 1024 / 1024).toStringAsFixed(1)} GB';
    }
    if (value > 1024 * 1024) {
      return '${(value / 1024 / 1024).toStringAsFixed(1)} MB';
    }
    return '${(value / 1024).toStringAsFixed(1)} KB';
  }
}

class _WelcomePanel extends StatelessWidget {
  const _WelcomePanel({
    required this.userName,
    required this.account,
    required this.serverUrl,
    required this.status,
  });

  final String userName;
  final String account;
  final String serverUrl;
  final String status;

  @override
  Widget build(BuildContext context) {
    final ok = status.toUpperCase() == 'UP';
    return AppPanel(
      margin: const EdgeInsets.fromLTRB(16, 8, 16, 2),
      padding: const EdgeInsets.all(16),
      prominent: true,
      borderColor: Colors.white.withValues(alpha: 0.10),
      gradient: const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [AppTheme.ink, AppTheme.inkSoft],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 4,
                height: 42,
                decoration: BoxDecoration(
                  color: ok ? AppTheme.info : AppTheme.danger,
                  borderRadius: BorderRadius.circular(999),
                ),
              ),
              const SizedBox(width: 10),
              SurfaceIcon(
                icon: Icons.dns_rounded,
                size: 40,
                color: ok ? AppTheme.info : AppTheme.danger,
                background: Colors.white.withValues(alpha: 0.10),
              ),
              const SizedBox(width: 11),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      ok ? '控制台运行正常' : '控制台状态异常',
                      style: Theme.of(
                        context,
                      ).textTheme.titleMedium?.copyWith(color: Colors.white),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      serverUrl,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                        color: Colors.white.withValues(alpha: 0.66),
                      ),
                    ),
                  ],
                ),
              ),
              _DarkStatusPill(text: status, ok: ok),
            ],
          ),
          const SizedBox(height: 15),
          Container(height: 1, color: Colors.white.withValues(alpha: 0.10)),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                flex: 3,
                child: _HeroMeta(
                  icon: Icons.account_circle_outlined,
                  label: '账号',
                  value: '$userName · $account',
                ),
              ),
              const SizedBox(width: 12),
              const Expanded(
                flex: 2,
                child: _HeroMeta(
                  icon: Icons.phone_android_rounded,
                  label: '终端',
                  value: 'Mobile',
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _DarkStatusPill extends StatelessWidget {
  const _DarkStatusPill({required this.text, required this.ok});

  final String text;
  final bool ok;

  @override
  Widget build(BuildContext context) {
    final color = ok ? AppTheme.info : AppTheme.danger;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.09),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: color.withValues(alpha: 0.38)),
      ),
      child: Text(
        text.isEmpty ? '-' : text,
        style: TextStyle(
          color: ok ? const Color(0xff67d9f5) : const Color(0xffffa0ae),
          fontSize: 12,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

class _HeroMeta extends StatelessWidget {
  const _HeroMeta({
    required this.icon,
    required this.label,
    required this.value,
  });

  final IconData icon;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 18, color: Colors.white.withValues(alpha: 0.72)),
        const SizedBox(width: 8),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: Colors.white.withValues(alpha: 0.54),
                ),
              ),
              const SizedBox(height: 2),
              Text(
                value,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: Colors.white,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _OperationStats extends StatelessWidget {
  const _OperationStats({
    required this.status,
    required this.license,
    required this.uptime,
    required this.memory,
  });

  final SystemStatus? status;
  final LicenseStatus? license;
  final String uptime;
  final String memory;

  @override
  Widget build(BuildContext context) {
    final environment = stringValue(status?.runtime['environment']);
    final allowed = license?.allowed ?? false;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Text('运行概览', style: Theme.of(context).textTheme.titleMedium),
            const Spacer(),
            Text(
              stringValue(status?.checkedAt, fallback: '实时状态'),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ),
        const SizedBox(height: 10),
        GridView.count(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          crossAxisCount: 2,
          mainAxisSpacing: 9,
          crossAxisSpacing: 9,
          childAspectRatio: 1.82,
          children: [
            _StatCell(
              label: '环境',
              value: environment,
              icon: Icons.layers_outlined,
              color: AppTheme.info,
            ),
            _StatCell(
              label: '运行时长',
              value: uptime,
              icon: Icons.timer_outlined,
              color: AppTheme.warning,
            ),
            _StatCell(
              label: '内存',
              value: memory,
              icon: Icons.memory_outlined,
              color: AppTheme.teal,
            ),
            _StatCell(
              label: '授权',
              value: license == null ? '-' : (allowed ? '有效' : '异常'),
              icon: Icons.verified_outlined,
              color: allowed ? AppTheme.accent : AppTheme.danger,
            ),
          ],
        ),
      ],
    );
  }
}

class _StatCell extends StatelessWidget {
  const _StatCell({
    required this.label,
    required this.value,
    required this.icon,
    required this.color,
  });

  final String label;
  final String value;
  final IconData icon;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return AppPanel(
      padding: const EdgeInsets.all(11),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
              SurfaceIcon(
                icon: icon,
                color: color,
                size: 30,
                background: color.withValues(alpha: 0.09),
              ),
            ],
          ),
          const Spacer(),
          SizedBox(
            width: double.infinity,
            child: FittedBox(
              alignment: Alignment.centerLeft,
              fit: BoxFit.scaleDown,
              child: Text(
                value.isEmpty ? '-' : value,
                maxLines: 1,
                style: Theme.of(context).textTheme.titleSmall,
              ),
            ),
          ),
          const SizedBox(height: 8),
          Container(
            width: 34,
            height: 3,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(999),
              gradient: LinearGradient(
                colors: [
                  color.withValues(alpha: 0.72),
                  color.withValues(alpha: 0.18),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ShortcutGrid extends StatelessWidget {
  const _ShortcutGrid({required this.modules, required this.controller});

  final List<AppModule> modules;
  final SessionController controller;

  @override
  Widget build(BuildContext context) {
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      itemCount: modules.take(6).length,
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        mainAxisSpacing: 9,
        crossAxisSpacing: 9,
        childAspectRatio: 2.25,
      ),
      itemBuilder: (context, index) {
        final module = modules[index];
        return _ShortcutItem(
          module: module,
          onTap: () => openModule(context, controller, module),
        );
      },
    );
  }
}

class _ShortcutItem extends StatelessWidget {
  const _ShortcutItem({required this.module, required this.onTap});

  final AppModule module;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final color = colorFor(module.icon);
    final isData = module.crud != null;
    return AppPanel(
      onTap: onTap,
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
      child: Row(
        children: [
          SurfaceIcon(
            icon: iconFor(module.icon),
            color: color,
            size: 34,
            background: color.withValues(alpha: 0.09),
          ),
          const SizedBox(width: 9),
          Expanded(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  module.label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.titleSmall,
                ),
                const SizedBox(height: 3),
                Text(
                  isData ? '数据管理' : '功能页面',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          ),
          const SizedBox(width: 6),
          Container(
            width: 22,
            height: 22,
            decoration: BoxDecoration(
              color: AppTheme.surfaceMuted,
              borderRadius: BorderRadius.circular(AppTheme.radius),
              border: Border.all(color: AppTheme.border),
            ),
            child: const Icon(
              Icons.arrow_forward_rounded,
              size: 14,
              color: AppTheme.textFaint,
            ),
          ),
        ],
      ),
    );
  }
}
