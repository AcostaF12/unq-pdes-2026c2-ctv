package unq.pdes.backend.search

import org.springframework.data.jpa.domain.Specification
import unq.pdes.backend.model.Purchase
import unq.pdes.backend.model.history.PurchaseTravelSnapshot
import unq.pdes.backend.model.user.User
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Locale

object PurchaseHistorySearchSpecification {
    fun forBuyer(
        buyerId: Long,
        criteria: PurchaseHistorySearchCriteria,
    ): Specification<Purchase> =
        Specification { root, _, criteriaBuilder ->
            val snapshot = root.get<PurchaseTravelSnapshot>("travelSnapshot")
            val predicates =
                buildList {
                    add(criteriaBuilder.equal(root.get<User>("buyer").get<Long>("id"), buyerId))
                    criteria.packageName?.let { name ->
                        add(criteriaBuilder.like(criteriaBuilder.lower(snapshot.get<String>("name")), "%${name.lowercase(Locale.ROOT)}%"))
                    }
                    criteria.destination?.let { destination ->
                        add(criteriaBuilder.equal(snapshot.get<String>("destinationCode"), destination))
                    }
                    criteria.from?.let { from ->
                        add(criteriaBuilder.greaterThanOrEqualTo(root.get<LocalDateTime>("purchasedAt"), from.atStartOfDay()))
                    }
                    criteria.to?.let { to ->
                        add(criteriaBuilder.lessThan(root.get<LocalDateTime>("purchasedAt"), to.plusDays(1).atStartOfDay()))
                    }
                    criteria.minPrice?.let { minPrice ->
                        add(criteriaBuilder.greaterThanOrEqualTo(root.get<BigDecimal>("purchasePrice"), minPrice))
                    }
                    criteria.maxPrice?.let { maxPrice ->
                        add(criteriaBuilder.lessThanOrEqualTo(root.get<BigDecimal>("purchasePrice"), maxPrice))
                    }
                }
            criteriaBuilder.and(*predicates.toTypedArray())
        }
}
