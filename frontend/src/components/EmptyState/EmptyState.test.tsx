import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { EmptyState } from './EmptyState'

describe('EmptyState', () => {
  it('renders an empty status by default', () => {
    render(<EmptyState title="Sin favoritos" message="Todavía no hay nada acá." />)

    expect(screen.getByRole('status')).toBeInTheDocument()
    expect(screen.getByText('Sin favoritos')).toBeInTheDocument()
    expect(screen.getByText('Todavía no hay nada acá.')).toBeInTheDocument()
  })

  it('uses an alert role for error tone', () => {
    render(
      <EmptyState
        tone="error"
        title="Algo salió mal"
        message="No pudimos cargar los paquetes. Intentá de nuevo."
      />,
    )

    expect(screen.getByRole('alert')).toBeInTheDocument()
    expect(screen.getByText('Señal perdida')).toBeInTheDocument()
    expect(screen.getByText('Algo salió mal')).toBeInTheDocument()
    expect(document.querySelector('.empty-state__stamp')).toHaveTextContent('Retraso')
  })
})
