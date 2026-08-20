package unq.pdes.backend.model.user

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import unq.pdes.backend.model.Agency

@Entity
@DiscriminatorValue("AGENCY")
class AgencyUser private constructor(builder: Builder) : User(builder) {

    @ManyToOne(optional = false)
    @JoinColumn(name = "agency_id")
    var agency: Agency = builder.agency!!

    override fun toString(): String {
        return "AgencyUser(id=$id, username='$username', agency=${agency.id})"
    }

    class Builder : BaseBuilder<Builder>() {
        var agency: Agency? = null

        fun agency(agency: Agency): Builder = self().apply { this.agency = agency }

        fun build(): AgencyUser {
            role = Role.AGENCY
            validateCommonFields()
            requireNotNull(agency) { "An agency user must belong to an agency." }
            return AgencyUser(this)
        }
    }
}
