<script lang="ts">
  /**
   * 日期选择器组件
   * @zhoolg/svelte-admin-framework
   */
  import { DatePicker, type SegmentPart } from 'bits-ui';
  import {
    DateFormatter,
    type DateValue,
    getLocalTimeZone,
    parseDate,
  } from '@internationalized/date';
  import { Calendar, ChevronLeft, ChevronRight } from 'lucide-svelte';
  import { getI18n } from '../locales';

  // Props
  interface Props {
    value?: string;
    placeholder?: string;
    disabled?: boolean;
    minValue?: DateValue;
    maxValue?: DateValue;
  }

  let {
    value = $bindable(),
    placeholder = '',
    disabled = false,
    minValue,
    maxValue,
  }: Props = $props();

  const { locale } = getI18n();

  // 内部日期值
  let dateValue = $state<DateValue | undefined>(undefined);

  // 日期格式化器
  const df = $derived(
    new DateFormatter($locale, {
      dateStyle: 'medium',
    })
  );

  // 显示文本
  const displayValue = $derived(
    dateValue ? df.format(dateValue.toDate(getLocalTimeZone())) : placeholder
  );

  // 同步外部 value 到内部 dateValue
  $effect(() => {
    if (value) {
      try {
        dateValue = parseDate(value);
      } catch {
        dateValue = undefined;
      }
    } else {
      dateValue = undefined;
    }
  });

  // 处理日期变化
  function handleValueChange(newValue: DateValue | undefined) {
    dateValue = newValue;
    if (newValue) {
      value = `${newValue.year}-${String(newValue.month).padStart(2, '0')}-${String(newValue.day).padStart(2, '0')}`;
    } else {
      value = undefined;
    }
  }
</script>

<DatePicker.Root
  value={dateValue}
  onValueChange={handleValueChange}
  locale={$locale}
  {disabled}
  {minValue}
  {maxValue}
  weekStartsOn={1}
>
  <div class="relative w-full">
    <DatePicker.Input
      class="flex h-9 w-full items-center justify-between rounded border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 px-3 text-sm focus-within:border-[#409eff] focus-within:ring-1 focus-within:ring-[#409eff] transition-all {disabled
        ? 'opacity-50 cursor-not-allowed'
        : ''}"
    >
      {#snippet children({ segments }: { segments: Array<{ part: SegmentPart; value: string }> })}
        {#each segments as { part, value: segValue }}
          {#if part === 'literal'}
            <span class="text-gray-400">{segValue}</span>
          {:else}
            <DatePicker.Segment
              {part}
              class="rounded px-0.5 focus:bg-[#409eff] focus:text-white focus:outline-none"
            >
              {segValue}
            </DatePicker.Segment>
          {/if}
        {/each}
        <DatePicker.Trigger class="ml-auto pl-2 text-gray-400 hover:text-[#409eff] transition-colors">
          <Calendar size={14} />
        </DatePicker.Trigger>
      {/snippet}
    </DatePicker.Input>

    <DatePicker.Content
      class="z-50 mt-1 rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-[#1d1d1d] p-3 shadow-lg"
    >
      <DatePicker.Calendar>
        {#snippet children({
          months,
          weekdays,
        }: {
          months: Array<{ value: DateValue; weeks: DateValue[][] }>;
          weekdays: string[];
        })}
          <DatePicker.Header class="flex items-center justify-between mb-3">
            <DatePicker.PrevButton
              class="w-8 h-8 flex items-center justify-center rounded hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-600 dark:text-gray-400 transition-colors"
            >
              <ChevronLeft size={12} />
            </DatePicker.PrevButton>
            <DatePicker.Heading class="text-sm font-medium text-gray-700 dark:text-gray-300" />
            <DatePicker.NextButton
              class="w-8 h-8 flex items-center justify-center rounded hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-600 dark:text-gray-400 transition-colors"
            >
              <ChevronRight size={12} />
            </DatePicker.NextButton>
          </DatePicker.Header>

          {#each months as month}
            <DatePicker.Grid class="w-full">
              <DatePicker.GridHead>
                <DatePicker.GridRow class="flex">
                  {#each weekdays as day}
                    <DatePicker.HeadCell
                      class="w-9 h-9 flex items-center justify-center text-xs font-medium text-gray-500 dark:text-gray-400"
                    >
                      {day}
                    </DatePicker.HeadCell>
                  {/each}
                </DatePicker.GridRow>
              </DatePicker.GridHead>
              <DatePicker.GridBody>
                {#each month.weeks as weekDates}
                  <DatePicker.GridRow class="flex">
                    {#each weekDates as date}
                      <DatePicker.Cell {date} month={month.value} class="p-0">
                        <DatePicker.Day
                          class="w-9 h-9 flex items-center justify-center text-sm rounded transition-colors
                            hover:bg-gray-100 dark:hover:bg-gray-800
                            data-[today]:text-[#409eff] data-[today]:font-semibold
                            data-[selected]:bg-[#409eff] data-[selected]:text-white data-[selected]:hover:bg-[#66b1ff]
                            data-[outside-month]:text-gray-300 dark:data-[outside-month]:text-gray-600
                            data-[disabled]:text-gray-300 dark:data-[disabled]:text-gray-600 data-[disabled]:cursor-not-allowed
                            data-[unavailable]:text-gray-300 data-[unavailable]:line-through"
                        />
                      </DatePicker.Cell>
                    {/each}
                  </DatePicker.GridRow>
                {/each}
              </DatePicker.GridBody>
            </DatePicker.Grid>
          {/each}
        {/snippet}
      </DatePicker.Calendar>
    </DatePicker.Content>
  </div>
</DatePicker.Root>
