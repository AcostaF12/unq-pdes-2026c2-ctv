import { Outlet, type RouteObject } from 'react-router-dom'
import { RequireAdmin } from '../auth/RequireAdmin'
import { lazyPage } from './lazyPage'

const AdminHome = lazyPage(() => import('../pages/Admin/AdminHome'), 'AdminHome')
const AdminPackages = lazyPage(() => import('../pages/Admin/AdminPackages'), 'AdminPackages')
const AdminFlights = lazyPage(() => import('../pages/Admin/AdminFlights'), 'AdminFlights')
const AdminAgencies = lazyPage(() => import('../pages/Admin/AdminAgencies'), 'AdminAgencies')

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
  ],
}
