<script lang="ts">
  import { onMount } from 'svelte';
  import { Button } from 'bits-ui';
  import {
    Check,
    ClipboardCheck,
    Code,
    Database,
    Eye,
    FileJson,
    History,
    KeyRound,
    ListChecks,
    Loader2,
    Network,
    RefreshCw,
    RotateCcw,
    Send,
    ShieldCheck,
    Sparkles,
  } from 'lucide-svelte';
  import {
    applyAiModule,
    clarifyAiModule,
    generateAiModuleStream,
    getAiProviderConfig,
    listAiModuleVersions,
    listAiModuleTasks,
    planAiModuleApply,
    previewAiModule,
    restoreAiModuleVersion,
    rollbackAiModule,
    saveAiModuleDesignerMetadata,
    saveAiProviderConfig,
    getAiModuleDesignerMetadata,
    type AiApplyPlan,
    type AiClarificationResponse,
    type AiGenerateResponse,
    type AiGenerateStreamProgress,
    type AiGenerationTaskSummary,
    type AiModuleVersionSummary,
    type AiPreviewResponse,
    type GeneratedFile,
  } from '../api/ai';
  import { loadRemoteModules } from '../config/app.modules';
  import { loadRemoteMenu } from '../config/menu';
  import { navigate } from '../stores/routerStore';
  import { toast } from '../utils/toast';
  import AiModuleVisualPreview from './AiModuleVisualPreview.svelte';
  import AiModuleDesigner from './AiModuleDesigner.svelte';

  let loading = false;
  let generationStatus = '';
  let generationSteps: GenerationStep[] = [];
  let clarifying = false;
  let applying = false;
  let rollingBack = false;
  let restoringVersion = false;
  let designerSaving = false;
  let refreshing = false;
  let savingProviderConfig = false;
  let tasksLoading = false;
  let tasks: AiGenerationTaskSummary[] = [];
  let versionsLoading = false;
  let versions: AiModuleVersionSummary[] = [];
  let taskNo = '';
  let schema: Record<string, unknown> | null = null;
  let preview: AiPreviewResponse | null = null;
  let designerMetadata: Record<string, unknown> | null = null;
  let designerMetadataRevision = 0;
  let applyPlan: AiApplyPlan | null = null;
  let clarification: AiClarificationResponse | null = null;
  let files: GeneratedFile[] = [];
  let savedProviderConfig = {
    hasApiKey: false,
    maskedApiKey: '',
  };

  let form = {
    description: '',
    moduleKey: '',
    moduleName: '',
    businessType: 'crud-workflow',
    provider: 'template',
    model: '',
    baseUrl: '',
    apiKey: '',
  };

  $: schemaText = schema ? JSON.stringify(schema, null, 2) : '';
  $: selectedFile = files[0] ?? null;
  $: effectiveDesignerMetadata = designerMetadata || preview?.metadata || null;
  $: activeStep = preview?.status === 'APPLIED_TO_METADATA' ? 3 : taskNo ? 2 : 1;
  $: blueprint = buildBlueprint(effectiveDesignerMetadata);
  $: qualityChecks = buildQualityChecks();
  $: passedQualityChecks = qualityChecks.filter(item => item.status === 'passed').length;
  $: blockingQualityChecks = qualityChecks.filter(item => item.status === 'failed').length;
  $: failedQualityChecks = qualityChecks.filter(item => item.status === 'failed');
  $: publishScore = buildPublishScore(qualityChecks);

  onMount(() => {
    void loadTasks();
    void loadProviderConfig();
  });

  async function handleGenerate() {
    if (!form.description.trim()) {
      toast.warning('请填写业务需求描述');
      return;
    }
    if (form.provider !== 'template' && !form.apiKey.trim() && !savedProviderConfig.hasApiKey) {
      toast.warning('请选择本地模板、填写 API Key 或先保存账号模型配置');
      return;
    }

    loading = true;
    generationStatus = '正在提交生成请求';
    generationSteps = [];
    updateGenerationStep({
      phase: 'submit',
      message: '正在提交生成请求',
      progress: 4,
      status: 'running',
    });
    try {
      const data: AiGenerateResponse = await generateAiModuleStream(
        {
          description: form.description.trim(),
          moduleKey: form.moduleKey.trim() || undefined,
          moduleName: form.moduleName.trim() || undefined,
          businessType: form.businessType,
          provider: form.provider,
          model: form.model.trim() || undefined,
          baseUrl: form.baseUrl.trim() || undefined,
          apiKey: form.apiKey.trim() || undefined,
        },
        event => {
          if (event.type === 'progress') {
            generationStatus = event.data.message || '正在生成模块草稿';
            updateGenerationStep(event.data);
          }
          if (event.type === 'done') {
            generationStatus = '正在加载生成预览';
            updateGenerationStep({
              phase: 'preview',
              message: '正在加载生成预览',
              progress: 100,
              status: 'passed',
            });
          }
        }
      );
      taskNo = data.taskNo;
      schema = data.schema;
      files = data.files || [];
      preview = await loadPreview(taskNo);
      await loadTasks();
      toast.success('AI 模块预览已生成');
    } catch (error) {
      markGenerationFailed(error instanceof Error ? error.message : '生成失败');
      toast.error(error instanceof Error ? error.message : '生成失败');
    } finally {
      loading = false;
      generationStatus = '';
    }
  }

  async function handleClarify() {
    if (!form.description.trim()) {
      toast.warning('请填写业务需求描述');
      return;
    }

    clarifying = true;
    try {
      const response = await clarifyAiModule({
        description: form.description.trim(),
        moduleKey: form.moduleKey.trim() || undefined,
        moduleName: form.moduleName.trim() || undefined,
        businessType: form.businessType,
        provider: form.provider,
        model: form.model.trim() || undefined,
        baseUrl: form.baseUrl.trim() || undefined,
      });
      clarification = response.data;
      if (clarification.needsClarification) {
        toast.warning('需求还可以继续补全');
      } else {
        toast.success('需求信息已经比较完整');
      }
    } catch (error) {
      toast.error(error instanceof Error ? error.message : '需求检查失败');
    } finally {
      clarifying = false;
    }
  }

  async function loadPreview(nextTaskNo = taskNo) {
    if (!nextTaskNo) return null;
    const response = await previewAiModule(nextTaskNo);
    const data = response.data;
    files = data.files || files;
    schema = data.schema || schema;
    preview = data;
    setDesignerMetadata(data.metadata || null);
    if (data.status === 'APPLIED_TO_METADATA') {
      await loadDesignerMetadata(data.moduleKey);
    }
    await loadApplyPlan(nextTaskNo);
    await loadModuleVersions(data.moduleKey);
    return data;
  }

  async function loadDesignerMetadata(moduleKey = preview?.moduleKey) {
    if (!moduleKey) {
      setDesignerMetadata(preview?.metadata || null);
      return;
    }
    try {
      const response = await getAiModuleDesignerMetadata(moduleKey);
      setDesignerMetadata(response.data || preview?.metadata || null);
    } catch (error) {
      setDesignerMetadata(preview?.metadata || null);
      toast.error(error instanceof Error ? error.message : '设计器配置加载失败');
    }
  }

  function setDesignerMetadata(metadata: Record<string, unknown> | null | undefined) {
    designerMetadata = metadata || null;
    designerMetadataRevision += 1;
  }

  async function handleApply() {
    if (!taskNo) {
      toast.warning('请先生成模块预览');
      return;
    }

    applying = true;
    try {
      const response = await applyAiModule(taskNo);
      const appliedPath = typeof response.data.path === 'string' ? response.data.path : '';
      const refreshed = await refreshRemoteModules(false);
      preview = await loadPreview(taskNo);
      await loadModuleVersions(preview?.moduleKey);
      await loadTasks();
      toast.success('模块已应用，正在打开页面');
      if (refreshed && appliedPath) {
        navigate(appliedPath);
      }
    } catch (error) {
      toast.error(error instanceof Error ? error.message : '应用失败');
    } finally {
      applying = false;
    }
  }

  async function handleRollback() {
    if (!taskNo) {
      toast.warning('请先选择已应用的模块');
      return;
    }

    rollingBack = true;
    try {
      await rollbackAiModule(taskNo);
      preview = await loadPreview(taskNo);
      await loadModuleVersions(preview?.moduleKey);
      await loadTasks();
      toast.success('模块已回滚');
      await refreshRemoteModules();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : '回滚失败');
    } finally {
      rollingBack = false;
    }
  }

  async function refreshRemoteModules(showToast = true) {
    refreshing = true;
    try {
      const modulesLoaded = await loadRemoteModules();
      const menuLoaded = modulesLoaded ? await loadRemoteMenu() : false;
      if (!modulesLoaded || !menuLoaded) {
        throw new Error('后端元数据刷新失败');
      }
      if (showToast) {
        toast.success('动态菜单已刷新');
      }
      return true;
    } catch (error) {
      toast.error(error instanceof Error ? error.message : '刷新失败');
      return false;
    } finally {
      refreshing = false;
    }
  }

  async function loadTasks() {
    tasksLoading = true;
    try {
      const response = await listAiModuleTasks();
      tasks = response.data || [];
    } catch (error) {
      toast.error(error instanceof Error ? error.message : '任务列表加载失败');
    } finally {
      tasksLoading = false;
    }
  }

  async function loadProviderConfig() {
    try {
      const response = await getAiProviderConfig();
      const config = response.data;
      form.provider = config.provider || 'template';
      form.model = config.model || '';
      form.baseUrl = config.baseUrl || '';
      form.apiKey = '';
      savedProviderConfig = {
        hasApiKey: config.hasApiKey,
        maskedApiKey: config.maskedApiKey || '',
      };
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'AI 模型配置加载失败');
    }
  }

  async function handleSaveProviderConfig(clearApiKey = false) {
    savingProviderConfig = true;
    try {
      const response = await saveAiProviderConfig({
        provider: form.provider,
        model: form.model.trim() || undefined,
        baseUrl: form.baseUrl.trim() || undefined,
        apiKey: form.apiKey.trim() || undefined,
        clearApiKey,
      });
      const config = response.data;
      form.provider = config.provider || 'template';
      form.model = config.model || '';
      form.baseUrl = config.baseUrl || '';
      form.apiKey = '';
      savedProviderConfig = {
        hasApiKey: config.hasApiKey,
        maskedApiKey: config.maskedApiKey || '',
      };
      toast.success(clearApiKey ? 'AI Key 已清除' : 'AI 模型配置已保存');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'AI 模型配置保存失败');
    } finally {
      savingProviderConfig = false;
    }
  }

  async function loadApplyPlan(nextTaskNo = taskNo) {
    if (!nextTaskNo) {
      applyPlan = null;
      return null;
    }
    try {
      const response = await planAiModuleApply(nextTaskNo);
      applyPlan = response.data;
      return response.data;
    } catch (error) {
      applyPlan = null;
      toast.error(error instanceof Error ? error.message : '应用计划加载失败');
      return null;
    }
  }

  async function loadModuleVersions(moduleKey = preview?.moduleKey || form.moduleKey) {
    if (!moduleKey) {
      versions = [];
      return;
    }
    versionsLoading = true;
    try {
      const response = await listAiModuleVersions(moduleKey);
      versions = response.data || [];
    } catch (error) {
      versions = [];
      toast.error(error instanceof Error ? error.message : '版本列表加载失败');
    } finally {
      versionsLoading = false;
    }
  }

  async function handleRestoreVersion(version: AiModuleVersionSummary) {
    restoringVersion = true;
    try {
      await restoreAiModuleVersion(version.moduleKey, version.versionNo);
      await refreshRemoteModules(false);
      await loadDesignerMetadata(version.moduleKey);
      await loadModuleVersions(version.moduleKey);
      await loadTasks();
      toast.success(`已恢复到 v${version.versionNo}`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : '版本恢复失败');
    } finally {
      restoringVersion = false;
    }
  }

  async function handleSelectTask(task: AiGenerationTaskSummary) {
    taskNo = task.taskNo;
    form.moduleKey = task.moduleKey;
    form.moduleName = task.moduleName;
    preview = await loadPreview(task.taskNo);
  }

  async function handleOpenTask(task: AiGenerationTaskSummary) {
    const refreshed = await refreshRemoteModules(false);
    if (refreshed) {
      navigate(`/ai/${task.moduleKey}`);
    }
  }

  async function handleSaveDesigner(nextMetadata: Record<string, unknown>) {
    const moduleKey = preview?.moduleKey || form.moduleKey;
    if (!moduleKey) {
      toast.warning('请先选择已应用模块');
      return;
    }
    designerSaving = true;
    try {
      const response = await saveAiModuleDesignerMetadata(moduleKey, nextMetadata);
      const saved = response.data?.metadata;
      setDesignerMetadata(
        saved && typeof saved === 'object' ? (saved as Record<string, unknown>) : nextMetadata
      );
      await refreshRemoteModules(false);
      await loadModuleVersions(moduleKey);
      await loadTasks();
      toast.success('页面设计已保存为新版本');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : '设计器保存失败');
    } finally {
      designerSaving = false;
    }
  }

  function statusLabel(status: string) {
    return (
      {
        PREVIEW_READY: '可预览',
        APPLIED_TO_METADATA: '已应用',
        ROLLED_BACK: '已回滚',
      }[status] || status
    );
  }

  function statusClasses(status: string) {
    if (status === 'APPLIED_TO_METADATA') {
      return 'border-green-200 bg-green-50 text-green-700 dark:border-green-900/50 dark:bg-green-900/20 dark:text-green-300';
    }
    if (status === 'ROLLED_BACK') {
      return 'border-gray-200 bg-gray-50 text-gray-500 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400';
    }
    return 'border-blue-200 bg-blue-50 text-blue-700 dark:border-blue-900/50 dark:bg-blue-900/20 dark:text-blue-300';
  }

  function smokeTestLabel(status?: string | null) {
    return (
      {
        PASSED: '冒烟通过',
        FAILED: '冒烟失败',
      }[status || ''] || '未测试'
    );
  }

  function smokeTestClasses(status?: string | null) {
    if (status === 'PASSED') {
      return 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-900/50 dark:bg-emerald-900/20 dark:text-emerald-300';
    }
    if (status === 'FAILED') {
      return 'border-red-200 bg-red-50 text-red-700 dark:border-red-900/50 dark:bg-red-900/20 dark:text-red-300';
    }
    return 'border-gray-200 bg-gray-50 text-gray-500 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400';
  }

  function riskClasses(level: string) {
    if (level === 'high') {
      return 'border-red-200 bg-red-50 text-red-700 dark:border-red-900/50 dark:bg-red-900/20 dark:text-red-300';
    }
    if (level === 'medium') {
      return 'border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-900/50 dark:bg-amber-900/20 dark:text-amber-300';
    }
    return 'border-green-200 bg-green-50 text-green-700 dark:border-green-900/50 dark:bg-green-900/20 dark:text-green-300';
  }

  function riskLabel(level: string) {
    return { low: '低风险', medium: '中风险', high: '高风险' }[level] || level;
  }

  function formatTime(value?: string) {
    if (!value) return '-';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value.replace('T', ' ');
    }
    return date.toLocaleString('zh-CN', { hour12: false });
  }

  function handleProviderChange() {
    if (form.provider === 'openai') {
      form.model = form.model || 'gpt-4o-mini';
      form.baseUrl = form.baseUrl || 'https://api.openai.com';
      return;
    }
    if (form.provider === 'claude') {
      form.model = form.model || 'claude-3-5-sonnet-latest';
      form.baseUrl = form.baseUrl || 'https://api.anthropic.com';
      return;
    }
    form.model = '';
    form.baseUrl = '';
    form.apiKey = '';
    savedProviderConfig = { hasApiKey: false, maskedApiKey: '' };
  }

  type QualityStatus = 'passed' | 'warning' | 'failed' | 'pending';
  type GenerationStepStatus = 'pending' | 'running' | 'passed' | 'failed';

  interface QualityCheck {
    label: string;
    detail: string;
    status: QualityStatus;
    category?: string;
    score?: number;
  }

  interface GenerationStep {
    phase: string;
    label: string;
    message: string;
    progress: number;
    status: GenerationStepStatus;
  }

  const generationStepLabels: Record<string, string> = {
    submit: '提交请求',
    request: '请求校验',
    requirement: '需求归一',
    schema: '生成蓝图',
    files: '生成草稿',
    persist: '保存快照',
    preview: '准备预览',
  };

  function updateGenerationStep(event: AiGenerateStreamProgress) {
    const phase = event.phase || 'schema';
    const nextStatus = normalizeGenerationStepStatus(event.status);
    const nextStep: GenerationStep = {
      phase,
      label: generationStepLabels[phase] || event.message || phase,
      message: event.message || generationStepLabels[phase] || '正在生成',
      progress: typeof event.progress === 'number' ? event.progress : 0,
      status: nextStatus,
    };
    const existingIndex = generationSteps.findIndex(item => item.phase === phase);
    generationSteps =
      existingIndex >= 0
        ? generationSteps.map((item, index) => (index === existingIndex ? nextStep : item))
        : [...generationSteps, nextStep];
  }

  function markGenerationFailed(message: string) {
    if (generationSteps.length === 0) {
      updateGenerationStep({ phase: 'schema', message, progress: 0, status: 'failed' });
      return;
    }
    const last = generationSteps[generationSteps.length - 1];
    generationSteps = generationSteps.map((item, index) =>
      index === generationSteps.length - 1 ? { ...last, message, status: 'failed' } : item
    );
  }

  function normalizeGenerationStepStatus(status?: string): GenerationStepStatus {
    if (status === 'passed' || status === 'failed' || status === 'running') {
      return status;
    }
    return 'pending';
  }

  function buildBlueprint(metadata: Record<string, unknown> | null | undefined) {
    const crud = objectValue(metadata?.crud);
    const columns = arrayValue(crud?.columns);
    const search = arrayValue(crud?.search);
    const formFields = arrayValue(crud?.form);
    const workflow = arrayValue(crud?.workflow);
    return {
      id: textValue(metadata?.id || preview?.moduleKey || form.moduleKey || '等待生成'),
      name: textValue(metadata?.label || preview?.moduleName || form.moduleName || '业务模块'),
      path: textValue(
        metadata?.path || (preview?.moduleKey ? `/ai/${preview.moduleKey}` : '生成后确定')
      ),
      title: textValue(crud?.title || metadata?.label || preview?.moduleName || 'CRUD 页面'),
      columns,
      search,
      formFields,
      workflow,
      permissions: arrayValue(metadata?.permissions),
    };
  }

  function buildQualityChecks(): QualityCheck[] {
    const validation = preview?.validation;
    const fieldCount = blueprint.columns.length;
    const searchCount = blueprint.search.length;
    const formCount = blueprint.formFields.length;
    const hasStatusField = blueprint.columns.some(field => textValue(field.field) === 'status');
    const localizedItems = [
      effectiveDesignerMetadata,
      objectValue(effectiveDesignerMetadata?.crud),
      ...blueprint.columns,
      ...blueprint.search,
      ...blueprint.formFields,
    ].filter(Boolean) as Array<Record<string, unknown>>;
    const localizedCount = localizedItems.filter(hasLocalizedLabel).length;
    return [
      {
        label: '需求已经转成结构化蓝图',
        detail: taskNo ? `任务 ${taskNo}` : '生成预览后会形成可审查的蓝图',
        status: taskNo ? 'passed' : 'pending',
        category: '需求',
        score: taskNo ? 100 : 0,
      },
      {
        label: '真实页面预览可渲染',
        detail: effectiveDesignerMetadata
          ? `${blueprint.columns.length} 个列表字段`
          : '等待 metadata',
        status: effectiveDesignerMetadata ? 'passed' : 'pending',
        category: '预览',
        score: effectiveDesignerMetadata ? 100 : 0,
      },
      {
        label: '字段数量适合后台首屏',
        detail: fieldCount ? `${fieldCount} 个列表字段，${formCount} 个表单字段` : '等待字段蓝图',
        status: !fieldCount
          ? 'pending'
          : fieldCount >= 3 && fieldCount <= 12
            ? 'passed'
            : 'warning',
        category: '字段',
        score: !fieldCount ? 0 : fieldCount >= 3 && fieldCount <= 12 ? 100 : 70,
      },
      {
        label: '查询入口保持克制',
        detail: searchCount ? `${searchCount} 个搜索/筛选项` : '建议至少保留一个高频查询入口',
        status: !taskNo ? 'pending' : searchCount > 0 && searchCount <= 6 ? 'passed' : 'warning',
        category: '搜索',
        score: searchCount > 0 && searchCount <= 6 ? 100 : 60,
      },
      {
        label: '状态与流程可判断',
        detail: blueprint.workflow.length
          ? `${blueprint.workflow.length} 个流转动作`
          : hasStatusField
            ? '有状态字段，暂无流转动作'
            : '建议保留状态字段',
        status: !taskNo ? 'pending' : hasStatusField ? 'passed' : 'warning',
        category: '流程',
        score: hasStatusField ? (blueprint.workflow.length ? 100 : 85) : 50,
      },
      {
        label: '动态国际化完整',
        detail: localizedItems.length
          ? `${localizedCount}/${localizedItems.length} 个文案带多语言`
          : '等待 metadata',
        status: !localizedItems.length
          ? 'pending'
          : localizedCount === localizedItems.length
            ? 'passed'
            : 'warning',
        category: '国际化',
        score: localizedItems.length
          ? Math.round((localizedCount / localizedItems.length) * 100)
          : 0,
      },
      {
        label: '服务端校验',
        detail: validation
          ? `${validation.score} 分，${validation.issues.length} 个提示`
          : '等待生成结果',
        status: !validation ? 'pending' : validation.passed ? 'passed' : 'failed',
        category: '校验',
        score: validation?.score ?? 0,
      },
      {
        label: '发布计划可执行',
        detail: applyPlan
          ? `${applyPlan.operations.length} 个操作，${riskLabel(applyPlan.riskLevel)}`
          : '等待应用计划',
        status: !applyPlan ? 'pending' : applyPlan.canApply ? 'passed' : 'failed',
        category: '发布',
        score: applyPlan ? Math.max(0, 100 - applyPlan.riskScore) : 0,
      },
      {
        label: '版本可回退',
        detail: versions.length > 0 ? `${versions.length} 个版本记录` : '应用后自动生成版本',
        status:
          versions.length > 0
            ? 'passed'
            : preview?.status === 'APPLIED_TO_METADATA'
              ? 'warning'
              : 'pending',
        category: '版本',
        score: versions.length > 0 ? 100 : preview?.status === 'APPLIED_TO_METADATA' ? 70 : 0,
      },
    ];
  }

  function objectValue(value: unknown): Record<string, unknown> | null {
    return value && typeof value === 'object' && !Array.isArray(value)
      ? (value as Record<string, unknown>)
      : null;
  }

  function hasLocalizedLabel(value: Record<string, unknown>): boolean {
    const i18n = value.labelI18n || value.titleI18n;
    return Boolean(i18n && typeof i18n === 'object' && 'zh-CN' in i18n && 'en-US' in i18n);
  }

  function buildPublishScore(items: QualityCheck[]): number {
    const scored = items.filter(
      item => typeof item.score === 'number' && item.status !== 'pending'
    );
    if (scored.length === 0) {
      return 0;
    }
    return Math.round(scored.reduce((total, item) => total + (item.score || 0), 0) / scored.length);
  }

  function arrayValue(value: unknown): Array<Record<string, unknown>> {
    return Array.isArray(value) ? (value as Array<Record<string, unknown>>) : [];
  }

  function textValue(value: unknown): string {
    return value == null || value === '' ? '' : String(value);
  }

  function qualityClasses(status: QualityStatus) {
    if (status === 'passed') {
      return 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-900/50 dark:bg-emerald-900/20 dark:text-emerald-300';
    }
    if (status === 'failed') {
      return 'border-red-200 bg-red-50 text-red-700 dark:border-red-900/50 dark:bg-red-900/20 dark:text-red-300';
    }
    if (status === 'warning') {
      return 'border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-900/50 dark:bg-amber-900/20 dark:text-amber-300';
    }
    return 'border-gray-200 bg-gray-50 text-gray-500 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400';
  }

  function qualityLabel(status: QualityStatus) {
    return { passed: '通过', warning: '提醒', failed: '阻塞', pending: '等待' }[status];
  }

  function generationStepClasses(status: GenerationStepStatus) {
    if (status === 'passed') {
      return 'border-emerald-200 bg-emerald-50 text-emerald-700 dark:border-emerald-900/50 dark:bg-emerald-900/20 dark:text-emerald-300';
    }
    if (status === 'failed') {
      return 'border-red-200 bg-red-50 text-red-700 dark:border-red-900/50 dark:bg-red-900/20 dark:text-red-300';
    }
    if (status === 'running') {
      return 'border-blue-200 bg-blue-50 text-blue-700 dark:border-blue-900/50 dark:bg-blue-900/20 dark:text-blue-300';
    }
    return 'border-gray-200 bg-gray-50 text-gray-500 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400';
  }

  function stepClasses(step: number) {
    if (activeStep === step) {
      return 'border-[color:var(--color-primary)] bg-[color:color-mix(in_srgb,var(--color-primary)_8%,white)] text-gray-900 dark:bg-[color:color-mix(in_srgb,var(--color-primary)_18%,#141414)] dark:text-white';
    }
    if (activeStep > step) {
      return 'border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-900/50 dark:bg-emerald-900/20 dark:text-emerald-200';
    }
    return 'border-gray-200 bg-white text-gray-500 dark:border-gray-800 dark:bg-[#1d1d1d] dark:text-gray-400';
  }

  function appendExample(example: string) {
    form.description = form.description.trim() ? `${form.description.trim()}\n${example}` : example;
  }
</script>

<div class="fade-in space-y-4">
  <div
    class="rounded-lg border border-gray-100 bg-white shadow-sm dark:border-gray-800 dark:bg-[#1d1d1d]"
  >
    <div class="flex flex-col gap-4 p-5 lg:flex-row lg:items-center lg:justify-between">
      <div>
        <h1 class="flex items-center gap-2 text-xl font-semibold text-gray-900 dark:text-white">
          <Sparkles size={22} class="text-[color:var(--color-primary)]" />
          AI CRUD 工厂
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          用一句业务描述生成可检查、可预览、可发布、可回滚的后台模块
        </p>
      </div>
      <Button.Root
        onclick={() => refreshRemoteModules()}
        disabled={refreshing}
        class="flex h-9 items-center gap-1 rounded-lg border border-gray-200 px-4 text-sm text-gray-600 transition-colors hover:border-[color:var(--color-primary)] hover:text-[color:var(--color-primary)] disabled:opacity-60 dark:border-gray-700 dark:text-gray-300"
      >
        <RefreshCw size={14} class={refreshing ? 'animate-spin' : ''} />
        刷新动态菜单
      </Button.Root>
    </div>

    <div
      class="grid grid-cols-1 gap-3 border-t border-gray-100 p-4 dark:border-gray-800 lg:grid-cols-3"
    >
      <div class="rounded-lg border px-4 py-3 {stepClasses(1)}">
        <div class="flex items-center gap-2 text-sm font-medium">
          <Sparkles size={15} />
          1. 描述需求
        </div>
        <div class="mt-1 text-xs opacity-75">只描述业务目标，字段和页面由系统推导</div>
      </div>
      <div class="rounded-lg border px-4 py-3 {stepClasses(2)}">
        <div class="flex items-center gap-2 text-sm font-medium">
          <Eye size={15} />
          2. 确认蓝图
        </div>
        <div class="mt-1 text-xs opacity-75">查看真实 CRUD 预览和质量检查</div>
      </div>
      <div class="rounded-lg border px-4 py-3 {stepClasses(3)}">
        <div class="flex items-center gap-2 text-sm font-medium">
          <Send size={15} />
          3. 发布模块
        </div>
        <div class="mt-1 text-xs opacity-75">应用到菜单，并保留版本回滚点</div>
      </div>
    </div>
  </div>

  <div class="grid grid-cols-1 gap-4 xl:grid-cols-[minmax(360px,0.85fr)_minmax(0,1.15fr)]">
    <div class="space-y-4 rounded-lg bg-white p-5 shadow-sm dark:bg-[#1d1d1d]">
      <div class="mb-4 flex items-center justify-between gap-3">
        <div>
          <div class="text-sm font-medium text-gray-900 dark:text-white">业务意图</div>
          <div class="mt-1 text-xs text-gray-500">保留自然语言，减少配置负担</div>
        </div>
        <span
          class="rounded-full border border-gray-200 px-2.5 py-1 text-xs text-gray-500 dark:border-gray-700"
        >
          {form.provider === 'template' ? '本地模板' : form.provider}
        </span>
      </div>

      <textarea
        id="ai-description"
        bind:value={form.description}
        placeholder="例如：我要做一个房源报修管理模块，用户可以提交报修，管理员可以派单，维修人员可以完成维修"
        class="h-36 w-full resize-none rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-800 focus:border-[color:var(--color-primary)] focus:outline-none dark:border-gray-700 dark:bg-[#141414] dark:text-white"
      ></textarea>

      <div class="mt-3 flex flex-wrap gap-2">
        <button
          type="button"
          onclick={() =>
            appendExample('需要列表、搜索、新增编辑表单，并按状态展示待处理、处理中、已完成。')}
          class="rounded-md border border-gray-200 px-2.5 py-1 text-xs text-gray-500 hover:border-[color:var(--color-primary)] hover:text-[color:var(--color-primary)] dark:border-gray-700"
        >
          审批流
        </button>
        <button
          type="button"
          onclick={() => appendExample('需要支持导出、批量删除、按时间范围查询，并保留操作权限。')}
          class="rounded-md border border-gray-200 px-2.5 py-1 text-xs text-gray-500 hover:border-[color:var(--color-primary)] hover:text-[color:var(--color-primary)] dark:border-gray-700"
        >
          运营管理
        </button>
        <button
          type="button"
          onclick={() => appendExample('需要把金额、状态、图片、标签字段用更适合的展示方式呈现。')}
          class="rounded-md border border-gray-200 px-2.5 py-1 text-xs text-gray-500 hover:border-[color:var(--color-primary)] hover:text-[color:var(--color-primary)] dark:border-gray-700"
        >
          精致字段
        </button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
          <label for="module-key" class="block text-sm text-gray-600 dark:text-gray-400 mb-1">
            模块标识
          </label>
          <input
            id="module-key"
            bind:value={form.moduleKey}
            placeholder="repair_order"
            class="w-full h-9 px-3 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-[#141414] text-gray-800 dark:text-white focus:border-[color:var(--color-primary)] focus:outline-none"
          />
        </div>
        <div>
          <label for="module-name" class="block text-sm text-gray-600 dark:text-gray-400 mb-1">
            模块名称
          </label>
          <input
            id="module-name"
            bind:value={form.moduleName}
            placeholder="报修管理"
            class="w-full h-9 px-3 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-[#141414] text-gray-800 dark:text-white focus:border-[color:var(--color-primary)] focus:outline-none"
          />
        </div>
        <div>
          <label for="business-type" class="block text-sm text-gray-600 dark:text-gray-400 mb-1">
            业务类型
          </label>
          <select
            id="business-type"
            bind:value={form.businessType}
            class="w-full h-9 px-3 text-sm border border-gray-200 dark:border-gray-700 rounded-lg bg-white dark:bg-[#141414] text-gray-800 dark:text-white focus:border-[color:var(--color-primary)] focus:outline-none"
          >
            <option value="crud-workflow">CRUD + 工作流</option>
            <option value="crud">标准 CRUD</option>
            <option value="metadata-only">仅元数据</option>
          </select>
        </div>
      </div>

      <div class="border-t border-gray-100 pt-4 dark:border-gray-800">
        <div class="mb-3 flex items-center gap-2 text-sm font-medium text-gray-800 dark:text-white">
          <Network size={16} class="text-cyan-500" />
          AI 协议
        </div>
        <div class="grid grid-cols-1 gap-4 lg:grid-cols-4">
          <div>
            <label for="ai-provider" class="mb-1 block text-sm text-gray-600 dark:text-gray-400">
              协议
            </label>
            <select
              id="ai-provider"
              bind:value={form.provider}
              onchange={handleProviderChange}
              class="h-9 w-full rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-800 focus:border-[color:var(--color-primary)] focus:outline-none dark:border-gray-700 dark:bg-[#141414] dark:text-white"
            >
              <option value="template">本地模板</option>
              <option value="openai">OpenAI 兼容</option>
              <option value="claude">Claude / Anthropic</option>
            </select>
          </div>
          <div>
            <label for="ai-model" class="mb-1 block text-sm text-gray-600 dark:text-gray-400">
              模型
            </label>
            <input
              id="ai-model"
              bind:value={form.model}
              disabled={form.provider === 'template'}
              placeholder={form.provider === 'claude' ? 'claude-3-5-sonnet-latest' : 'gpt-4o-mini'}
              class="h-9 w-full rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-800 focus:border-[color:var(--color-primary)] focus:outline-none disabled:cursor-not-allowed disabled:opacity-60 dark:border-gray-700 dark:bg-[#141414] dark:text-white"
            />
          </div>
          <div>
            <label for="ai-base-url" class="mb-1 block text-sm text-gray-600 dark:text-gray-400">
              Base URL
            </label>
            <input
              id="ai-base-url"
              bind:value={form.baseUrl}
              disabled={form.provider === 'template'}
              placeholder={form.provider === 'claude'
                ? 'https://api.anthropic.com'
                : 'https://api.openai.com'}
              class="h-9 w-full rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-800 focus:border-[color:var(--color-primary)] focus:outline-none disabled:cursor-not-allowed disabled:opacity-60 dark:border-gray-700 dark:bg-[#141414] dark:text-white"
            />
          </div>
          <div>
            <label
              for="ai-api-key"
              class="mb-1 flex items-center gap-1 text-sm text-gray-600 dark:text-gray-400"
            >
              <KeyRound size={13} />
              API Key
            </label>
            <input
              id="ai-api-key"
              bind:value={form.apiKey}
              disabled={form.provider === 'template'}
              type="password"
              autocomplete="off"
              placeholder={form.provider === 'template'
                ? '无需填写'
                : savedProviderConfig.hasApiKey
                  ? savedProviderConfig.maskedApiKey
                  : '仅本次生成使用'}
              class="h-9 w-full rounded-lg border border-gray-200 bg-white px-3 text-sm text-gray-800 focus:border-[color:var(--color-primary)] focus:outline-none disabled:cursor-not-allowed disabled:opacity-60 dark:border-gray-700 dark:bg-[#141414] dark:text-white"
            />
          </div>
        </div>
        <div class="mt-3 flex flex-wrap items-center justify-between gap-2">
          <div class="text-xs text-gray-500">
            配置跟随当前登录账号保存；API Key 加密存储，不写入生成任务，也不会完整回显。
          </div>
          <div class="flex items-center gap-2">
            {#if savedProviderConfig.hasApiKey && form.provider !== 'template'}
              <Button.Root
                onclick={() => handleSaveProviderConfig(true)}
                disabled={savingProviderConfig}
                class="h-8 rounded-lg border border-gray-200 px-3 text-xs text-gray-600 transition-colors hover:border-red-300 hover:text-red-500 disabled:opacity-60 dark:border-gray-700 dark:text-gray-300"
              >
                清除 Key
              </Button.Root>
            {/if}
            <Button.Root
              onclick={() => handleSaveProviderConfig(false)}
              disabled={savingProviderConfig}
              class="h-8 rounded-lg border border-gray-200 px-3 text-xs text-gray-600 transition-colors hover:border-[color:var(--color-primary)] hover:text-[color:var(--color-primary)] disabled:opacity-60 dark:border-gray-700 dark:text-gray-300"
            >
              {savingProviderConfig ? '保存中...' : '保存到账号'}
            </Button.Root>
          </div>
        </div>
      </div>

      <div class="flex justify-end gap-2">
        <Button.Root
          onclick={handleClarify}
          disabled={clarifying || loading}
          class="h-9 px-5 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 text-sm rounded-lg disabled:opacity-60 transition-colors flex items-center gap-1 hover:border-[color:var(--color-primary)] hover:text-[color:var(--color-primary)]"
        >
          {#if clarifying}
            <Loader2 size={14} class="animate-spin" />
          {:else}
            <ShieldCheck size={14} />
          {/if}
          {clarifying ? '检查中...' : '需求检查'}
        </Button.Root>
        <Button.Root
          onclick={handleGenerate}
          disabled={loading}
          class="h-9 px-5 text-white text-sm rounded-lg disabled:opacity-60 transition-colors flex items-center gap-1 hover:brightness-105 active:brightness-95"
          style="background-color: var(--color-primary);"
        >
          {#if loading}
            <Loader2 size={14} class="animate-spin" />
          {:else}
            <Sparkles size={14} />
          {/if}
          {loading ? '生成中...' : '生成预览'}
        </Button.Root>
      </div>
      {#if generationSteps.length > 0}
        <div
          class="mt-3 rounded-lg border border-gray-100 bg-gray-50 p-3 dark:border-gray-800 dark:bg-[#141414]"
        >
          <div class="mb-2 flex items-center justify-between gap-3">
            <div class="min-w-0 truncate text-xs text-gray-500">
              {generationStatus || '正在生成模块预览'}
            </div>
            <div class="text-xs font-medium text-gray-500">
              {Math.max(...generationSteps.map(item => item.progress))}%
            </div>
          </div>
          <div class="mb-3 h-1.5 overflow-hidden rounded-full bg-gray-200 dark:bg-gray-800">
            <div
              class="h-full rounded-full bg-[color:var(--color-primary)] transition-all"
              style:width={`${Math.max(...generationSteps.map(item => item.progress))}%`}
            ></div>
          </div>
          <div class="grid grid-cols-1 gap-2 md:grid-cols-2 xl:grid-cols-4">
            {#each generationSteps as step}
              <div class="rounded-lg border px-3 py-2 {generationStepClasses(step.status)}">
                <div class="flex items-center justify-between gap-2">
                  <span class="truncate text-xs font-medium">{step.label}</span>
                  {#if step.status === 'running'}
                    <Loader2 size={12} class="shrink-0 animate-spin" />
                  {:else if step.status === 'passed'}
                    <Check size={12} class="shrink-0" />
                  {/if}
                </div>
                <div class="mt-1 truncate text-[11px] opacity-75">{step.message}</div>
              </div>
            {/each}
          </div>
        </div>
      {/if}
    </div>

    <div class="space-y-4">
      <div class="rounded-lg bg-white shadow-sm dark:bg-[#1d1d1d]">
        <div class="border-b border-gray-100 px-4 py-3 dark:border-gray-800">
          <div class="flex items-center gap-2 text-sm font-medium text-gray-900 dark:text-white">
            <ClipboardCheck size={16} class="text-[color:var(--color-primary)]" />
            模块蓝图
          </div>
        </div>
        <div class="grid grid-cols-2 gap-3 p-4 md:grid-cols-4">
          <div class="rounded-lg border border-gray-100 p-3 dark:border-gray-800">
            <div class="text-xs text-gray-500">模块</div>
            <div class="mt-1 truncate text-sm font-medium text-gray-900 dark:text-white">
              {blueprint.name}
            </div>
          </div>
          <div class="rounded-lg border border-gray-100 p-3 dark:border-gray-800">
            <div class="text-xs text-gray-500">字段</div>
            <div class="mt-1 text-sm font-medium text-gray-900 dark:text-white">
              {blueprint.columns.length}
            </div>
          </div>
          <div class="rounded-lg border border-gray-100 p-3 dark:border-gray-800">
            <div class="text-xs text-gray-500">表单</div>
            <div class="mt-1 text-sm font-medium text-gray-900 dark:text-white">
              {blueprint.formFields.length}
            </div>
          </div>
          <div class="rounded-lg border border-gray-100 p-3 dark:border-gray-800">
            <div class="text-xs text-gray-500">流程</div>
            <div class="mt-1 text-sm font-medium text-gray-900 dark:text-white">
              {blueprint.workflow.length}
            </div>
          </div>
        </div>
        <div class="border-t border-gray-100 px-4 py-3 text-xs text-gray-500 dark:border-gray-800">
          <div class="truncate">路径：{blueprint.path}</div>
          <div class="mt-1 truncate">页面：{blueprint.title}</div>
        </div>
      </div>

      <div class="rounded-lg bg-white shadow-sm dark:bg-[#1d1d1d]">
        <div
          class="flex items-center justify-between gap-3 border-b border-gray-100 px-4 py-3 dark:border-gray-800"
        >
          <div class="flex items-center gap-2 text-sm font-medium text-gray-900 dark:text-white">
            <ListChecks size={16} class="text-emerald-500" />
            质量检查
          </div>
          <span
            class="rounded-full border border-gray-200 px-2.5 py-1 text-xs text-gray-500 dark:border-gray-700"
          >
            发布建议 {publishScore || 0} 分 · {passedQualityChecks}/{qualityChecks.length} 通过
          </span>
        </div>
        <div class="space-y-2 p-4">
          {#each qualityChecks as item}
            <div
              class="flex items-center justify-between gap-3 rounded-lg border px-3 py-2 {qualityClasses(
                item.status
              )}"
            >
              <div class="min-w-0">
                <div class="truncate text-sm font-medium">
                  {#if item.category}
                    <span class="mr-1 opacity-70">{item.category}</span>
                  {/if}
                  {item.label}
                </div>
                <div class="mt-0.5 truncate text-xs opacity-75">{item.detail}</div>
              </div>
              <span class="shrink-0 text-xs">
                {#if typeof item.score === 'number' && item.status !== 'pending'}
                  {item.score}分 ·
                {/if}
                {qualityLabel(item.status)}
              </span>
            </div>
          {/each}
        </div>
        {#if blockingQualityChecks > 0}
          <div
            class="border-t border-red-100 px-4 py-3 text-xs text-red-600 dark:border-red-900/40 dark:text-red-300"
          >
            <div class="font-medium">当前还有阻塞项，暂不建议发布。</div>
            <div class="mt-1 space-y-0.5">
              {#each failedQualityChecks as item}
                <div>{item.category ? `${item.category}：` : ''}{item.label} · {item.detail}</div>
              {/each}
            </div>
          </div>
        {/if}
        <div
          class="flex flex-wrap items-center justify-between gap-2 border-t border-gray-100 px-4 py-3 dark:border-gray-800"
        >
          <div class="text-xs text-gray-500">
            {taskNo ? `任务 ${taskNo}` : '生成蓝图后可发布'}
          </div>
          <div class="flex items-center gap-2">
            <Button.Root
              onclick={handleApply}
              disabled={!taskNo ||
                applying ||
                preview?.validation?.passed === false ||
                applyPlan?.canApply === false}
              class="flex h-8 items-center gap-1 rounded-lg bg-green-600 px-3 text-xs text-white transition-colors hover:bg-green-700 disabled:opacity-60"
            >
              {#if applying}
                <Loader2 size={12} class="animate-spin" />
              {:else}
                <Send size={12} />
              {/if}
              发布到菜单
            </Button.Root>
            <Button.Root
              onclick={handleRollback}
              disabled={rollingBack || preview?.status !== 'APPLIED_TO_METADATA'}
              class="flex h-8 items-center gap-1 rounded-lg border border-gray-200 px-3 text-xs text-gray-600 transition-colors hover:border-red-400 hover:text-red-500 disabled:opacity-60 dark:border-gray-700 dark:text-gray-300"
            >
              {#if rollingBack}
                <Loader2 size={12} class="animate-spin" />
              {:else}
                <RotateCcw size={12} />
              {/if}
              回滚
            </Button.Root>
          </div>
        </div>
      </div>
    </div>
  </div>

  {#if clarification}
    <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm overflow-hidden">
      <div
        class="px-4 py-3 border-b border-gray-100 dark:border-gray-800 flex flex-wrap items-center justify-between gap-3"
      >
        <div class="flex items-center gap-2">
          <ShieldCheck size={16} class="text-cyan-500" />
          <span class="text-sm font-medium text-gray-800 dark:text-white">需求澄清</span>
        </div>
        <div class="flex items-center gap-2">
          <span
            class="inline-flex items-center rounded-full border px-2.5 py-1 text-xs {clarification.needsClarification
              ? 'border-amber-200 bg-amber-50 text-amber-700 dark:border-amber-900/50 dark:bg-amber-900/20 dark:text-amber-300'
              : 'border-green-200 bg-green-50 text-green-700 dark:border-green-900/50 dark:bg-green-900/20 dark:text-green-300'}"
          >
            {clarification.needsClarification ? '建议补充' : '可以生成'} · {clarification.qualityScore}
            分
          </span>
        </div>
      </div>
      <div class="p-4 space-y-3">
        {#if clarification.warnings.length > 0}
          <div
            class="rounded-md border border-amber-200 bg-amber-50 p-3 text-xs text-amber-700 dark:border-amber-900/50 dark:bg-amber-900/20 dark:text-amber-300"
          >
            {#each clarification.warnings as warning}
              <div>{warning}</div>
            {/each}
          </div>
        {/if}
        {#if clarification.questions.length === 0}
          <div class="text-sm text-gray-600 dark:text-gray-300">
            核心字段、流程、权限和页面行为信息已具备，当前描述可以直接进入生成。
          </div>
        {:else}
          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            {#each clarification.questions as item}
              <div class="rounded-md border border-gray-100 dark:border-gray-800 p-3">
                <div class="text-sm font-medium text-gray-800 dark:text-white">{item.question}</div>
                <div class="mt-1 text-xs text-gray-500">{item.reason}</div>
                <div class="mt-2 flex flex-wrap gap-1">
                  {#each item.examples as example}
                    <span
                      class="rounded border border-gray-200 bg-gray-50 px-2 py-1 text-[11px] text-gray-500 dark:border-gray-700 dark:bg-gray-800 dark:text-gray-400"
                    >
                      {example}
                    </span>
                  {/each}
                </div>
              </div>
            {/each}
          </div>
        {/if}
      </div>
    </div>
  {/if}

  <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm overflow-hidden">
    <div
      class="px-4 py-3 border-b border-gray-100 dark:border-gray-800 flex items-center justify-between"
    >
      <div class="flex items-center gap-2">
        <FileJson size={16} class="text-gray-500" />
        <span class="text-sm font-medium text-gray-800 dark:text-white">最近生成任务</span>
      </div>
      <Button.Root
        onclick={() => loadTasks()}
        disabled={tasksLoading}
        class="h-8 px-3 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 text-xs rounded-lg hover:border-[color:var(--color-primary)] hover:text-[color:var(--color-primary)] disabled:opacity-60 transition-colors flex items-center gap-1"
      >
        <RefreshCw size={12} class={tasksLoading ? 'animate-spin' : ''} />
        刷新
      </Button.Root>
    </div>
    {#if tasksLoading && tasks.length === 0}
      <div class="p-5 text-sm text-gray-500 flex items-center gap-2">
        <Loader2 size={14} class="animate-spin" />
        正在加载任务...
      </div>
    {:else if tasks.length === 0}
      <div class="p-5 text-sm text-gray-500">暂无生成任务</div>
    {:else}
      <div class="divide-y divide-gray-100 dark:divide-gray-800">
        {#each tasks as task}
          <div
            class="p-4 flex flex-col gap-3 md:flex-row md:items-center md:justify-between {task.taskNo ===
            taskNo
              ? 'bg-blue-50/60 dark:bg-blue-900/10'
              : ''}"
          >
            <button type="button" onclick={() => handleSelectTask(task)} class="min-w-0 text-left">
              <div class="flex flex-wrap items-center gap-2">
                <span class="text-sm font-medium text-gray-800 dark:text-white">
                  {task.moduleName}
                </span>
                <span
                  class="inline-flex items-center rounded-full border px-2 py-0.5 text-[11px] {statusClasses(
                    task.status
                  )}"
                >
                  {statusLabel(task.status)}
                </span>
                <span
                  class="inline-flex items-center rounded-full border px-2 py-0.5 text-[11px] {smokeTestClasses(
                    task.smokeTestStatus
                  )}"
                >
                  {smokeTestLabel(task.smokeTestStatus)}
                </span>
              </div>
              <div class="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-xs text-gray-500">
                <span>{task.moduleKey}</span>
                <span>{task.taskNo}</span>
                <span>{formatTime(task.smokeTestTime || task.updateTime || task.createTime)}</span>
              </div>
            </button>
            <div class="flex shrink-0 items-center gap-2">
              <Button.Root
                onclick={() => handleSelectTask(task)}
                class="h-8 px-3 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 text-xs rounded-lg hover:border-[color:var(--color-primary)] hover:text-[color:var(--color-primary)] transition-colors"
              >
                预览
              </Button.Root>
              {#if task.status === 'APPLIED_TO_METADATA'}
                <Button.Root
                  onclick={() => handleOpenTask(task)}
                  class="h-8 px-3 bg-[color:var(--color-primary)] text-white text-xs rounded-lg hover:brightness-105 transition-colors"
                >
                  打开
                </Button.Root>
              {/if}
            </div>
          </div>
        {/each}
      </div>
    {/if}
  </div>

  {#if taskNo}
    {#if applyPlan}
      <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm overflow-hidden">
        <div
          class="px-4 py-3 border-b border-gray-100 dark:border-gray-800 flex flex-wrap items-center justify-between gap-3"
        >
          <div class="flex items-center gap-2">
            <ShieldCheck size={16} class="text-emerald-500" />
            <span class="text-sm font-medium text-gray-800 dark:text-white">应用计划</span>
          </div>
          <div class="flex items-center gap-2">
            <span
              class="inline-flex items-center rounded-full border px-2.5 py-1 text-xs {riskClasses(
                applyPlan.riskLevel
              )}"
            >
              {riskLabel(applyPlan.riskLevel)} · {applyPlan.riskScore} 分
            </span>
            <span
              class="inline-flex items-center rounded-full border px-2.5 py-1 text-xs {applyPlan.canApply
                ? 'border-green-200 bg-green-50 text-green-700 dark:border-green-900/50 dark:bg-green-900/20 dark:text-green-300'
                : 'border-red-200 bg-red-50 text-red-700 dark:border-red-900/50 dark:bg-red-900/20 dark:text-red-300'}"
            >
              {applyPlan.canApply ? '允许应用' : '禁止应用'}
            </span>
          </div>
        </div>

        <div class="p-4 space-y-3">
          {#if applyPlan.warnings.length > 0}
            <div
              class="rounded-md border border-amber-200 bg-amber-50 p-3 text-xs text-amber-700 dark:border-amber-900/50 dark:bg-amber-900/20 dark:text-amber-300"
            >
              {#each applyPlan.warnings as warning}
                <div>{warning}</div>
              {/each}
            </div>
          {/if}

          <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
            {#each applyPlan.operations as operation}
              <div class="rounded-md border border-gray-100 dark:border-gray-800 p-3">
                <div class="flex items-center justify-between gap-2">
                  <div class="min-w-0">
                    <div class="text-sm font-medium text-gray-800 dark:text-white truncate">
                      {operation.description}
                    </div>
                    <div class="mt-1 text-xs text-gray-500 truncate">
                      {operation.category} / {operation.action} / {operation.target}
                    </div>
                  </div>
                  <span
                    class="shrink-0 rounded-full border px-2 py-0.5 text-[11px] {riskClasses(
                      operation.riskLevel
                    )}"
                  >
                    {riskLabel(operation.riskLevel)}
                  </span>
                </div>
              </div>
            {/each}
          </div>
        </div>
      </div>
    {/if}

    <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm overflow-hidden">
      <div
        class="px-4 py-3 border-b border-gray-100 dark:border-gray-800 flex flex-wrap items-center justify-between gap-3"
      >
        <div class="flex items-center gap-2">
          <History size={16} class="text-indigo-500" />
          <span class="text-sm font-medium text-gray-800 dark:text-white">版本治理</span>
        </div>
        <Button.Root
          onclick={() => loadModuleVersions()}
          disabled={versionsLoading}
          class="h-8 px-3 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 text-xs rounded-lg hover:border-[color:var(--color-primary)] hover:text-[color:var(--color-primary)] disabled:opacity-60 transition-colors flex items-center gap-1"
        >
          <RefreshCw size={12} class={versionsLoading ? 'animate-spin' : ''} />
          刷新
        </Button.Root>
      </div>
      {#if versionsLoading && versions.length === 0}
        <div class="p-4 text-sm text-gray-500 flex items-center gap-2">
          <Loader2 size={14} class="animate-spin" />
          正在加载版本...
        </div>
      {:else if versions.length === 0}
        <div class="p-4 text-sm text-gray-500">暂无已应用版本</div>
      {:else}
        <div class="divide-y divide-gray-100 dark:divide-gray-800">
          {#each versions as version}
            <div class="p-4 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
              <div class="min-w-0">
                <div class="flex flex-wrap items-center gap-2">
                  <span class="text-sm font-medium text-gray-800 dark:text-white">
                    v{version.versionNo} · {version.moduleName}
                  </span>
                  {#if version.current}
                    <span
                      class="inline-flex items-center rounded-full border border-green-200 bg-green-50 px-2 py-0.5 text-[11px] text-green-700 dark:border-green-900/50 dark:bg-green-900/20 dark:text-green-300"
                    >
                      当前版本
                    </span>
                  {/if}
                </div>
                <div class="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-xs text-gray-500">
                  <span>{version.taskNo}</span>
                  <span>{version.schemaHash.slice(0, 12)}</span>
                  <span>{formatTime(version.createTime)}</span>
                </div>
              </div>
              <Button.Root
                onclick={() => handleRestoreVersion(version)}
                disabled={restoringVersion || version.current}
                class="h-8 px-3 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 text-xs rounded-lg hover:border-[color:var(--color-primary)] hover:text-[color:var(--color-primary)] disabled:opacity-60 transition-colors flex items-center gap-1"
              >
                {#if restoringVersion}
                  <Loader2 size={12} class="animate-spin" />
                {:else}
                  <RotateCcw size={12} />
                {/if}
                恢复
              </Button.Root>
            </div>
          {/each}
        </div>
      {/if}
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm overflow-hidden">
        <div
          class="px-4 py-3 border-b border-gray-100 dark:border-gray-800 flex items-center gap-2"
        >
          <Code size={16} class="text-blue-500" />
          <span class="text-sm font-medium text-gray-800 dark:text-white">生成文件</span>
        </div>
        <div class="p-3 space-y-2">
          <div class="text-xs text-gray-500">任务编号：{taskNo}</div>
          <div class="text-xs text-gray-500">状态：{preview?.status || 'PREVIEW_READY'}</div>
          {#if preview?.validation}
            <div
              class="rounded border p-2 text-xs {preview.validation.passed
                ? 'border-green-200 bg-green-50 text-green-700 dark:border-green-900/50 dark:bg-green-900/20 dark:text-green-300'
                : 'border-red-200 bg-red-50 text-red-700 dark:border-red-900/50 dark:bg-red-900/20 dark:text-red-300'}"
            >
              <div class="flex items-center gap-1 font-medium">
                <ShieldCheck size={12} />
                校验{preview.validation.passed ? '通过' : '未通过'} · {preview.validation.score} 分
              </div>
              {#each preview.validation.issues as issue}
                <div class="mt-1">
                  [{issue.level}] {issue.message}
                </div>
              {/each}
            </div>
          {/if}
          {#each files as file}
            <div class="p-2 rounded border border-gray-100 dark:border-gray-800">
              <div class="text-sm text-gray-700 dark:text-gray-200 truncate">{file.path}</div>
              <div class="text-xs text-gray-400 mt-1">{file.type}</div>
            </div>
          {/each}
        </div>
      </div>

      <div class="lg:col-span-2 bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm overflow-hidden">
        <div
          class="px-4 py-3 border-b border-gray-100 dark:border-gray-800 flex items-center justify-between"
        >
          <div class="flex items-center gap-2">
            <FileJson size={16} class="text-amber-500" />
            <span class="text-sm font-medium text-gray-800 dark:text-white">Schema / 文件预览</span>
          </div>
          <div class="text-xs text-gray-500">发布动作在质量检查区执行 · {publishScore || 0} 分</div>
        </div>
        <div class="grid grid-cols-1 xl:grid-cols-2 gap-0">
          <pre
            class="min-h-[360px] max-h-[520px] overflow-auto p-4 text-xs text-green-400 bg-[#1f2937]">{schemaText}</pre>
          <pre
            class="min-h-[360px] max-h-[520px] overflow-auto p-4 text-xs text-blue-300 bg-[#111827]">{selectedFile?.content ||
              '暂无文件内容'}</pre>
        </div>
      </div>
    </div>

    <AiModuleVisualPreview
      metadata={effectiveDesignerMetadata}
      resetKey={designerMetadataRevision}
      generated={Boolean(taskNo)}
      validationPassed={preview?.validation?.passed}
    />

    {#if preview?.status === 'APPLIED_TO_METADATA'}
      <AiModuleDesigner
        metadata={effectiveDesignerMetadata}
        resetKey={designerMetadataRevision}
        saving={designerSaving}
        onSave={handleSaveDesigner}
      />
    {/if}
  {/if}
</div>
