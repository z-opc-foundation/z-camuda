import {Navigate, Route, Routes} from 'react-router-dom'
import {AppLayout} from '@yuku123/z-frontend-common'
import {menuItems, routeTable} from '@yuku123/z-camuda-component/pages'

export default function App() {
    return (
        <Routes>
            <Route path="/" element={<Navigate to="/leave" replace/>}/>
            <Route path="/" element={
                <AppLayout menuItems={menuItems} appTitle="z-camuda 流程引擎" appShort="CAM"/>
            }>
                {routeTable.map((r) => (
                    <Route key={r.path} path={r.path} element={<r.Component/>}/>
                ))}
            </Route>
        </Routes>
    )
}
