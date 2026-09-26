import { useState } from 'react'
import type { PackageSearchParams } from '../../../api/packages'
import {
  buildPackageSearchFilters,
  createSearchFormValues,
  type PackageSearchField,
  type PackageSearchFormValues,
} from './packageSearchFilters'

export function usePackageSearch(initialOrigin = '') {
  const [formValues, setFormValues] = useState<PackageSearchFormValues>(() => createSearchFormValues(initialOrigin))
  const [filters, setFilters] = useState<PackageSearchParams>(() => buildPackageSearchFilters(createSearchFormValues(initialOrigin)).filters)
  const [error, setError] = useState('')

  const updateField = (field: PackageSearchField, value: string) => {
    setFormValues((current) => ({ ...current, [field]: value }))
    if (error) {
      setError('')
    }
  }

  const submit = () => {
    const result = buildPackageSearchFilters(formValues)
    if (result.error) {
      setError(result.error)
      return false
    }
    setError('')
    setFilters(result.filters)
    return true
  }

  const clear = () => {
    setFormValues(createSearchFormValues())
    setError('')
    setFilters({})
  }

  return { formValues, filters, error, updateField, submit, clear }
}
