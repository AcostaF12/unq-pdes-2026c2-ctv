import { Suspense } from 'react'
import { AppRouter } from './router/AppRouter'
import { AuthProvider } from './auth/AuthContext'
import { PageFallback } from './components/PageFallback/PageFallback'
import { QueryProvider } from './query/QueryProvider'

function App() {
  return (
    <QueryProvider>
      <AuthProvider>
        <Suspense fallback={<PageFallback />}>
          <AppRouter />
        </Suspense>
      </AuthProvider>
    </QueryProvider>
  )
}

export default App
