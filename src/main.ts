/*
 * Copyright 2025 zhoolg
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
