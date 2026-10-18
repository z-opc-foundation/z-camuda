// 整份转发，不要手挑名字：manifest 还导出 HomePage / LoginPage，
// suit 的 App.jsx 正好 import 这两个，手挑必漏。
export * from './pages-manifest.jsx';
export {configureCamuda} from './services/api.js'
