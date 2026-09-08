import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { BrandMark } from './BrandMark'

describe('BrandMark', () => {
  it('renders the CTV wordmark', () => {
    render(<BrandMark />)
    expect(screen.getByText('CTV')).toBeInTheDocument()
    expect(screen.getByText('Compra Tu Viaje')).toBeInTheDocument()
    expect(document.querySelector('.brand-mark__mark')).toHaveAttribute('src', '/logo.png')
  })
})
