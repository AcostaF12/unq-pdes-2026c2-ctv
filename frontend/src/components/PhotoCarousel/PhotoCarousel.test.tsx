import { describe, expect, it } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { photosForCity } from '../../data/cityPhotos'
import { PhotoCarousel } from './PhotoCarousel'

describe('PhotoCarousel', () => {
  it('renders the first destination photo', () => {
    render(<PhotoCarousel cityCode="PAR" alt="Hotel Palacio" />)

    expect(screen.getByRole('img')).toHaveAttribute('src', photosForCity('PAR')[0])
    expect(screen.getByRole('region', { name: 'Hotel Palacio' })).toBeInTheDocument()
  })

  it('goes to the next and previous photo', async () => {
    const user = userEvent.setup()
    const photos = photosForCity('PAR')
    render(<PhotoCarousel cityCode="PAR" alt="Hotel Palacio" />)

    await user.click(screen.getByRole('button', { name: 'Foto siguiente' }))
    expect(screen.getByRole('img')).toHaveAttribute('src', photos[1])

    await user.click(screen.getByRole('button', { name: 'Foto anterior' }))
    expect(screen.getByRole('img')).toHaveAttribute('src', photos[0])
  })

  it('wraps to the last photo from the first', async () => {
    const user = userEvent.setup()
    const photos = photosForCity('PAR')
    render(<PhotoCarousel cityCode="PAR" alt="Hotel Palacio" />)

    await user.click(screen.getByRole('button', { name: 'Foto anterior' }))
    expect(screen.getByRole('img')).toHaveAttribute('src', photos[photos.length - 1])
  })

  it('jumps to a photo from its dot', async () => {
    const user = userEvent.setup()
    const photos = photosForCity('PAR')
    render(<PhotoCarousel cityCode="PAR" alt="Hotel Palacio" />)

    await user.click(screen.getByRole('button', { name: 'Ir a la foto 3' }))
    expect(screen.getByRole('img')).toHaveAttribute('src', photos[2])
  })

  it('moves with the arrow keys', async () => {
    const user = userEvent.setup()
    const photos = photosForCity('PAR')
    render(<PhotoCarousel cityCode="PAR" alt="Hotel Palacio" />)

    screen.getByRole('region', { name: 'Hotel Palacio' }).focus()
    await user.keyboard('{ArrowRight}')
    expect(screen.getByRole('img')).toHaveAttribute('src', photos[1])

    await user.keyboard('{ArrowLeft}')
    expect(screen.getByRole('img')).toHaveAttribute('src', photos[0])
  })

  it('falls back to the city code when every photo fails', () => {
    const photos = photosForCity('PAR')
    render(<PhotoCarousel cityCode="PAR" alt="Hotel Palacio" />)

    for (let i = 0; i < photos.length; i += 1) {
      fireEvent.error(screen.getByRole('img'))
    }

    expect(screen.queryByRole('img')).not.toBeInTheDocument()
    expect(screen.getByText('PAR')).toBeInTheDocument()
  })
})
