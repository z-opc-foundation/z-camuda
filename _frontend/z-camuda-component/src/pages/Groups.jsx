/** Camunda 分组：/api/wf/group list + processes + set。 */
import {useEffect, useState} from 'react'
import {Button, Form, Input, Modal, Space, Tabs, Typography, message} from 'antd'
import {PlusOutlined, ReloadOutlined} from '@ant-design/icons'
import {ErrorState, ListTable, TableToolbar} from '@yuku123/z-frontend-common/ui'
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
    // 整表在前端 ⇒ 走基座 ListTable：分页/查询/空态三件套由基座统一承接。
    // 列是按后端返回的字段动态生成的，所以过滤字段也跟着列走（没数据时 searchKeys 为空 ⇒ 不渲染过滤框）。
    return <ListTable rows={rows} columns={columns} rowKey={(r, i) => r.id || i}
                      loading={loading && !rows.length}
                      searchKeys={dataColumns.map((c) => c.dataIndex).join(',')}
                      searchPlaceholder="按列表字段过滤"
                      emptyTitle="暂无数据"
                      emptyDescription="接口返回空列表"/>
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
            <TableToolbar
                left={<Title level={4} style={{margin: 0}}>引擎分组</Title>}
                right={
                    <Button icon={<ReloadOutlined/>} onClick={fetchAll} loading={loading}>刷新</Button>
                }
            />
            <Paragraph type="secondary">/api/wf/group/list · /api/wf/group/processes · set 绑定。</Paragraph>

            {error && (
                <div style={{marginBottom: 16}}>
                    <ErrorState error={error} title="后端未连接" onRetry={() => fetchAll()}/>
                </div>
            )}

            {/* 外面这层 Card 删掉：基座 ListTable 自带 Card，
                留着就是 Card 套 Card（双层边框 + 双层内边距）。 */}
            <Tabs items={[
                {key: 'groups', label: '分组清单', children: <GroupTable rows={groups} loading={loading} onSet={openSet}/>},
                {key: 'processes', label: '流程清单', children: <GroupTable rows={processes} loading={loading} onSet={openSet}/>},
            ]}/>

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
