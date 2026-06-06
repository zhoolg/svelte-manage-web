import 'dart:convert';
import 'dart:math' as math;
import 'dart:typed_data';

import 'package:flutter/material.dart';

import '../core/app_theme.dart';
import '../core/session_controller.dart';
import '../models/api_models.dart';
import '../widgets/async_state.dart';
import '../widgets/polished_surface.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key, required this.controller});

  final SessionController controller;

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _serverController;
  final _accountController = TextEditingController();
  final _passwordController = TextEditingController();
  final _captchaController = TextEditingController();
  CaptchaData? _captcha;
  bool _captchaLoading = false;
  bool _loggingIn = false;
  bool _obscurePassword = true;

  @override
  void initState() {
    super.initState();
    _serverController = TextEditingController(
      text: widget.controller.serverUrl,
    );
  }

  @override
  void dispose() {
    _serverController.dispose();
    _accountController.dispose();
    _passwordController.dispose();
    _captchaController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final contentWidth = math.min(
      414.0,
      math.max(0.0, MediaQuery.sizeOf(context).width - 40),
    );
    final bottomInset = MediaQuery.viewInsetsOf(context).bottom;
    return Scaffold(
      body: AppBackground(
        child: SafeArea(
          child: LayoutBuilder(
            builder: (context, constraints) {
              return SingleChildScrollView(
                padding: EdgeInsets.fromLTRB(
                  20,
                  22,
                  20,
                  math.max(22, bottomInset + 22),
                ),
                child: ConstrainedBox(
                  constraints: BoxConstraints(minHeight: constraints.maxHeight),
                  child: Center(
                    child: SizedBox(
                      width: contentWidth,
                      child: AutofillGroup(
                        child: Form(
                          key: _formKey,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              const _LoginTopBar(),
                              const SizedBox(height: 16),
                              _LoginPanel(
                                children: [
                                  TextFormField(
                                    controller: _serverController,
                                    keyboardType: TextInputType.url,
                                    textInputAction: TextInputAction.next,
                                    decoration: const InputDecoration(
                                      labelText: '服务器地址',
                                      prefixIcon: Icon(Icons.dns_outlined),
                                      hintText: 'http://10.0.2.2:8080',
                                    ),
                                    validator: _required,
                                  ),
                                  const SizedBox(height: 12),
                                  TextFormField(
                                    controller: _accountController,
                                    textInputAction: TextInputAction.next,
                                    autofillHints: const [
                                      AutofillHints.username,
                                    ],
                                    decoration: const InputDecoration(
                                      labelText: '账号',
                                      prefixIcon: Icon(Icons.person_outline),
                                    ),
                                    validator: _required,
                                  ),
                                  const SizedBox(height: 12),
                                  TextFormField(
                                    controller: _passwordController,
                                    obscureText: _obscurePassword,
                                    textInputAction: TextInputAction.next,
                                    autofillHints: const [
                                      AutofillHints.password,
                                    ],
                                    decoration: InputDecoration(
                                      labelText: '密码',
                                      prefixIcon: const Icon(
                                        Icons.lock_outline,
                                      ),
                                      suffixIcon: IconButton(
                                        tooltip: _obscurePassword
                                            ? '显示密码'
                                            : '隐藏密码',
                                        onPressed: () => setState(
                                          () => _obscurePassword =
                                              !_obscurePassword,
                                        ),
                                        icon: Icon(
                                          _obscurePassword
                                              ? Icons.visibility_outlined
                                              : Icons.visibility_off_outlined,
                                        ),
                                      ),
                                    ),
                                    validator: _required,
                                  ),
                                  const SizedBox(height: 12),
                                  TextFormField(
                                    controller: _captchaController,
                                    textInputAction: TextInputAction.done,
                                    onFieldSubmitted: (_) {
                                      if (!_loggingIn) {
                                        _login();
                                      }
                                    },
                                    decoration: InputDecoration(
                                      labelText: '验证码',
                                      prefixIcon: const Icon(
                                        Icons.verified_outlined,
                                      ),
                                      suffixIcon: IconButton(
                                        tooltip: '刷新验证码',
                                        onPressed: _captchaLoading
                                            ? null
                                            : _loadCaptcha,
                                        icon: _captchaLoading
                                            ? const SizedBox(
                                                width: 18,
                                                height: 18,
                                                child:
                                                    CircularProgressIndicator(
                                                      strokeWidth: 2,
                                                    ),
                                              )
                                            : const Icon(Icons.refresh_rounded),
                                      ),
                                    ),
                                    validator: _required,
                                  ),
                                  const SizedBox(height: 11),
                                  _CaptchaPreview(
                                    loading: _captchaLoading,
                                    bytes: _captchaBytes(),
                                    onRefresh: _loadCaptcha,
                                  ),
                                  const SizedBox(height: 18),
                                  FilledButton(
                                    onPressed: _loggingIn ? null : _login,
                                    child: _loggingIn
                                        ? const SizedBox(
                                            width: 20,
                                            height: 20,
                                            child: CircularProgressIndicator(
                                              strokeWidth: 2,
                                              color: Colors.white,
                                            ),
                                          )
                                        : const Row(
                                            mainAxisAlignment:
                                                MainAxisAlignment.center,
                                            children: [
                                              Text('进入控制台'),
                                              SizedBox(width: 8),
                                              Icon(
                                                Icons.arrow_forward_rounded,
                                                size: 20,
                                              ),
                                            ],
                                          ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 16),
                              Text(
                                'Svelte Manage Mobile',
                                textAlign: TextAlign.center,
                                style: Theme.of(context).textTheme.bodySmall,
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ),
    );
  }

  Uint8List? _captchaBytes() {
    final raw = _captcha?.image;
    if (raw == null || raw.isEmpty) {
      return null;
    }
    final index = raw.indexOf(',');
    final payload = index >= 0 ? raw.substring(index + 1) : raw;
    try {
      return base64Decode(payload);
    } catch (_) {
      return null;
    }
  }

  Future<void> _loadCaptcha() async {
    final server = _serverController.text.trim();
    if (server.isEmpty) {
      showMessage(context, '请填写服务器地址');
      return;
    }
    setState(() => _captchaLoading = true);
    try {
      final captcha = await widget.controller.captcha(server);
      if (!mounted) {
        return;
      }
      setState(() {
        _captcha = captcha;
        _captchaController.clear();
      });
    } on ApiException catch (error) {
      if (mounted) {
        showMessage(context, error.message);
      }
    } finally {
      if (mounted) {
        setState(() => _captchaLoading = false);
      }
    }
  }

  Future<void> _login() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    if (_captcha == null) {
      if (mounted) {
        await _loadCaptcha();
      }
      return;
    }
    setState(() => _loggingIn = true);
    try {
      await widget.controller.login(
        nextServerUrl: _serverController.text,
        account: _accountController.text,
        password: _passwordController.text,
        captchaId: _captcha!.captchaId,
        captcha: _captchaController.text,
      );
    } on ApiException catch (error) {
      if (mounted) {
        showMessage(context, error.message);
      }
      await _loadCaptcha();
    } finally {
      if (mounted) {
        setState(() => _loggingIn = false);
      }
    }
  }

  String? _required(String? value) {
    return value == null || value.trim().isEmpty ? '必填' : null;
  }
}

class _LoginTopBar extends StatelessWidget {
  const _LoginTopBar();

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        const BrandMark(size: 42, shadow: false),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Svelte Manage',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 2),
              Text(
                '云管理控制台',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ],
          ),
        ),
        const DetailPill(
          icon: Icons.phone_android_rounded,
          label: 'Mobile',
          color: AppTheme.info,
        ),
      ],
    );
  }
}

class _LoginPanel extends StatelessWidget {
  const _LoginPanel({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return AppPanel(
      prominent: true,
      padding: EdgeInsets.zero,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const _ConsoleBand(),
          Padding(
            padding: const EdgeInsets.fromLTRB(15, 15, 15, 15),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  children: [
                    const SurfaceIcon(
                      icon: Icons.terminal_rounded,
                      size: 34,
                      color: AppTheme.accent,
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        '登录控制台',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                    ),
                    const _PanelStatus(),
                  ],
                ),
                const SizedBox(height: 14),
                ...children,
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ConsoleBand extends StatelessWidget {
  const _ConsoleBand();

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 112,
      padding: const EdgeInsets.all(16),
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [AppTheme.ink, AppTheme.inkSoft],
        ),
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppTheme.radius),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 38,
                height: 38,
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.10),
                  borderRadius: BorderRadius.circular(AppTheme.radius),
                  border: Border.all(
                    color: Colors.white.withValues(alpha: 0.12),
                  ),
                ),
                child: const Icon(
                  Icons.cloud_queue_rounded,
                  color: Color(0xff67d9f5),
                  size: 21,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  'Console Access',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.titleSmall?.copyWith(
                    color: Colors.white,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              Container(
                width: 8,
                height: 8,
                decoration: const BoxDecoration(
                  color: Color(0xff67d9f5),
                  shape: BoxShape.circle,
                ),
              ),
            ],
          ),
          const Spacer(),
          Text(
            '后端服务认证',
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: Colors.white.withValues(alpha: 0.82),
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'Mobile control session',
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: Colors.white.withValues(alpha: 0.56),
            ),
          ),
        ],
      ),
    );
  }
}

class _PanelStatus extends StatelessWidget {
  const _PanelStatus();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
      decoration: BoxDecoration(
        color: AppTheme.accentSoft,
        borderRadius: BorderRadius.circular(AppTheme.radius),
        border: Border.all(color: AppTheme.accent.withValues(alpha: 0.10)),
      ),
      child: const Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.bolt_rounded, size: 14, color: AppTheme.accent),
          SizedBox(width: 6),
          Text(
            'Ready',
            style: TextStyle(
              color: AppTheme.accent,
              fontSize: 12,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}

class _CaptchaPreview extends StatelessWidget {
  const _CaptchaPreview({
    required this.loading,
    required this.bytes,
    required this.onRefresh,
  });

  final bool loading;
  final Uint8List? bytes;
  final VoidCallback onRefresh;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 52,
      child: Material(
        color: AppTheme.surfaceMuted,
        borderRadius: BorderRadius.circular(AppTheme.radius),
        child: InkWell(
          onTap: loading ? null : onRefresh,
          borderRadius: BorderRadius.circular(AppTheme.radius),
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(AppTheme.radius),
              border: Border.all(color: AppTheme.border),
            ),
            child: Center(
              child: loading
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : bytes == null
                  ? const Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.image_outlined,
                          size: 19,
                          color: AppTheme.textMuted,
                        ),
                        SizedBox(width: 8),
                        Text('获取验证码'),
                      ],
                    )
                  : ClipRRect(
                      borderRadius: BorderRadius.circular(6),
                      child: Image.memory(bytes!, fit: BoxFit.contain),
                    ),
            ),
          ),
        ),
      ),
    );
  }
}
