package game.domain

data class MoveLog(
    val initiatorUID: Int,
    val targetUID: Int?,
    val cardName: String
)

class Move(
    val initiator: Player,
    val target: Player?,
    val playerCard: Card,
    val details: MoveDetails = MoveDetails.None
) {
    fun toLog(): MoveLog {
        return MoveLog(
            initiatorUID = initiator.UID,
            targetUID = target?.UID,
            cardName = playerCard.title
        )
    }
}

data class CardChoice(val player: Player, val card: Card)

sealed interface MoveDetails {
    data object None : MoveDetails
    data class Theft(val cardToGive: Card?) : MoveDetails
    data class Trade(val initiatorCards: List<Card>, val targetCards: List<Card>) : MoveDetails
    data class Fog(val choices: List<CardChoice>) : MoveDetails
    data class Gossip(val choices: List<CardChoice>) : MoveDetails
    data class Hound(val cardToDiscard: Card?) : MoveDetails
}

sealed interface MoveResult {
    data class Rejected(val message: String) : MoveResult
    data class Applied(val roundStatus: RoundStatus, val event: MoveEvent? = null) : MoveResult
}

sealed interface MoveEvent {
    data class CardsRevealed(val player: Player, val cards: List<Card>) : MoveEvent
}
