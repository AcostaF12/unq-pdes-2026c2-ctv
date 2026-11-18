import type { Agency } from '../../../api/packages'
import type { City } from '../../../api/cities'
import type { PackageSearchFormValues } from './packageSearchFilters'

export interface PackageFilterChip {
  key: string
  label: string
  /** Form values to reset when the chip is dismissed. */
  clear: Partial<PackageSearchFormValues>
}

function cityName(cities: City[], code: string): string {
  return cities.find((city) => city.code === code)?.name ?? code
}

export function buildPackageFilterChips(
  values: PackageSearchFormValues,
  cities: City[],
  agencies: Agency[],
): PackageFilterChip[] {
  const chips: PackageFilterChip[] = []
  const text = (value: string) => value.trim()

  if (text(values.name)) {
    chips.push({ key: 'name', label: `Nombre: ${text(values.name)}`, clear: { name: '' } })
  }
  if (text(values.origin)) {
    chips.push({
      key: 'origin',
      label: `Desde ${cityName(cities, text(values.origin))}`,
      clear: { origin: '' },
    })
  }
  if (text(values.destination)) {
    chips.push({
      key: 'destination',
      label: `Hacia ${cityName(cities, text(values.destination))}`,
      clear: { destination: '' },
    })
  }
  if (text(values.minPrice)) {
    chips.push({
      key: 'minPrice',
      label: `Desde USD ${text(values.minPrice)}`,
      clear: { minPrice: '' },
    })
  }
  if (text(values.maxPrice)) {
    chips.push({
      key: 'maxPrice',
      label: `Hasta USD ${text(values.maxPrice)}`,
      clear: { maxPrice: '' },
    })
  }
  if (text(values.agencyId)) {
    const agency = agencies.find((candidate) => String(candidate.id) === text(values.agencyId))
    chips.push({
      key: 'agencyId',
      label: `Agencia: ${agency?.name ?? text(values.agencyId)}`,
      clear: { agencyId: '' },
    })
  }
  if (text(values.hotel)) {
    chips.push({ key: 'hotel', label: `Hotel: ${text(values.hotel)}`, clear: { hotel: '' } })
  }
  if (text(values.minRating)) {
    chips.push({
      key: 'minRating',
      label: `Puntaje ${text(values.minRating)}+`,
      clear: { minRating: '' },
    })
  }
  return chips
}
