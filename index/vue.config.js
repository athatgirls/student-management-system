const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: [],
  
  // 添加这个配置来修复进度插件问题
  chainWebpack: config => {
    // 删除有问题的进度插件
    config.plugins.delete('progress')
    
    // 配置 Vue feature flags
    config.plugin('define').tap(definitions => {
      Object.assign(definitions[0], {
        __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: 'false'
      })
      return definitions
    })
  },
  
  devServer: {
    port: 3000,
    open: true,
    host: 'localhost',
    client: {
      overlay: {
        errors: true,
        warnings: false,
        // 过滤 ResizeObserver 错误
        runtimeErrors: (error) => {
          const errorMessage = error?.message || error?.toString() || '';
          // 如果是 ResizeObserver 错误，不显示 overlay
          if (errorMessage.includes('ResizeObserver loop') || 
              errorMessage.includes('ResizeObserver loop limit exceeded') ||
              errorMessage.includes('ResizeObserver loop completed')) {
            return false;
          }
          return true;
        }
      }
    }
  }
})