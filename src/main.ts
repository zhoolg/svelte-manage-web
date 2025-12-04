/*
 * Copyright 2025 zhoolg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { mount } from 'svelte'
import './app.css'
import App from './App.svelte'
import { APP_CONFIG } from './config'
import { getLocale } from './lib/locales'

// 设置页面标题
document.title = APP_CONFIG.title

// 设置 favicon（如果有自定义图标）
const favicon = document.querySelector('link[rel="icon"]') as HTMLLinkElement
if (favicon && import.meta.env.VITE_APP_FAVICON) {
  favicon.href = import.meta.env.VITE_APP_FAVICON
}

// 初始化语言设置
const currentLocale = getLocale()
document.documentElement.lang = currentLocale
// console.log('[i18n] Initial locale:', currentLocale)

const app = mount(App, {
  target: document.getElementById('app')!,
})

export default app
