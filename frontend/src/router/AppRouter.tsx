import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom'
import { Login } from '../pages/Login/Login'
import { Register } from '../pages/Register/Register'
import { NotFound } from '../pages/NotFound/NotFound'
import { ErrorPage } from '../pages/Error/ErrorPage'
import { AppShell } from '../components/AppShell/AppShell'
import { RequireAuth } from '../auth/RequireAuth'
import { RequireAdmin } from '../auth/RequireAdmin'
import { RequireAgency, RequireBuyer } from '../auth/RequireRole'
import { RequireGuest } from '../auth/RequireGuest'
import { HOME_PATH } from './paths'
import { lazyPage } from './lazyPage'

const PackageList = lazyPage(() => import('../pages/Packages/PackageList'), 'PackageList')
const PackageDetail = lazyPage(() => import('../pages/Packages/PackageDetail'), 'PackageDetail')
const Favorites = lazyPage(() => import('../pages/Favorites/Favorites'), 'Favorites')
const Purchases = lazyPage(() => import('../pages/Purchases/Purchases'), 'Purchases')
const AgencyPackages = lazyPage(() => import('../pages/Agency/AgencyPackages'), 'AgencyPackages')
const AgencySales = lazyPage(() => import('../pages/Purchases/AgencySales'), 'AgencySales')
const Hotels = lazyPage(() => import('../pages/Hotels/Hotels'), 'Hotels')
const HotelDetail = lazyPage(() => import('../pages/HotelDetail/HotelDetail'), 'HotelDetail')
const HotelForm = lazyPage(() => import('../pages/HotelForm/HotelForm'), 'HotelForm')
const Profile = lazyPage(() => import('../pages/Profile/Profile'), 'Profile')

const router = createBrowserRouter([
  {
    path: '/',
    errorElement: <ErrorPage />,
    children: [
      { index: true, element: <Navigate to={HOME_PATH} replace /> },
      {
        path: 'login',
        element: (
          <RequireGuest>
            <Login />
          </RequireGuest>
        ),
      },
      {
        path: 'register',
        element: (
          <RequireGuest>
            <Register />
          </RequireGuest>
        ),
      },
      {
        element: (
          <RequireAuth>
            <AppShell />
          </RequireAuth>
        ),
        children: [
          { path: 'trips', element: <PackageList /> },
          { path: 'trips/:id', element: <PackageDetail /> },
          {
            path: 'favorites',
            element: (
              <RequireBuyer>
                <Favorites />
              </RequireBuyer>
            ),
          },
          {
            path: 'purchases',
            element: (
              <RequireBuyer>
                <Purchases />
              </RequireBuyer>
            ),
          },
          {
            path: 'agency',
            element: (
              <RequireAgency>
                <AgencyPackages />
              </RequireAgency>
            ),
          },
          {
            path: 'agency/sales',
            element: (
              <RequireAgency>
                <AgencySales />
              </RequireAgency>
            ),
          },
          { path: 'hotels', element: <Hotels /> },
          {
            path: 'hotels/new',
            element: (
              <RequireAdmin>
                <HotelForm />
              </RequireAdmin>
            ),
          },
          { path: 'hotels/:id', element: <HotelDetail /> },
          {
            path: 'hotels/:id/edit',
            element: (
              <RequireAdmin>
                <HotelForm />
              </RequireAdmin>
            ),
          },
          { path: 'profile', element: <Profile /> },
        ],
      },
      { path: 'test-error', element: <ErrorPage /> },
      { path: '*', element: <NotFound /> },
    ],
  },
])

export function AppRouter() {
  return <RouterProvider router={router} />
}
