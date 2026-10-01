/* eslint-disable vue/multi-word-component-names */
<template>
  <div class="daily-page">
    <!-- 页面头部 -->
    <el-card class="page-header-card" shadow="never">
      <div class="page-header">
        <div class="header-stats">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon pending">
                <el-icon><Clock /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ stat.pending }}</div>
                <div class="stat-label">待审核</div>
              </div>
            </div>
          </el-card>
          <el-card class="stat-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon success">
                <el-icon><CircleCheck /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ stat.passed }}</div>
                <div class="stat-label">已通过</div>
              </div>
            </div>
          </el-card>
          <el-card class="stat-card" shadow="hover">
            <div class="stat-content">
              <div class="stat-icon info">
                <el-icon><Document /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-number">{{ rows.length }}</div>
                <div class="stat-label">总记录</div>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </el-card>

    <div class="page-content">
      <!-- 切换 + 操作区 -->
      <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <el-tabs
          v-model="moduleKey"
          type="card"
          class="module-tabs"
          :class="{ 'external-tab-mode': isSeparatedSubPage }"
          @tab-change="onTabChange"
        >
          <el-tab-pane label="学术活动" name="academic" />
          <el-tab-pane label="日常活动" name="daily" />
          <el-tab-pane label="请假管理" name="leave" />
          <el-tab-pane label="荣誉" name="honor" />
          <el-tab-pane label="志愿活动" name="volunteer" />
          <el-tab-pane label="日常任务" name="tasks" />
        </el-tabs>
          <div v-if="!['tasks', 'volunteer'].includes(moduleKey)" class="toolbar-right">
          <el-input
              v-model="searchKey"
              placeholder="按标题/发起人搜索"
              clearable
              style="width: 240px"
              @keyup.enter="applySearch"
              @clear="applySearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button v-if="['leave', 'honor'].includes(moduleKey)" type="primary" :icon="Plus" @click="openCreate">新增</el-button>
        </div>
      </div>
      </el-card>

      <!-- 列表卡片 -->
      <el-card v-if="!['tasks', 'volunteer'].includes(moduleKey)" class="content-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="card-title">
              <span class="title-text">{{ moduleTitle }} 列表</span>
              <div class="title-tags">
                <el-tag class="ml8" type="info" effect="plain" size="small">共 {{ rows.length }} 条</el-tag>
                <el-tag class="ml8" type="warning" effect="plain" size="small">待审 {{ stat.pending }}</el-tag>
                <el-tag class="ml8" type="success" effect="plain" size="small">通过 {{ stat.passed }}</el-tag>
              </div>
            </div>
          </div>
        </template>

        <el-table :data="rows" style="width: 100%" stripe>
          <!-- 请假特殊显示 -->
          <template v-if="moduleKey === 'leave'">
            <el-table-column prop="reason" label="请假原因" min-width="240" show-overflow-tooltip>
              <template #default="scope">
                <span style="font-weight: 500; color: #303133;">{{ scope.row.reason || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="请假时间" min-width="240">
              <template #default="scope">
                <div class="date-cell">
                  <div class="date-item">
                    <div class="date-icon-wrapper">
                      <el-icon><Calendar /></el-icon>
                    </div>
                    <div class="date-content">
                      <div class="date-label">开始</div>
                      <div class="date-value">{{ fmtDate(scope.row.startDate) || '-' }}</div>
                      <div v-if="scope.row.endDate" class="date-separator">至</div>
                      <div v-if="scope.row.endDate" class="date-label">结束</div>
                      <div v-if="scope.row.endDate" class="date-value">{{ fmtDate(scope.row.endDate) }}</div>
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="days" label="天数" width="80">
              <template #default="scope">
                <span>{{ scope.row.days || 0 }} 天</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="请假状态" width="140">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row)" effect="light" class="status-tag" size="default">
                  {{ getStatusText(scope.row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="auditStatus" label="审核状态" width="120">
              <template #default="scope">
                <el-tag :type="statusMeta(scope.row.auditStatus).type" effect="light" class="status-tag" size="default">
                  <el-icon class="mr4">
                    <component :is="statusMeta(scope.row.auditStatus).icon" />
                  </el-icon>
                  {{ statusMeta(scope.row.auditStatus).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" align="right" fixed="right">
              <template #default="scope">
                <el-button link type="primary" :icon="View" @click="openDetail(scope.row)">查看详情</el-button>
                <el-button 
                  v-if="scope.row.status === 'on_leave' || scope.row.status === 'pending_check_in' || scope.row.status === 'overdue'"
                  link 
                  type="success" 
                  @click="openCheckInDialog(scope.row)"
                >
                  销假
                </el-button>
              </template>
            </el-table-column>
          </template>
          <!-- 其他显示 -->
          <template v-else>
            <el-table-column prop="title" :label="moduleTitle + '标题'" :min-width="isMobile ? 150 : 240" show-overflow-tooltip>
              <template #default="scope">
                <span style="font-weight: 500; color: #303133;">{{ scope.row.title }}</span>
              </template>
            </el-table-column>
            <el-table-column label="时间" :min-width="isMobile ? 140 : 240">
              <template #default="scope">
                <div class="date-cell">
                  <div v-if="moduleKey !== 'honor'" class="date-item">
                    <div class="date-icon-wrapper">
                      <el-icon><Calendar /></el-icon>
                    </div>
                    <div class="date-content">
                      <div class="date-label">开始</div>
                      <div class="date-value">{{ fmtDate(scope.row.startDate) || '-' }}</div>
                      <div v-if="scope.row.endDate" class="date-separator">至</div>
                      <div v-if="scope.row.endDate" class="date-label">结束</div>
                      <div v-if="scope.row.endDate" class="date-value">{{ fmtDate(scope.row.endDate) }}</div>
                    </div>
                  </div>
                  <div v-else class="date-item">
                    <div class="date-icon-wrapper">
                      <el-icon><Calendar /></el-icon>
                    </div>
                    <div class="date-content">
                      <div class="date-label">获奖日期</div>
                      <div class="date-value">{{ fmtDate(scope.row.awardDate) || '-' }}</div>
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column
                prop="studentName"
                label="发起人"
                :min-width="isMobile ? 0 : 140"
                :class-name="isMobile ? 'hidden-mobile' : ''"
            >
              <template #default="scope">
                <div style="display: flex; align-items: center; gap: 6px;">
                  <el-icon><User /></el-icon>
                  <span>{{ scope.row.studentName || '-' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="auditStatus" label="状态" :width="isMobile ? 100 : 160">
              <template #default="scope">
                <el-tag :type="statusMeta(scope.row.auditStatus).type" effect="light" class="status-tag" :size="isMobile ? 'small' : 'default'">
                  <el-icon class="mr4">
                    <component :is="statusMeta(scope.row.auditStatus).icon" />
                  </el-icon>
                  {{ statusMeta(scope.row.auditStatus).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" :width="isMobile ? 80 : 140" align="right" fixed="right">
              <template #default="scope">
                <el-button link type="primary" :icon="View" :size="isMobile ? 'small' : 'default'" @click="openDetail(scope.row)">
                  {{ isMobile ? '详情' : '查看详情' }}
                </el-button>
              </template>
            </el-table-column>
          </template>
        </el-table>
      </el-card>

      <el-card v-if="moduleKey === 'volunteer'" class="content-card">
        <template #header>志愿活动记录（与党员发展与管理同步）</template>
        <el-table :data="volunteerHistory">
          <el-table-column prop="serviceName" label="活动名称" /><el-table-column prop="serviceDate" label="活动日期" />
          <el-table-column prop="serviceLocation" label="地点" /><el-table-column prop="auditStatus" label="审核状态" />
        </el-table>
      </el-card>
      <!-- 日常任务列表 (学生端) -->
      <el-card v-if="['academic', 'daily', 'volunteer', 'tasks'].includes(moduleKey)" class="content-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <div class="card-title">
              <span class="title-text">{{ moduleKey === 'tasks' ? '日常任务' : moduleTitle + '任务' }}</span>
              <el-tag type="info" effect="plain" size="small" class="ml8">共 {{ categoryTasks.length }} 项</el-tag>
    </div>
          </div>
        </template>

        <!-- 任务分类标签页 -->
        <el-tabs v-model="taskTab" @tab-change="handleTaskTabChange">
          <el-tab-pane label="普通任务" name="normal">
            <el-tag type="info" effect="plain" size="small" style="margin-bottom: 10px;">
              共 {{ normalTasks.length }} 项（按管理员设置的接收范围发布）
            </el-tag>
            <el-table :data="normalTasks" style="width: 100%" stripe class="task-table">
          <el-table-column prop="title" label="任务名称" :min-width="isMobile ? 150 : 200">
            <template #default="scope">
              <div style="display: flex; align-items: center; gap: 8px;">
                <el-icon v-if="!scope.row.completed" style="color: #E6A23C;"><Clock /></el-icon>
                <el-icon v-else style="color: #67C23A;"><CircleCheck /></el-icon>
                <span style="font-weight: 500; color: #303133;">{{ scope.row.title }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" :min-width="isMobile ? 0 : 250" :class-name="isMobile ? 'hidden-mobile' : ''" show-overflow-tooltip>
            <template #default="scope">
              <span style="color: #606266;">{{ scope.row.description || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="截止/完成时间" :width="isMobile ? 140 : 240">
            <template #default="scope">
              <div class="time-cell">
                <div v-if="scope.row.completed" class="time-item completed">
                  <div class="time-icon-wrapper success">
                    <el-icon><CircleCheck /></el-icon>
                  </div>
                  <div class="time-content-wrapper">
                    <div class="time-content">
                      <div class="time-label">完成时间</div>
                      <div class="time-value">{{ fmtDateTime(scope.row.submissionTime) }}</div>
                    </div>
                    <el-tag type="success" :size="isMobile ? 'small' : 'small'" effect="plain" class="status-badge">已完成</el-tag>
                  </div>
                </div>
                <div v-else class="time-item pending">
                  <div class="time-icon-wrapper warning">
                    <el-icon><Clock /></el-icon>
                  </div>
                  <div class="time-content-wrapper">
                    <div class="time-content">
                      <div class="time-label">截止时间</div>
                      <div class="time-value">{{ fmtDateTime(scope.row.deadline) }}</div>
                    </div>
                    <el-tag type="warning" :size="isMobile ? 'small' : 'small'" effect="plain" class="status-badge">待完成</el-tag>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" :width="isMobile ? 100 : 150" align="right">
            <template #default="scope">
              <el-button 
                :type="scope.row.completed ? 'success' : 'primary'" 
                :icon="EditPen" 
                :size="isMobile ? 'small' : 'default'"
                @click="openSubmit(scope.row)"
              >
                {{ isMobile ? (scope.row.completed ? '修改' : '完成') : (scope.row.completed ? '修改提交' : '去完成') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
          </el-tab-pane>
          
          <el-tab-pane label="报名型任务" name="registration">
            <el-tag type="warning" effect="plain" size="small" style="margin-bottom: 10px;">
              共 {{ registrationTasks.length }} 项（限制报名人数）
            </el-tag>
            <el-table :data="registrationTasks" style="width: 100%" stripe class="task-table">
              <el-table-column prop="title" label="任务名称" :min-width="isMobile ? 150 : 200">
                <template #default="scope">
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <el-icon v-if="!scope.row.completed" style="color: #E6A23C;"><Clock /></el-icon>
                    <el-icon v-else style="color: #67C23A;"><CircleCheck /></el-icon>
                    <span style="font-weight: 500; color: #303133;">{{ scope.row.title }}</span>
                    <el-tag v-if="scope.row.taskCategory === 'registration'" type="warning" size="small" effect="plain" style="margin-left: 8px;">
                      报名型
                    </el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="description" label="描述" :min-width="isMobile ? 0 : 250" :class-name="isMobile ? 'hidden-mobile' : ''" show-overflow-tooltip>
                <template #default="scope">
                  <span style="color: #606266;">{{ scope.row.description || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="报名情况" :width="isMobile ? 100 : 140">
                <template #default="scope">
                  <div style="font-size: 13px;">
                    <div style="color: #606266;">
                      已报名: <span style="font-weight: 500; color: #409EFF;">{{ scope.row.currentParticipants || 0 }}</span> / 
                      <span style="color: #909399;">{{ scope.row.maxParticipants || 0 }}</span>
                    </div>
                    <el-tag 
                      v-if="scope.row.maxParticipants && scope.row.currentParticipants >= scope.row.maxParticipants" 
                      type="danger" 
                      size="small" 
                      effect="plain"
                      style="margin-top: 4px;"
                    >
                      已满员
                    </el-tag>
                    <el-tag 
                      v-else-if="scope.row.completed" 
                      type="success" 
                      size="small" 
                      effect="plain"
                      style="margin-top: 4px;"
                    >
                      已报名
                    </el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="截止/完成时间" :width="isMobile ? 140 : 240">
                <template #default="scope">
                  <div class="time-cell">
                    <div v-if="scope.row.completed" class="time-item completed">
                      <div class="time-icon-wrapper success">
                        <el-icon><CircleCheck /></el-icon>
                      </div>
                      <div class="time-content-wrapper">
                        <div class="time-content">
                          <div class="time-label">完成时间</div>
                          <div class="time-value">{{ fmtDateTime(scope.row.submissionTime) }}</div>
                        </div>
                        <el-tag type="success" :size="isMobile ? 'small' : 'small'" effect="plain" class="status-badge">已完成</el-tag>
                      </div>
                    </div>
                    <div v-else class="time-item pending">
                      <div class="time-icon-wrapper warning">
                        <el-icon><Clock /></el-icon>
                      </div>
                      <div class="time-content-wrapper">
                        <div class="time-content">
                          <div class="time-label">截止时间</div>
                          <div class="time-value">{{ fmtDateTime(scope.row.deadline) }}</div>
                        </div>
                        <el-tag type="warning" :size="isMobile ? 'small' : 'small'" effect="plain" class="status-badge">待完成</el-tag>
                      </div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="操作" :width="isMobile ? 100 : 150" align="right">
                <template #default="scope">
                  <el-button 
                    v-if="scope.row.maxParticipants && scope.row.currentParticipants >= scope.row.maxParticipants && !scope.row.completed"
                    disabled
                    type="info"
                    :size="isMobile ? 'small' : 'default'"
                  >
                    已满员
                  </el-button>
                  <el-button 
                    v-else
                    :type="scope.row.completed ? 'success' : 'primary'" 
                    :icon="EditPen" 
                    :size="isMobile ? 'small' : 'default'"
                    @click="openSubmit(scope.row)"
                  >
                    {{ isMobile ? (scope.row.completed ? '修改' : '报名') : (scope.row.completed ? '修改提交' : '去报名') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>

    <!-- 任务提交弹窗 -->
    <el-dialog v-model="submitDialogVisible" :title="'完成任务：' + currentTask.title" width="700px" destroy-on-close>
      <div style="margin-bottom: 15px; color: #666;">
        {{ currentTask.description }}
      </div>
      <el-form :model="submitForm" label-width="120px">
        <!-- 如果有字段定义，动态渲染表单 -->
        <template v-if="currentTask.fields && currentTask.fields.length > 0">
          <el-form-item 
            v-for="field in currentTask.fields" 
            :key="field.fieldName"
            :label="field.fieldName"
            :required="field.required && shouldShowField(field)"
            v-show="shouldShowField(field)"
          >
            <!-- 文本类型 -->
            <el-input 
              v-if="field.fieldType === 'text'"
              v-model="submitForm.fieldData[field.fieldName]"
              :placeholder="field.placeholder || '请输入' + field.fieldName"
            />
            <!-- 数字类型 -->
            <el-input-number 
              v-else-if="field.fieldType === 'number'"
              v-model="submitForm.fieldData[field.fieldName]"
              :placeholder="field.placeholder || '请输入' + field.fieldName"
              style="width: 100%"
            />
            <!-- 日期类型 -->
            <el-date-picker
              v-else-if="field.fieldType === 'date'"
              v-model="submitForm.fieldData[field.fieldName]"
              type="date"
              :placeholder="field.placeholder || '请选择' + field.fieldName"
              style="width: 100%"
            />
            <!-- 下拉选择类型 -->
            <el-select
              v-else-if="field.fieldType === 'select'"
              v-model="submitForm.fieldData[field.fieldName]"
              :placeholder="field.placeholder || '请选择' + field.fieldName"
              style="width: 100%"
              @change="() => handleFieldChange(field.fieldName)"
            >
              <el-option 
                v-for="option in getSelectOptions(field)" 
                :key="option" 
                :label="option" 
                :value="option" 
              />
            </el-select>
          </el-form-item>
        </template>
        <!-- 如果没有字段定义，使用原来的文本域 -->
        <el-form-item v-else label="填写内容">
          <el-input 
            v-model="submitForm.content" 
            type="textarea" 
            rows="6" 
            placeholder="请按任务要求填写相关信息…" 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitTask">提交</el-button>
      </template>
    </el-dialog>

    <!-- 新增弹窗（随联动字段） -->
    <el-dialog v-model="createVisible" :title="'新增' + moduleTitle" width="760px" destroy-on-close>
      <el-form ref="createFormRef" :model="form" :rules="rules" label-width="110px">
        <!-- 学术活动 -->
        <template v-if="moduleKey === 'academic'">
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="学生ID" prop="studentId"><el-input v-model="form.studentId" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="学生姓名" prop="studentName"><el-input v-model="form.studentName" /></el-form-item></el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="活动标题" prop="title"><el-input v-model="form.title" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="活动类型" prop="type"><el-select v-model="form.type"><el-option v-for="t in dict.academicType" :key="t" :label="t" :value="t" /></el-select></el-form-item></el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="主办方" prop="organizer"><el-input v-model="form.organizer" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="主讲人" prop="speaker"><el-input v-model="form.speaker" /></el-form-item></el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="开始时间" prop="startDate"><el-date-picker v-model="form.startDate" type="date" style="width:100%" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="结束时间" prop="endDate"><el-date-picker v-model="form.endDate" type="date" style="width:100%" /></el-form-item></el-col>
          </el-row>
          <el-form-item label="附件URL" prop="attachmentUrl"><el-input v-model="form.attachmentUrl" placeholder="PDF/日程链接" /></el-form-item>
        </template>

        <!-- 日常活动 -->
        <template v-else-if="moduleKey === 'daily'">
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="学生ID" prop="studentId"><el-input v-model="form.studentId" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="学生姓名" prop="studentName"><el-input v-model="form.studentName" /></el-form-item></el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="活动标题" prop="title"><el-input v-model="form.title" /></el-form-item></el-col>
            <el-col :span="12">
              <el-form-item label="活动类型" prop="activityType">
                <el-select v-model="form.activityType">
                  <el-option v-for="o in dict.activityType" :key="o.value" :label="o.label" :value="o.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="主办/授予" prop="organizer"><el-input v-model="form.organizer" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="级别" prop="level"><el-select v-model="form.level"><el-option v-for="lv in dict.level" :key="lv" :label="lv" :value="lv" /></el-select></el-form-item></el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="开始时间" prop="startDate"><el-date-picker v-model="form.startDate" type="date" style="width:100%" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="结束时间" prop="endDate"><el-date-picker v-model="form.endDate" type="date" style="width:100%" /></el-form-item></el-col>
          </el-row>
          <el-form-item label="证明材料URL" prop="attachmentUrl"><el-input v-model="form.attachmentUrl" /></el-form-item>
        </template>

        <!-- 请假管理 -->
        <template v-else-if="moduleKey === 'leave'">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="请假开始时间" prop="startDate" required>
                <el-date-picker 
                  v-model="form.startDate" 
                  type="date" 
                  placeholder="选择开始日期" 
                  style="width:100%"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="请假结束时间" prop="endDate" required>
                <el-date-picker 
                  v-model="form.endDate" 
                  type="date" 
                  placeholder="选择结束日期" 
                  style="width:100%"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="请假原因" prop="reason" required>
            <el-input 
              v-model="form.reason" 
              type="textarea" 
              :rows="4" 
              placeholder="请输入请假原因"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="请假单/证明材料" prop="evidenceUrls">
            <el-upload
              :action="''"
              :auto-upload="false"
              :on-change="handleLeaveFileChange"
              :on-remove="handleLeaveFileRemove"
              :file-list="leaveFileList"
              list-type="text"
              :limit="5"
              accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
            >
              <el-button type="primary" plain>
                <el-icon><Plus /></el-icon>
                上传材料
              </el-button>
            </el-upload>
            <div class="el-upload__tip" style="margin-top: 8px; color: #909399; font-size: 12px;">
              学生填写请假说明后上传请假单、医院证明或其他材料；支持 PDF/Word/图片，最多5个文件
            </div>
          </el-form-item>
        </template>

        <!-- 荣誉 -->
        <template v-else>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="学生ID" prop="userId"><el-input v-model="form.userId" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="荣誉名称" prop="title"><el-input v-model="form.title" /></el-form-item></el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="类别" prop="category"><el-select v-model="form.category"><el-option v-for="c in dict.honorCategory" :key="c" :label="c" :value="c" /></el-select></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="级别" prop="level"><el-select v-model="form.level"><el-option v-for="lv in dict.level" :key="lv" :label="lv" :value="lv" /></el-select></el-form-item></el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="授予单位" prop="awardOrg"><el-input v-model="form.awardOrg" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="获奖日期" prop="awardDate"><el-date-picker v-model="form.awardDate" type="date" style="width:100%" /></el-form-item></el-col>
          </el-row>
          <el-form-item label="证明材料" required><RecordAttachments v-model="honorAttachments" :limit="1" editable /></el-form-item>
          <el-form-item label="标签(逗号分隔)"><el-input v-model="form.tags" placeholder="示例：AI, 一作, Top会议" /></el-form-item>
          <el-form-item label="是否公开"><el-switch v-model="form.isPublic" /></el-form-item>
          <el-form-item label="荣誉描述"><el-input type="textarea" :rows="4" v-model="form.description" /></el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看详情：大尺寸美观弹窗 -->
    <el-dialog
        v-model="detailVisible"
        :title="moduleTitle + ' · 详情'"
        width="1060px"
        top="6vh"
        destroy-on-close
        class="detail-dialog"
    >
      <!-- 顶部信息条 -->
      <div class="detail-header">
        <div class="left">
          <div class="main-title">{{ moduleKey === 'leave' ? (detailEntity.reason || '-') : (detailEntity.title || '-') }}</div>
          <div class="sub">
            <el-tag size="small" effect="plain">{{ moduleTitle }}</el-tag>
            <span class="dot">·</span>
            <span class="muted">发起人：</span><span>{{ detailEntity.studentName || detailEntity.userId || '-' }}</span>
            <span class="dot">·</span>
            <span class="muted">时间：</span>
            <span v-if="moduleKey === 'leave'">
              {{ fmtDate(detailEntity.startDate) || '-' }}
              <span v-if="detailEntity.endDate">至 {{ fmtDate(detailEntity.endDate) }}</span>
              <span v-if="detailEntity.days">（{{ detailEntity.days }} 天）</span>
            </span>
            <span v-else-if="moduleKey !== 'honor'">
              {{ fmtDate(detailEntity.startDate) || '-' }}
              <span v-if="detailEntity.endDate">至 {{ fmtDate(detailEntity.endDate) }}</span>
            </span>
            <span v-else>{{ fmtDate(detailEntity.awardDate) || '-' }}</span>
          </div>
        </div>
        <div class="right">
          <div class="status-tags">
            <el-tag v-if="moduleKey === 'leave'" :type="getStatusType(detailEntity)" effect="dark" class="status-tag-item">
              {{ getStatusText(detailEntity) }}
            </el-tag>
            <el-tag :type="statusMeta(detailEntity.auditStatus).type" effect="dark" class="status-tag-item">
              <el-icon class="mr4"><component :is="statusMeta(detailEntity.auditStatus).icon" /></el-icon>
              {{ statusMeta(detailEntity.auditStatus).text }}
            </el-tag>
          </div>
        </div>
      </div>

      <el-row :gutter="isMobile ? 0 : 16">
        <!-- 左侧：信息 -->
        <el-col :xs="24" :sm="24" :md="16" :lg="16">
          <el-card shadow="never" class="section-card">
            <template #header><div class="section-title">详细信息</div></template>
            <el-descriptions :column="isMobile ? 1 : 2" border class="detail-descriptions">
              <el-descriptions-item 
                v-for="(item, index) in detailPairs" 
                :key="`detail-${index}-${item.label}`"
                :label="item.label"
              >
                <template v-if="Array.isArray(item.value)">
                  <el-space wrap><el-tag v-for="t in item.value" :key="t" effect="plain">{{ t }}</el-tag></el-space>
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

            <div v-if="moduleKey === 'leave' && detailEntity.auditStatus === 'approved'" class="seal-card">
              <div class="seal-mark">准假</div>
              <div>
                <strong>已审核盖章</strong>
                <p>管理员通过审核后，系统自动生成电子盖章状态。</p>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 右侧：小提示/状态 -->
        <el-col :xs="24" :sm="24" :md="8" :lg="8">
          <el-card shadow="never" class="section-card status-card">
            <template #header><div class="section-title">当前状态</div></template>
            <el-result
                :icon="statusResultIcon"
                :title="statusMeta(detailEntity.auditStatus).text"
                sub-title="如有更正，请联系辅导员或管理员"
            />
          </el-card>
        </el-col>
      </el-row>
    </el-dialog>

    <!-- 销假对话框 -->
    <el-dialog v-model="checkInDialogVisible" title="销假签到" width="500px">
      <el-form ref="checkInFormRef" :model="checkInForm" label-width="100px">
        <el-form-item label="当前位置">
          <div style="display: flex; align-items: center; gap: 10px;">
            <el-button 
              type="primary" 
              :icon="Location" 
              :loading="checkInForm.locating"
              @click="getCurrentLocation"
            >
              {{ checkInForm.locating ? '定位中...' : '获取定位' }}
            </el-button>
            <span v-if="checkInForm.locationError" style="color: #f56c6c; font-size: 12px;">
              {{ checkInForm.locationError }}
            </span>
          </div>
        </el-form-item>
        <el-form-item v-if="checkInForm.address" label="当前位置">
          <el-input 
            v-model="checkInForm.address" 
            readonly
            style="color: #67c23a;"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
        <el-form-item label="销假备注" prop="checkInComment">
          <el-input 
            v-model="checkInForm.checkInComment" 
            type="textarea" 
            :rows="4"
            placeholder="请输入销假备注（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="checkInDialogVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="submitCheckIn"
          :disabled="!checkInForm.latitude || !checkInForm.longitude"
        >
          确定销假
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import RecordAttachments from '@/components/RecordAttachments.vue'
import { taskDateError, orderedDates, hasAttachments } from '@/utils/submissionValidation'
import { volunteerServiceApi } from '@/api/party'
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Clock, CircleCheck, CircleClose, Link, EditPen, Search, Plus, Calendar, User, View, Location } from '@element-plus/icons-vue'
import { getActiveDailyTasks, submitDailyTask, getMyDailyTaskSubmission, listItems, createItem } from '@/api/daily'
import { createLeaveRequest, getMyLeaveRequests, checkIn, uploadLeaveEvidence } from '@/api/leave'
import { useStore } from 'vuex'
import { getImageUrl, isImage, getFileName } from '@/utils/imageUrl'

const store = useStore()
const userInfo = computed(() => store.state.user || {})

// 检测是否为移动端
const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1920)
const isMobile = computed(() => windowWidth.value <= 768)

// 请假文件列表
const leaveFileList = ref([])

// 处理请假文件变化
const handleLeaveFileChange = (file, fileList) => {
  leaveFileList.value = fileList
}

// 处理请假文件移除
const handleLeaveFileRemove = (file, fileList) => {
  leaveFileList.value = fileList
}

// 字典
const dict = {
  academicType: ['会议', '讲座', '研讨会'],
  activityType: [
    { label: '讲座', value: 'lecture' },
    { label: '会议', value: 'conference' },
    { label: '竞赛', value: 'competition' },
    { label: '荣誉', value: 'honor' }
  ],
  honorCategory: ['竞赛', '论文', '专利', '奖学金', '社会服务', '其他'],
  level: ['国家', '省级', '校级', '院级', '其他']
}

// 路由
const route = useRoute()
const router = useRouter()

// 当前页面
const moduleKey = ref('academic')
const tabPathMap = {
  volunteer: '/daily/volunteer',
  academic: '/daily/academic',
  daily: '/daily/daily',
  leave: '/daily/leave',
  honor: '/daily/honor',
  tasks: '/daily/tasks'
}
const isSeparatedSubPage = computed(() => Boolean(route.path.split('/')[2]))
const getRouteTab = () => {
  const pathTab = route.path.split('/')[2]
  if (['academic', 'daily', 'leave', 'honor', 'tasks', 'volunteer'].includes(pathTab)) return pathTab
  if (['academic', 'daily', 'leave', 'honor', 'tasks', 'volunteer'].includes(route.query.tab)) return route.query.tab
  return 'academic'
}
const moduleTitle = computed(() => {
    if (moduleKey.value === 'volunteer') return '志愿活动'
    if (moduleKey.value === 'academic') return '学术活动'
    if (moduleKey.value === 'daily') return '日常活动'
    if (moduleKey.value === 'leave') return '请假管理'
    if (moduleKey.value === 'honor') return '荣誉'
    return '日常任务'
})

/** ========= 日常任务相关 ========= */
const activeTasks = ref([])
const volunteerHistory = ref([])
const honorAttachments = ref('')
const categoryTasks = computed(() => activeTasks.value.filter(task => (task.activityCategory || 'daily') === (moduleKey.value === 'tasks' ? 'daily' : moduleKey.value)))
const taskTab = ref('normal') // 任务标签页：normal(普通任务) 或 registration(报名型任务)
const submitDialogVisible = ref(false)
const currentTask = ref({})

// 普通任务列表（所有人都需要完成）
const normalTasks = computed(() => {
  return categoryTasks.value.filter(task => !task.taskCategory || task.taskCategory === 'normal')
})

// 报名型任务列表（限制报名人数）
const registrationTasks = computed(() => {
  return categoryTasks.value.filter(task => task.taskCategory === 'registration')
})

// 任务标签页切换
const handleTaskTabChange = () => {
  // 可以在这里添加切换逻辑
}
const submitForm = reactive({
  taskId: '',
  content: '',
  fieldData: {} // 字段化数据
})

// 判断字段是否应该显示（条件显示逻辑）
const shouldShowField = (field) => {
  // 如果没有条件字段，始终显示
  if (!field.conditionalField || !field.conditionalValue) {
    return true
  }
  
  // 获取依赖字段的值
  const dependentValue = submitForm.fieldData[field.conditionalField]
  
  // 如果依赖字段的值等于条件值，则显示
  return dependentValue === field.conditionalValue
}

// 处理字段值变化（当条件字段的值改变时，清空被隐藏字段的值）
const handleFieldChange = (fieldName) => {
  // 检查是否有字段依赖当前字段
  if (currentTask.value.fields) {
    currentTask.value.fields.forEach(field => {
      if (field.conditionalField === fieldName) {
        // 如果依赖字段的值不等于条件值，清空当前字段的值
        if (submitForm.fieldData[fieldName] !== field.conditionalValue) {
          submitForm.fieldData[field.fieldName] = ''
        }
      }
    })
  }
}

// 获取下拉选项数组（处理字符串和数组两种情况）
const getSelectOptions = (field) => {
  if (!field || field.fieldType !== 'select') {
    return []
  }
  
  if (!field.options) {
    return []
  }
  
  // 如果是字符串，按空格分割（支持多个空格）
  if (typeof field.options === 'string') {
    return field.options.split(/\s+/).map(opt => opt.trim()).filter(opt => opt)
  }
  
  // 如果是数组，确保每个元素都是字符串
  if (Array.isArray(field.options)) {
    return field.options.map(opt => String(opt).trim()).filter(opt => opt)
  }
  
  return []
}

const loadActiveTasks = async () => {
  try {
    const res = await getActiveDailyTasks()
    if (res.code === 200) {
      // 处理任务数据，确保 options 是数组
      const processedTasks = res.data.map(task => {
        if (task.fields && task.fields.length > 0) {
          task.fields = task.fields.map(field => {
            const processedField = { ...field }
            // 如果 options 是字符串，转换为数组（使用空格分隔）
            if (field.fieldType === 'select' && field.options) {
              if (typeof field.options === 'string') {
                processedField.options = field.options.split(/\s+/).map(opt => opt.trim()).filter(opt => opt)
              } else if (Array.isArray(field.options)) {
                processedField.options = field.options.map(opt => String(opt).trim()).filter(opt => opt)
              } else {
                processedField.options = []
              }
            } else if (field.fieldType === 'select' && !field.options) {
              processedField.options = []
            }
            return processedField
          })
        }
        return task
      })
      
      // 对任务进行排序
      const sortedTasks = [...processedTasks].sort((a, b) => {
        // 1. 未完成的排前面，已完成的排后面
        if (a.completed !== b.completed) {
          return a.completed ? 1 : -1
        }
        
        // 2. 如果都是未完成，按截止时间升序排序（越临近越靠前）
        if (!a.completed && !b.completed) {
          const deadlineA = a.deadline ? new Date(a.deadline).getTime() : Number.MAX_SAFE_INTEGER
          const deadlineB = b.deadline ? new Date(b.deadline).getTime() : Number.MAX_SAFE_INTEGER
          return deadlineA - deadlineB // 升序：越早的截止时间越靠前
        }
        
        // 3. 如果都是已完成，按完成时间降序排序（最近完成的在前）
        if (a.completed && b.completed) {
          const submissionTimeA = a.submissionTime ? new Date(a.submissionTime).getTime() : 0
          const submissionTimeB = b.submissionTime ? new Date(b.submissionTime).getTime() : 0
          return submissionTimeB - submissionTimeA // 降序：最近完成的在前
        }
        
        return 0
      })
      
      activeTasks.value = sortedTasks
    }
  } catch (e) {
    console.error('加载任务失败', e)
  }
}

const openSubmit = async (task) => {
  // 处理任务数据，确保 options 是数组
  const processedTask = {
    ...task,
    fields: task.fields ? task.fields.map(field => {
      const processedField = { ...field }
      // 如果 options 是字符串，转换为数组（使用空格分隔）
      if (field.fieldType === 'select' && field.options) {
        if (typeof field.options === 'string') {
          // 如果是字符串，按空格分割（支持多个空格）
          processedField.options = field.options.split(/\s+/).map(opt => opt.trim()).filter(opt => opt)
        } else if (Array.isArray(field.options)) {
          // 如果已经是数组，确保每个元素都是字符串
          processedField.options = field.options.map(opt => String(opt).trim()).filter(opt => opt)
        } else {
          processedField.options = []
        }
      } else if (field.fieldType === 'select' && !field.options) {
        // 如果没有 options，初始化为空数组
        processedField.options = []
      }
      return processedField
    }) : []
  }
  
  currentTask.value = processedTask
  submitForm.taskId = processedTask.id
  submitForm.content = ''
  submitForm.fieldData = {}
  
  // 如果有字段定义，初始化字段数据
  if (processedTask.fields && processedTask.fields.length > 0) {
    processedTask.fields.forEach(field => {
      submitForm.fieldData[field.fieldName] = ''
    })
  }
  
  // 检查报名型任务是否已满员
  if (processedTask.taskCategory === 'registration' && !processedTask.completed) {
    const maxParticipants = processedTask.maxParticipants
    const currentParticipants = processedTask.currentParticipants || 0
    if (maxParticipants && currentParticipants >= maxParticipants) {
      ElMessage.warning('报名人数已满，无法报名')
      return
    }
  }
  
  // 尝试获取之前的提交
  try {
    const res = await getMyDailyTaskSubmission(task.id)
    if (res.code === 200 && res.data) {
      const previousContent = res.data.content
      // 尝试解析为JSON（字段化数据）
      try {
        const parsed = JSON.parse(previousContent)
        if (typeof parsed === 'object' && !Array.isArray(parsed)) {
          submitForm.fieldData = parsed
        } else {
          submitForm.content = previousContent
        }
      } catch {
        // 如果不是JSON，使用原来的文本内容
        submitForm.content = previousContent
      }
    }
  } catch (e) {
    console.error('获取之前提交失败', e)
  }
  
  submitDialogVisible.value = true
}

const handleSubmitTask = async () => {
  const dateError = taskDateError((currentTask.value.fields || []).filter(shouldShowField), submitForm.fieldData)
  if (dateError) return ElMessage.warning(dateError)
  // 检查报名型任务是否已满员
  if (currentTask.value.taskCategory === 'registration' && !currentTask.value.completed) {
    const maxParticipants = currentTask.value.maxParticipants
    const currentParticipants = currentTask.value.currentParticipants || 0
    if (maxParticipants && currentParticipants >= maxParticipants) {
      return ElMessage.warning('报名人数已满，无法报名')
    }
  }
  
  // 如果有字段定义，验证必填字段（只验证显示的字段）
  if (currentTask.value.fields && currentTask.value.fields.length > 0) {
    for (const field of currentTask.value.fields) {
      // 只验证显示的字段
      if (shouldShowField(field) && field.required && (submitForm.fieldData[field.fieldName] == null || String(submitForm.fieldData[field.fieldName]).trim() === '')) {
        return ElMessage.warning(`请填写必填字段：${field.fieldName}`)
      }
    }
    // 将字段数据转换为JSON字符串
    submitForm.content = JSON.stringify(submitForm.fieldData)
  } else {
    // 如果没有字段定义，使用原来的文本内容
    if (!submitForm.content) {
      return ElMessage.warning('请填写提交内容')
    }
  }
  
  try {
    const res = await submitDailyTask(submitForm)
    if (res.code === 200) {
      ElMessage.success(currentTask.value.taskCategory === 'registration' ? '报名成功' : '提交成功')
      submitDialogVisible.value = false
      loadActiveTasks() // 刷新列表以更新完成状态
      if (moduleKey.value === 'volunteer') loadBusinessData()
    } else {
      ElMessage.warning(res.msg || '提交失败，请检查填写内容')
    }
  } catch (e) {
    // 检查是否是报名人数已满的错误
    if (e.response && e.response.data && e.response.data.msg && e.response.data.msg.includes('报名人数已满')) {
      ElMessage.warning('报名人数已满，无法报名')
    } else {
    ElMessage.error('提交失败')
    }
  }
}

// 列表数据（按存）
const allRows = reactive({ academic: [], daily: [], honor: [], leave: [] })
const rows = computed(() => allRows[moduleKey.value] || [])
const stat = computed(() => ({
  pending: rows.value.filter(x => x.auditStatus === 'pending').length,
  passed: rows.value.filter(x => x.auditStatus === 'approved').length
}))

// 获取业务列表
const loadBusinessData = async () => {
  if (moduleKey.value === 'volunteer') {
    try { volunteerHistory.value = (await volunteerServiceApi.getStudentVolunteerServices(userInfo.value.studentId || userInfo.value.id)).data || [] } catch { /* shared interceptor */ }
  }
  if (['academic', 'daily', 'tasks', 'volunteer'].includes(moduleKey.value)) loadActiveTasks()
  if (['tasks', 'volunteer'].includes(moduleKey.value)) return
  try {
    if (moduleKey.value === 'leave') {
      // 请假，使用专门的API
      // 从当前登录用户获取学号
      const studentId = userInfo.value.studentId || (store.state.user && store.state.user.studentId)
      if (!studentId) {
        ElMessage.warning('无法获取学号，请重新登录')
        return
      }
      const res = await getMyLeaveRequests(studentId)
      if (res.code === 200) {
        allRows.leave = res.data || []
      } else {
        ElMessage.error(res.msg || '加载请假记录失败')
      }
    } else {
      const userId = localStorage.getItem('userId')
      const res = await listItems(moduleKey.value, { studentId: userId })
      if (res.code === 200) {
        allRows[moduleKey.value] = res.data
      }
    }
  } catch (e) {
    console.error('加载业务数据失败', e)
  }
}

// 搜索
const searchKey = ref('')
const applySearch = () => {
  if (!searchKey.value) {
    loadBusinessData()
    return
  }
  const key = searchKey.value.trim().toLowerCase()
  if (moduleKey.value === 'leave') {
    // 请假：支持搜索请假原因、学生姓名
    allRows[moduleKey.value] = allRows[moduleKey.value].filter(r => 
      (r.reason && r.reason.toLowerCase().includes(key)) ||
      (r.studentName && r.studentName.toLowerCase().includes(key)) ||
      (r.studentId && r.studentId.toLowerCase().includes(key))
    )
  } else {
    allRows[moduleKey.value] = allRows[moduleKey.value].filter(r => 
      (r.title && r.title.toLowerCase().includes(key))
    )
  }
}

// 新增
const createVisible = ref(false)
const createFormRef = ref()
const initAcademic = {
  id: '', studentId: '', studentName: '', title: '', type: '讲座',
  organizer: '', speaker: '', attachmentUrl: '',
  startDate: '', endDate: '',
  auditStatus: 'pending', auditComment: '', auditorId: '', auditTime: '',
  createTime: '', updateTime: ''
}
const initDaily = {
  id: '', studentId: '', studentName: '', activityType: 'lecture', title: '',
  organizer: '', level: '校级', attachmentUrl: '',
  startDate: '', endDate: '',
  auditStatus: 'pending', auditComment: '', auditorId: '', auditTime: '',
  createTime: '', updateTime: ''
}
const initLeave = {
  id: '', studentId: '', // 只传学号，其他信息后端自动获取
  reason: '', startDate: '', endDate: '', days: 0,
  evidenceUrls: [], status: 'pending',
  auditStatus: 'pending', auditComment: '', auditorId: '', auditorName: '', auditTime: '',
  checkInTime: '', checkInComment: '',
  createTime: '', updateTime: ''
}
const initHonor = {
  id: '', userId: '', title: '', category: '竞赛', level: '校级',
  awardOrg: '', awardDate: '', description: '',
  evidenceUrl: '', tags: '', isPublic: false,
  auditStatus: 'pending', auditComment: '', auditorId: '', auditTime: '',
  createTime: '', updateTime: ''
}
const form = reactive({ ...initAcademic })
const rules = reactive({
  title: [{ required: computed(() => moduleKey.value !== 'leave').value, message: '请填写标题', trigger: 'blur' }],
  type: [{ required: computed(() => moduleKey.value === 'academic').value, message: '请选择类型', trigger: 'change' }],
  startDate: [{ required: computed(() => moduleKey.value !== 'honor' && moduleKey.value !== 'leave').value, message: '请选择开始时间', trigger: 'change' }],
  endDate: [
    { required: computed(() => moduleKey.value === 'leave').value, message: '请选择结束时间', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        if (moduleKey.value === 'leave' && form.startDate && value) {
          if (new Date(value) < new Date(form.startDate)) {
            callback(new Error('结束时间不能早于开始时间'))
          } else {
            callback()
          }
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  reason: [{ required: computed(() => moduleKey.value === 'leave').value, message: '请输入请假原因', trigger: 'blur' }],
  awardDate: [{ required: computed(() => moduleKey.value === 'honor').value, message: '请选择获奖日期', trigger: 'change' }]
})
const openCreate = () => {
  honorAttachments.value = ''
  createVisible.value = true
  if (moduleKey.value === 'leave') {
    // 请假表单，只初始化请假相关字段，学生信息从后端自动获取
    Object.assign(form, {
      ...initLeave,
      studentId: userInfo.value.studentId || '' // 从当前登录用户获取学号
    })
    leaveFileList.value = []
  } else {
    Object.assign(
      form,
      moduleKey.value === 'academic' ? { ...initAcademic } :
          moduleKey.value === 'daily' ? { ...initDaily } : { ...initHonor }
    )
  }
}

const submitCreate = async () => {
  if (!orderedDates(form.startDate, form.endDate)) return ElMessage.warning('结束时间不能早于开始时间')
  if (moduleKey.value === 'honor') {
    if (!hasAttachments(honorAttachments.value)) return ElMessage.warning('请上传荣誉证明材料')
    form.evidenceUrl = JSON.parse(honorAttachments.value)[0].url
  }
  await createFormRef.value.validate()
  try {
    if (moduleKey.value === 'leave') {
      // 请假提交逻辑
      // 先上传请假单/证明材料
      const files = leaveFileList.value.map(f => f.raw).filter(f => f)
      let evidenceUrls = []
      if (files.length > 0) {
        const uploadRes = await uploadLeaveEvidence(files)
        if (uploadRes.code === 200) {
          evidenceUrls = Array.isArray(uploadRes.data) ? uploadRes.data : [uploadRes.data]
        } else {
          ElMessage.error('材料上传失败')
          return
        }
      }
      
      const leaveData = {
        studentId: form.studentId, // 只传学号
        reason: form.reason,
        startDate: form.startDate,
        endDate: form.endDate,
        evidenceUrls
      }
      const res = await createLeaveRequest(leaveData)
      if (res.code === 200) {
        ElMessage.success('请假申请提交成功，待审核')
        createVisible.value = false
        loadBusinessData()
      } else {
        // 检查是否是个人信息不完整的错误
        if (res.msg && res.msg.includes('个人信息不完整')) {
          ElMessage.warning(res.msg + '，请前往个人信息页面完善信息')
        } else {
          ElMessage.error(res.msg || '提交失败')
        }
      }
    } else {
      const res = await createItem(moduleKey.value, form)
      if (res.code === 200) {
        ElMessage.success('提交成功，待审核')
        createVisible.value = false
        loadBusinessData()
      }
    }
  } catch (e) {
    // 处理异常情况
    if (e.response && e.response.data && e.response.data.msg) {
      const errorMsg = e.response.data.msg
      if (errorMsg.includes('个人信息不完整')) {
        ElMessage.warning(errorMsg + '，请前往个人信息页面完善信息')
      } else {
        ElMessage.error(errorMsg)
      }
    } else {
      ElMessage.error('提交失败')
    }
  }
}

// 状态渲染
const statusMeta = (v) => {
  const map = {
    pending: { text: '待审核', type: 'warning', icon: Clock },
    approved: { text: '已通过', type: 'success', icon: CircleCheck },
    rejected: { text: '已驳回', type: 'danger', icon: CircleClose }
  }
  return map[v] || { text: v, type: 'info', icon: Clock }
}

// 请假状态渲染
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

const getStatusType = (row) => {
  if (moduleKey.value === 'leave') {
    const status = row.status
    // 如果状态是on_leave，需要根据结束日期判断类型
    if (status === 'on_leave' && row.endDate) {
      const endDate = new Date(row.endDate)
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
  } else {
    return statusMeta(row.auditStatus).type
  }
}

const getStatusText = (row) => {
  if (moduleKey.value === 'leave') {
    return getLeaveStatusText(row.status, row)
  } else {
    return statusMeta(row.auditStatus).text
  }
}
const statusResultIcon = computed(() => {
  const v = detailEntity.auditStatus
  return v === 'approved' ? 'success' : v === 'rejected' ? 'error' : 'info'
})

// 详情弹窗
const detailVisible = ref(false)
const detailEntity = reactive({})

// 销假相关
const checkInDialogVisible = ref(false)
const checkInFormRef = ref()
const currentCheckIn = ref(null)
const checkInForm = reactive({
  checkInComment: '',
  latitude: null,
  longitude: null,
  address: '',
  locating: false,
  locationError: ''
})

// 根据经纬度获取地址（逆地理编码）
const getAddressFromCoordinates = async (latitude, longitude) => {
  try {
    // 使用Nominatim API（OpenStreetMap的免费逆地理编码服务）
    // 注意：如果需要更高精度，可以替换为高德地图或百度地图API
    const response = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}&zoom=18&addressdetails=1&accept-language=zh-CN`,
      {
        headers: {
          'User-Agent': 'MIS-System/1.0' // Nominatim要求设置User-Agent
        }
      }
    )
    
    if (response.ok) {
      const data = await response.json()
      if (data.address) {
        // 构建地址字符串（优先使用中文地址）
        const addressParts = []
        if (data.address.country) addressParts.push(data.address.country)
        if (data.address.province || data.address.state) addressParts.push(data.address.province || data.address.state)
        if (data.address.city || data.address.town || data.address.village) {
          addressParts.push(data.address.city || data.address.town || data.address.village)
        }
        if (data.address.district || data.address.county) addressParts.push(data.address.district || data.address.county)
        if (data.address.road || data.address.street) addressParts.push(data.address.road || data.address.street)
        if (data.address.house_number) addressParts.push(data.address.house_number)
        
        if (addressParts.length > 0) {
          checkInForm.address = addressParts.join('')
        } else {
          // 如果无法解析详细地址，使用display_name
          checkInForm.address = data.display_name || `纬度: ${latitude.toFixed(6)}, 经度: ${longitude.toFixed(6)}`
        }
      } else {
        checkInForm.address = data.display_name || `纬度: ${latitude.toFixed(6)}, 经度: ${longitude.toFixed(6)}`
      }
    } else {
      // 如果API调用失败，显示经纬度
      checkInForm.address = `纬度: ${latitude.toFixed(6)}, 经度: ${longitude.toFixed(6)}`
    }
  } catch (e) {
    console.warn('逆地理编码失败，使用经纬度显示', e)
    // 如果逆地理编码失败，显示经纬度
    checkInForm.address = `纬度: ${latitude.toFixed(6)}, 经度: ${longitude.toFixed(6)}`
  }
}

// 获取当前定位
const getCurrentLocation = () => {
  if (!navigator.geolocation) {
    checkInForm.locationError = '您的浏览器不支持定位功能'
    return
  }
  
  checkInForm.locating = true
  checkInForm.locationError = ''
  
  navigator.geolocation.getCurrentPosition(
    async (position) => {
      try {
        const { latitude, longitude } = position.coords
        checkInForm.latitude = latitude
        checkInForm.longitude = longitude
        
        // 使用逆地理编码获取地址
        await getAddressFromCoordinates(latitude, longitude)
        
        checkInForm.locating = false
        ElMessage.success('定位成功')
      } catch (e) {
        checkInForm.locating = false
        checkInForm.locationError = '获取定位信息失败'
        console.error('定位处理失败', e)
      }
    },
    (error) => {
      checkInForm.locating = false
      let errorMsg = '定位失败'
      switch (error.code) {
        case error.PERMISSION_DENIED:
          errorMsg = '用户拒绝了定位请求，请在浏览器设置中允许定位权限'
          break
        case error.POSITION_UNAVAILABLE:
          errorMsg = '定位信息不可用'
          break
        case error.TIMEOUT:
          errorMsg = '定位请求超时'
          break
      }
      checkInForm.locationError = errorMsg
      ElMessage.error(errorMsg)
    },
    {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 0
    }
  )
}

// 打开销假对话框
const openCheckInDialog = (row) => {
  currentCheckIn.value = row
  checkInForm.checkInComment = ''
  checkInForm.latitude = null
  checkInForm.longitude = null
  checkInForm.address = ''
  checkInForm.locating = false
  checkInForm.locationError = ''
  checkInDialogVisible.value = true
  // 自动获取定位
  getCurrentLocation()
}

// 提交销假
const submitCheckIn = async () => {
  if (!currentCheckIn.value || !currentCheckIn.value.id) {
    ElMessage.error('销假信息错误')
    return
  }
  
  if (!checkInForm.latitude || !checkInForm.longitude) {
    ElMessage.warning('请先获取定位信息')
    return
  }
  
  try {
    const res = await checkIn(
      currentCheckIn.value.id, 
      checkInForm.checkInComment || '',
      checkInForm.latitude,
      checkInForm.longitude,
      checkInForm.address
    )
    if (res.code === 200) {
      ElMessage.success('销假成功')
      checkInDialogVisible.value = false
      // 刷新请假列表
      loadBusinessData()
    } else {
      ElMessage.error(res.msg || '销假失败')
    }
  } catch (e) {
    console.error('销假失败', e)
    ElMessage.error('销假失败')
  }
}
const openDetail = (row) => {
  Object.assign(detailEntity, row)
  detailVisible.value = true
}

// 详情展示的键值对
const detailPairs = computed(() => {
  const e = detailEntity
  if (moduleKey.value === 'academic') {
    return [
      { label: '学生ID', value: e.studentId },
      { label: '学生姓名', value: e.studentName },
      { label: '活动类型', value: e.type },
      { label: '主办方', value: e.organizer },
      { label: '主讲人', value: e.speaker },
      { label: '开始时间', value: fmtDate(e.startDate) },
      { label: '结束时间', value: fmtDate(e.endDate) }
    ]
  } else if (moduleKey.value === 'leave') {
    return [
      { label: '学号', value: e.studentId },
      { label: '姓名', value: e.studentName },
      { label: '年级', value: e.grade },
      { label: '专业', value: e.major },
      { label: '班级', value: e.className },
      { label: '电话', value: e.phone },
      { label: '请假原因', value: e.reason },
      { label: '开始时间', value: fmtDate(e.startDate) },
      { label: '结束时间', value: fmtDate(e.endDate) },
      { label: '请假天数', value: e.days + ' 天' },
      { label: '请假状态', value: getLeaveStatusText(e.status) },
      { label: '审核状态', value: statusMeta(e.auditStatus).text },
      { label: '审核意见', value: e.auditComment || '-' },
      { label: '审核人', value: e.auditorName || '-' },
      { label: '审核时间', value: e.auditTime ? fmtDateTime(e.auditTime) : '-' },
      { label: '销假时间', value: e.checkInTime ? fmtDateTime(e.checkInTime) : '-' },
      { label: '销假备注', value: e.checkInComment || '-' }
    ]
  } else if (moduleKey.value === 'daily') {
    return [
      { label: '学生ID', value: e.studentId },
      { label: '学生姓名', value: e.studentName },
      { label: '活动类型', value: dict.activityType.find(i=>i.value===e.activityType)?.label || e.activityType },
      { label: '主办/授予', value: e.organizer },
      { label: '级别', value: e.level },
      { label: '开始时间', value: fmtDate(e.startDate) },
      { label: '结束时间', value: fmtDate(e.endDate) }
    ]
  } else {
    return [
      { label: '学生ID', value: e.userId },
      { label: '荣誉类别', value: e.category },
      { label: '级别', value: e.level },
      { label: '授予单位', value: e.awardOrg },
      { label: '获奖日期', value: fmtDate(e.awardDate) },
      { label: '荣誉描述', value: e.description },
      { label: '标签', value: Array.isArray(e.tags) ? e.tags : (e.tags ? String(e.tags).split(',').map(s => s.trim()) : []) }
    ]
  }
})


// 附件/材料预览
const proofUrl = computed(() => {
  if (moduleKey.value === 'honor') return getImageUrl(detailEntity.evidenceUrl)
  if (moduleKey.value === 'academic') return getImageUrl(detailEntity.attachmentUrl)
  if (moduleKey.value === 'daily') return getImageUrl(detailEntity.attachmentUrl)
  if (moduleKey.value === 'leave') {
    // 请假证据图片，返回第一张用于预览
    if (detailEntity.evidenceUrls && detailEntity.evidenceUrls.length > 0) {
      return getImageUrl(detailEntity.evidenceUrls[0])
    }
    return ''
  }
  return ''
})

// 请假证据图片列表（用于多图预览）- 暂时未使用，保留以备后用
// const leaveEvidenceUrls = computed(() => {
//   if (moduleKey.value === 'leave' && detailEntity.evidenceUrls) {
//     return detailEntity.evidenceUrls.map(url => getImageUrl(url))
//   }
//   return []
// })

// 使用工具函数
const fileName = getFileName

// 工具
const fmtDate = (d) => {
  if (!d) return ''
  const date = (d instanceof Date) ? d : new Date(d)
  if (Number.isNaN(+date)) return String(d)
  const y = date.getFullYear()
  const m = String(date.getMonth()+1).padStart(2,'0')
  const day = String(date.getDate()).padStart(2,'0')
  return `${y}-${m}-${day}`
}
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
const onTabChange = (tabName = moduleKey.value) => { 
  searchKey.value = '' 
  if (route.path !== tabPathMap[tabName]) {
    router.replace({
      path: tabPathMap[tabName],
      query: route.query.taskId ? { taskId: route.query.taskId } : {}
    })
  }
  loadBusinessData()
}

// 初始化
// 处理从主页跳转过来的任务ID
const handleRouteTask = async () => {
  const taskId = route.query.taskId
  const tab = getRouteTab()
  
  if (moduleKey.value !== tab) {
    moduleKey.value = tab
    searchKey.value = ''
    await loadBusinessData()
  }
  
  if (taskId && moduleKey.value === 'tasks') {
    // 等待任务列表加载完成
    await loadActiveTasks()
    // 查找对应的任务
    const task = activeTasks.value.find(t => t.id === taskId)
    if (task) {
      const category = task.activityCategory || 'daily'
      if (category !== 'daily') {
        await router.replace({ path: tabPathMap[category], query: { taskId } })
        moduleKey.value = category
      }
      // 自动打开填写弹窗
      await openSubmit(task)
    }
  }
}

onMounted(() => {
  moduleKey.value = getRouteTab()
  loadBusinessData()
  // 处理路由参数
  handleRouteTask()
  // 初始化窗口宽度并监听变化
  if (typeof window !== 'undefined') {
    windowWidth.value = window.innerWidth
    const handleResize = () => {
      windowWidth.value = window.innerWidth
    }
    window.addEventListener('resize', handleResize)
  }
})

// 监听路由变化
watch(() => [route.path, route.query], () => {
  handleRouteTask()
}, { immediate: false })

</script>

<style scoped>
/* 页面基础样式 */
.daily-page {
  padding: 20px;
  min-height: calc(100vh - 60px);
  background-color: #f0f2f5;
}

.page-content {
  margin-top: 20px;
}

.external-tab-mode :deep(.el-tabs__header) {
  display: none;
}

/* 页面头部 */
.page-header-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.header-content h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.header-content p {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.header-stats {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.stat-card {
  min-width: 140px;
  flex: 1;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  font-size: 36px;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
}

.stat-icon.pending {
  color: #E6A23C;
  background-color: #FDF6EC;
}

.stat-icon.success {
  color: #67C23A;
  background-color: #F0F9FF;
}

.stat-icon.info {
  color: #409EFF;
  background-color: #ECF5FF;
}

.stat-info {
  flex: 1;
}

.stat-number {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

/* 工具栏 */
.toolbar-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 15px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 内容卡片 */
.content-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.title-text {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.title-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.ml8 {
  margin-left: 8px;
}

.status-tag .el-icon {
  vertical-align: middle;
}

.mr4 {
  margin-right: 4px;
}

/* 表格优化 */
:deep(.el-table) {
  border-radius: 4px;
  overflow: hidden;
}

:deep(.el-table th) {
  background-color: #fafafa;
  font-weight: 600;
}

:deep(.el-table tr:hover > td) {
  background-color: #f5f7fa;
}

/* 操作按钮优化 */
:deep(.el-button--link) {
  padding: 4px 8px;
  font-weight: 500;
}

/* 详情弹窗：大尺寸 + 自适应 */
.detail-dialog :deep(.el-dialog){
  max-width:1280px;
}
@media (max-width:1280px){
  .detail-dialog :deep(.el-dialog){ width:96vw !important; }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .daily-page {
    padding: 10px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .header-content h2 {
    font-size: 20px;
  }

  .header-content p {
    font-size: 13px;
  }

  .header-stats {
    width: 100%;
    justify-content: space-between;
  }

  .stat-card {
    flex: 1;
    min-width: calc(33.333% - 10px);
  }

  .stat-icon {
    font-size: 28px;
    width: 40px;
    height: 40px;
  }

  .stat-number {
    font-size: 20px;
  }

  .stat-label {
    font-size: 12px;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .toolbar-right {
    width: 100%;
    flex-wrap: wrap;
  }

  .toolbar-right .el-input {
    width: 100% !important;
    flex: 1;
  }

  .toolbar-right .el-button {
    width: 100%;
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

  /* 表格移动端优化 */
  :deep(.el-table) {
    font-size: 11px;
  }

  :deep(.el-table th),
  :deep(.el-table td) {
    padding: 8px 4px;
  }

  :deep(.el-table .cell) {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    padding: 0 4px;
  }

  /* 隐藏移动端不需要的列 */
  .hidden-mobile {
    display: none !important;
  }

  /* 操作列优化 */
  :deep(.el-table__fixed-right) {
    width: 100px !important;
  }

  :deep(.el-table__fixed-right .el-button) {
    padding: 4px 8px;
    font-size: 11px;
  }

  :deep(.el-table__fixed-right .el-button + .el-button) {
    margin-left: 4px;
  }

  /* 操作按钮容器 */
  .action-buttons {
    display: flex;
    flex-direction: column;
    gap: 4px;
    align-items: flex-end;
  }

  .action-buttons .el-button {
    padding: 2px 4px;
    font-size: 11px;
    width: auto;
    min-width: 60px;
  }

  /* 日期显示优化 */
  .date-cell {
    font-size: 11px;
  }

  .date-label {
    font-size: 10px;
  }

  .date-value {
    font-size: 11px;
  }

  /* 状态标签优化 */
  :deep(.status-tag) {
    font-size: 10px;
    padding: 2px 6px;
  }

  /* 卡片标题优化 */
  .card-title {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .title-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .title-tags .el-tag {
    font-size: 11px;
    padding: 2px 6px;
  }

  /* 时间单元格优化 */
  .time-cell {
    font-size: 11px;
  }

  .time-label {
    font-size: 10px;
  }

  .time-value {
    font-size: 11px;
  }

  .time-icon-wrapper {
    width: 24px;
    height: 24px;
  }

  .time-content-wrapper {
    gap: 4px;
  }

  .status-badge {
    font-size: 10px;
    padding: 1px 4px;
  }

  /* 操作按钮容器 */
  .action-buttons {
    display: flex;
    flex-direction: column;
    gap: 4px;
    align-items: flex-end;
  }

  .action-buttons .el-button {
    padding: 2px 4px;
    font-size: 11px;
    width: auto;
    min-width: 60px;
  }

  .detail-dialog :deep(.el-dialog) {
    width: 95vw !important;
    margin: 5vh auto !important;
  }

  /* 详情弹窗移动端优化 */
  .detail-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .detail-header .main-title {
    font-size: 16px;
  }

  .detail-header .sub {
    font-size: 12px;
    flex-wrap: wrap;
    line-height: 1.6;
  }

  .detail-header .right {
    width: 100%;
  }

  .detail-header .status-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    width: 100%;
  }

  .detail-header .status-tag-item {
    flex: 1;
    min-width: calc(50% - 4px);
    text-align: center;
    font-size: 12px;
    padding: 6px 8px;
  }

  .detail-descriptions :deep(.el-descriptions__label) {
    width: 90px !important;
    font-size: 12px;
    padding: 8px 6px;
  }

  .detail-descriptions :deep(.el-descriptions__content) {
    font-size: 12px;
    padding: 8px 6px;
  }

  .section-card {
    margin-bottom: 12px;
  }

  .status-card {
    margin-top: 12px;
  }

  .status-card :deep(.el-result__icon) {
    width: 60px;
    height: 60px;
  }

  .status-card :deep(.el-result__title) {
    font-size: 14px;
    margin-top: 12px;
  }

  .status-card :deep(.el-result__subtitle) {
    font-size: 12px;
    margin-top: 8px;
  }

  .proof-area :deep(.el-image) {
    height: 200px !important;
  }
}

@media (max-width: 480px) {
  .daily-page {
    padding: 8px;
  }

  .header-content h2 {
    font-size: 18px;
  }

  .header-content p {
    font-size: 12px;
  }

  .header-stats {
    flex-direction: column;
    width: 100%;
  }

  .stat-card {
    width: 100%;
  }

  .stat-icon {
    font-size: 24px;
    width: 36px;
    height: 36px;
  }

  .stat-number {
    font-size: 18px;
  }

  .stat-label {
    font-size: 11px;
  }

  :deep(.el-tabs__item) {
    padding: 0 8px;
    font-size: 12px;
  }

  :deep(.el-table) {
    font-size: 10px;
  }

  /* 时间单元格进一步优化 */
  .time-cell {
    font-size: 10px;
  }

  .time-label {
    font-size: 9px;
  }

  .time-value {
    font-size: 10px;
  }

  .time-icon-wrapper {
    width: 20px;
    height: 20px;
  }

  .status-badge {
    font-size: 9px;
    padding: 1px 3px;
  }

  .action-buttons .el-button {
    font-size: 10px;
    padding: 1px 3px;
    min-width: 50px;
  }

  .detail-dialog :deep(.el-dialog) {
    width: 98vw !important;
    margin: 2vh auto !important;
  }

  /* 详情弹窗小屏优化 */
  .detail-header .main-title {
    font-size: 15px;
  }

  .detail-header .sub {
    font-size: 11px;
  }

  .detail-descriptions :deep(.el-descriptions__label) {
    width: 80px !important;
    font-size: 11px;
    padding: 6px 4px;
  }

  .detail-descriptions :deep(.el-descriptions__content) {
    font-size: 11px;
    padding: 6px 4px;
  }

  .status-card :deep(.el-result__icon) {
    width: 50px;
    height: 50px;
  }

  .status-card :deep(.el-result__title) {
    font-size: 13px;
  }

  .status-card :deep(.el-result__subtitle) {
    font-size: 11px;
  }

  .proof-area :deep(.el-image) {
    height: 180px !important;
  }
}
.detail-dialog :deep(.el-dialog__header) {
  background: linear-gradient(90deg, #eef2ff, #ffffff);
  border-bottom: 1px solid #eef2ff;
  padding: 20px;
}

/* 详情弹窗内部样式 */
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.detail-header .main-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.detail-header .sub {
  margin-top: 6px;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.detail-header .dot {
  margin: 0 6px;
  color: #cbd5e1;
}

.detail-header .muted {
  color: #94a3b8;
}

.detail-header .status-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-header .status-tag-item {
  white-space: nowrap;
}

.section-card {
  border: 1px solid #eef2ff;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 8px;
  padding: 16px;
}

.section-title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.proof-area {
  margin-top: 6px;
}

.file-box {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 12px;
  border: 1px dashed #e5e7eb;
  border-radius: 8px;
  background: #fafafa;
  transition: all 0.3s;
}

.file-box:hover {
  border-color: #409EFF;
  background: #f0f9ff;
}

.file-meta .name {
  font-weight: 600;
  margin-bottom: 2px;
  color: #303133;
}

.empty-proof {
  color: #94a3b8;
  background: #f8fafc;
  border: 1px dashed #e5e7eb;
  padding: 12px;
  border-radius: 8px;
  text-align: center;
}

.seal-card {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
  padding: 14px;
  border: 1px solid rgba(220, 38, 38, 0.24);
  border-radius: 12px;
  background: #fff7f7;
}

.seal-mark {
  display: grid;
  width: 72px;
  height: 72px;
  place-items: center;
  border: 3px solid #dc2626;
  border-radius: 50%;
  color: #dc2626;
  font-size: 20px;
  font-weight: 800;
  transform: rotate(-12deg);
}

.seal-card p {
  margin: 4px 0 0;
  color: #667085;
}

/* 表格行悬停效果优化 */
:deep(.el-table__row) {
  transition: background-color 0.2s;
}

:deep(.el-table__row:hover) {
  background-color: #f5f7fa;
}

/* 标签页优化 */
:deep(.el-tabs__item) {
  font-weight: 500;
  transition: all 0.3s;
}

:deep(.el-tabs__item:hover) {
  color: #409EFF;
}

:deep(.el-tabs__item.is-active) {
  color: #409EFF;
  font-weight: 600;
}

/* 卡片阴影优化 */
.content-card {
  transition: box-shadow 0.3s;
}

.content-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 时间单元格样式优化 */
.time-cell {
  padding: 4px 0;
}

.time-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 6px 0;
}

.time-icon-wrapper {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.time-icon-wrapper.success {
  background-color: #f0f9ff;
  color: #67C23A;
}

.time-icon-wrapper.warning {
  background-color: #fdf6ec;
  color: #E6A23C;
}

.time-content-wrapper {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.time-content {
  flex: 1;
  min-width: 0;
}

.time-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
  line-height: 1.4;
}

.time-value {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  line-height: 1.4;
  word-break: break-all;
}

.time-item.completed .time-value {
  color: #67C23A;
}

.time-item.pending .time-value {
  color: #E6A23C;
}

.status-badge {
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: 0;
}

/* 日期单元格样式优化 */
.date-cell {
  padding: 4px 0;
}

.date-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 6px 0;
}

.date-icon-wrapper {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background-color: #f5f7fa;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.date-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.date-label {
  font-size: 12px;
  color: #909399;
}

.date-value {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.date-separator {
  font-size: 12px;
  color: #c0c4cc;
  margin: 2px 0;
  text-align: center;
}

/* 移动端时间显示优化 */
@media (max-width: 768px) {
  .time-item {
    flex-wrap: wrap;
    gap: 8px;
  }

  .time-icon-wrapper {
    width: 28px;
    height: 28px;
  }

  .time-content-wrapper {
    width: 100%;
    gap: 4px;
  }

  .time-label {
    font-size: 11px;
  }

  .time-value {
    font-size: 13px;
  }

  .status-badge {
    align-self: flex-start;
    margin-top: 0;
  }

  .date-item {
    flex-wrap: wrap;
  }

  .date-icon-wrapper {
    width: 28px;
    height: 28px;
  }

  .date-label {
    font-size: 11px;
  }

  .date-value {
    font-size: 13px;
  }
}
</style>
