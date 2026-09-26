import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import type { PackageSearchParams } from '../../../api/packages'
import {
  buildPackageSearchFilters,
  type PackageSearchField,
  type PackageSearchFormValues,
} from './packageSearchFilters'
import { isDefaultSort, sortToQueryParam, type PackageSort } from './packageSearchSort'
import {
  filtersToSearchParams,
  formValuesFromSearchParams,
  pageFromSearchParams,
  sortFromSearchParams,
} from './packageSearchUrl'

export function usePackageSearch(initialOrigin = '') {
  const [searchParams, setSearchParams] = useSearchParams()
  const searchKey = searchParams.toString()
  const [formValues, setFormValues] = useState<PackageSearchFormValues>(() =>
    formValuesFromSearchParams(searchParams, initialOrigin),
  )
  const [error, setError] = useState('')

  // Resets the form whenever the URL changes externally (submit, clear, pagination,
  // sorting or browser back/forward navigation), following React's guidance for
  // adjusting state during render instead of in an effect.
  const [syncedSearchKey, setSyncedSearchKey] = useState(searchKey)
  if (searchKey !== syncedSearchKey) {
    setSyncedSearchKey(searchKey)
    setFormValues(formValuesFromSearchParams(searchParams, initialOrigin))
  }

  const filters = useMemo(
    () => buildPackageSearchFilters(formValuesFromSearchParams(searchParams, initialOrigin)).filters,
    [searchParams, initialOrigin],
  )
  const page = useMemo(() => pageFromSearchParams(searchParams), [searchParams])
  const sort = useMemo(() => sortFromSearchParams(searchParams), [searchParams])

  const query = useMemo<PackageSearchParams>(
    () => ({
      ...filters,
      ...(page > 0 ? { page } : {}),
      ...(isDefaultSort(sort) ? {} : { sort: sortToQueryParam(sort) }),
    }),
    [filters, page, sort],
  )

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
    const nextParams = filtersToSearchParams(result.filters)
    if (!isDefaultSort(sort)) {
      nextParams.set('sort', sortToQueryParam(sort))
    }
    setSearchParams(nextParams)
    return true
  }

  const clear = () => {
    setError('')
    setSearchParams(new URLSearchParams())
  }

  const setPage = (nextPage: number) => {
    const nextParams = new URLSearchParams(searchParams)
    if (nextPage > 0) {
      nextParams.set('page', String(nextPage))
    } else {
      nextParams.delete('page')
    }
    setSearchParams(nextParams)
  }

  const setSort = (nextSort: PackageSort) => {
    const nextParams = new URLSearchParams(searchParams)
    if (isDefaultSort(nextSort)) {
      nextParams.delete('sort')
    } else {
      nextParams.set('sort', sortToQueryParam(nextSort))
    }
    nextParams.delete('page')
    setSearchParams(nextParams)
  }

  return { formValues, filters, query, page, sort, error, updateField, submit, clear, setPage, setSort }
}
