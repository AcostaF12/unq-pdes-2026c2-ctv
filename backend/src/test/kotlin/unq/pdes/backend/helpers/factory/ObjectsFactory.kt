package unq.pdes.backend.helpers.factory

import unq.pdes.backend.model.City
import unq.pdes.backend.model.Hotel

open class ObjectsFactory {
    open fun anyCity(): City = City("BUE", "Buenos Aires")

    open fun cityWith(
        code: String,
        name: String,
    ): City = City(code, name)

    open fun anyHotel(): Hotel = hotelIn(anyCity())

    open fun hotelNamed(name: String): Hotel =
        Hotel
            .Builder()
            .name(name)
            .city(anyCity())
            .photoUrl(SOME_PHOTO_URL)
            .build()

    open fun hotelIn(city: City): Hotel =
        Hotel
            .Builder()
            .name("Some hotel name")
            .city(city)
            .photoUrl(SOME_PHOTO_URL)
            .build()

    companion object {
        const val SOME_PHOTO_URL = "https://images.ctv.demo/hotels/some-hotel.jpg"
    }
}
