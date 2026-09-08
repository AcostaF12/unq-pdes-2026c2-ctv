import { z } from 'zod'

export const hotelSchema = z.object({
  name: z.string().trim().min(1, 'Ingresá el nombre'),
  cityCode: z.string().trim().min(1, 'Elegí una ciudad'),
  photoUrl: z.string().trim().min(1, 'Ingresá la URL de la foto'),
})

export type HotelFormValues = z.infer<typeof hotelSchema>
