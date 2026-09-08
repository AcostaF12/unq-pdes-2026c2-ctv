import { lazy, type ComponentType } from 'react'

export function lazyPage<TModule extends Record<string, ComponentType>>(
  importer: () => Promise<TModule>,
  exportName: keyof TModule & string,
) {
  return lazy(async () => {
    const module = await importer()
    return { default: module[exportName] }
  })
}
