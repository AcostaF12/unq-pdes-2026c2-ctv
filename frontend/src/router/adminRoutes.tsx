import { Outlet, type RouteObject } from 'react-router-dom'
import { RequireAdmin } from '../auth/RequireAdmin'
import { lazyPage } from './lazyPage'

const AdminHome = lazyPage(() => import('../pages/Admin/AdminHome'), 'AdminHome')
const AdminPackages = lazyPage(() => import('../pages/Admin/AdminPackages'), 'AdminPackages')
const AdminFlights = lazyPage(() => import('../pages/Admin/AdminFlights'), 'AdminFlights')
const AdminAgencies = lazyPage(() => import('../pages/Admin/AdminAgencies'), 'AdminAgencies')
const AdminUsers = lazyPage(() => import('../pages/Admin/AdminQueries'), 'AdminUsers')
const AdminFavorites = lazyPage(() => import('../pages/Admin/AdminQueries'), 'AdminFavorites')
const AdminReviews = lazyPage(() => import('../pages/Admin/AdminQueries'), 'AdminReviews')
const AdminPurchases = lazyPage(() => import('../pages/Admin/AdminQueries'), 'AdminPurchases')
const AdminReports = lazyPage(() => import('../pages/Admin/AdminReports'), 'AdminReports')

export const adminRoute: RouteObject = {
  path: 'admin',
  element: (
    <RequireAdmin>
      <Outlet />
    </RequireAdmin>
  ),
  children: [
    { index: true, element: <AdminHome /> },
    { path: 'packages', element: <AdminPackages /> },
    { path: 'flights', element: <AdminFlights /> },
    { path: 'agencies', element: <AdminAgencies /> },
    { path: 'users', element: <AdminUsers /> },
    { path: 'favorites', element: <AdminFavorites /> },
    { path: 'reviews', element: <AdminReviews /> },
    { path: 'purchases', element: <AdminPurchases /> },
    { path: 'reports', element: <AdminReports /> },
  ],
}
