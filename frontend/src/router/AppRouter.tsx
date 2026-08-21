import { createBrowserRouter, Navigate, RouterProvider } from 'react-router-dom'
import { Login } from '../pages/Login/Login'
import { Register } from '../pages/Register/Register'
import { NotFound } from '../pages/NotFound/NotFound'
import { ErrorPage } from '../pages/Error/ErrorPage'

const router = createBrowserRouter([
  {
    path: '/',
    errorElement: <ErrorPage />,
    children: [
      { index: true, element: <Navigate to="/login" replace /> },
      { path: 'login', element: <Login /> },
      { path: 'register', element: <Register /> },
      { path: 'test-error', element: <ErrorPage /> },
      { path: '*', element: <NotFound /> },
    ],
  },
])

export function AppRouter() {
  return <RouterProvider router={router} />
}
