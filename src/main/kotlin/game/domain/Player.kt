package game.domain

class Player(val UID: Int, val name: String) {
    private val hand: MutableList<Card> = mutableListOf()
    private val discarded: MutableList<Card> = mutableListOf()
    private val played: MutableList<Card> = mutableListOf()

    fun addCard(card: Card) {
        hand.add(card)
    }

    fun removeCard(card: Card) {
        hand.remove(card)
    }

    fun playCard(card: Card) {
        hand.remove(card)
        played.add(card)
    }

    fun discardCard(card: Card) {
        hand.remove(card)
        discarded.add(card)
    }

    fun getCards(): List<Card> = hand.toList()

    fun getDiscarded(): List<Card> = discarded.toList()

    fun getPlayed(): List<Card> = played.toList()

    fun hasMonsterCard(): Boolean = hand.any { it.role == CardRole.MONSTER }

    fun hasHideCard(): Boolean = hand.any { it.role == CardRole.HIDE }

    fun hasCard(card: Card): Int = hand.count { it == card }

    fun resetForRound() {
        hand.clear()
        discarded.clear()
        played.clear()
    }
}
