<template>
  <div class="internship-page">
    <el-card class="module-tabs-card">
      <el-tabs
        v-model="secondaryTab"
        class="secondary-tabs"
        :class="{ 'external-tab-mode': isSeparatedSubPage }"
        @tab-change="handleSecondaryTabChange"
      >
        <el-tab-pane label="就业意向采集" name="intent">
          <section class="intent-board">
            <div class="intent-copy">
              <span class="intent-kicker">就业意向采集</span>
              <h2>操作指引</h2>
              <ol class="intent-steps">
                <li>选择毕业去向，确认当前发展方向。</li>
                <li>填写目标城市、目标行业和期望薪资。</li>
                <li>补充目标岗位后点击“保存意向”。</li>
              </ol>
            </div>
            <el-form :model="intentForm" label-position="top" class="intent-form">
              <el-row :gutter="14">
                <el-col :xs="24" :sm="12" :md="6">
                  <el-form-item label="毕业去向">
                    <el-select v-model="intentForm.intentionType" placeholder="请选择" style="width: 100%">
                      <el-option label="就业" value="就业" />
                      <el-option label="升学" value="升学" />
                      <el-option label="考公/事业编" value="考公/事业编" />
                      <el-option label="暂未确定" value="暂未确定" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12" :md="6">
                  <el-form-item label="目标城市">
                    <el-input v-model="intentForm.targetCity" placeholder="如：武汉 / 杭州" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12" :md="6">
                  <el-form-item label="目标行业">
                    <el-select v-model="intentForm.targetIndustry" placeholder="请选择" style="width: 100%">
                      <el-option label="互联网/软件" value="互联网/软件" />
                      <el-option label="人工智能" value="人工智能" />
                      <el-option label="金融科技" value="金融科技" />
                      <el-option label="教育科研" value="教育科研" />
                      <el-option label="其他" value="其他" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12" :md="6">
                  <el-form-item label="期望薪资">
                    <el-select v-model="intentForm.expectedSalary" placeholder="请选择" style="width: 100%">
                      <el-option label="3k-5k" value="3k-5k" />
                      <el-option label="5k-8k" value="5k-8k" />
                      <el-option label="8k-12k" value="8k-12k" />
                      <el-option label="12k以上" value="12k以上" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <div class="intent-actions">
                <el-input v-model="intentForm.targetPosition" placeholder="目标岗位，如：前端开发 / 产品助理" />
                <el-button type="primary" @click="saveIntent">保存意向</el-button>
              </div>
            </el-form>
          </section>
        </el-tab-pane>

        <el-tab-pane label="分类展示与记录" name="records">
          <section class="classification-grid">
            <article
              v-for="item in classificationCards"
              :key="item.key"
              class="classification-card"
              :class="{ active: selectedCategory === item.key }"
              @click="selectedCategory = item.key"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.count }}</strong>
              <em>{{ item.hint }}</em>
            </article>
          </section>

          <el-card>
      <template #header>
        <div class="card-header">
          <span>实习就业管理</span>
          <el-button type="primary" @click="handleAdd">添加记录</el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-row :gutter="isMobile ? 0 : 20">
          <el-col :xs="24" :sm="12" :md="6" :lg="6">
            <el-select v-model="searchForm.type" placeholder="请选择类型" clearable style="width: 100%">
              <el-option label="实习" value="实习" />
              <el-option label="就业" value="就业" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6" :lg="6">
            <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 100%">
              <el-option label="在岗" value="在岗" />
              <el-option label="已结束" value="已结束" />
              <el-option label="待入职" value="待入职" />
              <el-option label="已离职" value="已离职" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6" :lg="6">
            <el-select v-model="searchForm.approvalStatus" placeholder="请选择审批状态" clearable style="width: 100%">
              <el-option label="待审批" value="待审批" />
              <el-option label="已通过" value="已通过" />
              <el-option label="已拒绝" value="已拒绝" />
            </el-select>
          </el-col>
          <el-col :xs="24" :sm="24" :md="6" :lg="6">
            <div :class="isMobile ? 'mobile-button-group' : ''">
              <el-button type="primary" @click="handleSearch" class="search-btn">搜索</el-button>
              <el-button @click="handleReset" class="reset-btn">重置</el-button>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 实习就业列表 -->
      <el-table
        :data="displayInternshipList"
        v-loading="loading"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="type" label="类型" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.type === '实习' ? 'warning' : 'success'">
              {{ scope.row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="company" label="单位名称" width="150" />
        <el-table-column prop="position" label="职位" width="120" />
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="workLocation" label="工作地点" width="120" />
        <el-table-column prop="salary" label="薪资" width="100" />
        <el-table-column prop="startDate" label="开始时间" width="120">
          <template #default="scope">
            {{ formatDate(scope.row.startDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="endDate" label="结束时间" width="120">
          <template #default="scope">
            {{ formatDate(scope.row.endDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="approvalStatus" label="审批状态" width="100">
          <template #default="scope">
            <el-tag :type="getApprovalStatusType(scope.row.approvalStatus)">
              {{ scope.row.approvalStatus }}
            </el-tag>
          </template>
        </el-table-column>
                 <el-table-column label="操作" width="150" fixed="right">
           <template #default="scope">
             <div class="action-buttons">
               <!-- 查看按钮 -->
               <el-tooltip content="查看详情" placement="top">
                 <el-button 
                   size="small" 
                   type="info" 
                   :icon="View"
                   circle
                   @click="handleView(scope.row)"
                 />
               </el-tooltip>
               
               <!-- 编辑按钮 -->
               <el-tooltip content="编辑记录" placement="top">
                 <el-button 
                   size="small" 
                   type="primary" 
                   :icon="Edit"
                   circle
                   @click="handleEdit(scope.row)"
                   :disabled="scope.row.approvalStatus === '已通过'"
                 />
               </el-tooltip>
               
               <!-- 删除按钮 -->
               <el-tooltip content="删除记录" placement="top">
                 <el-button 
                   size="small" 
                   type="danger" 
                   :icon="Delete"
                   circle
                   @click="handleDelete(scope.row)"
                   :disabled="scope.row.approvalStatus === '已通过'"
                 />
               </el-tooltip>
             </div>
           </template>
         </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          :current-page="pagination.currentPage"
          :page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :width="isMobile ? '95%' : '900px'"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        :label-width="isMobile ? '80px' : '120px'"
      >
        <el-row :gutter="isMobile ? 0 : 20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
                <el-option label="实习" value="实习" />
                <el-option label="就业" value="就业" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="单位名称" prop="company">
              <el-input v-model="form.company" placeholder="请输入单位名称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="isMobile ? 0 : 20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="单位类型" prop="companyType">
              <el-select v-model="form.companyType" placeholder="请选择单位类型" style="width: 100%">
                <el-option label="国企" value="国企" />
                <el-option label="外企" value="外企" />
                <el-option label="民企" value="民企" />
                <el-option label="事业单位" value="事业单位" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="行业" prop="industry">
              <el-select v-model="form.industry" placeholder="请选择行业" style="width: 100%">
                <el-option label="IT" value="IT" />
                <el-option label="金融" value="金融" />
                <el-option label="教育" value="教育" />
                <el-option label="制造业" value="制造业" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="isMobile ? 0 : 20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="职位" prop="position">
              <el-input v-model="form.position" placeholder="请输入职位" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="部门" prop="department">
              <el-input v-model="form.department" placeholder="请输入部门" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="isMobile ? 0 : 20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="工作地点" prop="workLocation">
              <el-input v-model="form.workLocation" placeholder="请输入工作地点" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="工作类型" prop="workType">
              <el-select v-model="form.workType" placeholder="请选择工作类型" style="width: 100%">
                <el-option label="全职" value="全职" />
                <el-option label="兼职" value="兼职" />
                <el-option label="实习" value="实习" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="isMobile ? 0 : 20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="薪资" prop="salary">
              <el-input v-model="form.salary" placeholder="请输入薪资" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="薪资类型" prop="salaryType">
              <el-select v-model="form.salaryType" placeholder="请选择薪资类型" style="width: 100%">
                <el-option label="月薪" value="月薪" />
                <el-option label="年薪" value="年薪" />
                <el-option label="时薪" value="时薪" />
                <el-option label="项目制" value="项目制" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="isMobile ? 0 : 20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="薪资范围" prop="salaryRange">
              <el-select v-model="form.salaryRange" placeholder="请选择薪资范围" style="width: 100%">
                <el-option label="3k以下" value="3k以下" />
                <el-option label="3k-5k" value="3k-5k" />
                <el-option label="5k-8k" value="5k-8k" />
                <el-option label="8k-12k" value="8k-12k" />
                <el-option label="12k-20k" value="12k-20k" />
                <el-option label="20k以上" value="20k以上" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="技术栈" prop="technologyStack">
              <el-select v-model="form.technologyStack" multiple placeholder="请选择技术栈" style="width: 100%">
                <el-option label="Java" value="Java" />
                <el-option label="Python" value="Python" />
                <el-option label="JavaScript" value="JavaScript" />
                <el-option label="C++" value="C++" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="isMobile ? 0 : 20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="开始时间" prop="startDate">
              <el-date-picker
                v-model="form.startDate"
                type="date"
                placeholder="请选择开始时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="结束时间" prop="endDate">
              <el-date-picker
                v-model="form.endDate"
                type="date"
                placeholder="请选择结束时间"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="isMobile ? 0 : 20">
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
                <el-option label="在岗" value="在岗" />
                <el-option label="已结束" value="已结束" />
                <el-option label="待入职" value="待入职" />
                <el-option label="已离职" value="已离职" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="12" :lg="12">
            <el-form-item label="合同类型" prop="contractType">
              <el-select v-model="form.contractType" placeholder="请选择合同类型" style="width: 100%">
                <el-option label="正式" value="正式" />
                <el-option label="实习" value="实习" />
                <el-option label="临时" value="临时" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="福利待遇" prop="benefits">
          <el-input
            v-model="form.benefits"
            type="textarea"
            :rows="3"
            placeholder="请输入福利待遇"
          />
        </el-form-item>

        <el-form-item label="工作描述" prop="jobDescription">
          <el-input
            v-model="form.jobDescription"
            type="textarea"
            :rows="3"
            placeholder="请输入工作描述"
          />
        </el-form-item>

        <el-form-item label="所需技能" prop="skills">
          <el-input
            v-model="form.skills"
            type="textarea"
            :rows="3"
            placeholder="请输入所需技能"
          />
        </el-form-item>

        <el-row :gutter="isMobile ? 0 : 20">
          <el-col :xs="24" :sm="24" :md="8" :lg="8">
            <el-form-item label="联系人" prop="contactPerson">
              <el-input v-model="form.contactPerson" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="8" :lg="8">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="24" :md="8" :lg="8">
            <el-form-item label="联系邮箱" prop="contactEmail">
              <el-input v-model="form.contactEmail" placeholder="请输入联系邮箱" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog
      v-model="viewDialogVisible"
      title="实习就业详情"
      width="800px"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="类型">{{ viewData.type }}</el-descriptions-item>
        <el-descriptions-item label="单位名称">{{ viewData.company }}</el-descriptions-item>
        <el-descriptions-item label="单位类型">{{ viewData.companyType }}</el-descriptions-item>
        <el-descriptions-item label="行业">{{ viewData.industry }}</el-descriptions-item>
        <el-descriptions-item label="职位">{{ viewData.position }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ viewData.department }}</el-descriptions-item>
        <el-descriptions-item label="工作地点">{{ viewData.workLocation }}</el-descriptions-item>
        <el-descriptions-item label="工作类型">{{ viewData.workType }}</el-descriptions-item>
        <el-descriptions-item label="薪资">{{ viewData.salary }}</el-descriptions-item>
        <el-descriptions-item label="薪资类型">{{ viewData.salaryType }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatDate(viewData.startDate) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ formatDate(viewData.endDate) }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ viewData.status }}</el-descriptions-item>
        <el-descriptions-item label="合同类型">{{ viewData.contractType }}</el-descriptions-item>
        <el-descriptions-item label="审批状态">{{ viewData.approvalStatus }}</el-descriptions-item>
        <el-descriptions-item label="审批备注">{{ viewData.approvalRemark }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ viewData.contactPerson }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ viewData.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="联系邮箱">{{ viewData.contactEmail }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(viewData.createTime) }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">详细信息</el-divider>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <h4>福利待遇</h4>
          <p>{{ viewData.benefits || '暂无' }}</p>
        </el-col>
        <el-col :span="12">
          <h4>所需技能</h4>
          <p>{{ viewData.skills || '暂无' }}</p>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="24">
          <h4>工作描述</h4>
          <p>{{ viewData.jobDescription || '暂无' }}</p>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="24">
          <h4>备注</h4>
          <p>{{ viewData.remark || '暂无' }}</p>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script setup>
import request from '@/api/request'
import { ref, reactive, onMounted, computed, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useStore } from 'vuex'
import { useRoute, useRouter } from 'vue-router'
import { 
  getMyInternshipRecords, 
  addInternship, 
  updateInternship, 
  deleteInternship,
  getInternshipById
} from '@/api/internship'
import { View, Edit, Delete } from '@element-plus/icons-vue'

const store = useStore()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const secondaryTab = ref('intent')
const tabPathMap = {
  intent: '/internship/intent',
  records: '/internship/records'
}
const isSeparatedSubPage = computed(() => Boolean(route.path.split('/')[2]))
const internshipList = ref([])
const selectedInternships = ref([])
const dialogVisible = ref(false)
const viewDialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const viewData = ref({})
const selectedCategory = ref('all')

const intentForm = reactive({ intentionType: '就业', targetCity: '', targetIndustry: '', expectedSalary: '', targetPosition: '' })

// 检测是否为移动端
const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1920)
const isMobile = computed(() => windowWidth.value <= 768)

// 搜索表单
const searchForm = reactive({
  type: '',
  status: '',
  approvalStatus: ''
})

// 分页
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

const classificationCards = computed(() => {
  const list = internshipList.value
  return [
    { key: 'all', label: '全部记录', count: list.length, hint: '完整过程台账' },
    { key: 'intent', label: '意向方向', count: intentForm.intentionType ? 1 : 0, hint: intentForm.targetCity || '待完善城市' },
    { key: 'internship', label: '实习记录', count: list.filter(item => item.type === '实习').length, hint: '过程跟踪' },
    { key: 'employment', label: '就业记录', count: list.filter(item => item.type === '就业').length, hint: '结果归档' },
    { key: 'pending', label: '待审批', count: list.filter(item => ['待审批', 'pending'].includes(item.approvalStatus)).length, hint: '需要关注' }
  ]
})

const displayInternshipList = computed(() => {
  let list = internshipList.value
  if (selectedCategory.value === 'internship') {
    list = list.filter(item => item.type === '实习')
  } else if (selectedCategory.value === 'employment') {
    list = list.filter(item => item.type === '就业')
  } else if (selectedCategory.value === 'pending') {
    list = list.filter(item => ['待审批', 'pending'].includes(item.approvalStatus))
  }

  if (searchForm.type) list = list.filter(item => item.type === searchForm.type)
  if (searchForm.status) list = list.filter(item => item.status === searchForm.status)
  if (searchForm.approvalStatus) list = list.filter(item => item.approvalStatus === searchForm.approvalStatus)
  return list
})

const saveIntent = async () => {
  try { Object.assign(intentForm, await request.post('/employment-intentions/me', intentForm)); ElMessage.success('就业意向已保存') } catch { /* shared interceptor */ }
}

const handleSecondaryTabChange = (tabName) => {
  if (route.path !== tabPathMap[tabName]) {
    router.replace({
      path: tabPathMap[tabName],
      query: route.query
    })
  }
}

watch(() => [route.path, route.query.tab], () => {
  const pathTab = route.path.split('/')[2]
  const tab = ['intent', 'records'].includes(pathTab) ? pathTab : route.query.tab
  if (['intent', 'records'].includes(tab) && secondaryTab.value !== tab) {
    secondaryTab.value = tab
  }
}, { immediate: true })

// 表单
const form = reactive({
  id: '',
  userId: '',
  studentId: '',
  studentName: '',
  studentClass: '',
  major: '',
  grade: '',
  type: '',
  category: '',
  company: '',
  companyType: '',
  industry: '',
  companySize: '',
  companyLocation: '',
  position: '',
  department: '',
  workLocation: '',
  workType: '',
  salary: '',
  salaryType: '',
  salaryRange: '',
  startDate: '',
  endDate: '',
  duration: '',
  status: '',
  contractType: '',
  technologyStack: [],
  programmingLanguages: '',
  frameworks: [],
  databases: [],
  cloudPlatforms: [],
  developmentTools: [],
  projectType: '',
  benefits: '',
  jobDescription: '',
  responsibilities: '',
  achievements: '',
  learningOutcomes: '',
  skills: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  supervisorName: '',
  supervisorContact: '',
  studentEvaluation: '',
  companyEvaluation: '',
  supervisorEvaluation: '',
  satisfactionScore: 0,
  remark: '',
  attachments: '',
  tags: ''
})

// 表单验证规则
const rules = {
  type: [
    { required: true, message: '请选择类型', trigger: 'change' }
  ],
  company: [
    { required: true, message: '请输入单位名称', trigger: 'blur' }
  ],
  position: [
    { required: true, message: '请输入职位', trigger: 'blur' }
  ],
  startDate: [
    { required: true, message: '请选择开始时间', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

// 获取当前用户信息
const currentUser = computed(() => store.state.user)

// 加载实习就业列表
const loadInternshipList = async () => {
  loading.value = true
  try {
    const res = await getMyInternshipRecords()
    if (res.code === 200) {
      internshipList.value = res.data || []
      pagination.total = internshipList.value.length
    }
  } catch (error) {
    ElMessage.error('加载实习就业列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.currentPage = 1
  loadInternshipList()
}

// 重置搜索
const handleReset = () => {
  Object.assign(searchForm, {
    type: '',
    status: '',
    approvalStatus: ''
  })
  handleSearch()
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedInternships.value = selection
}

// 添加记录
const handleAdd = () => {
  dialogTitle.value = '添加实习就业记录'
  Object.assign(form, {
    id: '',
    userId: currentUser.value.id,
    studentId: currentUser.value.studentId,
    studentName: currentUser.value.name,
    studentClass: currentUser.value.studentClass || '',
    major: currentUser.value.major || '',
    grade: currentUser.value.grade || '',
    type: '',
    category: '',
    company: '',
    companyType: '',
    industry: '',
    companySize: '',
    companyLocation: '',
    position: '',
    department: '',
    workLocation: '',
    workType: '',
    salary: '',
    salaryType: '',
    salaryRange: '',
    startDate: '',
    endDate: '',
    duration: '',
    status: '待入职',
    contractType: '',
    technologyStack: [],
    programmingLanguages: '',
    frameworks: [],
    databases: [],
    cloudPlatforms: [],
    developmentTools: [],
    projectType: '',
    benefits: '',
    jobDescription: '',
    responsibilities: '',
    achievements: '',
    learningOutcomes: '',
    skills: '',
    contactPerson: '',
    contactPhone: '',
    contactEmail: '',
    supervisorName: '',
    supervisorContact: '',
    studentEvaluation: '',
    companyEvaluation: '',
    supervisorEvaluation: '',
    satisfactionScore: 0,
    remark: '',
    attachments: '',
    tags: ''
  })
  dialogVisible.value = true
}

// 编辑记录
const handleEdit = (row) => {
  dialogTitle.value = '编辑实习就业记录'
  Object.assign(form, row)
  
  // 处理字符串字段，转换为数组
  if (typeof form.technologyStack === 'string' && form.technologyStack) {
    form.technologyStack = form.technologyStack.split(',')
  }
  if (typeof form.frameworks === 'string' && form.frameworks) {
    form.frameworks = form.frameworks.split(',')
  }
  if (typeof form.databases === 'string' && form.databases) {
    form.databases = form.databases.split(',')
  }
  if (typeof form.cloudPlatforms === 'string' && form.cloudPlatforms) {
    form.cloudPlatforms = form.cloudPlatforms.split(',')
  }
  if (typeof form.developmentTools === 'string' && form.developmentTools) {
    form.developmentTools = form.developmentTools.split(',')
  }
  
  dialogVisible.value = true
}

// 查看详情
const handleView = async (row) => {
  try {
    const res = await getInternshipById(row.id)
    if (res.code === 200) {
      viewData.value = res.data
      viewDialogVisible.value = true
    }
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

// 删除记录
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await deleteInternship(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadInternshipList()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    // 处理数组字段，转换为字符串
    const submitData = { ...form }
    if (Array.isArray(submitData.technologyStack)) {
      submitData.technologyStack = submitData.technologyStack.join(',')
    }
    if (Array.isArray(submitData.frameworks)) {
      submitData.frameworks = submitData.frameworks.join(',')
    }
    if (Array.isArray(submitData.databases)) {
      submitData.databases = submitData.databases.join(',')
    }
    if (Array.isArray(submitData.cloudPlatforms)) {
      submitData.cloudPlatforms = submitData.cloudPlatforms.join(',')
    }
    if (Array.isArray(submitData.developmentTools)) {
      submitData.developmentTools = submitData.developmentTools.join(',')
    }
    
    // 调试信息
    console.log('提交的数据:', submitData)
    
    // 确保日期格式正确
    if (submitData.startDate) {
      submitData.startDate = new Date(submitData.startDate).toISOString()
    }
    if (submitData.endDate) {
      submitData.endDate = new Date(submitData.endDate).toISOString()
    }
    
    // 确保必填字段不为空
    if (!submitData.type || !submitData.company || !submitData.position || !submitData.startDate) {
      ElMessage.error('请填写必填字段')
      return
    }
    
    let res
    if (submitData.id) {
      res = await updateInternship(submitData)
    } else {
      res = await addInternship(submitData)
    }
    
    if (res.code === 200) {
      ElMessage.success(submitData.id ? '更新成功' : '添加成功')
      dialogVisible.value = false
      loadInternshipList()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (error) {
    ElMessage.error('表单验证失败')
  }
}

// 对话框关闭
const handleDialogClose = () => {
  formRef.value?.resetFields()
}

// 分页大小变化
const handleSizeChange = (size) => {
  pagination.pageSize = size
  loadInternshipList()
}

// 当前页变化
const handleCurrentChange = (page) => {
  pagination.currentPage = page
  loadInternshipList()
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString()
}

// 获取状态类型
const getStatusType = (status) => {
  const statusMap = {
    '在岗': 'success',
    '已结束': 'info',
    '待入职': 'warning',
    '已离职': 'danger'
  }
  return statusMap[status] || 'info'
}

// 获取审批状态类型
const getApprovalStatusType = (approvalStatus) => {
  const statusMap = {
    '待审批': 'warning',
    '已通过': 'success',
    '已拒绝': 'danger',
    pending: 'warning',
    approved: 'success',
    rejected: 'danger'
  }
  return statusMap[approvalStatus] || 'info'
}

onMounted(() => {
  request.get('/employment-intentions/me').then(data => Object.assign(intentForm, Object.fromEntries(Object.entries(data).filter(([,v]) => v != null)))).catch(() => {})
  loadInternshipList()
  // 初始化窗口宽度并监听变化
  if (typeof window !== 'undefined') {
    windowWidth.value = window.innerWidth
    const handleResize = () => {
      windowWidth.value = window.innerWidth
    }
    window.addEventListener('resize', handleResize)
    // 清理函数
    onUnmounted(() => {
      window.removeEventListener('resize', handleResize)
    })
  }
})
</script>

<style scoped>
.internship-page {
  padding: 20px;
  background: linear-gradient(180deg, #f6faf8 0%, #f5f7fb 100%);
}

.module-tabs-card {
  margin-bottom: 20px;
  border: none;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(18, 32, 55, 0.06);
}

.secondary-tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.external-tab-mode :deep(.el-tabs__header) {
  display: none;
}

.intent-board {
  display: grid;
  grid-template-columns: minmax(260px, 0.55fr) minmax(0, 1fr);
  gap: 20px;
  margin-bottom: 18px;
  padding: 24px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: linear-gradient(90deg, #eefaf5 0%, #ffffff 55%);
}

.intent-kicker {
  color: #0f766e;
  font-size: 13px;
  font-weight: 600;
}

.intent-copy h2 {
  margin: 8px 0 12px;
  color: #303133;
  font-size: 18px;
}

.intent-steps {
  margin: 0;
  padding-left: 18px;
  color: #606266;
  font-size: 14px;
  line-height: 1.9;
}

.intent-form {
  padding: 18px;
  border: 1px solid rgba(228, 231, 237, 0.9);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.88);
}

.intent-actions {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
}

.classification-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.classification-card {
  padding: 18px;
  border: 1px solid rgba(15, 118, 110, 0.12);
  border-radius: 18px;
  background: white;
  cursor: pointer;
  transition: all 0.2s ease;
}

.classification-card.active,
.classification-card:hover {
  border-color: #0f766e;
  background: #f0faf7;
  box-shadow: 0 12px 28px rgba(15, 118, 110, 0.12);
}

.classification-card span,
.classification-card em {
  display: block;
  color: #667085;
  font-style: normal;
}

.classification-card strong {
  display: block;
  margin: 10px 0 6px;
  color: #17212f;
  font-size: 32px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-bar {
  margin-bottom: 20px;
}

.mobile-button-group {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  width: 100%;
}

.mobile-button-group .search-btn,
.mobile-button-group .reset-btn {
  width: 100%;
}

.mobile-button-group .search-btn {
  margin-bottom: 10px;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

.dialog-footer {
  text-align: right;
}

h4 {
  margin: 10px 0 5px 0;
  color: #333;
}

p {
  margin: 0;
  color: #666;
  line-height: 1.5;
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
  align-items: center;
}

.action-buttons .el-button {
  margin: 0;
  transition: all 0.3s ease;
}

.action-buttons .el-button:hover {
  transform: scale(1.1);
}

.action-buttons .el-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .internship-page {
    padding: 10px;
  }

  .intent-board,
  .classification-grid {
    grid-template-columns: 1fr;
  }

  .intent-actions {
    grid-template-columns: 1fr;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .search-bar {
    margin-bottom: 15px;
  }

  .search-bar .el-row {
    margin: 0;
  }

  .search-bar .el-col {
    margin-bottom: 10px;
  }

  :deep(.el-table) {
    font-size: 12px;
  }

  :deep(.el-dialog) {
    width: 95vw !important;
  }

  :deep(.el-form-item__label) {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .internship-page {
    padding: 8px;
  }

  :deep(.el-table) {
    font-size: 11px;
  }

  :deep(.el-dialog) {
    width: 98vw !important;
  }
}
</style> 
