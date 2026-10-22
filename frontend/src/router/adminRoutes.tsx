import { Outlet, type RouteObject } from 'react-router-dom'
import { RequireAdmin } from '../auth/RequireAdmin'
import { lazyPage } from './lazyPage'

const AdminHome = lazyPage(() => import('../pages/Admin/AdminHome'), 'AdminHome')
const AdminPackages = lazyPage(() => import('../pages/Admin/AdminPackages'), 'AdminPackages')
const AdminFlights = lazyPage(() => import('../pages/Admin/AdminFlights'), 'AdminFlights')
const AdminUsers = lazyPage(() => import('../pages/Admin/AdminUsers'), 'AdminUsers')
const AdminAgencies = lazyPage(() => import('../pages/Admin/AdminAgencies'), 'AdminAgencies')
const AdminSales = lazyPage(() => import('../pages/Admin/AdminSales'), 'AdminSales')
const PurchaseDetail = lazyPage(() => import('../pages/Purchases/PurchaseDetail'), 'PurchaseDetail')

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
    { path: 'users', element: <AdminUsers /> },
    { path: 'agencies', element: <AdminAgencies /> },
    { path: 'sales', element: <AdminSales /> },
    { path: 'sales/:id', element: <PurchaseDetail /> },
  ],
}
