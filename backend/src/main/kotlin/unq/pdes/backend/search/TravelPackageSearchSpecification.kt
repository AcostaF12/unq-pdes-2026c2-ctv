package unq.pdes.backend.search

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Root
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import unq.pdes.backend.model.Agency
import unq.pdes.backend.model.City
import unq.pdes.backend.model.Hotel
import unq.pdes.backend.model.Review
import unq.pdes.backend.model.TravelPackage
import java.math.BigDecimal
import java.util.Locale

object TravelPackageSearchSpecification {
    private const val NO_REVIEWS_RATING = -1.0

    fun matching(
        criteria: TravelPackageSearchCriteria,
        ratingDirection: Sort.Direction? = null,
        active: Boolean? = true,
        includeInactiveAgencies: Boolean = false,
    ): Specification<TravelPackage> =
        Specification { root, query, criteriaBuilder ->
            val predicates =
                buildList {
                    if (!includeInactiveAgencies) {
                        add(criteriaBuilder.isTrue(root.get<Agency>("agency").get("active")))
                    }
                    active?.let { add(criteriaBuilder.equal(root.get<Boolean>("active"), it)) }
                    criteria.name?.let { name ->
                        add(criteriaBuilder.like(criteriaBuilder.lower(root.get<String>("name")), likePattern(name)))
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
                    criteria.agencyId?.let { agencyId ->
                        add(criteriaBuilder.equal(root.get<Agency>("agency").get<Long>("id"), agencyId))
                    }
                    criteria.hotel?.let { hotel ->
                        add(
                            criteriaBuilder.like(
                                criteriaBuilder.lower(root.get<Hotel>("hotel").get<String>("name")),
                                likePattern(hotel),
                            ),
                        )
                    }
                    criteria.minRating?.let { minRating ->
                        add(criteriaBuilder.greaterThanOrEqualTo(averageScore(root, query, criteriaBuilder), minRating.toDouble()))
                    }
                }
            if (ratingDirection != null && query.resultType != Long::class.javaObjectType) {
                orderByRating(root, query, criteriaBuilder, ratingDirection)
            }
            criteriaBuilder.and(*predicates.toTypedArray())
        }

    private fun likePattern(text: String): String = "%${text.lowercase(Locale.ROOT)}%"

    private fun averageScore(
        root: Root<TravelPackage>,
        query: CriteriaQuery<*>?,
        criteriaBuilder: CriteriaBuilder,
    ): Expression<Double> {
        val subquery = requireNotNull(query) { "A query is required to rank packages by rating." }.subquery(Double::class.javaObjectType)
        val review = subquery.from(Review::class.java)
        return subquery
            .select(criteriaBuilder.avg(review.get<Int>("score")))
            .where(criteriaBuilder.equal(review.get<TravelPackage>("travelPackage"), root))
    }

    private fun orderByRating(
        root: Root<TravelPackage>,
        query: CriteriaQuery<*>,
        criteriaBuilder: CriteriaBuilder,
        direction: Sort.Direction,
    ) {
        val rating = criteriaBuilder.coalesce(averageScore(root, query, criteriaBuilder), NO_REVIEWS_RATING)
        val byRating = if (direction.isAscending) criteriaBuilder.asc(rating) else criteriaBuilder.desc(rating)
        query.orderBy(byRating, criteriaBuilder.asc(root.get<String>("name")), criteriaBuilder.asc(root.get<Long>("id")))
    }
}
