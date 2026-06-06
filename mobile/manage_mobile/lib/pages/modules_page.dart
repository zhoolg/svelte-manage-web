import 'package:flutter/material.dart';

import '../core/app_theme.dart';
import '../core/session_controller.dart';
import '../models/api_models.dart';
import '../models/module_models.dart';
import '../widgets/async_state.dart';
import '../widgets/polished_surface.dart';
import 'crud_page.dart';
import 'system_status_page.dart';

class ModulesPage extends StatefulWidget {
  const ModulesPage({super.key, required this.controller});

  final SessionController controller;

  @override
  State<ModulesPage> createState() => _ModulesPageState();
}

class _ModulesPageState extends State<ModulesPage> {
  bool _loading = true;
  String _error = '';
  String _keyword = '';
  ModuleScope _scope = ModuleScope.all;
  List<AppModule> _modules = [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const LoadingView(message: '正在读取功能菜单');
    }
    if (_error.isNotEmpty) {
      return ErrorView(message: _error, onRetry: _load);
    }
    final visible = _modules.where((module) {
      final text = '${module.label} ${module.id}'.toLowerCase();
      final keywordMatched = text.contains(_keyword.toLowerCase());
      final scopeMatched = switch (_scope) {
        ModuleScope.all => true,
        ModuleScope.crud => module.crud != null,
        ModuleScope.page => module.crud == null,
      };
      return keywordMatched && scopeMatched;
    }).toList();

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          AppPageHeader(
            title: '功能矩阵',
            subtitle: '${visible.length} 个可用入口',
            icon: Icons.apps_rounded,
            padding: const EdgeInsets.fromLTRB(0, 0, 0, 12),
          ),
          AppPanel(
            padding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                TextField(
                  decoration: const InputDecoration(
                    prefixIcon: Icon(Icons.search),
                    hintText: '搜索功能',
                  ),
                  onChanged: (value) => setState(() => _keyword = value),
                ),
                const SizedBox(height: 12),
                SegmentedButton<ModuleScope>(
                  showSelectedIcon: false,
                  segments: const [
                    ButtonSegment(
                      value: ModuleScope.all,
                      icon: Icon(Icons.grid_view_outlined),
                      label: Text('全部'),
                    ),
                    ButtonSegment(
                      value: ModuleScope.crud,
                      icon: Icon(Icons.table_rows_outlined),
                      label: Text('数据'),
                    ),
                    ButtonSegment(
                      value: ModuleScope.page,
                      icon: Icon(Icons.auto_awesome_motion_outlined),
                      label: Text('页面'),
                    ),
                  ],
                  selected: {_scope},
                  onSelectionChanged: (value) =>
                      setState(() => _scope = value.first),
                ),
              ],
            ),
          ),
          const SectionHeader(
            title: '全部功能',
            padding: EdgeInsets.fromLTRB(0, 20, 0, 8),
          ),
          if (visible.isEmpty)
            const EmptyState(
              icon: Icons.search_off_outlined,
              title: '没有匹配功能',
              subtitle: '换一个关键词或筛选条件',
            )
          else
            for (final module in visible)
              Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: ModuleTile(
                  module: module,
                  onTap: () => openModule(context, widget.controller, module),
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
      final modules = await widget.controller.api.modules();
      setState(() {
        _modules = flattenModules(modules)
            .where(
              (module) =>
                  module.path != '/' &&
                  (module.crud != null || module.customPage != null),
            )
            .toList();
      });
    } on ApiException catch (error) {
      setState(() => _error = error.message);
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }
}

enum ModuleScope { all, crud, page }

class ModuleTile extends StatelessWidget {
  const ModuleTile({super.key, required this.module, required this.onTap});

  final AppModule module;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final color = colorFor(module.icon);
    final icon = iconFor(module.icon);
    final typeIcon = module.crud == null
        ? Icons.auto_awesome_motion_outlined
        : Icons.table_rows_outlined;
    final typeLabel = module.crud == null ? '页面' : '数据';
    return LayoutBuilder(
      builder: (context, constraints) {
        if (constraints.maxWidth < 230) {
          return AppPanel(
            onTap: onTap,
            padding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    SurfaceIcon(icon: icon, color: color, size: 36),
                    const Spacer(),
                    const Icon(
                      Icons.chevron_right_rounded,
                      color: AppTheme.textFaint,
                    ),
                  ],
                ),
                const Spacer(),
                Text(
                  module.label,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.titleSmall,
                ),
                const SizedBox(height: 8),
                DetailPill(icon: typeIcon, label: typeLabel, color: color),
              ],
            ),
          );
        }
        return ActionListTile(
          onTap: onTap,
          icon: icon,
          color: color,
          title: module.label,
          subtitle: module.crud == null
              ? '功能页面'
              : '${module.crud!.title} · 数据管理',
          trailing: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              DetailPill(icon: typeIcon, label: typeLabel, color: color),
              const SizedBox(width: 8),
              const Icon(
                Icons.chevron_right_rounded,
                color: AppTheme.textFaint,
              ),
            ],
          ),
        );
      },
    );
  }
}

void openModule(
  BuildContext context,
  SessionController controller,
  AppModule module,
) {
  final page = switch (module.customPage) {
    'SystemStatus' => SystemStatusPage(controller: controller),
    'AiModules' => AiModulesPage(module: module),
    _ =>
      module.crud == null
          ? AiModulesPage(module: module)
          : CrudPage(controller: controller, module: module),
  };
  Navigator.of(context).push(MaterialPageRoute(builder: (_) => page));
}

IconData iconFor(String icon) {
  return switch (icon) {
    'home' => Icons.home_outlined,
    'users' || 'user-circle' => Icons.people_outline,
    'clipboard-check' => Icons.assignment_turned_in_outlined,
    'message-circle' => Icons.chat_bubble_outline,
    'question-circle' => Icons.help_outline,
    'file-text' => Icons.receipt_long_outlined,
    'book-open' => Icons.menu_book_outlined,
    'settings' || 'cog' => Icons.settings_outlined,
    'list-tree' => Icons.account_tree_outlined,
    'bell' => Icons.notifications_none_outlined,
    'activity' => Icons.monitor_heart_outlined,
    'sparkles' => Icons.auto_awesome,
    _ => Icons.circle_outlined,
  };
}

Color colorFor(String icon) {
  return switch (icon) {
    'users' || 'user-circle' => AppTheme.indigo,
    'clipboard-check' || 'file-text' => AppTheme.warning,
    'message-circle' || 'question-circle' => AppTheme.teal,
    'settings' || 'cog' || 'list-tree' => AppTheme.textMuted,
    'bell' => AppTheme.warning,
    'activity' => AppTheme.success,
    'sparkles' => AppTheme.violet,
    _ => AppTheme.accent,
  };
}

class AiModulesPage extends StatelessWidget {
  const AiModulesPage({super.key, required this.module});

  final AppModule module;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(module.label)),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
        children: [
          AppPageHeader(
            title: module.label,
            subtitle: 'AI 模块',
            icon: Icons.auto_awesome,
            padding: const EdgeInsets.fromLTRB(0, 0, 0, 12),
          ),
          AppPanel(
            prominent: true,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SurfaceIcon(
                  icon: Icons.auto_awesome,
                  color: Color(0xffbf5af2),
                ),
                const SizedBox(height: 12),
                Text(
                  module.label,
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                const SizedBox(height: 12),
                const SoftDivider(),
                const SizedBox(height: 12),
                Text(
                  'Web 设计器',
                  style: Theme.of(
                    context,
                  ).textTheme.bodyMedium?.copyWith(color: AppTheme.textMuted),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
