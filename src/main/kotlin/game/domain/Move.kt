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
    // Доп данные — например, угадываемый игрок
    val additionalData: Map<Player, List<Card>>? = null
) {
    fun toLog(): MoveLog {
        return MoveLog(
            initiatorUID = initiator.UID,
            targetUID = target?.UID,
            cardName = playerCard.title
        )
    }
}
