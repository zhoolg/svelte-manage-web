/**
   * 日期选择器组件
   * @zhoolg/svelte-admin-framework
   */
import { DatePicker } from 'bits-ui';
import { type DateValue } from '@internationalized/date';
interface Props {
    value?: string;
    placeholder?: string;
    disabled?: boolean;
    minValue?: DateValue;
    maxValue?: DateValue;
}
declare const DatePicker: import("svelte").Component<Props, {}, "value">;
type DatePicker = ReturnType<typeof DatePicker>;
export default DatePicker;
//# sourceMappingURL=DatePicker.svelte.d.ts.map