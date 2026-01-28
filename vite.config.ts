import { defineConfig, loadEnv, type Plugin } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';
import path from 'path';

// HTML 转换插件 - 替换环境变量占位符
function htmlPlugin(env: Record<string, string>): Plugin {
  return {
    name: 'html-transform',
    transformIndexHtml(html) {
      return html.replace(/%(\w+)%/g, (match, key) => {
        return env[key] || match;
      });
    },
  };
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // 加载环境变量
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [svelte(), htmlPlugin(env)],

    // 路径别名配置
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
        $lib: path.resolve(__dirname, './src/lib'),
        $config: path.resolve(__dirname, './src/config'),
        $components: path.resolve(__dirname, './src/components'),
        $stores: path.resolve(__dirname, './src/stores'),
        $utils: path.resolve(__dirname, './src/utils'),
      },
    },

    // 开发服务器配置
    server: {
      port: Number(env.VITE_PORT),
      host: true,
      open: false,
      // API 代理配置
      proxy: {
        '/api': {
          target: env.VITE_APP_TARGET_URL,
          changeOrigin: true,
          rewrite: path => path.replace(/^\/api/, ''),
          // 配置代理日志
          configure: (proxy, _options) => {
            proxy.on('error', (err, _req, _res) => {
              console.log('proxy error', err);
            });
            proxy.on('proxyReq', (proxyReq, req, _res) => {
              console.log('Sending Request:', req.method, req.url, '→', proxyReq.path);
            });
            proxy.on('proxyRes', (proxyRes, req, _res) => {
              console.log('Received Response:', proxyRes.statusCode, req.url);
            });
          },
        },
      },
    },

    // 构建配置
    build: {
      outDir: 'dist',
      assetsDir: 'assets',
      sourcemap: mode === 'development',
      // 代码分割
      rollupOptions: {
        output: {
          manualChunks: {
            vendor: ['svelte'],
          },
        },
      },
      // 压缩配置
      minify: 'esbuild',
      // 资源内联限制
      assetsInlineLimit: 4096,
    },

    // 预览服务器配置
    preview: {
      port: Number(env.VITE_PORT) || 7052,
      host: true,
    },

    // 优化配置
    optimizeDeps: {
      include: ['svelte'],
    },
  };
});
