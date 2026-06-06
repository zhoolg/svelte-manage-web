import 'api_models.dart';

class AppModule {
  AppModule({
    required this.id,
    required this.label,
    required this.icon,
    required this.path,
    this.hidden = false,
    this.customPage,
    this.crud,
    this.children = const [],
  });

  final String id;
  final String label;
  final String icon;
  final String path;
  final bool hidden;
  final String? customPage;
  final CrudConfig? crud;
  final List<AppModule> children;

  factory AppModule.fromJson(Map<String, dynamic> json) {
    return AppModule(
      id: stringValue(json['id']),
      label: labelText(json['label'], fallback: stringValue(json['id'])),
      icon: stringValue(json['icon'], fallback: 'circle'),
      path: stringValue(json['path']),
      hidden: boolValue(json['hidden']),
      customPage: nullableString(json['customPage']),
      crud: json['crud'] is Map
          ? CrudConfig.fromJson(mapValue(json['crud']))
          : null,
      children: listValue(json['children'])
          .whereType<Map>()
          .map((item) => AppModule.fromJson(item.cast<String, dynamic>()))
          .toList(),
    );
  }
}

class CrudConfig {
  CrudConfig({
    required this.title,
    required this.apiBase,
    required this.api,
    required this.columns,
    required this.search,
    required this.form,
    required this.showAdd,
    required this.showExport,
  });

  final String title;
  final String apiBase;
  final CrudApi api;
  final List<CrudColumn> columns;
  final List<CrudField> search;
  final List<CrudField> form;
  final bool showAdd;
  final bool showExport;

  factory CrudConfig.fromJson(Map<String, dynamic> json) {
    final apiBase = stringValue(json['apiBase']);
    return CrudConfig(
      title: labelText(json['title'], fallback: '数据列表'),
      apiBase: apiBase,
      api: CrudApi.fromJson(apiBase, mapValue(json['api'])),
      columns: listValue(json['columns'])
          .whereType<Map>()
          .map((item) => CrudColumn.fromJson(item.cast<String, dynamic>()))
          .toList(),
      search: listValue(json['search'])
          .whereType<Map>()
          .map((item) => CrudField.fromJson(item.cast<String, dynamic>()))
          .toList(),
      form: listValue(json['form'])
          .whereType<Map>()
          .map((item) => CrudField.fromJson(item.cast<String, dynamic>()))
          .toList(),
      showAdd: json.containsKey('showAdd') ? boolValue(json['showAdd']) : true,
      showExport: boolValue(json['showExport']),
    );
  }
}

class CrudApi {
  CrudApi({
    required this.list,
    this.add,
    this.edit,
    this.delete,
    this.workflow,
  });

  final String list;
  final String? add;
  final String? edit;
  final String? delete;
  final String? workflow;

  factory CrudApi.fromJson(String apiBase, Map<String, dynamic> json) {
    final base = apiBase.endsWith('/')
        ? apiBase.substring(0, apiBase.length - 1)
        : apiBase;
    return CrudApi(
      list: stringValue(json['list'], fallback: '$base/list'),
      add: nullableString(json['add']) ?? '$base/add',
      edit: nullableString(json['edit']) ?? '$base/update',
      delete: nullableString(json['delete']) ?? '$base/delete',
      workflow: nullableString(json['workflow']) ?? base,
    );
  }
}

class CrudColumn {
  CrudColumn({
    required this.field,
    required this.label,
    this.format,
    this.statusMap = const {},
  });

  final String field;
  final String label;
  final String? format;
  final Map<String, StatusView> statusMap;

  factory CrudColumn.fromJson(Map<String, dynamic> json) {
    final status = <String, StatusView>{};
    final rawStatus = mapValue(json['statusMap']);
    for (final entry in rawStatus.entries) {
      if (entry.value is Map) {
        final item = mapValue(entry.value);
        status[entry.key] = StatusView(
          label: labelText(item['label'], fallback: entry.key),
          color: stringValue(item['color'], fallback: 'gray'),
        );
      }
    }
    return CrudColumn(
      field: stringValue(json['field']),
      label: labelText(json['label'], fallback: stringValue(json['field'])),
      format: nullableString(json['format']),
      statusMap: status,
    );
  }
}

class StatusView {
  StatusView({required this.label, required this.color});

  final String label;
  final String color;
}

class CrudField {
  CrudField({
    required this.field,
    required this.label,
    required this.type,
    this.placeholder,
    this.required = false,
    this.disabled = false,
    this.defaultValue,
    this.options = const [],
    this.rows = 3,
  });

  final String field;
  final String label;
  final String type;
  final String? placeholder;
  final bool required;
  final bool disabled;
  final Object? defaultValue;
  final List<FieldOption> options;
  final int rows;

  factory CrudField.fromJson(Map<String, dynamic> json) {
    return CrudField(
      field: stringValue(json['field']),
      label: labelText(json['label'], fallback: stringValue(json['field'])),
      type: stringValue(json['type'], fallback: 'input'),
      placeholder: nullableString(json['placeholder']),
      required: boolValue(json['required']) || _rulesRequire(json['rules']),
      disabled: boolValue(json['disabled']),
      defaultValue: json['defaultValue'],
      options: listValue(json['options'])
          .whereType<Map>()
          .map((item) => FieldOption.fromJson(item.cast<String, dynamic>()))
          .toList(),
      rows: intValue(json['rows'], fallback: 3),
    );
  }

  static bool _rulesRequire(Object? rules) {
    return listValue(
      rules,
    ).whereType<Map>().any((rule) => boolValue(rule['required']));
  }
}

class FieldOption {
  FieldOption({required this.label, required this.value});

  final String label;
  final Object value;

  factory FieldOption.fromJson(Map<String, dynamic> json) {
    return FieldOption(
      label: labelText(json['label'], fallback: stringValue(json['value'])),
      value: json['value'] ?? '',
    );
  }
}

List<AppModule> flattenModules(List<AppModule> modules) {
  final result = <AppModule>[];
  void visit(AppModule module) {
    if (!module.hidden && module.path.isNotEmpty) {
      result.add(module);
    }
    for (final child in module.children) {
      visit(child);
    }
  }

  for (final module in modules) {
    visit(module);
  }
  return result;
}

String labelText(Object? raw, {String fallback = ''}) {
  final text = stringValue(raw, fallback: fallback);
  return labels[text] ?? _humanize(text, fallback);
}

String _humanize(String text, String fallback) {
  if (text.isEmpty) {
    return fallback;
  }
  if (!text.contains('.')) {
    return text;
  }
  final last = text.split('.').last;
  return labels[last] ?? last;
}

const labels = {
  'menu.home': '首页',
  'menu.tenant': '租户管理',
  'menu.applications': '应用申请',
  'menu.service': '服务管理',
  'menu.faq': '常见问题',
  'menu.system': '系统管理',
  'menu.admins': '管理员',
  'menu.logs': '日志',
  'menu.dict': '字典',
  'menu.settings': '系统设置',
  'menu.menus': '菜单',
  'menu.notifications': '通知中心',
  'menu.systemStatus': '系统状态',
  'menu.aiModules': 'AI 模块',
  'common.edit': '编辑',
  'common.delete': '删除',
  'home': '首页',
  'applications': '应用申请',
  'faq': '常见问题',
  'admins': '管理员',
  'logs': '日志',
  'dict': '字典',
  'settings': '系统设置',
  'menus': '菜单',
  'system-status': '系统状态',
  'ai-modules': 'AI 模块',
};
