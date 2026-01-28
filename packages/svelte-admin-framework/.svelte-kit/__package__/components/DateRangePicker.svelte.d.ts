/**
   * 日期范围选择器组件
   * @zhoolg/svelte-admin-framework
   */
import { DateRangePicker } from 'bits-ui';
import { type DateValue } from '@internationalized/date';
interface Props {
    startValue?: string;
    endValue?: string;
    placeholder?: string;
    disabled?: boolean;
    minValue?: DateValue;
    maxValue?: DateValue;
}
declare const DateRangePicker: import("svelte").Component<Props, {}, "startValue" | "endValue">;
type DateRangePicker = ReturnType<typeof DateRangePicker>;
export default DateRangePicker;
//# sourceMappingURL=DateRangePicker.svelte.d.ts.map