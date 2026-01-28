interface Props {
    /** 图片值：单图模式为 URL 字符串；多图模式为逗号分隔的 URL 字符串 */
    value?: string;
    /** 是否支持多图上传 */
    multiple?: boolean;
    /** 最大文件大小（MB） */
    maxSize?: number;
    /** 接受的文件类型 */
    accept?: string;
    /** 是否禁用 */
    disabled?: boolean;
    /** 最大图片数量限制（0 表示不限制） */
    limit?: number;
    /** 上传函数，返回上传后的 URL */
    onUpload?: (file: File) => Promise<string>;
    /** 获取图片完整 URL 的函数 */
    getImageUrl?: (url: string) => string;
    /** 值变化回调 */
    onValueChange?: (value: string) => void;
}
declare const ImageUpload: import("svelte").Component<Props, {}, "value">;
type ImageUpload = ReturnType<typeof ImageUpload>;
export default ImageUpload;
//# sourceMappingURL=ImageUpload.svelte.d.ts.map