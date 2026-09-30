import type { City } from '../../../api/cities'
import { Button } from '../../../components/Button/Button'
import { Input } from '../../../components/Input/Input'
import { Select } from '../../../components/Select/Select'
import type { PackageSearchField, PackageSearchFormValues } from './packageSearchFilters'
import { PACKAGE_SORT_OPTIONS, sortToOptionValue, type PackageSort } from './packageSearchSort'

interface PackageSearchFormProps {
  cities: City[]
  values: PackageSearchFormValues
  error: string
  sort: PackageSort
  onChange: (field: PackageSearchField, value: string) => void
  onSortChange: (value: string) => void
  onSubmit: () => void
}

export function PackageSearchForm({ cities, values, error, sort, onChange, onSortChange, onSubmit }: PackageSearchFormProps) {
  return (
    <form
      className="catalog__search"
      onSubmit={(event) => {
        event.preventDefault()
        onSubmit()
      }}
    >
      <div className="catalog__search-copy">
        <h2>Buscá tu viaje</h2>
        <p>Filtrá por nombre, ciudad de origen o destino, y rango de precio.</p>
      </div>
      <div className="catalog__filters">
        <Input label="Nombre" value={values.name} onChange={(event) => onChange('name', event.target.value)} />
        <Select label="Origen" value={values.origin} onChange={(event) => onChange('origin', event.target.value)}>
          <option value="">Todos</option>
          {cities.map((city) => (
            <option key={`origin-${city.code}`} value={city.code}>
              {city.name}
            </option>
          ))}
        </Select>
        <Select label="Destino" value={values.destination} onChange={(event) => onChange('destination', event.target.value)}>
          <option value="">Todos</option>
          {cities.map((city) => (
            <option key={`destination-${city.code}`} value={city.code}>
              {city.name}
            </option>
          ))}
        </Select>
        <Input
          label="Precio mínimo"
          type="number"
          min="0"
          step="0.01"
          inputMode="decimal"
          value={values.minPrice}
          onChange={(event) => onChange('minPrice', event.target.value)}
        />
        <Input
          label="Precio máximo"
          type="number"
          min="0"
          step="0.01"
          inputMode="decimal"
          value={values.maxPrice}
          onChange={(event) => onChange('maxPrice', event.target.value)}
          error={error}
        />
        <Select
          label="Ordenar por"
          value={sortToOptionValue(sort)}
          onChange={(event) => onSortChange(event.target.value)}
        >
          {PACKAGE_SORT_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </Select>
        <Button type="submit" className="catalog__filters-submit">
          Buscar
        </Button>
      </div>
    </form>
  )
}
