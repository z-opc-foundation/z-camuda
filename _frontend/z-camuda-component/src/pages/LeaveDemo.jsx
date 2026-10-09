/** 请假演示流程：/api/leave start + todo/complete。 */
import {useEffect, useState} from 'react'
import {Alert, Button, Card, Col, Form, Input, Row, Space, Table, Tag, Typography, message} from 'antd'
import {ReloadOutlined, SendOutlined} from '@ant-design/icons'
import {camudaApi} from '../services/api'

const {Title, Paragraph} = Typography

function listify(v) {
    if (Array.isArray(v)) return v
    if (v && Array.isArray(v.data)) return v.data
    if (v && Array.isArray(v.tasks)) return v.tasks
    if (v && Array.isArray(v.list)) return v.list
    return []
}

export default function LeaveDemo() {
    const [approver, setApprover] = useState('admin')
    const [rows, setRows] = useState([])
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState(null)
    const [form] = Form.useForm()

    const fetchTodo = async (a = approver) => {
        if (!a) return
        setLoading(true)
        try {
            setRows(listify(await camudaApi.leaveApprovalTasks(a)))
            setError(null)
        } catch (e) {
            setError(e?.message || String(e))
        } finally { setLoading(false) }
    }

    useEffect(() => { fetchTodo() }, [])

    const start = async () => {
        try {
            const values = await form.validateFields()
            const res = await camudaApi.leaveStart(values.applicant, values.approver)
            message.success(`流程已发起：${JSON.stringify(res ?? {})}`)
            fetchTodo(approver)
        } catch (e) {
            if (e?.errorFields) return
            message.error(e?.response?.data?.message || e?.message || '发起失败')
        }
    }

    const complete = async (r, result) => {
        try {
            await camudaApi.leaveComplete(r.id || r.taskId, result)
            message.success(result === 'approve' ? '已通过' : '已驳回')
            fetchTodo()
        } catch (e) {
            message.error(e?.response?.data?.message || e?.message || '操作失败')
        }
    }

    const dataColumns = Object.keys(rows[0] || {}).slice(0, 6).map(k => ({
        title: k, dataIndex: k, key: k, ellipsis: true,
        render: (v) => typeof v === 'object' ? JSON.stringify(v) : String(v ?? '—'),
    }))
    const columns = [
        ...(dataColumns.length ? dataColumns : [{title: '（空）', key: 'e'}]),
        {
            title: '操作', key: 'op', width: 160, fixed: 'right',
            render: (_, r) => (
                <Space>
                    <Button size="small" type="primary" onClick={() => complete(r, 'approve')}>通过</Button>
                    <Button size="small" danger onClick={() => complete(r, 'reject')}>驳回</Button>
                </Space>
            ),
        },
    ]

    return (
        <div>
            <Space style={{marginBottom: 16}} wrap>
                <Title level={4} style={{margin: 0}}>请假流程演示</Title>
                <Input value={approver} onChange={(e) => setApprover(e.target.value)} style={{width: 160}}
                       placeholder="审批人" addonBefore="审批人"/>
                <Button icon={<ReloadOutlined/>} onClick={() => fetchTodo()} loading={loading}>刷新</Button>
            </Space>
            <Paragraph type="secondary">/api/leave/start 发起 · /api/leave/getApprovalTasks 待审 · /api/leave/complete 审批。</Paragraph>

            {error && <Alert type="error" showIcon style={{marginBottom: 16}} message="后端未连接" description={error}/>}

            <Row gutter={16}>
                <Col span={8}>
                    <Card title="发起新流程">
                        <Form form={form} layout="vertical" onFinish={start}>
                            <Form.Item name="applicant" label="申请人" rules={[{required: true}]}>
                                <Input placeholder="如 zhangsan"/>
                            </Form.Item>
                            <Form.Item name="approver" label="审批人" rules={[{required: true}]}>
                                <Input placeholder="如 admin"/>
                            </Form.Item>
                            <Button type="primary" icon={<SendOutlined/>} htmlType="submit" block>发起</Button>
                        </Form>
                    </Card>
                </Col>
                <Col span={16}>
                    <Card title={`待审批任务（审批人：${approver || '—'}）`}>
                        <Table rowKey={(r, i) => r.id || r.taskId || i} dataSource={rows} columns={columns}
                               loading={loading && !rows.length} size="small" pagination={{pageSize: 10}}/>
                    </Card>
                </Col>
            </Row>
        </div>
    )
}
