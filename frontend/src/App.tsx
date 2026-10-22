import { Suspense } from 'react'
import { AppRouter } from './router/AppRouter'
import { AuthProvider } from './auth/AuthContext'
import { PageFallback } from './components/PageFallback/PageFallback'
import { ToastProvider } from './components/Toast/ToastProvider'
import { QueryProvider } from './query/QueryProvider'

function App() {
  return (
    <QueryProvider>
      <AuthProvider>
        <ToastProvider>
          <Suspense fallback={<PageFallback />}>
            <AppRouter />
          </Suspense>
        </ToastProvider>
      </AuthProvider>
    </QueryProvider>
  )
}

export default App
