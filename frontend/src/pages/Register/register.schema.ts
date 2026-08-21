import { z } from 'zod'

export const registerSchema = z
  .object({
    firstName: z.string().trim().min(1, 'Ingresá tu nombre'),
    lastName: z.string().trim().min(1, 'Ingresá tu apellido'),
    username: z.string().trim().min(1, 'Ingresá un usuario'),
    password: z
      .string()
      .min(1, 'Ingresá una contraseña')
      .min(6, 'Debe tener al menos 6 caracteres'),
    confirmPassword: z.string().min(1, 'Confirmá tu contraseña'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Las contraseñas no coinciden',
    path: ['confirmPassword'],
  })

export type RegisterFormValues = z.infer<typeof registerSchema>
