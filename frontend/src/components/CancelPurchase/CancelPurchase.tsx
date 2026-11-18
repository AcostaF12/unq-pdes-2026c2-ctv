import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { getApiErrorMessage } from '../../api/errors'
import { cancelPurchase } from '../../api/purchases'
import { queryKeys } from '../../query/keys'
import { Button } from '../Button/Button'
import { useToast } from '../Toast/ToastProvider'
import './CancelPurchase.css'

interface CancelPurchaseProps {
  purchaseId: number
  packageName: string
}

/** "Cancelar compra" with an explicit confirmation step. The purchase is kept as CANCELLED. */
export function CancelPurchase({ purchaseId, packageName }: CancelPurchaseProps) {
  const queryClient = useQueryClient()
  const toast = useToast()
  const [confirming, setConfirming] = useState(false)

  const mutation = useMutation({
    mutationFn: () => cancelPurchase(purchaseId),
    onSuccess: async () => {
      setConfirming(false)
      toast.show('Compra cancelada. Liberamos tus asientos.')
      // Histories, detail and "ya compraste" checks, plus seat availability shown in packages and flights.
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.purchases.all }),
        queryClient.invalidateQueries({ queryKey: ['admin', 'sales'] }),
        queryClient.invalidateQueries({ queryKey: queryKeys.packages.all }),
        queryClient.invalidateQueries({ queryKey: queryKeys.flights.all }),
      ])
    },
  })

  if (!confirming) {
    return (
      <Button
        type="button"
        variant="ghost"
        onClick={() => {
          mutation.reset()
          setConfirming(true)
        }}
      >
        Cancelar compra
      </Button>
    )
  }

  const error = mutation.isError
    ? (getApiErrorMessage(mutation.error) ?? 'No pudimos cancelar la compra. Intentá de nuevo.')
    : null

  return (
    <div className="cancel-purchase" role="group" aria-label={`Confirmar cancelación de ${packageName}`}>
      <p>
        ¿Cancelar la compra de <strong>{packageName}</strong>? Liberamos tus asientos de ida y vuelta. La compra
        queda en tu historial como cancelada.
      </p>
      {error ? (
        <p className="cancel-purchase__error" role="alert">
          {error}
        </p>
      ) : null}
      <div className="page__actions">
        <Button type="button" loading={mutation.isPending} onClick={() => mutation.mutate()}>
          Confirmar cancelación
        </Button>
        <Button
          type="button"
          variant="ghost"
          autoFocus
          disabled={mutation.isPending}
          onClick={() => setConfirming(false)}
        >
          Volver
        </Button>
      </div>
    </div>
  )
}
