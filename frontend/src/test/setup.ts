import '@testing-library/jest-dom/vitest'
import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'
import { queryClient } from '../query/queryClient'

afterEach(() => {
  cleanup()
  localStorage.clear()
  queryClient.clear()
})
