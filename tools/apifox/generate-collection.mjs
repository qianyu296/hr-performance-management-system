import { writeFileSync } from 'node:fs';

const base = '{{baseUrl}}';
const jsonHeader = { key: 'Content-Type', value: 'application/json' };
const auth = (token) => ({ key: 'Authorization', value: `Bearer {{${token}}}` });
const success = `pm.test('HTTP 200', () => pm.response.to.have.status(200));\npm.test('business success', () => pm.expect(pm.response.json().code).to.eql('SUCCESS'));`;
const expected = (status, code) => `pm.test('expected HTTP status', () => pm.response.to.have.status(${status}));\npm.test('expected business code', () => pm.expect(pm.response.json().code).to.eql('${code}'));`;
const expectedStatus = (status) => `pm.test('expected HTTP status', () => pm.response.to.have.status(${status}));`;
const capture = (statements) => `${success}\n${statements.join('\n')}`;

function request(name, method, path, { token, body, test = success, pre } = {}) {
  const headers = [];
  if (token) headers.push(auth(token));
  if (body !== undefined) headers.push(jsonHeader);
  const item = {
    name,
    request: {
      method,
      header: headers,
      url: { raw: `${base}${path}`, host: [base], path: path.replace(/^\//, '').split('/') }
    },
    event: [{ listen: 'test', script: { type: 'text/javascript', exec: test.split('\n') } }]
  };
  if (body !== undefined) item.request.body = { mode: 'raw', raw: JSON.stringify(body, null, 2), options: { raw: { language: 'json' } } };
  if (pre) item.event.unshift({ listen: 'prerequest', script: { type: 'text/javascript', exec: pre.split('\n') } });
  return item;
}

function folder(name, item) { return { name, item }; }
function login(name, username, token, refresh) {
  return request(name, 'POST', '/auth/login', {
    body: { username: `{{${username}}}`, password: '{{password}}' },
    test: capture([
      `const data = pm.response.json().data;`,
      `pm.environment.set('${token}', data.accessToken);`,
      refresh ? `pm.environment.set('${refresh}', data.refreshToken);` : ''
    ].filter(Boolean))
  });
}

const runStamp = `if (!pm.environment.get('runStamp')) pm.environment.set('runStamp', Date.now().toString());`;
const timestampCode = (prefix) => `${prefix}_{{runStamp}}`;

const bootstrap = folder('00 Bootstrap and Authentication', [
  request('API-PUB-01 Health', 'GET', '/health', { test: `${success}\npm.test('health is UP', () => pm.expect(pm.response.json().data.status).to.eql('UP'));`, pre: runStamp }),
  login('API-AUTH-01 Login api-admin', 'adminUsername', 'adminAccessToken', 'adminRefreshToken'),
  login('API-AUTH-01 Login api-hr', 'hrUsername', 'hrAccessToken'),
  login('API-AUTH-01 Login api-manager', 'managerUsername', 'managerAccessToken'),
  login('API-AUTH-01 Login api-employee', 'employeeUsername', 'employeeAccessToken'),
  login('API-AUTH-01 Login api-attendance-admin', 'attendanceAdminUsername', 'attendanceAdminAccessToken'),
  login('API-AUTH-01 Login api-workflow-admin', 'workflowAdminUsername', 'workflowAdminAccessToken'),
  login('API-AUTH-01 Login api-readonly', 'readonlyUsername', 'readonlyAccessToken'),
  request('API-AUTH-01 Invalid credentials', 'POST', '/auth/login', { body: { username: '{{adminUsername}}', password: 'wrong-password' }, test: expected(401, 'AUTH_INVALID_CREDENTIALS') }),
  request('API-AUTH-02 Refresh admin token', 'POST', '/auth/refresh', { body: { refreshToken: '{{adminRefreshToken}}' }, test: capture([`pm.environment.set('adminAccessToken', pm.response.json().data.accessToken);`]) }),
  request('API-AUTH-04 Change password validation', 'POST', '/auth/change-password', { token: 'adminAccessToken', body: { currentPassword: '{{password}}', newPassword: 'short' }, test: expected(400, 'VALIDATION_FAILED') }),
  request('API-ME-01 Current user', 'GET', '/me', { token: 'adminAccessToken' }),
  request('API-ME-02 Current permissions', 'GET', '/me/permissions', { token: 'adminAccessToken' }),
  request('API-ME-03 Current menus', 'GET', '/me/menus', { token: 'adminAccessToken' })
]);

const organization = folder('10 Organization Master Data', [
  request('API-ORG-01 List departments', 'GET', '/departments', { token: 'hrAccessToken' }),
  request('API-ORG-02 Create department', 'POST', '/departments', { token: 'hrAccessToken', body: { code: timestampCode('APIFOX_DEPT'), name: 'APIFOX Department {{runStamp}}', parentId: '{{testDepartmentId}}', sortNo: 1, status: 'ACTIVE', effectiveDate: '2026-07-15' }, test: capture([`pm.environment.set('departmentId', pm.response.json().data.id);`, `pm.environment.set('departmentVersion', pm.response.json().data.version.toString());`]) }),
  request('API-ORG-03 Update department', 'PATCH', '/departments/{{departmentId}}', { token: 'hrAccessToken', body: { name: 'APIFOX Department Updated {{runStamp}}', sortNo: 2, status: 'ACTIVE', effectiveDate: '2026-07-15', version: '{{departmentVersion}}' }, test: capture([`pm.environment.set('departmentVersion', pm.response.json().data.version.toString());`]) }),
  request('API-ORG-04 Move department', 'POST', '/departments/{{departmentId}}/move', { token: 'hrAccessToken', body: { parentId: '{{testDepartmentId}}', version: '{{departmentVersion}}' }, test: capture([`pm.environment.set('departmentVersion', pm.response.json().data.version.toString());`]) }),
  request('API-ORG-05 Disable department', 'POST', '/departments/{{departmentId}}/disable', { token: 'hrAccessToken', body: { version: '{{departmentVersion}}' } }),
  request('API-ORG-06 List positions', 'GET', '/positions', { token: 'hrAccessToken' }),
  request('API-ORG-07 Create position', 'POST', '/positions', { token: 'hrAccessToken', body: { code: timestampCode('APIFOX_POSITION'), name: 'APIFOX Position {{runStamp}}', jobFamily: 'APIFOX', description: 'Created by API test', sortNo: 1, status: 'ACTIVE' }, test: capture([`pm.environment.set('positionId', pm.response.json().data.id);`, `pm.environment.set('positionVersion', pm.response.json().data.version);`]) }),
  request('API-ORG-08 Update position', 'PATCH', '/positions/{{positionId}}', { token: 'hrAccessToken', body: { name: 'APIFOX Position Updated {{runStamp}}', jobFamily: 'APIFOX', description: 'Updated by API test', sortNo: 2, status: 'ACTIVE', version: '{{positionVersion}}' }, test: capture([`pm.environment.set('positionVersion', pm.response.json().data.version);`]) }),
  request('API-ORG-09 List ranks', 'GET', '/ranks', { token: 'hrAccessToken' }),
  request('API-ORG-10 Create rank', 'POST', '/ranks', { token: 'hrAccessToken', body: { code: timestampCode('APIFOX_RANK'), name: 'APIFOX Rank {{runStamp}}', rankOrder: 99, status: 'ACTIVE' }, test: capture([`pm.environment.set('rankId', pm.response.json().data.id);`, `pm.environment.set('rankVersion', pm.response.json().data.version);`]) }),
  request('API-ORG-11 Update rank', 'PATCH', '/ranks/{{rankId}}', { token: 'hrAccessToken', body: { name: 'APIFOX Rank Updated {{runStamp}}', rankOrder: 100, status: 'ACTIVE', version: '{{rankVersion}}' } }),
  request('API-ORG-12 List employees', 'GET', '/employees?page=1&pageSize=20&keyword=APIFOX_', { token: 'hrAccessToken' }),
  request('API-ORG-13 Create employee', 'POST', '/employees', { token: 'hrAccessToken', body: { employeeNo: timestampCode('APIFOX_EMP'), name: 'APIFOX New Employee {{runStamp}}', gender: 'FEMALE', departmentId: '{{testDepartmentId}}', positionId: '{{positionId}}', rankId: '{{rankId}}', managerEmployeeId: '{{managerEmployeeId}}', employmentStatus: 'PROBATION', hireDate: '2026-07-15' }, test: capture([`const employee = pm.response.json().data.employee;`, `pm.environment.set('createdEmployeeId', employee.id);`, `pm.environment.set('createdEmployeeVersion', employee.version);`]) }),
  request('API-ORG-14 Get employee', 'GET', '/employees/{{createdEmployeeId}}', { token: 'hrAccessToken' }),
  request('API-ORG-15 Update employee profile', 'PATCH', '/employees/{{createdEmployeeId}}', { token: 'hrAccessToken', body: { name: 'APIFOX Employee Updated {{runStamp}}', gender: 'MALE', version: '{{createdEmployeeVersion}}' }, test: capture([`pm.environment.set('createdEmployeeVersion', pm.response.json().data.version);`]) }),
  request('API-ORG-16 Employee history', 'GET', '/employees/{{createdEmployeeId}}/history', { token: 'hrAccessToken' }),
  request('API-ORG-AUTH-01 Readonly cannot create a department', 'POST', '/departments', { token: 'readonlyAccessToken', body: { code: timestampCode('APIFOX_DENY'), name: 'Denied', sortNo: 0, status: 'ACTIVE', effectiveDate: '2026-07-15' }, test: expectedStatus(403) })
]);

const templates = folder('20 Workflow Template Setup', [
  request('API-WF-08 List workflow templates', 'GET', '/workflow/templates', { token: 'workflowAdminAccessToken' }),
  request('API-WF-10 Create leave template', 'POST', '/workflow/templates', { token: 'workflowAdminAccessToken', body: { code: timestampCode('APIFOX_LEAVE_WF'), name: 'APIFOX Leave Workflow {{runStamp}}', businessType: 'LEAVE', priority: 999, templateVersion: 1, status: 'ACTIVE', departmentIds: ['{{testDepartmentId}}'], nodes: [{ nodeNo: 1, nodeType: 'SPECIFIC_USER', approverRule: { type: 'SPECIFIC_USER', userId: 9900103 } }] }, test: capture([`pm.environment.set('leaveTemplateId', pm.response.json().data.id);`, `pm.environment.set('leaveTemplateVersion', pm.response.json().data.version);`]) }),
  request('API-WF-10 Create overtime template', 'POST', '/workflow/templates', { token: 'workflowAdminAccessToken', body: { code: timestampCode('APIFOX_OT_WF'), name: 'APIFOX Overtime Workflow {{runStamp}}', businessType: 'OVERTIME', priority: 999, templateVersion: 1, status: 'ACTIVE', departmentIds: ['{{testDepartmentId}}'], nodes: [{ nodeNo: 1, nodeType: 'SPECIFIC_USER', approverRule: { type: 'SPECIFIC_USER', userId: 9900103 } }] }, test: capture([`pm.environment.set('overtimeTemplateId', pm.response.json().data.id);`]) }),
  request('API-WF-10 Create personnel template', 'POST', '/workflow/templates', { token: 'workflowAdminAccessToken', body: { code: timestampCode('APIFOX_PC_WF'), name: 'APIFOX Personnel Workflow {{runStamp}}', businessType: 'PERSONNEL_CHANGE', priority: 999, templateVersion: 1, status: 'ACTIVE', departmentIds: ['{{testDepartmentId}}'], nodes: [{ nodeNo: 1, nodeType: 'SPECIFIC_USER', approverRule: { type: 'SPECIFIC_USER', userId: 9900103 } }] }, test: capture([`pm.environment.set('personnelTemplateId', pm.response.json().data.id);`]) }),
  request('API-WF-09 Get leave template', 'GET', '/workflow/templates/{{leaveTemplateId}}', { token: 'workflowAdminAccessToken' }),
  request('API-WF-11 Update leave template', 'PUT', '/workflow/templates/{{leaveTemplateId}}', { token: 'workflowAdminAccessToken', body: { name: 'APIFOX Leave Workflow Updated {{runStamp}}', businessType: 'LEAVE', priority: 999, status: 'ACTIVE', departmentIds: ['{{testDepartmentId}}'], nodes: [{ nodeNo: 1, nodeType: 'SPECIFIC_USER', approverRule: { type: 'SPECIFIC_USER', userId: 9900103 } }], version: '{{leaveTemplateVersion}}' }, test: capture([`pm.environment.set('leaveTemplateVersion', pm.response.json().data.version);`]) })
]);

const attendance = folder('30 Attendance', [
  request('API-ATT-01 List active leave types', 'GET', '/leave-types', { token: 'employeeAccessToken' }),
  request('API-ATT-02 Create leave type', 'POST', '/leave-types', { token: 'attendanceAdminAccessToken', body: { code: timestampCode('APIFOX_LEAVE'), name: 'APIFOX Leave {{runStamp}}', deductBalance: false, annualQuota: null, minUnitHours: 1 }, test: capture([`pm.environment.set('leaveTypeId', pm.response.json().data.id);`, `pm.environment.set('leaveTypeVersion', pm.response.json().data.version);`]) }),
  request('API-ATT-03 Update leave type', 'PATCH', '/leave-types/{{leaveTypeId}}', { token: 'attendanceAdminAccessToken', body: { name: 'APIFOX Leave Updated {{runStamp}}', deductBalance: false, annualQuota: null, minUnitHours: 1, version: '{{leaveTypeVersion}}' }, test: capture([`pm.environment.set('leaveTypeVersion', pm.response.json().data.version);`]) }),
  request('API-ATT-04 List all leave types', 'GET', '/leave-types?includeInactive=true', { token: 'attendanceAdminAccessToken' }),
  request('API-ATT-05 My leave balances', 'GET', '/leave-balances', { token: 'employeeAccessToken' }),
  request('API-ATT-06 Employee leave balances', 'GET', '/leave-balances/employees/{{employeeId}}', { token: 'attendanceAdminAccessToken', test: capture([`const row = pm.response.json().data.find(value => String(value.id) === pm.environment.get('testBalanceId'));`, `pm.expect(row).to.exist;`, `pm.environment.set('testBalanceVersion', row.version.toString());`]) }),
  request('API-ATT-07 Adjust leave balance', 'POST', '/leave-balances/{{testBalanceId}}/adjust', { token: 'attendanceAdminAccessToken', body: { deltaHours: 1, direction: 'INCREASE', reason: 'APIFOX adjustment {{runStamp}}', version: '{{testBalanceVersion}}' } }),
  request('API-ATT-08 Leave balance changes', 'GET', '/leave-balances/{{testBalanceId}}/changes', { token: 'attendanceAdminAccessToken' }),
  request('API-ATT-09 Create leave request', 'POST', '/leave-requests', { token: 'employeeAccessToken', pre: `const day = 10 + (Number(Date.now().toString().slice(-3)) % 10);\npm.environment.set('leaveRequestDate', \`2026-08-\${day.toString().padStart(2, '0')}\`);`, body: { leaveTypeId: '{{leaveTypeId}}', startTime: '{{leaveRequestDate}}T09:00:00Z', endTime: '{{leaveRequestDate}}T11:00:00Z', reason: 'APIFOX leave {{runStamp}}' }, test: capture([`pm.environment.set('leaveRequestId', pm.response.json().data.id);`]) }),
  request('API-ATT-10 List leave requests', 'GET', '/leave-requests', { token: 'employeeAccessToken', test: capture([`const row = pm.response.json().data.find(value => value.id === pm.environment.get('leaveRequestId'));`, `pm.expect(row).to.exist;`, `pm.environment.set('leaveRequestVersion', row.version.toString());`]) }),
  request('API-ATT-11 Submit leave request', 'POST', '/leave-requests/{{leaveRequestId}}/submit', { token: 'employeeAccessToken', body: { version: '{{leaveRequestVersion}}' } }),
  request('API-ATT-12 Cancel draft leave request state error', 'POST', '/leave-requests/{{leaveRequestId}}/cancel', { token: 'employeeAccessToken', body: { version: '1' }, test: expected(409, 'STATE_CONFLICT') }),
  request('API-ATT-13 Create overtime request', 'POST', '/overtime-requests', { token: 'employeeAccessToken', pre: `const day = 20 + (Number(Date.now().toString().slice(-3)) % 8);\npm.environment.set('overtimeRequestDate', \`2026-08-\${day.toString().padStart(2, '0')}\`);`, body: { startTime: '{{overtimeRequestDate}}T01:00:00Z', endTime: '{{overtimeRequestDate}}T03:00:00Z', reason: 'APIFOX overtime {{runStamp}}', compensationType: 'OVERTIME_PAY' }, test: capture([`pm.environment.set('overtimeRequestId', pm.response.json().data.id);`]) }),
  request('API-ATT-14 List overtime requests', 'GET', '/overtime-requests', { token: 'employeeAccessToken', test: capture([`const row = pm.response.json().data.find(value => value.id === pm.environment.get('overtimeRequestId'));`, `pm.expect(row).to.exist;`, `pm.environment.set('overtimeRequestVersion', row.version.toString());`]) }),
  request('API-ATT-15 Submit overtime request', 'POST', '/overtime-requests/{{overtimeRequestId}}/submit', { token: 'employeeAccessToken', body: { version: '{{overtimeRequestVersion}}' } }),
  request('API-ATT-16 Cancel draft overtime state error', 'POST', '/overtime-requests/{{overtimeRequestId}}/cancel', { token: 'employeeAccessToken', body: { version: '1' }, test: expected(409, 'STATE_CONFLICT') }),
  request('API-ATT-17 Create work calendar', 'POST', '/work-calendars', { token: 'attendanceAdminAccessToken', pre: `const year = 2031 + (Number(Date.now().toString().slice(-2)) % 50);\npm.environment.set('calendarYear', year.toString());`, body: { calendarYear: '{{calendarYear}}', name: 'APIFOX Calendar {{runStamp}}', timeZone: 'Asia/Shanghai', status: 'ACTIVE', days: [{ workDate: '{{calendarYear}}-01-02', workday: true, workHours: 8, holidayName: null }] }, test: capture([`pm.environment.set('calendarId', pm.response.json().data.id);`, `pm.environment.set('calendarVersion', pm.response.json().data.version);`]) }),
  request('API-ATT-18 Get work calendar', 'GET', '/work-calendars?year={{calendarYear}}', { token: 'attendanceAdminAccessToken' }),
  request('API-ATT-19 Update work calendar', 'PUT', '/work-calendars/{{calendarId}}', { token: 'attendanceAdminAccessToken', body: { name: 'APIFOX Calendar Updated {{runStamp}}', timeZone: 'Asia/Shanghai', status: 'ACTIVE', days: [{ workDate: '{{calendarYear}}-01-02', workday: false, workHours: 0, holidayName: 'APIFOX holiday' }], version: '{{calendarVersion}}' } }),
  request('API-ATT-20 Rebuild monthly summary', 'POST', '/attendance/monthly-summaries/rebuild', { token: 'attendanceAdminAccessToken', body: { month: '2026-08' } }),
  request('API-ATT-21 List monthly summaries', 'GET', '/attendance/monthly-summaries?month=2026-08&departmentId={{testDepartmentId}}', { token: 'attendanceAdminAccessToken' }),
  request('API-ATT-22 Disable leave type', 'POST', '/leave-types/{{leaveTypeId}}/disable', { token: 'attendanceAdminAccessToken', body: { version: '{{leaveTypeVersion}}' } })
]);

const personnel = folder('40 Personnel Changes and Handover', [
  request('API-PER-01 List personnel changes', 'GET', '/personnel-changes?page=1&pageSize=20', { token: 'hrAccessToken' }),
  request('API-PER-02 Create onboarding change', 'POST', '/personnel-changes', { token: 'hrAccessToken', body: { employeeId: null, changeType: 'ONBOARD', effectiveDate: '2099-12-31', reason: 'APIFOX onboarding {{runStamp}}', afterAssignment: { employeeNo: timestampCode('APIFOX_ONBOARD'), name: 'APIFOX Onboard {{runStamp}}', gender: 'FEMALE', departmentId: '{{testDepartmentId}}', positionId: '{{testPositionId}}', rankId: '{{testRankId}}', managerEmployeeId: '{{managerEmployeeId}}', employmentStatus: 'PROBATION', hireDate: '2099-12-31' } }, test: capture([`pm.environment.set('personnelChangeId', pm.response.json().data.id);`, `pm.environment.set('personnelChangeVersion', pm.response.json().data.version);`]) }),
  request('API-PER-03 Get personnel change', 'GET', '/personnel-changes/{{personnelChangeId}}', { token: 'hrAccessToken' }),
  request('API-PER-04 Update personnel change', 'PATCH', '/personnel-changes/{{personnelChangeId}}', { token: 'hrAccessToken', body: { employeeId: null, changeType: 'ONBOARD', effectiveDate: '2099-12-31', reason: 'APIFOX onboarding updated {{runStamp}}', afterAssignment: { employeeNo: timestampCode('APIFOX_ONBOARD'), name: 'APIFOX Onboard Updated {{runStamp}}', gender: 'FEMALE', departmentId: '{{testDepartmentId}}', positionId: '{{testPositionId}}', rankId: '{{testRankId}}', managerEmployeeId: '{{managerEmployeeId}}', employmentStatus: 'PROBATION', hireDate: '2099-12-31' }, version: '{{personnelChangeVersion}}' }, test: capture([`pm.environment.set('personnelChangeVersion', pm.response.json().data.version);`]) }),
  request('API-PER-05 Submit onboarding change', 'POST', '/personnel-changes/{{personnelChangeId}}/submit', { token: 'hrAccessToken', body: { version: '{{personnelChangeVersion}}' }, test: capture([`pm.environment.set('personnelChangeVersion', pm.response.json().data.version.toString());`]) }),
  request('API-PER-06 Create termination change', 'POST', '/personnel-changes', { token: 'hrAccessToken', body: { employeeId: '{{employeeId}}', changeType: 'TERMINATION', effectiveDate: '2099-12-31', reason: 'APIFOX handover {{runStamp}}', afterAssignment: { employmentStatus: 'TERMINATED', terminationDate: '2099-12-31' } }, test: capture([`pm.environment.set('terminationChangeId', pm.response.json().data.id);`, `pm.environment.set('terminationChangeVersion', pm.response.json().data.version);`]) }),
  request('API-PER-07 Add exit handover item', 'POST', '/personnel-changes/{{terminationChangeId}}/handover-items', { token: 'hrAccessToken', body: { itemType: 'ASSET', receiverEmployeeId: '{{hrEmployeeId}}', required: true, remark: 'APIFOX asset handover' }, test: capture([`const item = pm.response.json().data.handoverItems[0];`, `pm.environment.set('handoverItemId', item.id);`, `pm.environment.set('handoverItemVersion', item.version);`]) }),
  request('API-PER-08 Confirm exit handover item', 'POST', '/personnel-changes/{{terminationChangeId}}/handover-items/{{handoverItemId}}/confirm', { token: 'hrAccessToken', body: { version: '{{handoverItemVersion}}', remark: 'APIFOX confirmed' } }),
  request('API-PER-09 Submit termination change', 'POST', '/personnel-changes/{{terminationChangeId}}/submit', { token: 'hrAccessToken', body: { version: '{{terminationChangeVersion}}' }, test: capture([`pm.environment.set('terminationChangeVersion', pm.response.json().data.version);`]) }),
  request('API-PER-10 Withdraw termination change', 'POST', '/personnel-changes/{{terminationChangeId}}/withdraw', { token: 'hrAccessToken', body: { version: '{{terminationChangeVersion}}' } }),
  request('API-PER-05 Create workflow withdrawal change', 'POST', '/personnel-changes', { token: 'hrAccessToken', body: { employeeId: '{{employeeId}}', changeType: 'CONFIRM', effectiveDate: '2099-12-31', reason: 'APIFOX workflow withdrawal {{runStamp}}', afterAssignment: { employmentStatus: 'FORMAL' } }, test: capture([`pm.environment.set('workflowWithdrawChangeId', pm.response.json().data.id);`, `pm.environment.set('workflowWithdrawChangeVersion', pm.response.json().data.version);`]) }),
  request('API-PER-05 Submit workflow withdrawal change', 'POST', '/personnel-changes/{{workflowWithdrawChangeId}}/submit', { token: 'hrAccessToken', body: { version: '{{workflowWithdrawChangeVersion}}' } })
]);

const tasks = folder('50 Workflow Tasks and Completion', [
  request('API-WF-01 List manager pending tasks', 'GET', '/workflow/tasks', { token: 'managerAccessToken', test: capture([`const tasks = pm.response.json().data;`, `const personnel = tasks.find(task => task.businessType === 'PERSONNEL_CHANGE' && task.businessId === pm.environment.get('personnelChangeId'));`, `const withdrawal = tasks.find(task => task.businessType === 'PERSONNEL_CHANGE' && task.businessId === pm.environment.get('workflowWithdrawChangeId'));`, `const leave = tasks.find(task => task.businessType === 'LEAVE' && task.businessId === pm.environment.get('leaveRequestId'));`, `const overtime = tasks.find(task => task.businessType === 'OVERTIME' && task.businessId === pm.environment.get('overtimeRequestId'));`, `pm.expect(personnel).to.exist; pm.expect(withdrawal).to.exist; pm.expect(leave).to.exist; pm.expect(overtime).to.exist;`, `pm.environment.set('personnelTaskId', personnel.id); pm.environment.set('personnelWorkflowInstanceId', personnel.instanceId); pm.environment.set('workflowWithdrawInstanceId', withdrawal.instanceId);`, `pm.environment.set('leaveTaskId', leave.id); pm.environment.set('leaveTaskVersion', leave.version.toString()); pm.environment.set('leaveWorkflowInstanceId', leave.instanceId);`, `pm.environment.set('overtimeTaskId', overtime.id); pm.environment.set('overtimeTaskVersion', overtime.version.toString());`]) }),
  request('API-WF-02 Get personnel workflow instance', 'GET', '/workflow/tasks/instances/{{personnelWorkflowInstanceId}}', { token: 'hrAccessToken' }),
  request('API-WF-03 Transfer leave task to api-admin', 'POST', '/workflow/tasks/{{leaveTaskId}}/transfer', { token: 'workflowAdminAccessToken', body: { version: '{{leaveTaskVersion}}', transferToUserId: 9900101, comment: 'APIFOX transfer' } }),
  request('API-WF-04 List admin pending tasks', 'GET', '/workflow/tasks', { token: 'adminAccessToken', test: capture([`const task = pm.response.json().data.find(value => value.businessType === 'LEAVE' && value.businessId === pm.environment.get('leaveRequestId'));`, `pm.expect(task).to.exist; pm.environment.set('leaveTaskId', task.id); pm.environment.set('leaveTaskVersion', task.version.toString());`]) }),
  request('API-WF-05 Approve transferred leave task', 'POST', '/workflow/tasks/{{leaveTaskId}}/approve', { token: 'adminAccessToken', body: { version: '{{leaveTaskVersion}}', comment: 'APIFOX approved leave' } }),
  request('API-WF-06 Return overtime task', 'POST', '/workflow/tasks/{{overtimeTaskId}}/return', { token: 'managerAccessToken', body: { version: '{{overtimeTaskVersion}}', comment: 'APIFOX return overtime' } }),
  request('API-WF-07 Withdraw personnel workflow', 'POST', '/workflow/tasks/instances/{{workflowWithdrawInstanceId}}/withdraw', { token: 'hrAccessToken', body: { version: 0, comment: 'APIFOX withdraw workflow' } }),
  request('API-WF-07 Withdraw personnel workflow denied for readonly user', 'POST', '/workflow/tasks/instances/{{personnelWorkflowInstanceId}}/withdraw', { token: 'readonlyAccessToken', body: { version: 0, comment: 'APIFOX denied' }, test: expected(422, 'WORKFLOW_TASK_INVALID') }),
  request('API-WF-07 Personnel effective date not reached', 'POST', '/personnel-changes/{{personnelChangeId}}/effective', { token: 'adminAccessToken', body: { version: '{{personnelChangeVersion}}' }, test: expected(409, 'STATE_CONFLICT') }),
  request('API-WF-08 Approve personnel change', 'POST', '/workflow/tasks/{{personnelTaskId}}/approve', { token: 'managerAccessToken', body: { version: 0, comment: 'APIFOX approve personnel' } }),
  request('API-WF-06 Reload returned overtime request', 'GET', '/overtime-requests', { token: 'employeeAccessToken', test: capture([`const row = pm.response.json().data.find(value => value.id === pm.environment.get('overtimeRequestId'));`, `pm.expect(row).to.exist; pm.environment.set('overtimeRequestVersion', row.version.toString());`]) }),
  request('API-WF-06 Resubmit returned overtime request', 'POST', '/overtime-requests/{{overtimeRequestId}}/submit', { token: 'employeeAccessToken', body: { version: '{{overtimeRequestVersion}}' } }),
  request('API-WF-01 Reload overtime approval task', 'GET', '/workflow/tasks', { token: 'managerAccessToken', test: capture([`const task = pm.response.json().data.find(value => value.businessType === 'OVERTIME' && value.businessId === pm.environment.get('overtimeRequestId'));`, `pm.expect(task).to.exist; pm.environment.set('overtimeTaskId', task.id); pm.environment.set('overtimeTaskVersion', task.version.toString());`]) }),
  request('API-WF-05 Reject overtime task', 'POST', '/workflow/tasks/{{overtimeTaskId}}/reject', { token: 'managerAccessToken', body: { version: '{{overtimeTaskVersion}}', comment: 'APIFOX reject overtime' } })
]);

const system = folder('60 System and Reports', [
  request('API-SYS-01 List roles', 'GET', '/system/roles', { token: 'adminAccessToken' }),
  request('API-SYS-02 List users', 'GET', '/system/users?page=1&pageSize=100', { token: 'adminAccessToken', test: capture([`const user = pm.response.json().data.records.find(value => value.id === '9900107');`, `pm.expect(user).to.exist; pm.environment.set('readonlyUserVersion', user.version);`]) }),
  request('API-SYS-03 Update readonly user roles', 'PUT', '/system/users/9900107/roles', { token: 'adminAccessToken', body: { roleIds: ['9000052'], version: '{{readonlyUserVersion}}' } }),
  request('API-REPORT-01 Headcount by department', 'GET', '/reports/headcount-by-department', { token: 'adminAccessToken' }),
  request('API-AUTH-03 Logout admin', 'POST', '/auth/logout', { token: 'adminAccessToken' })
]);

const collection = {
  info: {
    _postman_id: 'b942b455-2ea8-4ed2-a391-b0e71ef8cb04',
    name: 'HRPM Non-Performance API Regression',
    description: 'Apifox-importable local regression suite. It covers every non-performance endpoint and excludes all performance APIs.',
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json'
  },
  event: [{ listen: 'prerequest', script: { type: 'text/javascript', exec: [runStamp] } }],
  item: [bootstrap, organization, templates, attendance, personnel, tasks, system]
};

writeFileSync(new URL('./hrpm-non-performance-api-tests.postman_collection.json', import.meta.url), `${JSON.stringify(collection, null, 2)}\n`);
