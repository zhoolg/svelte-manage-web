import 'package:flutter/material.dart';

import '../core/app_theme.dart';
import '../core/session_controller.dart';
import '../models/api_models.dart';
import '../models/module_models.dart';
import '../widgets/async_state.dart';
import '../widgets/polished_surface.dart';

class CrudPage extends StatefulWidget {
  const CrudPage({super.key, required this.controller, required this.module});

  final SessionController controller;
  final AppModule module;

  @override
  State<CrudPage> createState() => _CrudPageState();
}

class _CrudPageState extends State<CrudPage> {
  bool _loading = true;
  String _error = '';
  int _pageNum = 1;
  final int _pageSize = 10;
  int _total = 0;
  List<Map<String, dynamic>> _rows = [];
  Map<String, Object?> _search = {};

  CrudConfig get _crud => widget.module.crud!;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_crud.title),
        actions: [
          IconButton(
            tooltip: '搜索',
            onPressed: _crud.search.isEmpty ? null : _openSearch,
            icon: const Icon(Icons.search),
          ),
          IconButton(
            tooltip: '刷新',
            onPressed: _load,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      floatingActionButton:
          _crud.showAdd && _crud.form.isNotEmpty && _crud.api.add != null
          ? FloatingActionButton.extended(
              onPressed: () => _openForm(),
              icon: const Icon(Icons.add),
              label: const Text('新增'),
            )
          : null,
      body: _body(),
    );
  }

  Widget _body() {
    if (_loading) {
      return const LoadingView(message: '正在加载数据');
    }
    if (_error.isNotEmpty) {
      return ErrorView(message: _error, onRetry: _load);
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 10, 16, 96),
        children: [
          AppPageHeader(
            title: _crud.title,
            subtitle: '共 $_total 条数据',
            icon: Icons.table_rows_outlined,
            trailing: _search.isEmpty
                ? DetailPill(
                    icon: Icons.layers_outlined,
                    label: '第 $_pageNum 页',
                    color: AppTheme.indigo,
                  )
                : const DetailPill(
                    icon: Icons.filter_alt_outlined,
                    label: '筛选中',
                    color: AppTheme.warning,
                  ),
            padding: const EdgeInsets.fromLTRB(0, 0, 0, 12),
          ),
          if (_search.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  for (final entry in _search.entries)
                    if (entry.value != null &&
                        entry.value.toString().isNotEmpty)
                      InputChip(
                        label: Text(
                          '${_fieldLabel(entry.key)}: ${entry.value}',
                        ),
                        onDeleted: () {
                          setState(() {
                            _search = Map.from(_search)..remove(entry.key);
                            _pageNum = 1;
                          });
                          _load();
                        },
                      ),
                ],
              ),
            ),
          if (_rows.isEmpty)
            const _EmptyList()
          else
            for (final row in _rows)
              Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: _RowCard(
                  row: row,
                  columns: _crud.columns,
                  onEdit: _crud.form.isEmpty || _crud.api.edit == null
                      ? null
                      : () => _openForm(row: row),
                  onDelete: _crud.api.delete == null
                      ? null
                      : () => _delete(row),
                ),
              ),
          const SizedBox(height: 8),
          _Pager(
            pageNum: _pageNum,
            pageSize: _pageSize,
            total: _total,
            onPrevious: _pageNum <= 1
                ? null
                : () {
                    setState(() => _pageNum -= 1);
                    _load();
                  },
            onNext: _pageNum * _pageSize >= _total
                ? null
                : () {
                    setState(() => _pageNum += 1);
                    _load();
                  },
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
      final page = await widget.controller.api.page(_crud.api.list, {
        'pageNum': _pageNum,
        'pageSize': _pageSize,
        ..._search,
      });
      setState(() {
        _rows = page.list;
        _total = page.total;
      });
    } on ApiException catch (error) {
      setState(() => _error = error.message);
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  Future<void> _openSearch() async {
    final result = await showModalBottomSheet<Map<String, Object?>>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (_) => CrudFormSheet(
        title: '搜索',
        fields: _crud.search,
        initialValues: _search,
        submitText: '搜索',
        includeEmptyValues: false,
      ),
    );
    if (result == null) {
      return;
    }
    setState(() {
      _search = result;
      _pageNum = 1;
    });
    _load();
  }

  Future<void> _openForm({Map<String, dynamic>? row}) async {
    final editing = row != null;
    final result = await showModalBottomSheet<Map<String, Object?>>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (_) => CrudFormSheet(
        title: editing ? '编辑${_crud.title}' : '新增${_crud.title}',
        fields: _crud.form,
        initialValues: row ?? _defaultValues(),
        submitText: editing ? '保存' : '新增',
      ),
    );
    if (result == null) {
      return;
    }
    try {
      if (editing) {
        final payload = <String, Object?>{...row, ...result};
        await widget.controller.api.submit(_crud.api.edit!, payload);
      } else {
        await widget.controller.api.submit(_crud.api.add!, result);
      }
      if (mounted) {
        showMessage(context, editing ? '保存成功' : '新增成功');
      }
      _load();
    } on ApiException catch (error) {
      if (mounted) {
        showMessage(context, error.message);
      }
    }
  }

  Future<void> _delete(Map<String, dynamic> row) async {
    final id = row['id'];
    if (id == null) {
      showMessage(context, '缺少 id，无法删除');
      return;
    }
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('确认删除'),
        content: const Text('删除后不可恢复，是否继续？'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('取消'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: FilledButton.styleFrom(backgroundColor: AppTheme.danger),
            child: const Text('删除'),
          ),
        ],
      ),
    );
    if (confirmed != true) {
      return;
    }
    try {
      await widget.controller.api.submit('${_crud.api.delete}/$id', {});
      if (mounted) {
        showMessage(context, '删除成功');
      }
      _load();
    } on ApiException catch (error) {
      if (mounted) {
        showMessage(context, error.message);
      }
    }
  }

  Map<String, Object?> _defaultValues() {
    return {
      for (final field in _crud.form)
        if (field.defaultValue != null) field.field: field.defaultValue,
    };
  }

  String _fieldLabel(String field) {
    return [..._crud.search, ..._crud.form]
            .where((item) => item.field == field)
            .map((item) => item.label)
            .firstOrNull ??
        field;
  }
}

class _RowCard extends StatelessWidget {
  const _RowCard({
    required this.row,
    required this.columns,
    this.onEdit,
    this.onDelete,
  });

  final Map<String, dynamic> row;
  final List<CrudColumn> columns;
  final VoidCallback? onEdit;
  final VoidCallback? onDelete;

  @override
  Widget build(BuildContext context) {
    final visibleColumns = columns
        .where((column) => column.field != 'id')
        .take(6)
        .toList();
    return AppPanel(
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const SurfaceIcon(
                icon: Icons.article_outlined,
                size: 36,
                background: AppTheme.surfaceMuted,
                color: AppTheme.textMuted,
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  _title(),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
              ),
              if (onEdit != null)
                IconButton(
                  tooltip: '编辑',
                  onPressed: onEdit,
                  style: IconButton.styleFrom(
                    backgroundColor: AppTheme.surfaceMuted,
                    fixedSize: const Size(36, 36),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(AppTheme.radius),
                    ),
                  ),
                  icon: const Icon(Icons.edit_outlined),
                ),
              if (onDelete != null)
                IconButton(
                  tooltip: '删除',
                  onPressed: onDelete,
                  style: IconButton.styleFrom(
                    backgroundColor: AppTheme.danger.withValues(alpha: 0.09),
                    fixedSize: const Size(36, 36),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(AppTheme.radius),
                    ),
                  ),
                  icon: const Icon(
                    Icons.delete_outline,
                    color: AppTheme.danger,
                  ),
                ),
            ],
          ),
          const SizedBox(height: 10),
          DecoratedBox(
            decoration: BoxDecoration(
              color: AppTheme.surfaceMuted.withValues(alpha: 0.62),
              borderRadius: BorderRadius.circular(AppTheme.radius),
            ),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              child: Column(
                children: [
                  for (final column in visibleColumns)
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 5),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          SizedBox(
                            width: 86,
                            child: Text(
                              column.label,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: Theme.of(context).textTheme.bodySmall,
                            ),
                          ),
                          Expanded(child: _cell(context, column)),
                        ],
                      ),
                    ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  String _title() {
    const candidates = [
      'name',
      'title',
      'username',
      'applicationName',
      'key',
      'id',
    ];
    for (final field in candidates) {
      final value = row[field];
      if (value != null && value.toString().isNotEmpty) {
        return value.toString();
      }
    }
    final first = row.values
        .where((value) => value != null && value.toString().isNotEmpty)
        .firstOrNull;
    return first?.toString() ?? '数据项';
  }

  Widget _cell(BuildContext context, CrudColumn column) {
    final value = row[column.field];
    if (column.format == 'status') {
      final status = column.statusMap[value?.toString() ?? ''];
      if (status != null) {
        return Align(
          alignment: Alignment.centerLeft,
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: _statusColor(status.color).withValues(alpha: 0.10),
              borderRadius: BorderRadius.circular(999),
            ),
            child: Text(
              status.label,
              style: TextStyle(
                color: _statusColor(status.color),
                fontWeight: FontWeight.w700,
                fontSize: 12,
              ),
            ),
          ),
        );
      }
    }
    if (column.format == 'switch') {
      final enabled = boolValue(value);
      return Text(
        enabled ? '启用' : '禁用',
        style: Theme.of(context).textTheme.bodyMedium,
      );
    }
    if (column.format == 'tags' && value is List) {
      return Wrap(
        spacing: 6,
        runSpacing: 6,
        children: value
            .map((item) => Chip(label: Text(item.toString())))
            .toList(),
      );
    }
    return Text(
      _display(value),
      maxLines: 3,
      overflow: TextOverflow.ellipsis,
      style: Theme.of(context).textTheme.bodyMedium,
    );
  }

  String _display(Object? value) {
    if (value == null) {
      return '-';
    }
    if (value is List) {
      return value.join(', ');
    }
    return value.toString().isEmpty ? '-' : value.toString();
  }

  Color _statusColor(String color) {
    return switch (color) {
      'success' || 'green' => AppTheme.success,
      'warning' || 'orange' || 'yellow' => const Color(0xffca8a04),
      'danger' || 'red' => AppTheme.danger,
      'primary' || 'blue' => AppTheme.accent,
      _ => const Color(0xff64748b),
    };
  }
}

class CrudFormSheet extends StatefulWidget {
  const CrudFormSheet({
    super.key,
    required this.title,
    required this.fields,
    required this.initialValues,
    required this.submitText,
    this.includeEmptyValues = true,
  });

  final String title;
  final List<CrudField> fields;
  final Map<String, Object?> initialValues;
  final String submitText;
  final bool includeEmptyValues;

  @override
  State<CrudFormSheet> createState() => _CrudFormSheetState();
}

class _CrudFormSheetState extends State<CrudFormSheet> {
  final _formKey = GlobalKey<FormState>();
  final Map<String, Object?> _values = {};
  final Map<String, TextEditingController> _controllers = {};

  @override
  void initState() {
    super.initState();
    _values.addAll(widget.initialValues);
    for (final field in widget.fields) {
      final value = _values[field.field] ?? field.defaultValue;
      _values[field.field] = value;
      if (_usesTextController(field)) {
        _controllers[field.field] = TextEditingController(
          text: value?.toString() ?? '',
        );
      }
    }
  }

  @override
  void dispose() {
    for (final controller in _controllers.values) {
      controller.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        left: 16,
        right: 16,
        top: 10,
        bottom: MediaQuery.of(context).viewInsets.bottom + 16,
      ),
      child: Form(
        key: _formKey,
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Center(
                child: Container(
                  width: 38,
                  height: 4,
                  decoration: BoxDecoration(
                    color: AppTheme.border,
                    borderRadius: BorderRadius.circular(999),
                  ),
                ),
              ),
              const SizedBox(height: 14),
              Row(
                children: [
                  Expanded(
                    child: Text(
                      widget.title,
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ),
                  IconButton(
                    onPressed: () => Navigator.of(context).pop(),
                    icon: const Icon(Icons.close),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              for (final field in widget.fields)
                Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: _field(field),
                ),
              FilledButton(onPressed: _submit, child: Text(widget.submitText)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _field(CrudField field) {
    String? validator(String? value) {
      if (field.required && (value == null || value.trim().isEmpty)) {
        return '请填写${field.label}';
      }
      return null;
    }

    if (field.type == 'select' || field.type == 'radio') {
      final current = _values[field.field]?.toString();
      return DropdownButtonFormField<String>(
        initialValue:
            field.options.any((option) => option.value.toString() == current)
            ? current
            : null,
        decoration: InputDecoration(labelText: field.label),
        items: field.options
            .map(
              (option) => DropdownMenuItem<String>(
                value: option.value.toString(),
                child: Text(option.label),
              ),
            )
            .toList(),
        validator: (_) =>
            field.required &&
                (_values[field.field] == null ||
                    _values[field.field].toString().isEmpty)
            ? '请选择${field.label}'
            : null,
        onChanged: field.disabled
            ? null
            : (value) {
                final option = field.options
                    .where((item) => item.value.toString() == value)
                    .firstOrNull;
                setState(() => _values[field.field] = option?.value ?? value);
              },
      );
    }

    if (field.type == 'switch') {
      return SwitchListTile(
        contentPadding: EdgeInsets.zero,
        title: Text(field.label),
        value: boolValue(_values[field.field]),
        onChanged: field.disabled
            ? null
            : (value) => setState(() => _values[field.field] = value),
      );
    }

    if (field.type == 'checkbox') {
      final selected = Set<String>.from(
        listValue(_values[field.field]).map((item) => item.toString()),
      );
      return InputDecorator(
        decoration: InputDecoration(labelText: field.label),
        child: Column(
          children: [
            for (final option in field.options)
              CheckboxListTile(
                dense: true,
                contentPadding: EdgeInsets.zero,
                title: Text(option.label),
                value: selected.contains(option.value.toString()),
                onChanged: field.disabled
                    ? null
                    : (checked) {
                        setState(() {
                          if (checked == true) {
                            selected.add(option.value.toString());
                          } else {
                            selected.remove(option.value.toString());
                          }
                          _values[field.field] = selected.toList();
                        });
                      },
              ),
          ],
        ),
      );
    }

    if (field.type == 'date' || field.type == 'datetime') {
      final controller = _controllers[field.field]!;
      return TextFormField(
        controller: controller,
        readOnly: true,
        validator: validator,
        decoration: InputDecoration(
          labelText: field.label,
          suffixIcon: const Icon(Icons.calendar_today_outlined),
        ),
        onTap: field.disabled ? null : () => _pickDate(field, controller),
      );
    }

    if (field.type == 'textarea' || field.type == 'editor') {
      final controller = _controllers[field.field]!;
      return TextFormField(
        controller: controller,
        minLines: field.rows,
        maxLines: field.rows + 2,
        enabled: !field.disabled,
        validator: validator,
        decoration: InputDecoration(
          labelText: field.label,
          hintText: field.placeholder,
        ),
        onChanged: (value) => _values[field.field] = value,
      );
    }

    final controller = _controllers[field.field]!;
    return TextFormField(
      controller: controller,
      enabled: !field.disabled,
      validator: validator,
      keyboardType: field.type == 'number'
          ? TextInputType.number
          : TextInputType.text,
      decoration: InputDecoration(
        labelText: field.label,
        hintText: field.placeholder,
      ),
      onChanged: (value) {
        _values[field.field] = field.type == 'number'
            ? (num.tryParse(value) ?? value)
            : value;
      },
    );
  }

  Future<void> _pickDate(
    CrudField field,
    TextEditingController controller,
  ) async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: now,
      firstDate: DateTime(now.year - 10),
      lastDate: DateTime(now.year + 20),
    );
    if (picked == null) {
      return;
    }
    var value =
        '${picked.year.toString().padLeft(4, '0')}-${picked.month.toString().padLeft(2, '0')}-${picked.day.toString().padLeft(2, '0')}';
    if (field.type == 'datetime' && mounted) {
      final time = await showTimePicker(
        context: context,
        initialTime: TimeOfDay.now(),
      );
      if (time != null) {
        value =
            '$value ${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}:00';
      }
    }
    setState(() {
      controller.text = value;
      _values[field.field] = value;
    });
  }

  void _submit() {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    final result = <String, Object?>{};
    for (final entry in _values.entries) {
      final value = entry.value;
      if (!widget.includeEmptyValues &&
          (value == null || value.toString().isEmpty)) {
        continue;
      }
      result[entry.key] = value;
    }
    Navigator.of(context).pop(result);
  }

  bool _usesTextController(CrudField field) {
    return !['select', 'radio', 'switch', 'checkbox'].contains(field.type);
  }
}

class _Pager extends StatelessWidget {
  const _Pager({
    required this.pageNum,
    required this.pageSize,
    required this.total,
    required this.onPrevious,
    required this.onNext,
  });

  final int pageNum;
  final int pageSize;
  final int total;
  final VoidCallback? onPrevious;
  final VoidCallback? onNext;

  @override
  Widget build(BuildContext context) {
    final totalPages = total == 0 ? 1 : ((total + pageSize - 1) ~/ pageSize);
    return AppPanel(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
      child: Row(
        children: [
          IconButton(
            tooltip: '上一页',
            onPressed: onPrevious,
            icon: const Icon(Icons.chevron_left_rounded),
          ),
          Expanded(
            child: Text(
              '$pageNum / $totalPages，共 $total 条',
              textAlign: TextAlign.center,
              style: Theme.of(
                context,
              ).textTheme.bodySmall?.copyWith(color: AppTheme.textMuted),
            ),
          ),
          IconButton(
            tooltip: '下一页',
            onPressed: onNext,
            icon: const Icon(Icons.chevron_right_rounded),
          ),
        ],
      ),
    );
  }
}

class _EmptyList extends StatelessWidget {
  const _EmptyList();

  @override
  Widget build(BuildContext context) {
    return const EmptyState(
      icon: Icons.inbox_outlined,
      title: '暂无数据',
      subtitle: '当前条件下没有可显示记录',
    );
  }
}
