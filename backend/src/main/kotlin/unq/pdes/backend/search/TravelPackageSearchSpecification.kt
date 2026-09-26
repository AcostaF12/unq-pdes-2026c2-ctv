package unq.pdes.backend.search

import java.math.BigDecimal
import java.util.Locale
import org.springframework.data.jpa.domain.Specification
import unq.pdes.backend.model.City
import unq.pdes.backend.model.TravelPackage

object TravelPackageSearchSpecification {

    fun matching(criteria: TravelPackageSearchCriteria): Specification<TravelPackage> {
        return Specification { root, _, criteriaBuilder ->
            val predicates = buildList {
                criteria.name?.let { name ->
                    add(criteriaBuilder.like(criteriaBuilder.lower(root.get<String>("name")), "%${name.lowercase(Locale.ROOT)}%"))
                }
                criteria.origin?.let { origin ->
                    add(criteriaBuilder.equal(root.get<City>("origin").get<String>("code"), origin))
                }
                criteria.destination?.let { destination ->
                    add(criteriaBuilder.equal(root.get<City>("destination").get<String>("code"), destination))
                }
                criteria.minPrice?.let { minPrice ->
                    add(criteriaBuilder.greaterThanOrEqualTo(root.get<BigDecimal>("price"), minPrice))
                }
                criteria.maxPrice?.let { maxPrice ->
                    add(criteriaBuilder.lessThanOrEqualTo(root.get<BigDecimal>("price"), maxPrice))
                }
            }
            criteriaBuilder.and(*predicates.toTypedArray())
        }
    }
}
