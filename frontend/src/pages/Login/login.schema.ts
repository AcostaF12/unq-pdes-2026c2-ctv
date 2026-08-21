import { z } from 'zod'

export const loginSchema = z.object({
  username: z.string().trim().min(1, 'Ingresá tu usuario'),
  password: z
    .string()
    .min(1, 'Ingresá tu contraseña')
    .min(6, 'Debe tener al menos 6 caracteres'),
})

export type LoginFormValues = z.infer<typeof loginSchema>
