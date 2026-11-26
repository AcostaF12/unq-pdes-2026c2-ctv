import { screen } from '@testing-library/react'
import type { UserEvent } from '@testing-library/user-event'
import { describeFlight } from '../components/FlightPicker/flightOptions'
import { outboundToParis, parisHotel, returnFromParis } from './fixtures'

export async function fillParisPackage(user: UserEvent, name: string, price: string) {
  await user.type(screen.getByLabelText('Nombre'), name)
  await user.type(screen.getByLabelText('Precio'), price)
  await user.selectOptions(await screen.findByLabelText('Hotel'), String(parisHotel.id))
  await user.click(await screen.findByRole('radio', { name: describeFlight(outboundToParis) }))
  await user.click(await screen.findByRole('radio', { name: describeFlight(returnFromParis) }))
}
