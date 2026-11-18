import '@testing-library/jest-dom/vitest'
import { afterEach, vi } from 'vitest'
import { cleanup } from '@testing-library/react'
import { queryClient } from '../query/queryClient'

// jsdom does not implement scrolling; AppShell scrolls to top on navigation.
window.scrollTo = vi.fn()

afterEach(() => {
  cleanup()
  localStorage.clear()
  queryClient.clear()
})
