<script lang="ts">
  import {
    ArrowDown,
    ArrowUp,
    ClipboardList,
    GripVertical,
    Plus,
    RotateCcw,
    Save,
    Search,
    Settings2,
    Table2,
    Trash2,
  } from 'lucide-svelte';

  export let metadata: Record<string, unknown> | null | undefined = null;
  export let saving = false;
  export let resetKey: string | number = '';
  export let onSave: ((metadata: Record<string, unknown>) => void | Promise<void>) | undefined;

  type CrudListName = 'columns' | 'search' | 'form';
  type CrudField = Record<string, unknown>;

  let draft: Record<string, unknown> | null = null;
  let lastModuleId = '';
  let lastResetKey = '';
  let dragging: { list: CrudListName; index: number } | null = null;

  $: moduleId = metadata && typeof metadata.id === 'string' ? metadata.id : '';
  $: if (metadata && (moduleId !== lastModuleId || String(resetKey) !== lastResetKey)) {
    resetDraft();
  }
  $: crud = crudDraft();
  $: columns = listDraft('columns');
  $: search = listDraft('search');
  $: form = listDraft('form');

  function resetDraft() {
    draft = metadata ? deepClone(metadata) : null;
    lastModuleId = moduleId;
    lastResetKey = String(resetKey);
  }

  function deepClone<T>(value: T): T {
    return JSON.parse(JSON.stringify(value));
  }

  function crudDraft(): Record<string, unknown> {
    if (!draft || typeof draft.crud !== 'object' || draft.crud === null) {
      return {};
    }
    return draft.crud as Record<string, unknown>;
  }

  function listDraft(name: CrudListName): CrudField[] {
    const value = crudDraft()[name];
    return Array.isArray(value) ? (value as CrudField[]) : [];
  }

  function patchDraft(next: Record<string, unknown>) {
    draft = { ...(draft || {}), ...next };
  }

  function patchCrud(next: Record<string, unknown>) {
    patchDraft({ crud: { ...crudDraft(), ...next } });
  }

  function updateRoot(key: string, value: string) {
    patchDraft({ [key]: value });
  }

  function updateCrudValue(key: string, value: string) {
    patchCrud({ [key]: value });
  }

  function updateListField(listName: CrudListName, index: number, key: string, value: unknown) {
    const next = listDraft(listName).map((item, itemIndex) =>
      itemIndex === index ? { ...item, [key]: value } : item
    );
    patchCrud({ [listName]: next });
  }

  function moveItem(listName: CrudListName, from: number, to: number) {
    const list = [...listDraft(listName)];
    if (to < 0 || to >= list.length || from === to) {
      return;
    }
    const [item] = list.splice(from, 1);
    list.splice(to, 0, item);
    patchCrud({ [listName]: list });
  }

  function removeItem(listName: CrudListName, index: number) {
    patchCrud({ [listName]: listDraft(listName).filter((_, itemIndex) => itemIndex !== index) });
  }

  function addFromColumn(listName: 'search' | 'form') {
    const used = new Set(listDraft(listName).map(item => String(item.field || '')));
    const source = columns.find(item => !used.has(String(item.field || '')));
    if (!source) {
      return;
    }
    const item =
      listName === 'search'
        ? {
            field: source.field,
            label: source.label || source.field,
            type: 'input',
            placeholder: `请输入${source.label || source.field}`,
          }
        : {
            field: source.field,
            label: source.label || source.field,
            type: 'input',
            required: false,
            placeholder: `请输入${source.label || source.field}`,
          };
    patchCrud({ [listName]: [...listDraft(listName), item] });
  }

  function onDragStart(list: CrudListName, index: number) {
    dragging = { list, index };
  }

  function onDrop(list: CrudListName, index: number) {
    if (!dragging || dragging.list !== list) {
      dragging = null;
      return;
    }
    moveItem(list, dragging.index, index);
    dragging = null;
  }

  async function handleSave() {
    if (!draft || !onSave) {
      return;
    }
    await onSave(deepClone(draft));
  }

  function text(value: unknown): string {
    return value == null ? '' : String(value);
  }
</script>

<div class="bg-white dark:bg-[#1d1d1d] rounded-lg shadow-sm overflow-hidden">
  <div
    class="px-4 py-3 border-b border-gray-100 dark:border-gray-800 flex flex-wrap items-center justify-between gap-3"
  >
    <div class="flex items-center gap-2">
      <Settings2 size={16} class="text-violet-500" />
      <div>
        <div class="text-sm font-medium text-gray-800 dark:text-white">页面设计器</div>
        <div class="text-xs text-gray-400">编辑已应用模块的页面 metadata，并保存为新版本</div>
      </div>
    </div>
    <div class="flex items-center gap-2">
      <button
        type="button"
        onclick={resetDraft}
        disabled={!metadata || saving}
        class="h-8 px-3 rounded-md border border-gray-200 dark:border-gray-700 text-xs text-gray-600 dark:text-gray-300 disabled:opacity-60 flex items-center gap-1"
      >
        <RotateCcw size={12} />
        重置
      </button>
      <button
        type="button"
        onclick={handleSave}
        disabled={!draft || saving}
        class="h-8 px-3 rounded-md bg-[color:var(--color-primary)] text-xs text-white disabled:opacity-60 flex items-center gap-1"
      >
        <Save size={12} />
        {saving ? '保存中' : '保存新版'}
      </button>
    </div>
  </div>

  {#if draft}
    <div class="p-4 space-y-4">
      <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-3">
        <label class="space-y-1">
          <span class="text-xs text-gray-500 dark:text-gray-400">菜单名称</span>
          <input
            value={text(draft.label)}
            oninput={event => updateRoot('label', event.currentTarget.value)}
            class="h-9 w-full rounded-md border border-gray-200 bg-white px-3 text-sm text-gray-700 focus:border-[color:var(--color-primary)] focus:outline-none dark:border-gray-700 dark:bg-[#141414] dark:text-gray-200"
          />
        </label>
        <label class="space-y-1">
          <span class="text-xs text-gray-500 dark:text-gray-400">图标</span>
          <input
            value={text(draft.icon)}
            oninput={event => updateRoot('icon', event.currentTarget.value)}
            class="h-9 w-full rounded-md border border-gray-200 bg-white px-3 text-sm text-gray-700 focus:border-[color:var(--color-primary)] focus:outline-none dark:border-gray-700 dark:bg-[#141414] dark:text-gray-200"
          />
        </label>
        <label class="space-y-1">
          <span class="text-xs text-gray-500 dark:text-gray-400">路径</span>
          <input
            value={text(draft.path)}
            oninput={event => updateRoot('path', event.currentTarget.value)}
            class="h-9 w-full rounded-md border border-gray-200 bg-white px-3 text-sm text-gray-700 focus:border-[color:var(--color-primary)] focus:outline-none dark:border-gray-700 dark:bg-[#141414] dark:text-gray-200"
          />
        </label>
        <label class="space-y-1">
          <span class="text-xs text-gray-500 dark:text-gray-400">页面标题</span>
          <input
            value={text(crud.title)}
            oninput={event => updateCrudValue('title', event.currentTarget.value)}
            class="h-9 w-full rounded-md border border-gray-200 bg-white px-3 text-sm text-gray-700 focus:border-[color:var(--color-primary)] focus:outline-none dark:border-gray-700 dark:bg-[#141414] dark:text-gray-200"
          />
        </label>
      </div>

      {@render DesignerList(
        '表格列',
        'table',
        'columns',
        columns,
        false,
        onDragStart,
        onDrop,
        moveItem,
        removeItem,
        updateListField
      )}

      {@render DesignerList(
        '搜索项',
        'search',
        'search',
        search,
        true,
        onDragStart,
        onDrop,
        moveItem,
        removeItem,
        updateListField,
        () => addFromColumn('search')
      )}

      {@render DesignerList(
        '表单项',
        'form',
        'form',
        form,
        true,
        onDragStart,
        onDrop,
        moveItem,
        removeItem,
        updateListField,
        () => addFromColumn('form')
      )}
    </div>
  {:else}
    <div class="p-6 text-center text-sm text-gray-400">应用模块后可在这里编辑页面配置</div>
  {/if}
</div>

{#snippet DesignerList(
  title: string,
  icon: string,
  listName: CrudListName,
  items: CrudField[],
  allowAdd: boolean,
  onDragStart: (list: CrudListName, index: number) => void,
  onDrop: (list: CrudListName, index: number) => void,
  moveItem: (list: CrudListName, from: number, to: number) => void,
  removeItem: (list: CrudListName, index: number) => void,
  updateListField: (list: CrudListName, index: number, key: string, value: unknown) => void,
  onAdd?: () => void
)}
  <div class="rounded-md border border-gray-100 dark:border-gray-800 overflow-hidden">
    <div
      class="px-3 py-2 border-b border-gray-100 dark:border-gray-800 flex items-center justify-between gap-2"
    >
      <div class="flex items-center gap-2 text-sm font-medium text-gray-800 dark:text-white">
        {#if icon === 'table'}
          <Table2 size={15} class="text-blue-500" />
        {:else if icon === 'search'}
          <Search size={15} class="text-amber-500" />
        {:else}
          <ClipboardList size={15} class="text-emerald-500" />
        {/if}
        {title}
        <span class="text-xs font-normal text-gray-400">{items.length} 项</span>
      </div>
      {#if allowAdd && onAdd}
        <button
          type="button"
          onclick={onAdd}
          class="h-7 px-2 rounded border border-gray-200 dark:border-gray-700 text-xs text-gray-600 dark:text-gray-300 flex items-center gap-1"
        >
          <Plus size={11} />
          从表格列添加
        </button>
      {/if}
    </div>
    <div class="divide-y divide-gray-100 dark:divide-gray-800">
      {#each items as item, index}
        <div
          role="listitem"
          class="grid grid-cols-1 xl:grid-cols-[28px_1fr_1fr_120px_150px] gap-2 p-3 items-center"
          draggable="true"
          ondragstart={() => onDragStart(listName, index)}
          ondragover={event => event.preventDefault()}
          ondrop={() => onDrop(listName, index)}
        >
          <div class="text-gray-300 cursor-grab">
            <GripVertical size={15} />
          </div>
          <label class="space-y-1">
            <span class="text-[11px] text-gray-400">字段</span>
            <input
              value={text(item.field)}
              disabled
              class="h-8 w-full rounded-md border border-gray-100 bg-gray-50 px-2 text-xs text-gray-500 dark:border-gray-800 dark:bg-gray-900 dark:text-gray-400"
            />
          </label>
          <label class="space-y-1">
            <span class="text-[11px] text-gray-400">显示名称</span>
            <input
              value={text(item.label)}
              oninput={event =>
                updateListField(listName, index, 'label', event.currentTarget.value)}
              class="h-8 w-full rounded-md border border-gray-200 bg-white px-2 text-xs text-gray-700 focus:border-[color:var(--color-primary)] focus:outline-none dark:border-gray-700 dark:bg-[#141414] dark:text-gray-200"
            />
          </label>
          <label class="space-y-1">
            <span class="text-[11px] text-gray-400">{listName === 'columns' ? '宽度' : '类型'}</span
            >
            {#if listName === 'columns'}
              <input
                value={text(item.width || item.minWidth)}
                oninput={event =>
                  updateListField(
                    listName,
                    index,
                    'width',
                    Number(event.currentTarget.value) || ''
                  )}
                class="h-8 w-full rounded-md border border-gray-200 bg-white px-2 text-xs text-gray-700 focus:border-[color:var(--color-primary)] focus:outline-none dark:border-gray-700 dark:bg-[#141414] dark:text-gray-200"
              />
            {:else}
              <select
                value={text(item.type || 'input')}
                onchange={event =>
                  updateListField(listName, index, 'type', event.currentTarget.value)}
                class="h-8 w-full rounded-md border border-gray-200 bg-white px-2 text-xs text-gray-700 focus:border-[color:var(--color-primary)] focus:outline-none dark:border-gray-700 dark:bg-[#141414] dark:text-gray-200"
              >
                <option value="input">input</option>
                <option value="textarea">textarea</option>
                <option value="select">select</option>
                <option value="number">number</option>
                <option value="date">date</option>
                <option value="datetime">datetime</option>
                <option value="switch">switch</option>
              </select>
            {/if}
          </label>
          <div class="flex items-center justify-end gap-1">
            {#if listName === 'form'}
              <label class="mr-2 inline-flex items-center gap-1 text-xs text-gray-500">
                <input
                  type="checkbox"
                  checked={item.required === true}
                  onchange={event =>
                    updateListField(listName, index, 'required', event.currentTarget.checked)}
                />
                必填
              </label>
            {/if}
            <button
              type="button"
              onclick={() => moveItem(listName, index, index - 1)}
              disabled={index === 0}
              class="h-7 w-7 rounded border border-gray-200 text-gray-500 disabled:opacity-40 dark:border-gray-700"
            >
              <ArrowUp size={12} class="mx-auto" />
            </button>
            <button
              type="button"
              onclick={() => moveItem(listName, index, index + 1)}
              disabled={index === items.length - 1}
              class="h-7 w-7 rounded border border-gray-200 text-gray-500 disabled:opacity-40 dark:border-gray-700"
            >
              <ArrowDown size={12} class="mx-auto" />
            </button>
            {#if listName !== 'columns'}
              <button
                type="button"
                onclick={() => removeItem(listName, index)}
                class="h-7 w-7 rounded border border-red-200 text-red-500 dark:border-red-900/60"
              >
                <Trash2 size={12} class="mx-auto" />
              </button>
            {/if}
          </div>
        </div>
      {/each}
    </div>
  </div>
{/snippet}
