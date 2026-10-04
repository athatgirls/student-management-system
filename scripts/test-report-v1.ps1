# Requires an already built backend JAR and frontend dist. Uses disposable fixtures only.
$ErrorActionPreference = 'Stop'
$env:REPORT_TEST_SECRET = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
$env:REPORT_TEST_ADMIN_PASSWORD = [guid]::NewGuid().ToString('N')
$composeArgs = @('compose', '-p', ('mis-report-v1-' + [guid]::NewGuid().ToString('N').Substring(0,8)), '-f', (Join-Path $PSScriptRoot 'compose.report-test.yml'))
$base = 'http://127.0.0.1:18086/SCSE@hbut/msi'
$checks = 0
function Check($ok, $label) { if (-not $ok) { throw "FAIL: $label" }; $script:checks++; Write-Output "PASS: $label" }
function Api($method, $path, $body, $token) {
  $headers = @{}; if ($token) { $headers.Authorization = "Bearer $token" }
  $args = @{ Uri="$base$path"; Method=$method; Headers=$headers; ContentType='application/json'; SkipHttpErrorCheck=$true }
  if ($null -ne $body) { $args.Body = $body | ConvertTo-Json -Depth 12 -Compress }
  $r = Invoke-WebRequest @args
  try { $data = $r.Content | ConvertFrom-Json -DateKind String } catch { $data = $r.Content }
  return @{ status=[int]$r.StatusCode; data=$data }
}
try {
  & docker @composeArgs up -d --pull never
  if ($LASTEXITCODE) { throw 'Cannot start isolated test stack' }
  $ready = $false
  for ($i=0;$i -lt 60;$i++) {
    try { $r=Invoke-WebRequest 'http://127.0.0.1:18086/SCSE@hbut/actuator/health' -SkipHttpErrorCheck; if ($r.StatusCode -eq 200) { $ready=$true; break } } catch { }
    Start-Sleep -Seconds 2
  }
  Check $ready 'backend health through Nginx'
  $admin = (Api POST '/admin/login' @{username='admin';password=$env:REPORT_TEST_ADMIN_PASSWORD} $null).data.data.token
  Check ([bool]$admin) 'administrator login'
  $temporary='Temporary9!'+[guid]::NewGuid().ToString('N'); $permanent='Permanent9!'+[guid]::NewGuid().ToString('N')
  $created=(Api POST '/student/create' @{studentId='fixture-100001';name='测试学生';grade='2026';major='计算机';politicalStatus='发展对象';status='在读';password=$temporary} $admin).data
  Check ($created.code -eq 200) 'create fixture student'
  $studentDbId=$created.data.id
  Check ((Api POST '/student/change-initial-password' @{studentId='fixture-100001';initialPassword=$temporary;newPassword=$permanent} $null).data.code -eq 200) 'initial password POST'
  $student=(Api POST '/student/login' @{account='fixture-100001';password=$permanent} $null).data.data.token
  Check ([bool]$student) 'student login'
  $saved=(Api POST '/student/profile/update' @{major='软件工程';className='一班';politicalStatus='中共党员';supervisor='导师';researchDirection='人工智能';workStatus='学生助管';phone='13000000000';name='伪造';id='victim';password='Spoof123!'} $student).data
  Check ($saved.code -eq 200 -and $saved.data.major -eq '软件工程' -and $saved.data.className -eq '一班' -and $saved.data.supervisor -eq '导师' -and $saved.data.politicalStatus -eq '发展对象' -and $saved.data.workStatus -eq '学生助管' -and $saved.data.name -eq '测试学生') 'self-service fields persist without identity escalation'
  $intent=(Api POST '/employment-intentions/me' @{id='victim';intentionType='就业';targetCity='武汉';targetPosition='研发'} $student).data
  Check ($intent.id -eq $studentDbId) 'intention bound to authenticated owner'
  Check ((Api GET '/employment-intentions/me' $null $student).data.targetCity -eq '武汉') 'intention survives server reload'
  Check ((Api GET '/employment-intentions/admin/list' $null $admin).data.Count -eq 1) 'administrator sees intention'
  Check ((Api GET '/employment-intentions/admin/list' $null $student).status -eq 403) 'student cannot list all intentions'
  $upload=Invoke-RestMethod -Uri "$base/upload" -Method Post -Headers @{Authorization="Bearer $student"} -Form @{file=Get-Item (Join-Path $PSScriptRoot '../index/src/assets/logo.png')}
  Check ($upload.code -eq 200 -and $upload.data.Count -eq 1) 'proof file upload'
  $materialUrl=$upload.data[0]
  Check ((Invoke-WebRequest -Uri "http://127.0.0.1:18086$materialUrl" -Headers @{Authorization="Bearer $student"}).StatusCode -eq 200) 'owner can view uploaded proof'
  Check ((Invoke-WebRequest -Uri "http://127.0.0.1:18086$materialUrl" -SkipHttpErrorCheck).StatusCode -eq 401) 'proof remains private'
  $attachments=ConvertTo-Json -InputObject @(@{name='测试材料.png';url=$materialUrl}) -Compress
  $study=(Api POST '/study-records' @{semester='2026秋';course='课程';score=85;credit=2;attachments=$attachments;studentDbId='victim'} $student).data
  Check ($study.studentDbId -eq $studentDbId -and $study.source -eq 'student') 'learning record owner and source'
  $study.score=90
  Check ((Api POST '/study-records' $study $student).data.score -eq 90) 'student edits own learning record'
  Check ((Api GET '/student/study-records' $null $student).data.data.Count -eq 1) 'learning record listed'
  foreach ($type in @('competitions','papers','patents','projects')) {
    Check ((Api POST "/$type" @{} $student).status -eq 400) "$type requires materials"
    $entry=(Api POST "/$type" @{competitionName='测试竞赛';paperTitle='测试论文';patentTitle='测试专利';projectName='测试项目';attachments=$attachments;teamMembers='本人';instructorName='导师'} $student).data
    Check ([bool]$entry.id) "$type creation"
    Check ((Api POST "/$type/admin/$($entry.id)/audit" @{auditStatus='通过'} $student).status -eq 403) "$type student cannot audit"
    Check ((Api POST "/$type/admin/$($entry.id)/audit" @{auditStatus='已通过';auditComment='通过'} $admin).data.auditStatus -eq '已通过') "$type POST approve"
    Check ((Api POST "/$type/admin/$($entry.id)/audit" @{auditStatus='已驳回';auditComment='补充'} $admin).data.auditStatus -eq '已驳回') "$type POST reject"
    Check ((Api POST "/$type/$($entry.id)/delete" $null $admin).status -eq 200) "$type POST delete"
  }
  Check ((Api POST '/honors/create' @{title='荣誉'} $student).status -eq 400) 'honor requires proof'
  Check ((Api POST '/honors/create' @{title='荣誉';evidenceUrl=$materialUrl} $student).data.code -eq 200) 'student submits honor'
  $course=(Api POST '/party-course/add' @{title='微党课';description='https://example.edu.cn/article';courseDate='2026-10-01'} $student).data
  Check ($course.code -eq 200) 'course accepts link and date without removed fields'
  $party=(Api POST '/party-member/add' @{name='测试学生';studentId='fixture-100001';branch='计算机科学与人工智能学院研究生第一党支部'} $admin).data
  Check ((Api POST "/party-member/delete/$($party.data.id)" $null $admin).data.code -eq 200) 'POST party information delete'
  $intern=(Api POST '/internship-employment/add' @{company='测试公司';type='实习';startDate='2026-09-01';endDate='2026-10-01'} $student).data.data
  Check ((Api POST "/internship-employment/approve/$($intern.id)" @{approvalRemark='通过'} $admin).data.code -eq 200) 'internship approve'
  $intern.company='修改后公司'; $intern.approvalStatus='已通过'
  Check ((Api POST '/internship-employment/update' $intern $admin).data.data.company -eq '修改后公司') 'administrator edits approved internship through POST'
  Check ((Api POST "/internship-employment/delete/$($intern.id)" $null $admin).data.code -eq 200) 'POST internship delete'
  $deadline=(Get-Date).AddDays(7).ToString('yyyy-MM-ddTHH:mm:ss')
  $fields=@(@{fieldName='离校时间';fieldType='date';required=$true},@{fieldName='返校时间';fieldType='date';required=$true})
  $task=(Api POST '/daily-tasks/create' @{title='日期测试';taskCategory='normal';activityCategory='academic';deadline=$deadline;fields=$fields} $admin).data.data
  Check ((Api GET '/daily-tasks/active' $null $student).data.data.activityCategory -contains 'academic') 'task classification returned to students'
  $reversed=@{'离校时间'='2026-10-03';'返校时间'='2026-10-01'} | ConvertTo-Json -Compress
  Check ((Api POST '/daily-tasks/submit' @{taskId=$task.id;content=$reversed} $student).data.code -eq 400) 'reverse dates rejected on server'
  $task=(Api POST '/daily-tasks/create' @{title='志愿活动';taskCategory='registration';activityCategory='volunteer';deadline=$deadline;maxParticipants=1} $admin).data.data
  Check ((Api POST '/daily-tasks/submit' @{taskId=$task.id;content='报名'} $student).data.code -eq 200) 'volunteer signup'
  Check ((Api GET '/volunteer-service/student/fixture-100001' $null $student).data.data.Count -eq 0) 'signup alone does not create completed volunteer record'
  $activity=(Api POST '/activities/create' @{title='志愿活动';taskId=$task.id;activityTime='2026-10-01T10:00:00';location='校园'} $admin).data.data
  foreach ($i in 1..2) {
    $match=(Api POST "/activities/$($activity.id)/import-students" @{students=@(@{studentId='fixture-100001';name='测试学生'})} $admin).data
    Check ($match.code -eq 200 -and $match.data.matchedCount -eq 1) "attendance import $i succeeds even at signup capacity"
  }
  $history=(Api GET '/volunteer-service/student/fixture-100001' $null $student).data.data
  Check ($history.Count -eq 1 -and $history[0].auditStatus -eq '通过') 'completed volunteer sync is idempotent and visible in party records'
  Check ((Api POST "/activities/$($activity.id)/sync-volunteer-attendance" $null $student).status -eq 403) 'student cannot synchronize attendance'
  $sync=(Api POST "/activities/$($activity.id)/sync-volunteer-attendance" $null $admin).data
  Check ($sync.code -eq 200 -and $sync.data.skippedCount -eq 1) 'attendance resync skips existing stable record'
  Check ((Api POST '/activities/compensate-volunteer-attendance' $null $admin).data.data.activityCount -eq 1) 'historical confirmed attendance compensation'
  Check ((Api POST "/activities/delete/$($activity.id)" $null $student).status -eq 403) 'student cannot delete admin activity'
  Check ((Api POST "/activities/delete/$($activity.id)" $null $admin).data.code -eq 200) 'POST activity delete'
  # V2: administrator-distributed files preserve private-proof ownership and audience restrictions.
  $adminUpload=Invoke-RestMethod -Uri "$base/upload" -Method Post -Headers @{Authorization="Bearer $admin"} -Form @{file=Get-Item (Join-Path $PSScriptRoot '../index/src/assets/logo.png')}
  $handout=$adminUpload.data[0]
  Check ((Invoke-WebRequest -Uri "http://127.0.0.1:18086$handout" -Headers @{Authorization="Bearer $student"} -SkipHttpErrorCheck).StatusCode -eq 403) 'unpublished admin file is not public to students'
  Check ((Api POST '/daily-tasks/create' @{title='伪造任务附件';attachments=@($materialUrl)} $admin).data.code -eq 400) 'student proof cannot be republished as task handout'
  Check ((Api POST '/daily-tasks/create' @{title='非法附件';attachments=@('javascript:alert(1)')} $admin).data.code -eq 400) 'executable handout URL rejected'
  $handoutTask=(Api POST '/daily-tasks/create' @{title='日常任务文件';type='信息填写';taskCategory='normal';activityCategory='routine';deadline=$deadline;allowedGrades=@('2026');attachments=@($handout)} $admin).data.data
  Check ([bool]$handoutTask.id -and $handoutTask.deadline -eq $deadline) 'daily-task category and unchanged local deadline persist'
  Check ((Invoke-WebRequest -Uri "http://127.0.0.1:18086$handout" -Headers @{Authorization="Bearer $student"}).StatusCode -eq 200) 'eligible task recipient downloads admin handout'
  Check ((Invoke-WebRequest -Uri "http://127.0.0.1:18086$handout" -SkipHttpErrorCheck).StatusCode -eq 401) 'task handout still requires authentication'
  $otherTemp='Temporary9!'+[guid]::NewGuid().ToString('N'); $otherPass='Permanent9!'+[guid]::NewGuid().ToString('N')
  $otherCreated=(Api POST '/student/create' @{studentId='fixture-100002';name='范围外学生';grade='2025';major='计算机';password=$otherTemp} $admin).data.data
  Check ((Api POST '/student/change-initial-password' @{studentId='fixture-100002';initialPassword=$otherTemp;newPassword=$otherPass} $null).data.code -eq 200) 'other fixture initializes own password'
  $other=(Api POST '/student/login' @{account='fixture-100002';password=$otherPass} $null).data.data.token
  Check ([bool]$other) 'other fixture login'
  Check ((Invoke-WebRequest -Uri "http://127.0.0.1:18086$handout" -Headers @{Authorization="Bearer $other"} -SkipHttpErrorCheck).StatusCode -eq 403) 'out-of-scope student cannot download handout'
  Check ((Api GET '/daily-tasks/active' $null $other).data.data.id -notcontains $handoutTask.id) 'out-of-scope task hidden'
  $before=(Api GET '/daily-tasks/my-completion' $null $student).data.data
  Check ($before.totalRequired -eq 3 -and $before.completedCount -eq 0) 'homepage completion counts two ordinary fixtures and profile task, excludes registration'
  $submitted=(Api POST '/daily-tasks/submit' @{taskId=$handoutTask.id;content='测试内容'} $student).data.data
  Check ($submitted.submissionTime -match 'Z$' -and [Math]::Abs(([DateTimeOffset]::Parse($submitted.submissionTime)-[DateTimeOffset]::UtcNow).TotalSeconds) -lt 60) 'completion timestamp is accurate explicit UTC'
  $after=(Api GET '/daily-tasks/my-completion' $null $student).data.data
  Check ($after.totalRequired -eq 3 -and $after.completedCount -eq 1 -and $after.completionRate -eq 33.33) 'student homepage completion becomes 33.33 percent'
  $analysis=(Api GET '/daily-tasks/completion-analysis' $null $admin).data.data | Where-Object studentId -eq 'fixture-100001'
  Check ($analysis.completionRate -eq $after.completionRate -and $analysis.totalRequired -eq $after.totalRequired) 'administrator and student completion numbers agree'
  Check ((Api GET '/daily-tasks/completion-analysis' $null $student).status -eq 403) 'student cannot view other students completion'
  # V2: owner/admin honor maintenance and fresh approval after edits.
  $honor=(Api POST '/honors/create' @{title='荣誉维护';awardDate='2026-10-01';evidenceUrl=$materialUrl} $student).data.data
  Check ((Api GET "/honors/$($honor.id)" $null $other).status -eq 403) 'other student cannot view honor'
  Check ((Api POST "/honors/$($honor.id)/update" @{title='越权';evidenceUrl=$materialUrl} $other).status -eq 403) 'other student cannot edit honor'
  Check ((Api POST "/honors/$($honor.id)/delete" $null $other).status -eq 403) 'other student cannot delete honor'
  Check ((Api POST '/honors/audit' @{id=$honor.id;auditStatus='approved'} $admin).data.data.auditStatus -eq 'approved') 'administrator approves honor'
  $changed=(Api POST "/honors/$($honor.id)/update" @{title='修改后的荣誉';awardDate='2026-10-01';evidenceUrl=$materialUrl;userId=$otherCreated.id;auditStatus='approved';auditorId='spoof'} $student).data.data
  Check ($changed.userId -eq $studentDbId -and $changed.auditStatus -eq 'pending' -and -not $changed.auditorId -and [Math]::Abs(([DateTimeOffset]::Parse($changed.createTime)-[DateTimeOffset]::Parse($honor.createTime)).TotalMilliseconds) -lt 1) 'own honor edit preserves owner and creation time and clears audit'
  Check ((Api POST "/honors/$($honor.id)/update" @{title='管理员修改';awardDate='2026-10-01';evidenceUrl=$materialUrl} $admin).data.data.title -eq '管理员修改') 'administrator edits student honor'
  Check ((Api POST "/honors/$($honor.id)/delete" $null $student).status -eq 200) 'student deletes own honor'
  $honor=(Api POST '/honors/create' @{title='管理员删除';evidenceUrl=$materialUrl} $student).data.data
  Check ((Api POST "/honors/$($honor.id)/delete" $null $admin).status -eq 200) 'administrator deletes student honor'
  # V2: campus-compatible leave audit, state changes and check-in.
  $leave=(Api POST '/leave/create' @{reason='测试请假';startDate='2026-10-01';endDate='2026-10-02';studentId='spoof'} $student).data.data
  Check ([bool]$leave.id -and $leave.studentId -eq 'fixture-100001') 'student leave uses authenticated identity'
  Check ((Api POST "/leave/audit/$($leave.id)" @{auditStatus='approved'} $student).status -eq 403) 'student cannot approve leave'
  $approved=(Api POST "/leave/audit/$($leave.id)" @{auditStatus='approved';auditComment='测试通过';auditorId='spoof';auditorName='spoof'} $admin).data.data
  Check ($approved.auditStatus -eq 'approved' -and $approved.status -eq 'approved' -and $approved.auditorId -ne 'spoof') 'POST leave approval succeeds and uses authenticated auditor'
  Check ((Api POST "/leave/update-status/$($leave.id)" @{status='on_leave'} $admin).data.code -eq 200) 'POST manual leave status succeeds'
  Check ((Api POST "/leave/check-in/$($leave.id)" @{checkInComment='返校';latitude=30.0;longitude=114.0;address='校园'} $other).status -eq 403) 'other student cannot check in owner leave'
  Check ((Api POST "/leave/check-in/$($leave.id)" @{checkInComment='返校';latitude=30.0;longitude=114.0;address='校园'} $student).data.data.status -eq 'completed') 'POST owner leave check-in succeeds'
  Write-Output "REPORT_V2_INTEGRATION_OK checks=$checks"
} finally {
  & docker @composeArgs down --volumes
}
