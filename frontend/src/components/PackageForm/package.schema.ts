import { z } from 'zod'
import type { TravelPackagePayload } from '../../api/packages'

export function buildPackageSchema(requireAgency: boolean) {
  return z.object({
    name: z.string().trim().min(1, 'Ingresá el nombre'),
    agencyId: requireAgency ? z.string().min(1, 'Elegí una agencia') : z.string(),
    hotelId: z.string().min(1, 'Elegí un hotel'),
    outboundFlightId: z.string().min(1, 'Elegí el vuelo de ida'),
    returnFlightId: z.string().min(1, 'Elegí el vuelo de vuelta'),
    price: z
      .string()
      .trim()
      .min(1, 'Ingresá el precio')
      .refine((value) => Number(value) > 0, 'El precio debe ser mayor a 0'),
  })
}

export type PackageFormValues = z.infer<ReturnType<typeof buildPackageSchema>>

export function toPackagePayload(values: PackageFormValues, includeAgency: boolean): TravelPackagePayload {
  return {
    name: values.name.trim(),
    hotelId: Number(values.hotelId),
    outboundFlightId: Number(values.outboundFlightId),
    returnFlightId: Number(values.returnFlightId),
    price: Number(values.price),
    ...(includeAgency ? { agencyId: Number(values.agencyId) } : {}),
  }
}
