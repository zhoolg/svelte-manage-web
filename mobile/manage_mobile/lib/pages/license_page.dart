import 'package:flutter/material.dart';

import '../core/app_theme.dart';
import '../core/session_controller.dart';
import '../models/api_models.dart';
import '../widgets/async_state.dart';
import '../widgets/info_card.dart';
import '../widgets/polished_surface.dart';

class CommercialLicensePage extends StatefulWidget {
  const CommercialLicensePage({super.key, required this.controller});

  final SessionController controller;

  @override
  State<CommercialLicensePage> createState() => _CommercialLicensePageState();
}

class _CommercialLicensePageState extends State<CommercialLicensePage> {
  bool _loading = true;
  String _error = '';
  LicenseStatus? _status;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const LoadingView(message: '正在读取授权状态');
    }
    if (_error.isNotEmpty) {
      return ErrorView(message: _error, onRetry: _load);
    }
    final status = _status;
    if (status == null) {
      return ErrorView(message: '没有授权数据', onRetry: _load);
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.only(bottom: 24),
        children: [
          _LicenseHero(status: status),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 10, 16, 0),
            child: InsightStrip(
              items: [
                InsightItem(
                  label: '版本',
                  value: status.edition,
                  icon: Icons.workspace_premium_outlined,
                  color: AppTheme.indigo,
                ),
                InsightItem(
                  label: '需要授权',
                  value: status.requiresLicense ? '是' : '否',
                  icon: Icons.key_outlined,
                  color: AppTheme.warning,
                ),
                InsightItem(
                  label: '完整性',
                  value: status.integrityTrusted ? '可信' : '异常',
                  icon: Icons.security_outlined,
                  color: status.integrityTrusted
                      ? AppTheme.success
                      : AppTheme.danger,
                ),
              ],
            ),
          ),
          InfoCard(
            title: '授权详情',
            icon: Icons.workspace_premium_outlined,
            color: status.allowed ? AppTheme.success : AppTheme.danger,
            trailing: StatusPill(text: status.allowed ? 'ALLOWED' : 'DENIED'),
            children: [
              InfoRow(label: '状态码', value: status.code),
              InfoRow(label: '说明', value: status.message),
              InfoRow(label: '机器码', value: status.machineId),
              InfoRow(label: '需要授权', value: status.requiresLicense ? '是' : '否'),
            ],
          ),
          InfoCard(
            title: '授权信息',
            icon: Icons.badge_outlined,
            children: [
              InfoRow(label: '客户', value: status.issuedTo),
              InfoRow(label: '版本', value: status.edition),
              InfoRow(label: '过期时间', value: status.expiresAt),
            ],
          ),
          InfoCard(
            title: '完整性',
            icon: Icons.security_outlined,
            color: status.integrityTrusted ? AppTheme.success : AppTheme.danger,
            trailing: StatusPill(
              text: status.integrityTrusted ? 'TRUSTED' : 'UNTRUSTED',
            ),
            children: [
              InfoRow(label: '状态码', value: status.integrityCode),
              InfoRow(label: '说明', value: status.integrityMessage),
              InfoRow(label: '检查时间', value: status.integrityCheckedAt),
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
      final status = await widget.controller.api.licenseStatus();
      setState(() => _status = status);
    } on ApiException catch (error) {
      setState(() => _error = error.message);
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }
}

class _LicenseHero extends StatelessWidget {
  const _LicenseHero({required this.status});

  final LicenseStatus status;

  @override
  Widget build(BuildContext context) {
    final allowed = status.allowed;
    final color = allowed ? AppTheme.success : AppTheme.danger;
    return AppPanel(
      margin: const EdgeInsets.fromLTRB(16, 8, 16, 2),
      padding: const EdgeInsets.all(16),
      prominent: true,
      borderColor: color.withValues(alpha: 0.18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              SurfaceIcon(
                icon: allowed
                    ? Icons.verified_user_outlined
                    : Icons.gpp_bad_outlined,
                color: color,
                size: 48,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      allowed ? '授权有效' : '授权异常',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                    const SizedBox(height: 4),
                    Text(
                      status.message.isEmpty ? status.code : status.message,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                ),
              ),
              StatusPill(text: allowed ? 'VALID' : 'DENIED'),
            ],
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: _HeroValue(label: '客户', value: status.issuedTo),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: _HeroValue(label: '到期', value: status.expiresAt),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _HeroValue extends StatelessWidget {
  const _HeroValue({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: AppTheme.surfaceMuted,
        borderRadius: BorderRadius.circular(AppTheme.radius),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: Theme.of(context).textTheme.bodySmall),
          const SizedBox(height: 3),
          Text(
            value.isEmpty ? '-' : value,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.titleSmall,
          ),
        ],
      ),
    );
  }
}
