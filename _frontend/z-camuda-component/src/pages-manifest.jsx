import { HomeOutlined, ReadOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons'
import LeaveDemo from './pages/LeaveDemo'
import Groups from './pages/Groups'


export {default as LeaveDemo} from './pages/LeaveDemo'
export {default as Groups} from './pages/Groups'
import HomePage from './pages/HomePage'
import ComponentDocPage from './pages/ComponentDoc.jsx'

/** 菜单 + 路由清单（lead 008 §10/§14/§16 批量落地）。App 壳在 suit 侧组装。 */
export const appMeta = { title: 'z-camuda 流程引擎', short: 'z-camuda' }

export const menuItems = [
    { key: '/z-camuda/home', label: '首页', icon: <HomeOutlined /> },
    { key: '/z-camuda/leave', label: '请假流程', icon: <UserOutlined /> },
    { key: '/z-camuda/groups', label: '引擎分组', icon: <TeamOutlined /> },
    { key: '/z-camuda/docs', label: '组件文档', icon: <ReadOutlined /> },
]

export const routes = [
    { path: '/z-camuda/home', Component: HomePage },
    { path: '/z-camuda/leave', Component: LeaveDemo },
    { path: '/z-camuda/groups', Component: Groups },
    { path: '/z-camuda/docs', Component: ComponentDocPage },
]

export { default as HomePage } from './pages/HomePage'
export { default as LoginPage } from './pages/LoginPage'
// 主壳接线用：文档页统一以 ComponentDocPage 这个名字从主入口导出。
export { default as ComponentDocPage } from './pages/ComponentDoc.jsx'
