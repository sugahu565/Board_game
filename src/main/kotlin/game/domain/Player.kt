package game.domain

class Player(val UID: Int, val name: String) {
    private val hands: MutableList<Card> = mutableListOf<Card>()
    private val discarded: MutableList<Card> = mutableListOf<Card>()

    fun addCard(card: Card) {
        hands.add(card)
    }

    fun removeCard(card: Card) {
        hands.remove(card)
    }

    fun discardedCard(card: Card) {
        hands.remove(card)
        discarded.add(card)
    }

    fun getCards() : List<Card> {
        return hands.toList()
    }

    fun getDiscarded() : List<Card> {
        return discarded.toList()
    }
}