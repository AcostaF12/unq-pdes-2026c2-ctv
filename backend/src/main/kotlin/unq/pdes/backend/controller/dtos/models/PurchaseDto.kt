package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.Purchase
import java.math.BigDecimal
import java.time.LocalDateTime

data class PurchaseDto(
    val id: Long?,
    val buyer: UserDto,
    val travelPackage: TravelPackageDto,
    val purchasePrice: BigDecimal,
    val purchasedAt: LocalDateTime,
) {
    companion object {
        fun fromModel(purchase: Purchase): PurchaseDto {
            return PurchaseDto(
                id = purchase.id,
                buyer = UserDto.fromModel(purchase.buyer),
                travelPackage = TravelPackageDto.fromModel(purchase.travelPackage),
                purchasePrice = purchase.purchasePrice,
                purchasedAt = purchase.purchasedAt,
            )
        }
    }
}
