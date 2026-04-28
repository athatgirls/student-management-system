<template>
  <div class="preview-page">
    <section class="hero">
      <div class="eyebrow">Frontend Preview</div>
      <h1>只看前端，也能把系统跑起来</h1>
      <p>
        预览模式会自动跳过登录，并把接口请求切换成本地假数据。
        你可以先检查页面结构、菜单、表格、图表和交互，再决定后端什么时候接入。
      </p>

      <div class="actions">
        <el-button type="primary" size="large" @click="startPreview('student', '/home')">
          进入学生端
        </el-button>
        <el-button size="large" @click="startPreview('admin', '/admin/dashboard')">
          进入管理员端
        </el-button>
        <el-button text size="large" @click="stopPreview">
          关闭预览模式
        </el-button>
      </div>
    </section>

    <section class="cards">
      <article class="preview-card student">
        <span>Student</span>
        <h2>学生端页面</h2>
        <p>首页、个人信息、日常管理、实习就业、科创竞赛、校友、党员模块都可以直接浏览。</p>
        <el-button link type="primary" @click="startPreview('student', '/home')">查看学生首页</el-button>
      </article>

      <article class="preview-card admin">
        <span>Admin</span>
        <h2>后台管理页面</h2>
        <p>仪表盘、学生管理、实习分析、日常审批、科创竞赛和党建管理会加载预览数据。</p>
        <el-button link type="primary" @click="startPreview('admin', '/admin/dashboard')">查看后台仪表盘</el-button>
      </article>

      <article class="preview-card note">
        <span>Tip</span>
        <h2>怎么恢复真实接口</h2>
        <p>点击“关闭预览模式”后，前端会恢复使用 request.js 里配置的真实后端地址。</p>
        <el-button link type="primary" @click="stopPreview">退出预览</el-button>
      </article>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { disablePreviewMode, enablePreviewMode } from '@/utils/previewMode'

const router = useRouter()
const store = useStore()

const startPreview = (userType, path) => {
  const user = enablePreviewMode(userType)
  store.commit('setUser', user)
  store.commit('setToken', localStorage.getItem('token'))
  ElMessage.success('已开启前端预览模式')
  router.push(path)
}

const stopPreview = () => {
  disablePreviewMode()
  store.commit('logout')
  ElMessage.success('已关闭前端预览模式')
  router.push('/login')
}
</script>

<style scoped>
.preview-page {
  min-height: 100vh;
  padding: 64px min(7vw, 96px);
  color: #182230;
  background:
    radial-gradient(circle at 12% 18%, rgba(0, 142, 122, 0.18), transparent 28%),
    radial-gradient(circle at 88% 8%, rgba(245, 158, 11, 0.18), transparent 30%),
    linear-gradient(135deg, #f7f3e8 0%, #eef7f3 52%, #f6fbff 100%);
}

.hero {
  max-width: 860px;
}

.eyebrow {
  display: inline-flex;
  padding: 8px 14px;
  border: 1px solid rgba(24, 34, 48, 0.14);
  border-radius: 999px;
  margin-bottom: 22px;
  color: #00796b;
  background: rgba(255, 255, 255, 0.64);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero h1 {
  margin: 0;
  max-width: 760px;
  font-size: clamp(42px, 7vw, 88px);
  line-height: 0.95;
  letter-spacing: -0.06em;
}

.hero p {
  max-width: 720px;
  margin: 24px 0 0;
  color: #536171;
  font-size: 18px;
  line-height: 1.8;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 34px;
}

.cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-top: 58px;
}

.preview-card {
  min-height: 238px;
  padding: 28px;
  border: 1px solid rgba(24, 34, 48, 0.12);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.74);
  box-shadow: 0 24px 80px rgba(24, 34, 48, 0.08);
  backdrop-filter: blur(18px);
}

.preview-card span {
  color: #00796b;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.preview-card h2 {
  margin: 18px 0 12px;
  font-size: 26px;
}

.preview-card p {
  min-height: 78px;
  color: #5d6875;
  line-height: 1.75;
}

.preview-card.admin span {
  color: #c47a00;
}

.preview-card.note span {
  color: #315f8c;
}

@media (max-width: 900px) {
  .preview-page {
    padding: 42px 20px;
  }

  .cards {
    grid-template-columns: 1fr;
  }

  .preview-card p {
    min-height: auto;
  }
}
</style>
