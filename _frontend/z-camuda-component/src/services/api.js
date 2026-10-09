/**
 * z-camuda API client：/api/leave（请假演示流程）+ /api/wf/group + /api/wf/health。
 */
import {createRequest} from '@yuku123/z-frontend-common'

const request = createRequest({baseURL: '', tokenKey: 'zcamuda_token'})

export default request

export function configureCamuda(config) {
    if (config && config.apiBase !== undefined) {
        request.defaults.baseURL = config.apiBase
    }
}

/** 后端部分端点直接回 Map（非 Result 包裹），这里统一解一层 data。 */
async function unwrap(promise) {
    const resp = await promise
    const body = resp?.data ?? resp
    if (body && typeof body === 'object' && ('success' in body || ('code' in body && 'data' in body))) {
        if (body.success === false || (typeof body.code === 'number' && body.code >= 400)) {
            throw new Error(body.message || 'request failed')
        }
        return body.data
    }
    return body
}

export const camudaApi = {
    health: () => unwrap(request.get('/api/wf/health')),

    leaveStart: (applicant, approver) => request.post('/api/leave/start', {applicant, approver}),
    leaveTodo: (approver) => unwrap(request.get('/api/leave/todo', {params: {approver}})),
    leaveApprovalTasks: (approver) => unwrap(request.get('/api/leave/getApprovalTasks', {params: {approver}})),
    leaveComplete: (taskId, approvalResult) => request.post('/api/leave/complete', {taskId, approvalResult}),

    groupList: () => unwrap(request.get('/api/wf/group/list')),
    groupSet: (data) => request.post('/api/wf/group/set', data),
    groupProcesses: () => unwrap(request.get('/api/wf/group/processes')),
}
