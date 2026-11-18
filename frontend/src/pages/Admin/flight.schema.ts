import { z } from 'zod'
import type { FlightPayload } from '../../api/flights'

const WHOLE_NUMBER = /^\d+$/

export const flightSchema = z
  .object({
    airline: z.string().trim().min(1, 'Ingresá la aerolínea'),
    origin: z.string().min(1, 'Elegí el origen'),
    destination: z.string().min(1, 'Elegí el destino'),
    flightDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Ingresá la fecha'),
    departureTime: z.string().regex(/^([01]\d|2[0-3]):[0-5]\d$/, 'Ingresá la hora de salida'),
    capacity: z
      .string()
      .regex(WHOLE_NUMBER, 'Ingresá un número entero')
      .refine((value) => Number(value) > 0, 'La capacidad debe ser mayor a 0'),
    availability: z.string().regex(WHOLE_NUMBER, 'Ingresá un número entero'),
  })
  .refine((values) => !values.origin || values.origin !== values.destination, {
    path: ['destination'],
    message: 'El destino debe ser distinto del origen',
  })
  .refine((values) => Number(values.availability) <= Number(values.capacity), {
    path: ['availability'],
    message: 'Los lugares disponibles no pueden superar la capacidad',
  })

export type FlightFormValues = z.infer<typeof flightSchema>

export function toFlightPayload(values: FlightFormValues): FlightPayload {
  return {
    airline: values.airline.trim(),
    origin: values.origin,
    destination: values.destination,
    flightDate: values.flightDate,
    departureTime: values.departureTime,
    capacity: Number(values.capacity),
    availability: Number(values.availability),
  }
}
