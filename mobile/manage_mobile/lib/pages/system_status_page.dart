import 'package:flutter/material.dart';

import '../core/app_theme.dart';
import '../core/session_controller.dart';
import '../models/api_models.dart';
import '../widgets/async_state.dart';
import '../widgets/info_card.dart';
import '../widgets/polished_surface.dart';

class SystemStatusPage extends StatefulWidget {
  const SystemStatusPage({super.key, required this.controller});

  final SessionController controller;

  @override
  State<SystemStatusPage> createState() => _SystemStatusPageState();
}

class _SystemStatusPageState extends State<SystemStatusPage> {
  bool _loading = true;
  String _error = '';
  SystemStatus? _status;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) {
    final content = _body();
    if (ModalRoute.of(context)?.isFirst ?? false) {
      return content;
    }
    return Scaffold(
      appBar: AppBar(title: const Text('系统状态')),
      body: content,
    );
  }

  Widget _body() {
    if (_loading) {
      return const LoadingView(message: '正在检查系统状态');
    }
    if (_error.isNotEmpty) {
      return ErrorView(message: _error, onRetry: _load);
    }
    final status = _status;
    if (status == null) {
      return ErrorView(message: '没有状态数据', onRetry: _load);
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.only(bottom: 24),
        children: [
          _StatusHero(status: status),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 10, 16, 0),
            child: InsightStrip(
              items: [
                InsightItem(
                  label: '环境',
                  value: stringValue(status.runtime['environment']),
                  icon: Icons.layers_outlined,
                  color: AppTheme.indigo,
                ),
                InsightItem(
                  label: '系统 CPU',
                  value: _percent(status.resources['systemCpuLoad']),
                  icon: Icons.speed_outlined,
                  color: AppTheme.warning,
                ),
                InsightItem(
                  label: '会话',
                  value: stringValue(status.security['activeSessions']),
                  icon: Icons.lock_outline,
                  color: AppTheme.success,
                ),
              ],
            ),
          ),
          InfoCard(
            title: '运行状态',
            icon: Icons.monitor_heart_outlined,
            trailing: StatusPill(text: status.status),
            children: [
              InfoRow(label: '检查时间', value: status.checkedAt),
              InfoRow(
                label: '应用',
                value: stringValue(status.runtime['application']),
              ),
              InfoRow(
                label: '环境',
                value: stringValue(status.runtime['environment']),
              ),
              InfoRow(
                label: 'Spring Boot',
                value: stringValue(status.runtime['springBootVersion']),
              ),
              InfoRow(
                label: 'Java',
                value: stringValue(status.runtime['javaVersion']),
              ),
              InfoRow(label: '系统', value: stringValue(status.runtime['os'])),
            ],
          ),
          InfoCard(
            title: '资源',
            icon: Icons.memory_outlined,
            color: AppTheme.indigo,
            children: [
              InfoRow(
                label: 'CPU 核心',
                value: stringValue(status.resources['processors']),
              ),
              InfoRow(
                label: '系统 CPU',
                value: _percent(status.resources['systemCpuLoad']),
              ),
              InfoRow(
                label: '进程 CPU',
                value: _percent(status.resources['processCpuLoad']),
              ),
              InfoRow(
                label: '堆内存',
                value: _bytesPair(
                  status.resources['heapUsedBytes'],
                  status.resources['heapMaxBytes'],
                ),
              ),
              InfoRow(
                label: '磁盘',
                value: _bytesPair(
                  status.resources['diskUsedBytes'],
                  status.resources['diskTotalBytes'],
                ),
              ),
            ],
          ),
          InfoCard(
            title: '依赖检查',
            icon: Icons.hub_outlined,
            color: AppTheme.success,
            children: [
              _component('数据库', status.database),
              _component('Redis', status.redis),
              _component('AI 工厂', status.aiFactory),
              for (final check in status.checks)
                InfoRow(
                  label: stringValue(
                    check['label'],
                    fallback: stringValue(check['key']),
                  ),
                  value:
                      '${stringValue(check['status'])} ${stringValue(check['message'])}',
                ),
            ],
          ),
          InfoCard(
            title: '安全会话',
            icon: Icons.lock_outline,
            color: AppTheme.warning,
            children: [
              InfoRow(
                label: '活跃会话',
                value: stringValue(status.security['activeSessions']),
              ),
              InfoRow(
                label: '登录失败',
                value: stringValue(status.security['recentLoginFailures']),
              ),
              InfoRow(
                label: '锁定 IP',
                value: stringValue(status.security['lockedIpCount']),
              ),
              InfoRow(
                label: '最后登录',
                value: stringValue(status.security['lastAdminLoginTime']),
              ),
            ],
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
      final status = await widget.controller.api.systemStatus();
      setState(() => _status = status);
    } on ApiException catch (error) {
      setState(() => _error = error.message);
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  Widget _component(String label, Map<String, dynamic> data) {
    return InfoRow(
      label: label,
      value: '${stringValue(data['status'])} ${stringValue(data['message'])}',
    );
  }

  String _percent(Object? value) {
    final number = value is num
        ? value.toDouble()
        : double.tryParse(value?.toString() ?? '');
    if (number == null || number < 0) {
      return '-';
    }
    return '${(number * 100).toStringAsFixed(1)}%';
  }

  String _bytesPair(Object? used, Object? total) {
    return '${_bytes(intValue(used))} / ${_bytes(intValue(total))}';
  }

  String _bytes(int value) {
    if (value <= 0) {
      return '-';
    }
    if (value > 1024 * 1024 * 1024) {
      return '${(value / 1024 / 1024 / 1024).toStringAsFixed(1)} GB';
    }
    if (value > 1024 * 1024) {
      return '${(value / 1024 / 1024).toStringAsFixed(1)} MB';
    }
    return '${(value / 1024).toStringAsFixed(1)} KB';
  }
}

class _StatusHero extends StatelessWidget {
  const _StatusHero({required this.status});

  final SystemStatus status;

  @override
  Widget build(BuildContext context) {
    final ok = status.status.toUpperCase() == 'UP';
    final color = ok ? AppTheme.success : AppTheme.danger;
    return AppPanel(
      margin: const EdgeInsets.fromLTRB(16, 8, 16, 2),
      padding: const EdgeInsets.all(16),
      prominent: true,
      borderColor: color.withValues(alpha: 0.18),
      child: Row(
        children: [
          SurfaceIcon(
            icon: ok ? Icons.check_circle_outline : Icons.error_outline,
            color: color,
            size: 48,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '系统${ok ? '正常' : '异常'}',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                const SizedBox(height: 4),
                Text(
                  status.checkedAt,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          ),
          StatusPill(text: status.status),
        ],
      ),
    );
  }
}
