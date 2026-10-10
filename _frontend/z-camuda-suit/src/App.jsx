import {Navigate, Route, Routes} from 'react-router-dom'
import {AppLayout} from '../../../../_shared/z-frontend-common-local/dist/z-frontend-common.es.js'
import {menuItems, routeTable} from '@yuku123/z-camuda-component/pages'

export default function App() {
    return (
        <Routes>
            <Route path="/" element={<Navigate to="/leave" replace/>}/>
            <Route path="/" element={
                <AppLayout menuItems={menuItems} appTitle="z-camuda 流程引擎" appShort="CAM" appIcon={{icon: <img src="/icon.png" alt="CAM" style={{width: "100%", height: "100%", objectFit: "cover", borderRadius: 8}}/>, color: '#1d4ed8', label: 'CAM'}}/>
            }>
                {routeTable.map((r) => (
                    <Route key={r.path} path={r.path} element={<r.Component/>}/>
                ))}
            </Route>
        </Routes>
    )
}
