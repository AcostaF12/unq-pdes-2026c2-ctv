package unq.pdes.backend.model.history

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import unq.pdes.backend.model.user.User

@Embeddable
class PurchaseBuyerSnapshot private constructor(user: User) {

    @Column(name = "buyer_snapshot_id", nullable = false)
    var id: Long = user.id!!

    @Column(name = "buyer_snapshot_username", nullable = false, length = 60)
    var username: String = user.username

    @Column(name = "buyer_snapshot_role", nullable = false, length = 20)
    var role: String = user.role.name

    @Column(name = "buyer_snapshot_first_name", nullable = false, length = 80)
    var firstName: String = user.firstName

    @Column(name = "buyer_snapshot_last_name", nullable = false, length = 80)
    var lastName: String = user.lastName

    companion object {
        fun from(user: User): PurchaseBuyerSnapshot = PurchaseBuyerSnapshot(user)
    }
}
