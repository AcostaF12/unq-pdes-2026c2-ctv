import { useState } from 'react'
import type { City } from '../../../api/cities'
import type { Agency } from '../../../api/packages'
import { Button } from '../../../components/Button/Button'
import { Input } from '../../../components/Input/Input'
import { Select } from '../../../components/Select/Select'
import {
  hasAdvancedValues,
  type PackageSearchField,
  type PackageSearchFormValues,
} from './packageSearchFilters'

interface PackageSearchFormProps {
  cities: City[]
  agencies: Agency[]
  values: PackageSearchFormValues
  appliedValues: PackageSearchFormValues
  error: string
  onChange: (field: PackageSearchField, value: string) => void
  onQuickDestination: (code: string) => void
  onSubmit: () => void
}

const RATING_OPTIONS = [
  { value: '', label: 'Cualquiera' },
  { value: '6', label: '6 o más' },
  { value: '7', label: '7 o más' },
  { value: '8', label: '8 o más' },
  { value: '9', label: '9 o más' },
]

export function PackageSearchForm({
  cities,
  agencies,
  values,
  appliedValues,
  error,
  onChange,
  onQuickDestination,
  onSubmit,
}: PackageSearchFormProps) {
  const [advancedRequested, setAdvancedRequested] = useState(false)
  const advancedActive = hasAdvancedValues(values)
  const advancedOpen = advancedRequested || advancedActive
  const advancedCount = [values.agencyId, values.hotel, values.minRating].filter(
    (value) => value.trim() !== '',
  ).length
  const destinationCities = cities.filter((city) => city.code !== appliedValues.origin)

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
        <p>Filtrá por nombre, origen o destino, rango de precio, agencia, hotel y puntaje.</p>
      </div>

      {destinationCities.length > 0 ? (
        <div className="catalog__quick" role="group" aria-label="Destinos rápidos">
          <button
            type="button"
            className={
              appliedValues.destination === ''
                ? 'chip chip--toggle chip--active'
                : 'chip chip--toggle'
            }
            aria-pressed={appliedValues.destination === ''}
            onClick={() => onQuickDestination('')}
          >
            Todos los destinos
          </button>
          {destinationCities.map((city) => (
            <button
              key={city.code}
              type="button"
              className={
                appliedValues.destination === city.code
                  ? 'chip chip--toggle chip--active'
                  : 'chip chip--toggle'
              }
              aria-pressed={appliedValues.destination === city.code}
              onClick={() => onQuickDestination(city.code)}
            >
              {city.name}
            </button>
          ))}
        </div>
      ) : null}

      <div className="catalog__filters">
        <Input data-cy="search-name" label="Nombre" value={values.name} onChange={(event) => onChange('name', event.target.value)} />
        <Select data-cy="search-origin" label="Origen" value={values.origin} onChange={(event) => onChange('origin', event.target.value)}>
          <option value="">Todos</option>
          {cities.map((city) => (
            <option key={`origin-${city.code}`} value={city.code}>
              {city.name}
            </option>
          ))}
        </Select>
        <Select data-cy="search-destination" label="Destino" value={values.destination} onChange={(event) => onChange('destination', event.target.value)}>
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
        <Button type="submit" className="catalog__filters-submit">
          <svg className="catalog__filters-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <circle cx="11" cy="11" r="6.5" />
            <path d="M16 16l4 4" />
          </svg>
          Buscar
        </Button>
      </div>

      <div className="catalog__advanced">
        <button
          type="button"
          className="catalog__advanced-toggle"
          aria-expanded={advancedOpen}
          aria-controls="catalog-advanced-filters"
          onClick={() => setAdvancedRequested((open) => !open)}
        >
          <span className="catalog__advanced-caret" aria-hidden="true" />
          Más filtros
          {advancedCount > 0 ? <span className="catalog__advanced-count">{advancedCount}</span> : null}
        </button>
        <div
          id="catalog-advanced-filters"
          className={advancedOpen ? 'catalog__advanced-panel catalog__advanced-panel--open' : 'catalog__advanced-panel'}
        >
          <div className="catalog__advanced-inner" inert={!advancedOpen}>
            <div className="catalog__advanced-grid">
              <Select label="Agencia" value={values.agencyId} onChange={(event) => onChange('agencyId', event.target.value)}>
                <option value="">Todas</option>
                {agencies.map((agency) => (
                  <option key={agency.id} value={String(agency.id)}>
                    {agency.name}
                  </option>
                ))}
              </Select>
              <Input label="Hotel" value={values.hotel} onChange={(event) => onChange('hotel', event.target.value)} />
              <Select label="Puntaje mínimo" value={values.minRating} onChange={(event) => onChange('minRating', event.target.value)}>
                {RATING_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </Select>
            </div>
          </div>
        </div>
      </div>
    </form>
  )
}
