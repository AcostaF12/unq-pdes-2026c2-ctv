import { describe, expect, it } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { photosForCity } from '../../data/cityPhotos'
import { HotelPhoto } from './HotelPhoto'

describe('HotelPhoto', () => {
  it('renders the hotel url when it looks usable', () => {
    render(
      <HotelPhoto src="https://example.com/hotel.jpg" alt="Hotel Palacio" cityCode="PAR" />,
    )

    expect(screen.getByRole('img', { name: 'Hotel Palacio' })).toHaveAttribute(
      'src',
      'https://example.com/hotel.jpg',
    )
    expect(screen.getByText('PAR')).toBeInTheDocument()
  })

  it('skips the demo host and shows a city photo instead', () => {
    render(
      <HotelPhoto
        src="https://images.ctv.demo/hotels/real-de-londres.jpg"
        alt="Hotel Real"
        cityCode="LON"
      />,
    )

    const img = screen.getByRole('img', { name: 'Hotel Real' })
    expect(img).toHaveAttribute('src', photosForCity('LON')[0])
    expect(img.getAttribute('src')).not.toContain('ctv.demo')
  })

  it('uses a city photo when the src is empty', () => {
    render(<HotelPhoto src="" alt="Hotel Real" cityCode="LON" />)

    expect(screen.getByRole('img', { name: 'Hotel Real' })).toHaveAttribute(
      'src',
      photosForCity('LON')[0],
    )
  })

  it('falls back to the city photo if the hotel url fails', () => {
    render(
      <HotelPhoto src="https://example.com/broken.jpg" alt="Hotel Palacio" cityCode="PAR" />,
    )

    fireEvent.error(screen.getByRole('img'))

    expect(screen.getByRole('img', { name: 'Hotel Palacio' })).toHaveAttribute(
      'src',
      photosForCity('PAR')[0],
    )
  })

  it('falls back to the IATA code if the city photo also fails', () => {
    render(<HotelPhoto src="" alt="Hotel Palacio" cityCode="PAR" />)

    fireEvent.error(screen.getByRole('img'))

    expect(screen.queryByRole('img')).not.toBeInTheDocument()
    expect(screen.getByText('PAR')).toBeInTheDocument()
  })
})
