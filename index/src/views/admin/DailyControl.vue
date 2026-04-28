/* eslint-disable vue/multi-word-component-names */
<template>
  <div class="page-wrap">
    <!-- Hero -->
    <section class="hero">
      <div class="hero-inner container">
        <div>
          <div class="hero-title">管理员端 · 审核中心</div>
          <div class="hero-sub">对学生提交的学术/日常/荣誉进行统一审批</div>
        </div>
        <div class="hero-stats">
          <el-card class="stat-card" shadow="never">
            <div class="stat-title">待审核</div>
            <div class="stat-value warn">{{ stat.pending }}</div>
          </el-card>
          <el-card class="stat-card" shadow="never">
            <div class="stat-title">已通过</div>
            <div class="stat-value ok">{{ stat.passed }}</div>
          </el-card>
          <el-card class="stat-card" shadow="never">
            <div class="stat-title">总记录</div>
            <div class="stat-value">{{ rows.length }}</div>
          </el-card>
        </div>
      </div>
    </section>

    <div class="container">
      <!-- 模块 + 状态筛选 + 搜索 -->
      <div class="toolbar">
        <el-tabs v-model="moduleKey" type="card" @tab-change="onTabChange">
          <el-tab-pane label="学术活动" name="academic" />
          <el-tab-pane label="请假管理" name="leave" />
          <el-tab-pane label="荣誉" name="honor" />
          <el-tab-pane label="任务发布与统计" name="task_mgmt" />
        </el-tabs>

        <div class="toolbar-right">
          <el-segmented v-model="statusFilter" :options="statusOptions" size="small" />
          <el-input
              v-model="searchKey"
              placeholder="按标题/发起人搜索"
              clearable
              style="width: 240px"
              @keyup.enter="applySearch"
              @clear="applySearch"
          />
        </div>
      </div>

      <!-- 列表 -->
      <el-card v-if="moduleKey !== 'task_mgmt'" class="glass" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="card-title">
              {{ moduleTitle }} 待办列表
              <el-tag class="ml8" type="info" effect="plain">共 {{ rows.length }} 条</el-tag>
              <el-tag class="ml8" type="warning" effect="plain">待审 {{ stat.pending }}</el-tag>
              <el-tag class="ml8" type="success" effect="plain">通过 {{ stat.passed }}</el-tag>
            </div>
            <el-space>
              <el-button @click="batchApprove" :disabled="!multipleSelection.length" type="success" plain>批量通过</el-button>
              <el-button @click="batchReject" :disabled="!multipleSelection.length" type="danger" plain>批量驳回</el-button>
            </el-space>
          </div>
        </template>

        <el-table
            :data="filteredRows"
            style="width: 100%"
            @selection-change="onSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <!-- 请假模块特殊显示 -->
          <template v-if="moduleKey === 'leave'">
            <el-table-column prop="reason" label="请假原因" min-width="240" show-overflow-tooltip />
            <el-table-column prop="studentName" label="学生姓名" min-width="120" />
            <el-table-column prop="studentId" label="学号" min-width="120" />
            <el-table-column label="请假时间" min-width="220">
              <template #default="scope">
                <span>
                  {{ fmtDate(scope.row.startDate) || '-' }}
                  <span v-if="scope.row.endDate">至 {{ fmtDate(scope.row.endDate) }}</span>
                  <span v-if="scope.row.days">（{{ scope.row.days }} 天）</span>
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="请假状态" width="120">
              <template #default="scope">
                <el-tag :type="getLeaveStatusType(scope.row.status, scope.row)" effect="light" class="status-tag">
                  {{ getLeaveStatusText(scope.row.status, scope.row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="auditStatus" label="审核状态" width="120">
              <template #default="scope">
                <el-tag :type="statusMeta(scope.row.auditStatus).type" effect="light" class="status-tag">
                  <el-icon class="mr4"><component :is="statusMeta(scope.row.auditStatus).icon" /></el-icon>
                  {{ statusMeta(scope.row.auditStatus).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" align="right" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="openReview(scope.row)">
                  查看 & 审核
                </el-button>
              </template>
            </el-table-column>
          </template>
          <!-- 其他模块显示 -->
          <template v-else>
            <el-table-column prop="title" :label="moduleTitle + '标题'" min-width="240" show-overflow-tooltip />
            <el-table-column label="时间" min-width="220">
              <template #default="scope">
                <span v-if="moduleKey !== 'honor'">
                  {{ fmtDate(scope.row.startDate) || '-' }}
                  <span v-if="scope.row.endDate">至 {{ fmtDate(scope.row.endDate) }}</span>
                </span>
                <span v-else>{{ fmtDate(scope.row.awardDate) || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="studentName" label="学生姓名" min-width="120" />
            <el-table-column :prop="(moduleKey === 'academic' || moduleKey === 'daily') ? 'studentNumber' : 'studentId'" label="学号" min-width="120" />
            <el-table-column prop="auditStatus" label="状态" width="160">
              <template #default="scope">
                <el-tag :type="statusMeta(scope.row.auditStatus).type" effect="light" class="status-tag">
                  <el-icon class="mr4"><component :is="statusMeta(scope.row.auditStatus).icon" /></el-icon>
                  {{ statusMeta(scope.row.auditStatus).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" align="right" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="openReview(scope.row)">
                  查看 & 审核
                </el-button>
              </template>
            </el-table-column>
          </template>
        </el-table>
      </el-card>

      <!-- 任务管理列表 -->
      <el-card v-else class="glass" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="card-title">日常任务发布与统计</div>
            <el-button type="primary" :icon="Plus" @click="handleOpenTaskDialog">发布新任务</el-button>
          </div>
        </template>

        <!-- 筛选按钮组 -->
        <div class="task-filters">
          <!-- 横向：任务类型筛选 -->
          <div class="filter-row">
            <div class="filter-label">任务类型：</div>
            <el-button-group>
              <el-button 
                :type="taskCategoryFilter === 'all' ? 'primary' : ''" 
                size="small"
                @click="taskCategoryFilter = 'all'"
              >
                全部
              </el-button>
              <el-button 
                :type="taskCategoryFilter === 'normal' ? 'primary' : ''" 
                size="small"
                @click="taskCategoryFilter = 'normal'"
              >
                普通任务
              </el-button>
              <el-button 
                :type="taskCategoryFilter === 'registration' ? 'primary' : ''" 
                size="small"
                @click="taskCategoryFilter = 'registration'"
              >
                报名型任务
              </el-button>
            </el-button-group>
          </div>
          
          <!-- 纵向：限制类型筛选 -->
          <div class="filter-row">
            <div class="filter-label">限制类型：</div>
            <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap;">
              <el-button-group>
                <el-button 
                  :type="restrictionFilter === 'all' ? 'primary' : ''" 
                  size="small"
                  @click="restrictionFilter = 'all'; selectedGradeFilter = null"
                >
                  全部
                </el-button>
                <el-button 
                  :type="restrictionFilter === 'grade' ? 'primary' : ''" 
                  size="small"
                  @click="restrictionFilter = 'grade'"
                >
                  年级限制
                </el-button>
                <el-button 
                  :type="restrictionFilter === 'political' ? 'primary' : ''" 
                  size="small"
                  @click="restrictionFilter = 'political'; selectedGradeFilter = null"
                >
                  政治面貌限制
                </el-button>
                <el-button 
                  :type="restrictionFilter === 'party' ? 'primary' : ''" 
                  size="small"
                  @click="restrictionFilter = 'party'; selectedGradeFilter = null"
                >
                  入党阶段限制
                </el-button>
                <el-button 
                  :type="restrictionFilter === 'none' ? 'primary' : ''" 
                  size="small"
                  @click="restrictionFilter = 'none'; selectedGradeFilter = null"
                >
                  无限制
                </el-button>
              </el-button-group>
              <!-- 年级筛选下拉框（仅在选择"年级限制"时显示） -->
              <el-select
                v-if="restrictionFilter === 'grade'"
                v-model="selectedGradeFilter"
                placeholder="选择年级"
                size="small"
                clearable
                style="width: 150px"
                filterable
                @focus="loadGradeListIfNeeded"
              >
                <el-option label="所有年级限制" value="" />
                <el-option 
                  v-for="grade in gradeList" 
                  :key="grade" 
                  :label="grade" 
                  :value="grade" 
                />
              </el-select>
            </div>
          </div>
        </div>

        <el-table :data="filteredDailyTasks" style="width: 100%">
          <el-table-column prop="title" label="任务标题" min-width="200" />
          <el-table-column prop="description" label="描述" min-width="250" show-overflow-tooltip />
          <el-table-column label="任务类别" width="140">
            <template #default="scope">
              <el-tag :type="scope.row.taskCategory === 'registration' ? 'warning' : 'info'" effect="plain">
                {{ scope.row.taskCategory === 'registration' ? '报名型' : '普通任务' }}
              </el-tag>
              <div v-if="scope.row.taskCategory === 'registration'" style="margin-top: 4px; font-size: 12px; color: #909399;">
                已报名: {{ scope.row.currentParticipants || 0 }} / {{ scope.row.maxParticipants || 0 }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="范围限制" width="200" show-overflow-tooltip>
            <template #default="scope">
              <div style="font-size: 12px; line-height: 1.6;">
                <div v-if="scope.row.allowedGrades && scope.row.allowedGrades.length > 0" style="margin-bottom: 4px;">
                  <el-tag type="info" size="small" effect="plain" style="margin-right: 4px;">年级</el-tag>
                  <span>{{ scope.row.allowedGrades.join(', ') }}</span>
                </div>
                <div v-if="scope.row.allowedPoliticalStatuses && scope.row.allowedPoliticalStatuses.length > 0" style="margin-bottom: 4px;">
                  <el-tag type="success" size="small" effect="plain" style="margin-right: 4px;">政治面貌</el-tag>
                  <span>{{ scope.row.allowedPoliticalStatuses.join(', ') }}</span>
                </div>
                <div v-if="scope.row.allowedPartyStages && scope.row.allowedPartyStages.length > 0" style="margin-bottom: 4px;">
                  <el-tag type="warning" size="small" effect="plain" style="margin-right: 4px;">入党阶段</el-tag>
                  <span>{{ scope.row.allowedPartyStages.join(', ') }}</span>
                </div>
                <div v-if="(!scope.row.allowedGrades || scope.row.allowedGrades.length === 0) && 
                           (!scope.row.allowedPoliticalStatuses || scope.row.allowedPoliticalStatuses.length === 0) && 
                           (!scope.row.allowedPartyStages || scope.row.allowedPartyStages.length === 0)" 
                     style="color: #909399;">
                  无限制
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="截止时间" width="180">
            <template #default="scope">
              {{ fmtDate(scope.row.deadline) || '无' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.active ? 'success' : 'info'">
                {{ scope.row.active ? '进行中' : '已关闭' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" align="right">
            <template #default="scope">
              <el-button link type="primary" :icon="View" @click="showStats(scope.row)">完成情况</el-button>
              <el-button link type="success" :icon="Download" @click="handleExport(scope.row.id)">导出表格</el-button>
              <el-button link type="danger" :icon="Delete" @click="handleDeleteTask(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 任务统计弹窗 -->
    <el-dialog v-model="statsVisible" :title="currentTaskCategory === 'registration' ? '报名人员统计' : '任务完成情况统计'" width="1200px">
      <div style="margin-bottom: 16px; padding: 12px; background-color: #f0f9ff; border-radius: 4px;">
        <el-text type="info" v-if="currentTaskCategory === 'registration'">
          <el-icon style="margin-right: 4px;"><InfoFilled /></el-icon>
          报名型任务：仅显示已报名人员，共 {{ taskStats.length }} 人
        </el-text>
        <el-text type="info" v-else>
          <el-icon style="margin-right: 4px;"><InfoFilled /></el-icon>
          <span v-if="currentTaskRestrictions && (currentTaskRestrictions.grades || currentTaskRestrictions.political || currentTaskRestrictions.party)">
            普通任务：根据限制条件显示符合条件的学生
            <span v-if="currentTaskRestrictions.grades" style="margin-left: 8px;">
              <el-tag type="info" size="small" effect="plain">年级: {{ currentTaskRestrictions.grades.join(', ') }}</el-tag>
            </span>
            <span v-if="currentTaskRestrictions.political" style="margin-left: 8px;">
              <el-tag type="success" size="small" effect="plain">政治面貌: {{ currentTaskRestrictions.political.join(', ') }}</el-tag>
            </span>
            <span v-if="currentTaskRestrictions.party" style="margin-left: 8px;">
              <el-tag type="warning" size="small" effect="plain">入党阶段: {{ currentTaskRestrictions.party.join(', ') }}</el-tag>
            </span>
            ，共 {{ taskStats.length }} 人
          </span>
          <span v-else>
            普通任务：显示所有学生，共 {{ taskStats.length }} 人
          </span>
        </el-text>
      </div>
      <el-table :data="taskStats" style="width: 100%" height="500px" border>
        <el-table-column prop="studentId" label="学号" width="120" fixed="left" />
        <el-table-column prop="name" label="姓名" width="100" fixed="left" />
        <el-table-column prop="major" label="专业" width="150" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === '已完成' || scope.row.status === '已报名' ? 'success' : 'danger'">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submissionTime" label="提交时间" width="180">
          <template #default="scope">
            {{ scope.row.submissionTime ? new Date(scope.row.submissionTime).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <!-- 动态显示字段列 -->
        <el-table-column 
          v-for="field in currentTaskFields" 
          :key="field.fieldName"
          :prop="'field_' + field.fieldName"
          :label="field.fieldName"
          min-width="120"
          show-overflow-tooltip
        >
          <template #default="scope">
            {{ scope.row['field_' + field.fieldName] || '-' }}
          </template>
        </el-table-column>
        <!-- 如果没有字段定义，显示原始提交内容 -->
        <el-table-column v-if="currentTaskFields.length === 0" prop="content" label="提交内容" min-width="200" show-overflow-tooltip />
      </el-table>
      <template #footer>
        <el-button @click="statsVisible = false">关闭</el-button>
        <el-button type="success" @click="handleExport(currentTaskId)">导出 Excel</el-button>
      </template>
    </el-dialog>

    <!-- 发布任务弹窗 -->
    <el-dialog v-model="taskDialogVisible" title="发布新日常任务" width="800px" destroy-on-close>
      <el-form :model="taskForm" label-width="100px">
        <el-form-item label="任务标题" required>
          <el-input v-model="taskForm.title" placeholder="请输入任务标题" />
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input v-model="taskForm.description" type="textarea" rows="3" placeholder="请输入任务详细说明" />
        </el-form-item>
        <el-form-item label="截止日期" required>
          <el-date-picker v-model="taskForm.deadline" type="datetime" placeholder="选择截止时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="任务类型">
          <el-select v-model="taskForm.type" style="width: 100%">
            <el-option label="信息填写" value="信息填写" />
            <el-option label="文件上传" value="文件上传" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="任务类别" required>
          <el-radio-group v-model="taskForm.taskCategory">
            <el-radio label="normal">普通任务（所有人都需要完成）</el-radio>
            <el-radio label="registration">报名型任务（限制报名人数）</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item 
          v-if="taskForm.taskCategory === 'registration'" 
          label="报名人数限制" 
          required
        >
          <el-input-number 
            v-model="taskForm.maxParticipants" 
            :min="1" 
            :max="10000"
            placeholder="请输入最大报名人数"
            style="width: 100%"
          />
          <div style="margin-top: 8px; color: #909399; font-size: 12px;">
            报名人数达到上限后，学生将无法继续报名
          </div>
        </el-form-item>
        
        <!-- 范围限制 -->
        <el-divider>范围限制（可选）</el-divider>
        <el-form-item label="年级限制">
          <el-select 
            v-model="taskForm.allowedGrades" 
            multiple 
            placeholder="请选择年级（不选择表示不限制年级）"
            style="width: 100%"
            clearable
            filterable
            :loading="gradeListLoading"
            @focus="loadGradeListIfNeeded"
          >
            <el-option 
              v-for="grade in gradeList" 
              :key="grade" 
              :label="grade" 
              :value="grade" 
            />
          </el-select>
          <div style="margin-top: 8px; color: #909399; font-size: 12px;">
            <span v-if="gradeList.length === 0" style="color: #E6A23C;">提示：当前没有可用的年级，请先在学生管理中创建年级</span>
            <span v-else>已选择 {{ taskForm.allowedGrades.length }} 个年级，只有指定年级的学生可以看到此任务</span>
          </div>
        </el-form-item>
        
        <el-form-item label="政治面貌限制">
          <el-select 
            v-model="taskForm.allowedPoliticalStatuses" 
            multiple 
            placeholder="不选择表示不限制政治面貌"
            style="width: 100%"
            clearable
          >
            <el-option label="中共党员" value="中共党员" />
            <el-option label="中共预备党员" value="中共预备党员" />
            <el-option label="共青团员" value="共青团员" />
            <el-option label="群众" value="群众" />
            <el-option label="民主党派" value="民主党派" />
          </el-select>
          <div style="margin-top: 8px; color: #909399; font-size: 12px;">
            选择后，只有指定政治面貌的学生可以看到此任务
          </div>
        </el-form-item>
        
        <el-form-item label="入党阶段限制">
          <el-select 
            v-model="taskForm.allowedPartyStages" 
            multiple 
            placeholder="不选择表示不限制入党阶段"
            style="width: 100%"
            clearable
          >
            <el-option label="提交申请书" value="提交申请书" />
            <el-option label="入党积极分子" value="入党积极分子" />
            <el-option label="发展对象" value="发展对象" />
            <el-option label="预备党员" value="预备党员" />
            <el-option label="正式党员" value="正式党员" />
          </el-select>
          <div style="margin-top: 8px; color: #909399; font-size: 12px;">
            选择后，只有处于指定入党阶段的学生可以看到此任务（需在入党申请模块中填写）
          </div>
        </el-form-item>
        
        <!-- 字段定义区域 -->
        <el-divider>定义填写字段（类似Excel表头）</el-divider>
        <el-form-item label="字段列表">
          <div class="field-list">
            <div v-for="(field, index) in taskForm.fields" :key="index" class="field-item">
              <el-row :gutter="10" align="middle">
                <el-col :span="6">
                  <el-input v-model="field.fieldName" placeholder="字段名称" />
                </el-col>
                <el-col :span="5">
                  <el-select 
                    v-model="field.fieldType" 
                    placeholder="字段类型" 
                    style="width: 100%"
                    @change="() => handleFieldTypeChange(index)"
                  >
                    <el-option label="文本" value="text" />
                    <el-option label="数字" value="number" />
                    <el-option label="日期" value="date" />
                    <el-option label="下拉选择" value="select" />
                  </el-select>
                </el-col>
                <el-col :span="4">
                  <el-checkbox v-model="field.required">必填</el-checkbox>
                </el-col>
                <el-col :span="7" v-show="field.fieldType === 'select'">
                  <el-input 
                    v-model="field.optionsText" 
                    placeholder="选项(空格分隔)" 
                    @focus="() => {}"
                  />
                </el-col>
                <el-col :span="2">
                  <el-button type="danger" :icon="Delete" circle @click="removeField(index)" />
                </el-col>
              </el-row>
              <!-- 条件显示配置 -->
              <el-row :gutter="10" align="middle" style="margin-top: 10px;">
                <el-col :span="12">
                  <el-select 
                    v-model="field.conditionalField" 
                    placeholder="条件字段（依赖的字段）" 
                    clearable
                    style="width: 100%"
                  >
                    <el-option 
                      v-for="(prevField, prevIndex) in taskForm.fields.slice(0, index)" 
                      :key="prevIndex"
                      :label="prevField.fieldName || `字段${prevIndex + 1}`" 
                      :value="prevField.fieldName" 
                    />
                  </el-select>
                </el-col>
                <el-col :span="12" v-if="field.conditionalField">
                  <el-input 
                    v-model="field.conditionalValue" 
                    :placeholder="`当${getFieldName(field.conditionalField)}等于此值时才显示`"
                  />
                </el-col>
              </el-row>
            </div>
            <el-button type="primary" :icon="Plus" plain @click="addField">添加字段</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateTask">发布</el-button>
      </template>
    </el-dialog>

    <!-- 合并的 查看 + 审核 弹窗（放大版） -->
    <el-dialog
        v-model="reviewVisible"
        :title="moduleTitle + ' · 查看与审核'"
        width="1080px"
        top="6vh"
        destroy-on-close
        class="review-dialog"
    >
      <!-- 顶部信息条 -->
      <div class="review-header">
        <div class="left">
          <div class="main-title">{{ reviewEntity.title || '-' }}</div>
          <div class="sub">
            <el-tag size="small" effect="plain">{{ moduleTitle }}</el-tag>
            <span class="dot">·</span>
            <span class="muted">发起人：</span>
            <span>{{ reviewEntity.studentName || '-' }}</span>
            <span v-if="((moduleKey === 'academic' || moduleKey === 'daily') ? reviewEntity.studentNumber : reviewEntity.studentId)" class="dot">·</span>
            <span v-if="((moduleKey === 'academic' || moduleKey === 'daily') ? reviewEntity.studentNumber : reviewEntity.studentId)" class="muted">学号：</span>
            <span v-if="((moduleKey === 'academic' || moduleKey === 'daily') ? reviewEntity.studentNumber : reviewEntity.studentId)">
              {{ (moduleKey === 'academic' || moduleKey === 'daily') ? (reviewEntity.studentNumber || '-') : (reviewEntity.studentId || '-') }}
            </span>
            <span class="dot">·</span>
            <span class="muted">时间：</span>
            <span v-if="moduleKey !== 'honor' && moduleKey !== 'leave'">
              {{ fmtDate(reviewEntity.startDate) || '-' }}
              <span v-if="reviewEntity.endDate">至 {{ fmtDate(reviewEntity.endDate) }}</span>
            </span>
            <span v-else-if="moduleKey === 'leave'">
              {{ fmtDate(reviewEntity.startDate) || '-' }} 至 {{ fmtDate(reviewEntity.endDate) || '-' }}
              <span style="margin-left: 8px; color: #909399;">(共 {{ reviewEntity.days || 0 }} 天)</span>
            </span>
            <span v-else>{{ fmtDate(reviewEntity.awardDate) || '-' }}</span>
          </div>
        </div>
        <div class="right">
          <el-tag :type="statusMeta(reviewForm.auditStatus).type" effect="dark">
            <el-icon class="mr4"><component :is="statusMeta(reviewForm.auditStatus).icon" /></el-icon>
            {{ statusMeta(reviewForm.auditStatus).text }}
          </el-tag>
        </div>
      </div>

      <el-row :gutter="16">
        <!-- 左侧：详情 -->
        <el-col :span="16">
          <el-card shadow="never" class="section-card">
            <template #header><div class="section-title">详细信息</div></template>

            <el-descriptions :column="2" border>
              <el-descriptions-item 
                v-for="(item, index) in detailPairs" 
                :key="`detail-${index}-${item.label}`"
                :label="item.label"
              >
                <template v-if="Array.isArray(item.value)">
                  <el-space wrap>
                    <el-tag v-for="t in item.value" :key="t" effect="plain">{{ t }}</el-tag>
                  </el-space>
                </template>
                <template v-else>
                  {{ item.value || '-' }}
                </template>
              </el-descriptions-item>
            </el-descriptions>

            <el-divider>材料预览</el-divider>
            <div class="proof-area" v-if="proofUrl">
              <el-image
                  v-if="isImage(proofUrl)"
                  :src="proofUrl"
                  fit="contain"
                  :preview-src-list="[proofUrl]"
                  style="width:100%;height:340px;border-radius:8px"
              />
              <div v-else class="file-box">
                <el-icon><Link /></el-icon>
                <div class="file-meta">
                  <div class="name">{{ fileName(proofUrl) }}</div>
                  <el-link :href="proofUrl" target="_blank" type="primary">在新窗口打开</el-link>
                </div>
              </div>
            </div>
            <div v-else class="empty-proof">未提供材料链接</div>

            <div v-if="moduleKey === 'leave' && reviewForm.auditStatus === 'approved'" class="seal-card">
              <div class="seal-mark">准假</div>
              <div>
                <strong>自动盖章预览</strong>
                <p>点击“通过”后，该请假单会进入已审核盖章状态。</p>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 右侧：审批 -->
        <el-col :span="8">
          <el-card shadow="never" class="section-card review-pane">
            <template #header><div class="section-title">审批处理</div></template>

            <el-form :model="reviewForm" label-width="90px" class="review-form">
              <el-form-item v-if="moduleKey === 'leave'" label="请假状态">
                <el-select v-model="reviewForm.status" placeholder="请选择请假状态" style="width: 100%">
                  <el-option label="待审核" value="pending" />
                  <el-option label="已批准" value="approved" />
                  <el-option label="已拒绝" value="rejected" />
                  <el-option label="假期中" value="on_leave" />
                  <el-option label="待销假" value="pending_check_in" />
                  <el-option label="已销假" value="completed" />
                  <el-option label="逾期未销假" value="overdue" />
                </el-select>
              </el-form-item>
              <el-form-item label="审批状态">
                <el-segmented
                    v-model="reviewForm.auditStatus"
                    :options="[
                    { label: '待审核', value: 'pending' },
                    { label: '通过', value: 'approved' },
                    { label: '驳回', value: 'rejected' }
                  ]"
                />
              </el-form-item>
              <el-form-item label="审核意见">
                <el-input
                    v-model="reviewForm.auditComment"
                    type="textarea"
                    :rows="6"
                    maxlength="500"
                    show-word-limit
                    placeholder="请填写审核说明…"
                />
              </el-form-item>
              <el-form-item label="审核人ID">
                <el-input v-model="reviewForm.auditorId" placeholder="如：admin001" />
              </el-form-item>
              <el-form-item label="审核时间">
                <el-date-picker v-model="reviewForm.auditTime" type="datetime" style="width:100%" />
              </el-form-item>

              <div class="btns">
                <el-button @click="reviewVisible=false">取消</el-button>
                <el-button type="danger" plain @click="quickReject">驳回</el-button>
                <el-button type="success" plain @click="quickApprove">通过</el-button>
                <el-button type="primary" @click="submitReview">提交</el-button>
              </div>
            </el-form>
          </el-card>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, CircleCheck, CircleClose, Link, Plus, Download, View, Delete, InfoFilled } from '@element-plus/icons-vue'
import { 
  getAllDailyTasks, createDailyTask, deleteDailyTask, 
  getDailyTaskStats, exportDailyTaskExcel,
  listItems, auditItem
} from '@/api/daily'
import { getAllLeaveRequests, auditLeaveRequest, updateLeaveStatus } from '@/api/leave'
import { getAllGrades } from '@/api/student'

/** ========= 字典 ========= */
const dict = {
  academicType: ['会议', '讲座', '研讨会'],
  level: ['国家', '省级', '校级', '院级', '其他'],
  activityTypeLabel: {
    lecture: '讲座',
    conference: '会议',
    competition: '竞赛',
    honor: '荣誉',
    training: '培训',
  }
}

/** ========= 模块 ========= */
const moduleKey = ref('honor') // 默认演示“荣誉”，可改：'academic' | 'daily' | 'honor'
const moduleTitle = computed(() => {
    if (moduleKey.value === 'academic') return '学术活动'
    if (moduleKey.value === 'leave') return '请假管理'
    if (moduleKey.value === 'honor') return '荣誉'
    return '任务管理'
})

/** ========= 任务管理相关 ========= */
const dailyTasks = ref([])
const taskStats = ref([])
const currentTaskId = ref('')
const currentTaskFields = ref([]) // 当前任务的字段定义
const currentTaskCategory = ref('') // 当前任务类别
const currentTaskRestrictions = ref(null) // 当前任务的限制条件
const statsVisible = ref(false)
const taskDialogVisible = ref(false)
const gradeList = ref([]) // 年级列表
const gradeListLoading = ref(false) // 年级列表加载状态

// 筛选条件
const taskCategoryFilter = ref('all') // 任务类型筛选：all(全部) | normal(普通任务) | registration(报名型任务)
const restrictionFilter = ref('all') // 限制类型筛选：all(全部) | grade(年级限制) | political(政治面貌限制) | party(入党阶段限制) | none(无限制)
const selectedGradeFilter = ref(null) // 选中的年级筛选（当restrictionFilter为'grade'时使用）

// 过滤后的任务列表
const filteredDailyTasks = computed(() => {
  let filtered = dailyTasks.value
  
  // 按任务类型筛选
  if (taskCategoryFilter.value !== 'all') {
    filtered = filtered.filter(task => {
      if (taskCategoryFilter.value === 'normal') {
        return !task.taskCategory || task.taskCategory === 'normal'
      } else if (taskCategoryFilter.value === 'registration') {
        return task.taskCategory === 'registration'
      }
      return true
    })
  }
  
  // 按限制类型筛选
  if (restrictionFilter.value !== 'all') {
    filtered = filtered.filter(task => {
      if (restrictionFilter.value === 'grade') {
        // 如果有年级限制
        if (!task.allowedGrades || task.allowedGrades.length === 0) {
          return false
        }
        // 如果选择了具体年级，则只显示包含该年级的任务
        if (selectedGradeFilter.value) {
          return task.allowedGrades.includes(selectedGradeFilter.value)
        }
        // 如果没有选择具体年级，显示所有有年级限制的任务
        return true
      } else if (restrictionFilter.value === 'political') {
        return task.allowedPoliticalStatuses && task.allowedPoliticalStatuses.length > 0
      } else if (restrictionFilter.value === 'party') {
        return task.allowedPartyStages && task.allowedPartyStages.length > 0
      } else if (restrictionFilter.value === 'none') {
        return (!task.allowedGrades || task.allowedGrades.length === 0) &&
               (!task.allowedPoliticalStatuses || task.allowedPoliticalStatuses.length === 0) &&
               (!task.allowedPartyStages || task.allowedPartyStages.length === 0)
      }
      return true
    })
  }
  
  return filtered
})

// 获取字段名称（用于条件字段选择）
const getFieldName = (fieldName) => {
  if (!fieldName) return ''
  const field = taskForm.fields.find(f => f.fieldName === fieldName)
  return field ? field.fieldName : fieldName
}
const taskForm = reactive({
  title: '',
  description: '',
  deadline: '',
  type: '信息填写',
  taskCategory: 'normal', // 任务类别：normal(普通任务) 或 registration(报名型任务)
  maxParticipants: null, // 报名人数限制（仅报名型任务有效）
  allowedGrades: [], // 允许的年级列表
  allowedPoliticalStatuses: [], // 允许的政治面貌列表
  allowedPartyStages: [], // 允许的入党阶段列表
  fields: [] // 字段定义列表
})

// 添加字段
const addField = () => {
  taskForm.fields.push({
    fieldName: '',
    fieldType: 'text',
    required: false,
    placeholder: '',
    optionsText: '',
    options: [],
    conditionalField: '', // 条件字段：依赖的字段名
    conditionalValue: ''  // 条件值：当依赖字段等于此值时才显示
  })
}

// 删除字段
const removeField = (index) => {
  taskForm.fields.splice(index, 1)
}

// 处理字段类型变化，避免ResizeObserver错误
const handleFieldTypeChange = async (index) => {
  try {
    // 使用nextTick确保DOM更新完成
    await nextTick()
    // 确保选项文本字段存在
    if (taskForm.fields[index] && taskForm.fields[index].fieldType === 'select') {
      if (!taskForm.fields[index].optionsText) {
        taskForm.fields[index].optionsText = ''
      }
    }
    // 再次等待，确保布局稳定
    await nextTick()
  } catch (e) {
    // 忽略错误
  }
}

const loadTasks = async () => {
  try {
    const res = await getAllDailyTasks()
    if (res.code === 200) {
      dailyTasks.value = res.data
    }
  } catch (e) {
    console.error('加载任务失败', e)
  }
}

const handleCreateTask = async () => {
  if (!taskForm.title || !taskForm.deadline) {
    ElMessage.warning('请填写任务标题和截止日期')
    return
  }
  
  // 处理字段数据：将 optionsText 转换为 options 数组
  const fieldsToSubmit = taskForm.fields.map(field => {
    const fieldData = {
      fieldName: field.fieldName,
      fieldType: field.fieldType,
      required: field.required,
      placeholder: field.placeholder || ''
    }
    // 如果是下拉选择类型，处理选项（使用空格分隔）
    if (field.fieldType === 'select' && field.optionsText) {
      fieldData.options = field.optionsText.split(/\s+/).map(opt => opt.trim()).filter(opt => opt)
    }
    // 添加条件字段配置
    if (field.conditionalField && field.conditionalValue) {
      fieldData.conditionalField = field.conditionalField
      fieldData.conditionalValue = field.conditionalValue
    }
    return fieldData
  }).filter(field => field.fieldName) // 过滤掉空字段名

  const taskData = {
    ...taskForm,
    fields: fieldsToSubmit
  }
  
  try {
    const res = await createDailyTask(taskData)
    if (res.code === 200) {
      ElMessage.success('任务发布成功')
      taskDialogVisible.value = false
      loadTasks()
      // 重置表单
      Object.assign(taskForm, { 
        title: '', 
        description: '', 
        deadline: '', 
        type: '信息填写',
        taskCategory: 'normal',
        maxParticipants: null,
        allowedGrades: [],
        allowedPoliticalStatuses: [],
        allowedPartyStages: [],
        fields: []
      })
    }
  } catch (e) {
    ElMessage.error('发布失败')
  }
}

const handleDeleteTask = (row) => {
  ElMessageBox.confirm('确定要删除该任务吗？', '警告', { type: 'warning' }).then(async () => {
    const res = await deleteDailyTask(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadTasks()
    }
  })
}

const showStats = async (row) => {
  currentTaskId.value = row.id
  currentTaskCategory.value = row.taskCategory || 'normal'
  // 获取任务详情以获取字段定义和限制条件
  try {
    const taskRes = await getAllDailyTasks()
    if (taskRes.code === 200) {
      const task = taskRes.data.find(t => t.id === row.id)
      currentTaskFields.value = task?.fields || []
      // 保存限制条件用于显示
      currentTaskRestrictions.value = {
        grades: task?.allowedGrades || [],
        political: task?.allowedPoliticalStatuses || [],
        party: task?.allowedPartyStages || []
      }
    }
  } catch (e) {
    console.error('获取任务字段失败', e)
  }
  
  try {
    const res = await getDailyTaskStats(row.id)
    if (res.code === 200) {
      taskStats.value = res.data
      statsVisible.value = true
    }
  } catch (e) {
    ElMessage.error('获取统计信息失败')
  }
}

const handleExport = async (taskId) => {
  try {
    const res = await exportDailyTaskExcel(taskId)
    const url = window.URL.createObjectURL(new Blob([res]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `任务完成情况_${taskId}.xlsx`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

/** ========= 列表数据 ========= */
const allRows = reactive({ academic: [], daily: [], honor: [], leave: [] })

const loadBusinessData = async () => {
  if (moduleKey.value === 'task_mgmt') {
    loadTasks()
    return
  }
  try {
    if (moduleKey.value === 'leave') {
      // 请假模块，使用专门的API
      const res = await getAllLeaveRequests()
      if (res.code === 200) {
        allRows.leave = res.data || []
      }
    } else {
      const res = await listItems(moduleKey.value)
      if (res.code === 200) {
        allRows[moduleKey.value] = res.data
      }
    }
  } catch (e) {
    console.error('加载业务数据失败', e)
  }
}

const rows = computed(() => {
  const list = allRows[moduleKey.value] || []
  if (!searchKey.value) return list
  const key = searchKey.value.trim().toLowerCase()
  if (moduleKey.value === 'leave') {
    // 请假模块：支持搜索请假原因、学生姓名、学号
    return list.filter(r =>
      (r.reason && r.reason.toLowerCase().includes(key)) ||
      (r.studentName && r.studentName.toLowerCase().includes(key)) ||
      (r.studentId && r.studentId.toLowerCase().includes(key))
    )
  } else {
    return list.filter(r =>
      (r.title && r.title.toLowerCase().includes(key)) ||
      (r.studentName && r.studentName.toLowerCase().includes(key)) ||
      (r.studentId && r.studentId.toLowerCase().includes(key)) ||
      (r.studentNumber && r.studentNumber.toLowerCase().includes(key)) ||
      (r.userId && r.userId.toLowerCase().includes(key))
    )
  }
})
const stat = computed(() => ({
  pending: rows.value.filter(x => x.auditStatus === 'pending').length,
  passed: rows.value.filter(x => x.auditStatus === 'approved').length
}))

/** ========= 状态筛选 ========= */
const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '待审', value: 'pending' },
  { label: '通过', value: 'approved' },
  { label: '驳回', value: 'rejected' }
]
const statusFilter = ref('all')
const filteredRows = computed(() => {
  const key = statusFilter.value
  const base = rows.value
  if (key === 'all') return base
  return base.filter(r => r.auditStatus === key)
})

/** ========= 搜索 ========= */
const searchKey = ref('')
const applySearch = () => {}

/** ========= 多选 ========= */
const multipleSelection = ref([])
const onSelectionChange = (val) => (multipleSelection.value = val)

/** ========= 状态渲染 ========= */
const statusMeta = (v) => {
  const map = {
    pending: { text: '待审核', type: 'warning', icon: Clock },
    approved: { text: '已通过', type: 'success', icon: CircleCheck },
    rejected: { text: '已驳回', type: 'danger', icon: CircleClose }
  }
  return map[v] || { text: v, type: 'info', icon: Clock }
}

/** ========= 合并的查看+审核 ========= */
const reviewVisible = ref(false)
const reviewEntity = reactive({})
const reviewForm = reactive({
  id: '',
  title: '',
  studentName: '',
  userId: '',
  status: '', // 请假状态（仅用于请假模块）
  auditStatus: 'pending',
  auditComment: '',
  auditorId: '',
  auditTime: new Date()
})

const openReview = (row) => {
  Object.assign(reviewEntity, row)
  Object.assign(reviewForm, {
    id: row.id || '',
    title: row.title || '',
    studentName: row.studentName || '',
    userId: row.userId || '',
    status: row.status || '', // 请假状态
    auditStatus: row.auditStatus || 'pending',
    auditComment: row.auditComment || '',
    auditorId: row.auditorId || '',
    auditTime: row.auditTime ? new Date(row.auditTime) : new Date()
  })
  reviewVisible.value = true
}

const quickApprove = () => {
  reviewForm.auditStatus = 'approved'
  if (moduleKey.value === 'leave') reviewForm.status = 'approved'
  submitReview()
}
const quickReject  = () => { reviewForm.auditStatus = 'rejected'; submitReview() }

const submitReview = async () => {
  try {
    if (moduleKey.value === 'leave') {
      // 请假审核和状态修改
      const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
      
      // 先更新审核状态
      const auditRes = await auditLeaveRequest(
        reviewForm.id,
        reviewForm.auditStatus,
        reviewForm.auditComment,
        userInfo.id || '',
        userInfo.name || '管理员'
      )
      
      if (auditRes.code !== 200) {
        ElMessage.error(auditRes.msg || '审核提交失败')
        return
      }
      
      // 如果修改了请假状态，调用更新状态API
      if (reviewForm.status && reviewForm.status !== reviewEntity.status) {
        const statusRes = await updateLeaveStatus(reviewForm.id, reviewForm.status)
        if (statusRes.code === 200) {
          ElMessage.success('审核和状态更新已提交')
        } else {
          ElMessage.warning('审核已提交，但状态更新失败：' + (statusRes.msg || '未知错误'))
        }
      } else {
        ElMessage.success('审核已提交')
      }
      
      reviewVisible.value = false
      loadBusinessData()
    } else {
      const res = await auditItem(moduleKey.value, reviewForm)
      if (res.code === 200) {
        ElMessage.success('审核已提交')
        reviewVisible.value = false
        loadBusinessData()
      }
    }
  } catch (e) {
    ElMessage.error('审核提交失败')
  }
}

/** ========= 详情展示 ========= */
const detailPairs = computed(() => {
  const e = reviewEntity
  if (moduleKey.value === 'academic') {
    return [
      { label: '学号', value: e.studentNumber || '-' },
      { label: '学生姓名', value: e.studentName },
      { label: '活动类型', value: e.type },
      { label: '主办方', value: e.organizer },
      { label: '主讲人', value: e.speaker },
      { label: '开始时间', value: fmtDate(e.startDate) },
      { label: '结束时间', value: fmtDate(e.endDate) }
    ]
  } else if (moduleKey.value === 'leave') {
    return [
      { label: '学号', value: reviewEntity.studentId },
      { label: '姓名', value: reviewEntity.studentName },
      { label: '年级', value: reviewEntity.grade },
      { label: '专业', value: reviewEntity.major },
      { label: '班级', value: reviewEntity.className },
      { label: '电话', value: reviewEntity.phone },
      { label: '请假原因', value: reviewEntity.reason },
      { label: '开始时间', value: fmtDate(reviewEntity.startDate) },
      { label: '结束时间', value: fmtDate(reviewEntity.endDate) },
      { label: '请假天数', value: reviewEntity.days + ' 天' },
      { label: '请假状态', value: getLeaveStatusText(reviewEntity.status, reviewEntity) },
      { label: '审核状态', value: statusMeta(reviewEntity.auditStatus).text },
      { label: '审核意见', value: reviewEntity.auditComment || '-' },
      { label: '审核人', value: reviewEntity.auditorName || '-' },
      { label: '审核时间', value: reviewEntity.auditTime ? fmtDateTime(reviewEntity.auditTime) : '-' },
      { label: '销假时间', value: reviewEntity.checkInTime ? fmtDateTime(reviewEntity.checkInTime) : '-' },
      { label: '销假备注', value: reviewEntity.checkInComment || '-' },
      { label: '销假定位地址', value: reviewEntity.checkInAddress || (reviewEntity.checkInLatitude && reviewEntity.checkInLongitude ? `纬度: ${reviewEntity.checkInLatitude.toFixed(6)}, 经度: ${reviewEntity.checkInLongitude.toFixed(6)}` : '-') },
      { label: '定位经纬度', value: reviewEntity.checkInLatitude && reviewEntity.checkInLongitude ? `纬度: ${reviewEntity.checkInLatitude.toFixed(6)}, 经度: ${reviewEntity.checkInLongitude.toFixed(6)}` : '-' }
    ]
  } else if (moduleKey.value === 'daily') {
    return [
      { label: '学号', value: e.studentNumber || '-' },
      { label: '学生姓名', value: e.studentName },
      { label: '活动类型', value: dict.activityTypeLabel[e.activityType] || e.activityType },
      { label: '主办/授予', value: e.organizer },
      { label: '级别', value: e.level },
      { label: '开始时间', value: fmtDate(e.startDate) },
      { label: '结束时间', value: fmtDate(e.endDate) }
    ]
  } else {
    return [
      { label: '学号', value: e.studentId || '-' },
      { label: '学生姓名', value: e.studentName || '-' },
      { label: '荣誉类别', value: e.category },
      { label: '级别', value: e.level },
      { label: '授予单位', value: e.awardOrg },
      { label: '获奖日期', value: fmtDate(e.awardDate) },
      { label: '荣誉描述', value: e.description },
      { label: '标签', value: Array.isArray(e.tags) ? e.tags : (e.tags ? String(e.tags).split(',').map(s=>s.trim()) : []) }
    ]
  }
})

// 导入图片URL工具函数
import { getImageUrl, isImage, getFileName } from '@/utils/imageUrl'

/** ========= 附件/材料预览 ========= */
const proofUrl = computed(() => {
  if (moduleKey.value === 'honor') return getImageUrl(reviewEntity.evidenceUrl)
  if (moduleKey.value === 'academic') return getImageUrl(reviewEntity.attachmentUrl)
  if (moduleKey.value === 'daily') return getImageUrl(reviewEntity.attachmentUrl)
  if (moduleKey.value === 'leave') {
    // 请假证据图片，返回第一张
    if (reviewEntity.evidenceUrls && reviewEntity.evidenceUrls.length > 0) {
      return getImageUrl(reviewEntity.evidenceUrls[0])
    }
    return ''
  }
  return ''
})


// 请假状态文本
const getLeaveStatusText = (status, leave) => {
  // 如果状态是on_leave，需要根据结束日期判断是否应该显示"待销假"
  if (status === 'on_leave' && leave && leave.endDate) {
    const endDate = new Date(leave.endDate)
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const tomorrow = new Date(today)
    tomorrow.setDate(tomorrow.getDate() + 1)
    
    endDate.setHours(0, 0, 0, 0)
    
    // 如果结束日期是今天或明天，显示"待销假"
    if (endDate <= tomorrow && endDate >= today) {
      return '待销假'
    }
    // 如果结束日期是昨天，显示"待销假"
    const yesterday = new Date(today)
    yesterday.setDate(yesterday.getDate() - 1)
    if (endDate.getTime() === yesterday.getTime()) {
      return '待销假'
    }
    // 如果结束日期是2天前或更早，显示"逾期未销假"
    if (endDate < yesterday) {
      return '逾期未销假'
    }
  }
  
  const map = {
    pending: '待审核',
    approved: '已批准',
    rejected: '已拒绝',
    on_leave: '假期中',
    pending_check_in: '待销假',
    completed: '已销假',
    overdue: '逾期未销假'
  }
  return map[status] || status
}

// 请假状态类型
const getLeaveStatusType = (status, leave) => {
  // 如果状态是on_leave，需要根据结束日期判断类型
  if (status === 'on_leave' && leave && leave.endDate) {
    const endDate = new Date(leave.endDate)
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const tomorrow = new Date(today)
    tomorrow.setDate(tomorrow.getDate() + 1)
    
    endDate.setHours(0, 0, 0, 0)
    
    // 如果结束日期是今天或明天，显示warning（待销假）
    if (endDate <= tomorrow && endDate >= today) {
      return 'warning'
    }
    // 如果结束日期是昨天，显示warning（待销假）
    const yesterday = new Date(today)
    yesterday.setDate(yesterday.getDate() - 1)
    if (endDate.getTime() === yesterday.getTime()) {
      return 'warning'
    }
    // 如果结束日期是2天前或更早，显示danger（逾期未销假）
    if (endDate < yesterday) {
      return 'danger'
    }
  }
  
  if (status === 'pending') return 'warning'
  if (status === 'approved') return 'success'
  if (status === 'rejected') return 'danger'
  if (status === 'on_leave') return 'info'
  if (status === 'pending_check_in') return 'warning'
  if (status === 'completed') return 'success'
  if (status === 'overdue') return 'danger'
  return 'info'
}

// 使用工具函数
const fileName = getFileName

/** ========= 批量操作 ========= */
const batchApprove = () => { multipleSelection.value.forEach(it => (it.auditStatus = 'approved')); ElMessage.success('已批量通过') }
const batchReject  = () => { multipleSelection.value.forEach(it => (it.auditStatus = 'rejected')); ElMessage.success('已批量驳回') }

/** ========= 公共 ========= */
const fmtDateTime = (d) => {
  if (!d) return ''
  const date = (d instanceof Date) ? d : new Date(d)
  if (Number.isNaN(+date)) return String(d)
  const y = date.getFullYear()
  const m = String(date.getMonth()+1).padStart(2,'0')
  const day = String(date.getDate()).padStart(2,'0')
  const h = String(date.getHours()).padStart(2,'0')
  const min = String(date.getMinutes()).padStart(2,'0')
  return `${y}-${m}-${day} ${h}:${min}`
}

const fmtDate = (d) => {
  if (!d) return ''
  const date = (d instanceof Date) ? d : new Date(d)
  if (Number.isNaN(+date)) return String(d)
  const y = date.getFullYear()
  const m = String(date.getMonth()+1).padStart(2,'0')
  const day = String(date.getDate()).padStart(2,'0')
  return `${y}-${m}-${day}`
}
const onTabChange = () => { 
  searchKey.value = ''
  statusFilter.value = 'all'
  loadBusinessData()
}

// 加载年级列表
const loadGradeList = async () => {
  if (gradeListLoading.value) return // 如果正在加载，不重复加载
  
  try {
    gradeListLoading.value = true
    const res = await getAllGrades()
    if (res.code === 200) {
      gradeList.value = res.data || []
    }
  } catch (e) {
    console.error('加载年级列表失败', e)
    ElMessage.warning('加载年级列表失败，请稍后重试')
  } finally {
    gradeListLoading.value = false
  }
}

// 如果需要，加载年级列表（用于下拉框聚焦时）
const loadGradeListIfNeeded = () => {
  if (gradeList.value.length === 0 && !gradeListLoading.value) {
    loadGradeList()
  }
}

// 打开任务发布对话框
const handleOpenTaskDialog = () => {
  // 确保年级列表已加载
  if (gradeList.value.length === 0) {
    loadGradeList()
  }
  taskDialogVisible.value = true
}

/** ========= 初始化 ========= */
const initPage = () => {
  loadBusinessData()
  loadGradeList()
}
onMounted(initPage)
</script>

<style scoped>
/* 页面结构 */
.container{max-width:1600px;margin:0 auto;}
.page-wrap{min-height:100vh;background:linear-gradient(180deg,#f7f9ff,#ffffff 40%);}
.hero{padding:28px 0;background: radial-gradient(1200px 220px at 50% -40px,#e8eeff,transparent);}
.hero-inner{display:flex;align-items:flex-end;justify-content:space-between;gap:20px;}
.hero-title{font-size:26px;font-weight:700;color:#1f2d3d;}
.hero-sub{color:#6b7280;margin-top:6px;}
.hero-stats{display:flex;gap:12px;}
.stat-card{width:150px;border:1px solid #eef2ff;background:rgba(255,255,255,.7);backdrop-filter: blur(4px);}
.stat-title{font-size:12px;color:#64748b;}
.stat-value{font-weight:700;font-size:20px;}
.stat-value.ok{color:#16a34a}.stat-value.warn{color:#d97706}
.toolbar{display:flex;align-items:center;justify-content:space-between;margin:14px 0;}
.toolbar-right{display:flex;align-items:center;gap:10px;}
.glass{border:1px solid #eef2ff;background:rgba(255,255,255,.8);backdrop-filter: blur(6px);}
.card-header{display:flex;align-items:center;justify-content:space-between;}
.card-title{display:flex;align-items:center;}
.ml8{margin-left:8px;}
.status-tag .el-icon{vertical-align:middle}
.mr4{margin-right:4px;}

/* 弹窗放大 + 自适应 */
.review-dialog :deep(.el-dialog){
  max-width:1280px;
}
@media (max-width:1280px){
  .review-dialog :deep(.el-dialog){ width:96vw !important; }
}
.review-dialog :deep(.el-dialog__header){
  background: linear-gradient(90deg,#eef2ff,#ffffff);
  border-bottom:1px solid #eef2ff;
}

/* 弹窗内部样式 */
.review-header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:12px}
.review-header .main-title{font-size:18px;font-weight:700;color:#1f2937}
.review-header .sub{margin-top:6px;color:#6b7280;display:flex;align-items:center;gap:6px;flex-wrap:wrap}
.review-header .dot{margin:0 6px;color:#cbd5e1}
.review-header .muted{color:#94a3b8}

.section-card{border:1px solid #eef2ff;background:rgba(255,255,255,.95)}
.section-title{font-weight:600}
.proof-area{margin-top:6px}
.file-box{display:flex;gap:10px;align-items:center;padding:12px;border:1px dashed #e5e7eb;border-radius:8px;background:#fafafa}
.file-meta .name{font-weight:600;margin-bottom:2px}
.empty-proof{color:#94a3b8;background:#f8fafc;border:1px dashed #e5e7eb;padding:12px;border-radius:8px;text-align:center}
.seal-card{display:flex;align-items:center;gap:16px;margin-top:16px;padding:14px;border:1px solid rgba(220,38,38,.24);border-radius:12px;background:#fff7f7}
.seal-mark{display:grid;width:72px;height:72px;place-items:center;border:3px solid #dc2626;border-radius:50%;color:#dc2626;font-size:20px;font-weight:800;transform:rotate(-12deg)}
.seal-card p{margin:4px 0 0;color:#667085}
.review-pane{position:sticky;top:0}
.review-form .btns{display:flex;justify-content:flex-end;gap:10px;margin-top:8px}

/* 移动端适配 */
@media (max-width: 768px) {
  .hero {
    padding: 20px 0;
  }

  .hero-inner {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .hero-title {
    font-size: 20px;
  }

  .hero-sub {
    font-size: 13px;
  }

  .hero-stats {
    flex-wrap: wrap;
    width: 100%;
  }

  .stat-card {
    width: calc(50% - 6px);
    min-width: 120px;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .toolbar-right {
    flex-wrap: wrap;
  }

  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  :deep(.el-tabs__item) {
    padding: 0 12px;
    font-size: 13px;
  }

  :deep(.el-table) {
    font-size: 12px;
  }

  :deep(.el-card__body) {
    padding: 15px;
  }

  .review-dialog :deep(.el-dialog) {
    width: 95vw !important;
    margin: 5vh auto !important;
  }

  .review-header {
    flex-direction: column;
    gap: 10px;
  }

  .review-form .btns {
    flex-direction: column;
  }

  .review-form .btns .el-button {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .hero-title {
    font-size: 18px;
  }

  .hero-sub {
    font-size: 12px;
  }

  .stat-card {
    width: 100%;
  }

  .stat-value {
    font-size: 18px;
  }

  :deep(.el-tabs__item) {
    padding: 0 8px;
    font-size: 12px;
  }

  :deep(.el-table) {
    font-size: 11px;
  }

  .review-dialog :deep(.el-dialog) {
    width: 98vw !important;
    margin: 2vh auto !important;
  }
}

/* 任务筛选样式 */
.task-filters {
  margin-bottom: 16px;
  padding: 12px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.filter-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.filter-row:last-child {
  margin-bottom: 0;
}

.filter-label {
  min-width: 80px;
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  margin-right: 12px;
}

@media (max-width: 768px) {
  .filter-row {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .filter-label {
    margin-bottom: 8px;
    margin-right: 0;
  }
  
  .task-filters .el-button-group {
    width: 100%;
    display: flex;
    flex-wrap: wrap;
  }
  
  .task-filters .el-button-group .el-button {
    flex: 1;
    min-width: 80px;
  }
}
</style>
