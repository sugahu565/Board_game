package game.domain

data class MoveLog(
    val initiatorUID: Int,
    val targetUID: Int?,
    val cardName: String
)

class Move(val initiator: Player,
           val target: Player?,
           val playerCard: Card,
           val additionalData: Map<Player, Card>? = null) {

    fun toLog(): MoveLog {
        return MoveLog(
            initiatorUID = initiator.UID,
            targetUID = target?.UID,
            cardName = playerCard.title
        )
    }
}