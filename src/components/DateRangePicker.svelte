<script lang="ts">
  /**
   * 日期范围选择器组件
   * 基于 bits-ui DateRangePicker 封装
   */
  import { DateRangePicker, type DateRange, type SegmentPart } from 'bits-ui';
  import {
    DateFormatter,
    type DateValue,
    getLocalTimeZone,
    parseDate,
  } from '@internationalized/date';
  import { Calendar, ChevronLeft, ChevronRight } from 'lucide-svelte';
  import { locale } from '$lib/locales';

  // Props
  interface Props {
    startValue?: string;
    endValue?: string;
    placeholder?: string;
    disabled?: boolean;
    minValue?: DateValue;
    maxValue?: DateValue;
  }

  let {
    startValue = $bindable(),
    endValue = $bindable(),
    placeholder = '',
    disabled = false,
    minValue,
    maxValue,
  }: Props = $props();

  // 内部日期范围值
  let dateRange = $state<DateRange | undefined>(undefined);

  // 日期格式化器
  const df = $derived(
    new DateFormatter($locale, {
      dateStyle: 'medium',
    })
  );

  // 显示文本
  const displayValue = $derived(() => {
    if (dateRange?.start && dateRange?.end) {
      return `${df.format(dateRange.start.toDate(getLocalTimeZone()))} - ${df.format(dateRange.end.toDate(getLocalTimeZone()))}`;
    }
    return placeholder;
  });

  // 同步外部 value 到内部 dateRange
  $effect(() => {
    if (startValue && endValue) {
      try {
        dateRange = {
          start: parseDate(startValue),
          end: parseDate(endValue),
        };
      } catch {
        dateRange = undefined;
      }
    } else {
      dateRange = undefined;
    }
  });

  // 处理日期范围变化
  function handleValueChange(newValue: DateRange | undefined) {
    dateRange = newValue;
    if (newValue?.start && newValue?.end) {
      startValue = `${newValue.start.year}-${String(newValue.start.month).padStart(2, '0')}-${String(newValue.start.day).padStart(2, '0')}`;
      endValue = `${newValue.end.year}-${String(newValue.end.month).padStart(2, '0')}-${String(newValue.end.day).padStart(2, '0')}`;
    } else {
      startValue = undefined;
      endValue = undefined;
    }
  }
</script>

<DateRangePicker.Root
  value={dateRange}
  onValueChange={handleValueChange}
  locale={$locale}
  {disabled}
  {minValue}
  {maxValue}
  weekStartsOn={1}
  numberOfMonths={2}
>
  <div class="relative w-full">
    <div
      class="flex h-9 w-full items-center rounded border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 px-3 text-sm focus-within:border-[#409eff] focus-within:ring-1 focus-within:ring-[#409eff] transition-all {disabled
        ? 'opacity-50 cursor-not-allowed'
        : ''}"
    >
      <DateRangePicker.Input type="start" class="flex items-center">
        {#snippet children({ segments }: { segments: Array<{ part: SegmentPart; value: string }> })}
          {#each segments as { part, value: segValue }}
            {#if part === 'literal'}
              <span class="text-gray-400">{segValue}</span>
            {:else}
              <DateRangePicker.Segment
                {part}
                class="rounded px-0.5 focus:bg-[#409eff] focus:text-white focus:outline-none"
              >
                {segValue}
              </DateRangePicker.Segment>
            {/if}
          {/each}
        {/snippet}
      </DateRangePicker.Input>

      <span class="mx-2 text-gray-400">-</span>

      <DateRangePicker.Input type="end" class="flex items-center">
        {#snippet children({ segments }: { segments: Array<{ part: SegmentPart; value: string }> })}
          {#each segments as { part, value: segValue }}
            {#if part === 'literal'}
              <span class="text-gray-400">{segValue}</span>
            {:else}
              <DateRangePicker.Segment
                {part}
                class="rounded px-0.5 focus:bg-[#409eff] focus:text-white focus:outline-none"
              >
                {segValue}
              </DateRangePicker.Segment>
            {/if}
          {/each}
        {/snippet}
      </DateRangePicker.Input>

      <DateRangePicker.Trigger
        class="ml-auto pl-2 text-gray-400 hover:text-[#409eff] transition-colors"
      >
        <Calendar size={14} />
      </DateRangePicker.Trigger>
    </div>

    <DateRangePicker.Content
      class="z-50 mt-1 rounded-lg border border-gray-200 dark:border-gray-700 bg-white dark:bg-[#1d1d1d] p-3 shadow-lg"
    >
      <DateRangePicker.Calendar>
        {#snippet children({
          months,
          weekdays,
        }: {
          months: Array<{ value: DateValue; weeks: DateValue[][] }>;
          weekdays: string[];
        })}
          <DateRangePicker.Header class="flex items-center justify-between mb-3">
            <DateRangePicker.PrevButton
              class="w-8 h-8 flex items-center justify-center rounded hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-600 dark:text-gray-400 transition-colors"
            >
              <ChevronLeft size={12} />
            </DateRangePicker.PrevButton>
            <DateRangePicker.Heading class="text-sm font-medium text-gray-700 dark:text-gray-300" />
            <DateRangePicker.NextButton
              class="w-8 h-8 flex items-center justify-center rounded hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-600 dark:text-gray-400 transition-colors"
            >
              <ChevronRight size={12} />
            </DateRangePicker.NextButton>
          </DateRangePicker.Header>

          <div class="flex gap-4">
            {#each months as month}
              <DateRangePicker.Grid class="w-[280px]">
                <DateRangePicker.GridHead>
                  <DateRangePicker.GridRow class="flex">
                    {#each weekdays as day}
                      <DateRangePicker.HeadCell
                        class="w-9 h-9 flex items-center justify-center text-xs font-medium text-gray-500 dark:text-gray-400"
                      >
                        {day}
                      </DateRangePicker.HeadCell>
                    {/each}
                  </DateRangePicker.GridRow>
                </DateRangePicker.GridHead>
                <DateRangePicker.GridBody>
                  {#each month.weeks as weekDates}
                    <DateRangePicker.GridRow class="flex">
                      {#each weekDates as date}
                        <DateRangePicker.Cell {date} month={month.value} class="p-0">
                          <DateRangePicker.Day
                            class="w-9 h-9 flex items-center justify-center text-sm rounded transition-colors
                              hover:bg-gray-100 dark:hover:bg-gray-800
                              data-[today]:text-[#409eff] data-[today]:font-semibold
                              data-[selected]:bg-[#409eff] data-[selected]:text-white
                              data-[selection-start]:rounded-l-md data-[selection-start]:bg-[#409eff] data-[selection-start]:text-white
                              data-[selection-end]:rounded-r-md data-[selection-end]:bg-[#409eff] data-[selection-end]:text-white
                              data-[highlighted]:bg-[#409eff]/10
                              data-[outside-month]:text-gray-300 dark:data-[outside-month]:text-gray-600
                              data-[disabled]:text-gray-300 dark:data-[disabled]:text-gray-600 data-[disabled]:cursor-not-allowed
                              data-[unavailable]:text-gray-300 data-[unavailable]:line-through"
                          />
                        </DateRangePicker.Cell>
                      {/each}
                    </DateRangePicker.GridRow>
                  {/each}
                </DateRangePicker.GridBody>
              </DateRangePicker.Grid>
            {/each}
          </div>
        {/snippet}
      </DateRangePicker.Calendar>
    </DateRangePicker.Content>
  </div>
</DateRangePicker.Root>
