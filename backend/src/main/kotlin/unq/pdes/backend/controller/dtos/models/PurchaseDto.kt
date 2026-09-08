package unq.pdes.backend.controller.dtos.models

import unq.pdes.backend.model.Purchase
import java.math.BigDecimal
import java.time.LocalDateTime

data class PurchaseDto(
    val id: Long?,
    val packageId: Long?,
    val packageName: String,
    val agency: AgencyDto,
    val purchasePrice: BigDecimal,
    val purchasedAt: LocalDateTime,
) {
    companion object {
        fun fromModel(purchase: Purchase): PurchaseDto {
            return PurchaseDto(
                id = purchase.id,
                packageId = purchase.travelPackage.id,
                packageName = purchase.travelPackage.name,
                agency = AgencyDto.fromModel(purchase.agency),
                purchasePrice = purchase.purchasePrice,
                purchasedAt = purchase.purchasedAt,
            )
        }
    }
}
