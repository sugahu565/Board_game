package game.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeckTest {
    @Test
    fun `deck has enough cards for eight players`() {
        val deck = Deck(8)

        repeat(32) { deck.nextCard() }

        assertTrue(deck.isEmpty())
    }

    @Test
    fun `every game starts with exactly four cards per player`() {
        val players = (1..8).map { Player(it, "Player $it") }

        Game(players)

        players.forEach { assertEquals(4, it.getCards().size) }
    }
}
