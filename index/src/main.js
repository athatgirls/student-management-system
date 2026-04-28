import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'

// 引入Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@/styles/mobile.css' // 引入移动端适配样式
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 引入中文语言包
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

// 解决 ResizeObserver 错误（这是一个已知的浏览器警告，不影响功能）
const isResizeObserverError = (error) => {
  if (!error) return false;
  
  const errorString = typeof error === 'string' 
    ? error 
    : error.message || error.toString() || '';
  
  return errorString.includes('ResizeObserver loop') || 
         errorString.includes('ResizeObserver loop limit exceeded') ||
         errorString.includes('ResizeObserver loop completed');
};

// 拦截 console.error
const originalError = window.console.error;
window.console.error = (...args) => {
  // 检查所有参数
  for (const arg of args) {
    if (isResizeObserverError(arg)) {
      return; // 忽略 ResizeObserver 相关错误
    }
  }
  originalError.apply(console, args);
};

// 拦截全局错误事件（使用 capture 阶段，确保最早捕获）
const errorHandler = (e) => {
  if (isResizeObserverError(e.message) || isResizeObserverError(e.error)) {
    e.stopImmediatePropagation();
    e.stopPropagation();
    e.preventDefault();
    return false;
  }
};
window.addEventListener('error', errorHandler, true);

// 拦截未处理的 Promise 拒绝
window.addEventListener('unhandledrejection', (e) => {
  if (isResizeObserverError(e.reason) || isResizeObserverError(e.reason?.message)) {
    e.preventDefault();
    return false;
  }
});

// 拦截 webpack-dev-server overlay 的错误处理
if (process.env.NODE_ENV === 'development') {
  // 方法1: 通过全局对象访问（立即执行，不等待）
  const setupWebpackOverlayHandler = () => {
    try {
      // 尝试多种可能的路径
      const possiblePaths = [
        window.__webpack_dev_server_client__?.overlay,
        window.__webpack_dev_server_client__?.default?.overlay,
        window.__webpack_dev_server_client__?.client?.overlay
      ];
      
      for (const overlay of possiblePaths) {
        if (overlay?.handleError) {
          const originalHandleError = overlay.handleError;
          overlay.handleError = (error) => {
            // 检查错误消息
            const errorStr = error?.message || error?.toString() || String(error) || '';
            if (!isResizeObserverError(errorStr)) {
              originalHandleError.call(overlay, error);
            }
          };
        }
      }
      
      // 也尝试直接拦截 overlay 的显示方法
      if (window.__webpack_dev_server_client__?.overlay?.show) {
        const originalShow = window.__webpack_dev_server_client__.overlay.show;
        window.__webpack_dev_server_client__.overlay.show = function(...args) {
          // 检查是否包含 ResizeObserver 错误
          const hasResizeObserverError = args.some(arg => {
            const errorStr = arg?.message || arg?.toString() || String(arg) || '';
            return isResizeObserverError(errorStr);
          });
          if (!hasResizeObserverError) {
            originalShow.apply(this, args);
          }
        };
      }
    } catch (e) {
      // 忽略初始化错误
    }
  };
  
  // 立即尝试设置
  setupWebpackOverlayHandler();
  
  // 延迟设置（等待 webpack-dev-server 初始化）
  setTimeout(setupWebpackOverlayHandler, 100);
  setTimeout(setupWebpackOverlayHandler, 500);
  setTimeout(setupWebpackOverlayHandler, 1000);
  setTimeout(setupWebpackOverlayHandler, 2000);
  
  // 方法2: 通过 DOM 查找 overlay 元素并隐藏 ResizeObserver 错误
  const hideResizeObserverErrors = () => {
    try {
      // 查找所有可能的 overlay 选择器
      const selectors = [
        '#webpack-dev-server-client-overlay',
        '[data-overlay-name]',
        '.webpack-dev-server-client-overlay'
      ];
      
      for (const selector of selectors) {
        const overlay = document.querySelector(selector);
        if (overlay) {
          const text = overlay.textContent || overlay.innerText || '';
          if (isResizeObserverError(text)) {
            overlay.style.display = 'none';
            overlay.remove(); // 直接移除 overlay
          }
        }
      }
    } catch (e) {
      // 忽略错误
    }
  };
  
  // 立即检查一次
  hideResizeObserverErrors();
  
  // 监听 DOM 变化（更频繁地检查）
  const observer = new MutationObserver(() => {
    hideResizeObserverErrors();
  });
  
  // 等待 DOM 加载完成
  if (document.body) {
    observer.observe(document.body, {
      childList: true,
      subtree: true
    });
  } else {
    document.addEventListener('DOMContentLoaded', () => {
      observer.observe(document.body, {
        childList: true,
        subtree: true
      });
    });
  }
  
  // 定期检查并移除 overlay（更频繁）
  setInterval(hideResizeObserverErrors, 50);
}

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(store)
   .use(router)
   .use(ElementPlus, {
     locale: zhCn,
   })
   .mount('#app')
