package game.domain

import game.domain.cards.*

class Deck(private val playerCount: Int) {
    private val cards: MutableList<Card> = mutableListOf()
    private val availableMonsters: List<Monster> = listOf(
        VampireCard,
        KumihoCard,
        WerewolfCard,
        MandragoraCard,
        MermaidCard
    )


    init {
        buildAndShuffleDeck()
    }

    private fun buildAndShuffleDeck() {
        val safeCards = mutableListOf<Card>()

        repeat(3) { safeCards.add(HunterCard) }
        repeat(2) { safeCards.add(FriendCard) }
        repeat(4) { safeCards.add(HideCard) }
        repeat(4) { safeCards.add(RumorsCard) }
        repeat(2) { safeCards.add(GossipCard) }
        repeat(3) { safeCards.add(TheftCard) }
        repeat(2) { safeCards.add(FogCard) }
        safeCards.add(TradeCard)
        repeat(2) { safeCards.add(InvestigationCard) }
        safeCards.add(HoundCard) // 1 шт

        safeCards.shuffle()

        val initialDeal = mutableListOf<Card>()

        val roundMonster = availableMonsters.random()
        initialDeal.add(roundMonster)

        val hunter = safeCards.first { it is Hunter }
        safeCards.remove(hunter)
        initialDeal.add(hunter)

        val hide = safeCards.first { it is Hide }
        safeCards.remove(hide)
        initialDeal.add(hide)

        initialDeal.add(SearchCard)

        while (initialDeal.size < playerCount) {
            initialDeal.add(safeCards.removeAt(0))
        }

        initialDeal.shuffle()

        cards.addAll(initialDeal)
        cards.addAll(safeCards)
    }

    fun nextCard(): Card {
        if (cards.isEmpty()) throw IllegalStateException("Колода пуста")
        return cards.removeAt(0)
    }

    fun isEmpty(): Boolean = cards.isEmpty()
}