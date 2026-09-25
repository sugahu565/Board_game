package game.domain

import game.domain.cards.SearchCard
import game.domain.cards.Villager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RoundTest {
    @Test
    fun `round returns a base move error instead of printing it`() {
        val players = (1..3).map { Player(it, "Player $it") }
        val round = Round(players, Deck(players.size))
        val currentPlayer = round.getCurrentPlayer()

        val error = round.checkMove(Move(currentPlayer, null, Villager))

        assertNotNull(error)
        assertEquals(0, round.getMoveLog().size)
    }

    @Test
    fun `round records the result without applying card rules`() {
        val players = (1..3).map { Player(it, "Player $it") }
        val round = Round(players, Deck(players.size))
        val currentPlayer = round.getCurrentPlayer()
        val move = Move(currentPlayer, null, SearchCard)

        round.recordMove(move, MoveResult.Applied(RoundStatus.HUNTERS_WON))

        assertEquals(RoundStatus.HUNTERS_WON, round.status)
        assertEquals(1, round.getMoveLog().size)
    }
}
