import {UserOutlined, TeamOutlined} from '@ant-design/icons'
import LeaveDemo from './pages/LeaveDemo'
import Groups from './pages/Groups'

export const menuItems = [
    {key: '/leave', icon: <UserOutlined/>, label: '请假流程'},
    {key: '/groups', icon: <TeamOutlined/>, label: '引擎分组'},
]

const routeTable = [
    {path: 'leave', Component: LeaveDemo},
    {path: 'groups', Component: Groups},
]
export {routeTable}
export {default as LeaveDemo} from './pages/LeaveDemo'
export {default as Groups} from './pages/Groups'
