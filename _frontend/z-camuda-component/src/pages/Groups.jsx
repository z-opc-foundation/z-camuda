/** Camunda 分组：/api/wf/group list + processes + set。 */
import {useEffect, useState} from 'react'
import {Alert, Button, Card, Form, Input, Modal, Space, Table, Tabs, Typography, message} from 'antd'
import {PlusOutlined, ReloadOutlined} from '@ant-design/icons'
import {camudaApi} from '../services/api'

const {Title, Paragraph} = Typography

function listify(v) {
    if (Array.isArray(v)) return v
    if (v && Array.isArray(v.data)) return v.data
    if (v && Array.isArray(v.list)) return v.list
    return []
}

function GroupTable({rows, loading, onSet}) {
    const dataColumns = Object.keys(rows[0] || {}).slice(0, 8).map(k => ({
        title: k, dataIndex: k, key: k, ellipsis: true,
        render: (v) => typeof v === 'object' ? JSON.stringify(v) : String(v ?? '—'),
    }))
    const columns = [
        ...(dataColumns.length ? dataColumns : [{title: '（空）', key: 'e'}]),
        {
            title: '操作', key: 'op', width: 100, fixed: 'right',
            render: (_, r) => <Button size="small" onClick={() => onSet(r)}>设置</Button>,
        },
    ]
    return <Table rowKey={(r, i) => r.id || i} dataSource={rows} columns={columns}
                  loading={loading && !rows.length} size="small" pagination={{pageSize: 10}}/>
}

export default function Groups() {
    const [groups, setGroups] = useState([])
    const [processes, setProcesses] = useState([])
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState(null)
    const [editing, setEditing] = useState(null)
    const [form] = Form.useForm()

    const fetchAll = async () => {
        setLoading(true)
        try {
            const [g, p] = await Promise.all([
                camudaApi.groupList().catch(() => []),
                camudaApi.groupProcesses().catch(() => []),
            ])
            setGroups(listify(g))
            setProcesses(listify(p))
            setError(null)
        } catch (e) {
            setError(e?.message || String(e))
        } finally { setLoading(false) }
    }

    useEffect(() => { fetchAll() }, [])

    const openSet = (r) => {
        setEditing(r)
        form.resetFields()
    }

    const submitSet = async () => {
        try {
            const values = await form.validateFields()
            await camudaApi.groupSet({...values, id: editing?.id})
            message.success('已保存')
            setEditing(null)
            fetchAll()
        } catch (e) {
            if (e?.errorFields) return
            message.error(e?.response?.data?.message || e?.message || '保存失败')
        }
    }

    return (
        <div>
            <Space style={{marginBottom: 16}}>
                <Title level={4} style={{margin: 0}}>引擎分组</Title>
                <Button icon={<ReloadOutlined/>} onClick={fetchAll} loading={loading}>刷新</Button>
            </Space>
            <Paragraph type="secondary">/api/wf/group/list · /api/wf/group/processes · set 绑定。</Paragraph>

            {error && <Alert type="error" showIcon style={{marginBottom: 16}} message="后端未连接" description={error}/>}

            <Card>
                <Tabs items={[
                    {key: 'groups', label: '分组清单', children: <GroupTable rows={groups} loading={loading} onSet={openSet}/>},
                    {key: 'processes', label: '流程清单', children: <GroupTable rows={processes} loading={loading} onSet={openSet}/>},
                ]}/>
            </Card>

            <Modal title={`设置 ${editing?.id ?? ''}`} open={!!editing} onOk={submitSet}
                   onCancel={() => setEditing(null)} destroyOnClose>
                <Form form={form} layout="vertical" initialValues={editing || {}}>
                    <Form.Item name="userId" label="用户 ID">
                        <Input/>
                    </Form.Item>
                    <Form.Item name="groupId" label="分组 ID">
                        <Input/>
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    )
}
