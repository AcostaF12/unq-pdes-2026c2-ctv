import { screen } from '@testing-library/react'
import type { UserEvent } from '@testing-library/user-event'
import { flightChoiceLabel } from '../components/FlightPicker/flightOptions'
import { outboundToParis, parisHotel, returnFromParis } from './fixtures'

export async function fillParisPackage(user: UserEvent, name: string, price: string) {
  await user.type(screen.getByLabelText('Nombre'), name)
  await user.selectOptions(screen.getByLabelText('Hotel'), String(parisHotel.id))
  await screen.findByRole('option', { name: flightChoiceLabel(outboundToParis) })
  await user.selectOptions(screen.getByLabelText('Vuelo de ida'), String(outboundToParis.id))
  await screen.findByRole('option', { name: flightChoiceLabel(returnFromParis) })
  await user.selectOptions(screen.getByLabelText('Vuelo de vuelta'), String(returnFromParis.id))
  await user.type(screen.getByLabelText('Precio'), price)
}
