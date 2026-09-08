import { z } from 'zod'

export const profileSchema = z.object({
  firstName: z.string().trim().min(1, 'Ingresá tu nombre'),
  lastName: z.string().trim().min(1, 'Ingresá tu apellido'),
})

export type ProfileFormValues = z.infer<typeof profileSchema>

export const preferencesSchema = z.object({
  originCityCode: z.string(),
  maxBudget: z
    .string()
    .refine(
      (value) => value.trim() === '' || (!Number.isNaN(Number(value)) && Number(value) > 0),
      'Ingresá un monto válido',
    ),
  applyOriginOnSearch: z.boolean(),
})

export type PreferencesFormValues = z.infer<typeof preferencesSchema>

export const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Ingresá tu contraseña actual'),
    newPassword: z
      .string()
      .min(1, 'Ingresá una contraseña nueva')
      .min(6, 'Debe tener al menos 6 caracteres'),
    confirmPassword: z.string().min(1, 'Confirmá tu contraseña nueva'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Las contraseñas no coinciden',
    path: ['confirmPassword'],
  })

export type PasswordFormValues = z.infer<typeof passwordSchema>
