package game.domain

import game.domain.cards.HoundCard
import game.domain.cards.Monster
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CardEffectHandlerTest {
    @Test
    fun `handler returns victory without changing round status`() {
        val players = (1..3).map { Player(it, "Player $it") }
        val round = Round(players, Deck(players.size))
        val initiator = round.getCurrentPlayer()
        val target = players.first { player -> player.getCards().any { it is Monster } }
        val monster = target.getCards().first { it is Monster }
        initiator.addCard(HoundCard)
        val move = Move(initiator, target, HoundCard, MoveDetails.Hound(monster))
        val handler = DefaultCardEffectHandler()

        assertEquals(null, handler.validate(move, round))
        val result = handler.apply(move, round)

        assertIs<MoveResult.Applied>(result)
        assertEquals(RoundStatus.HUNTERS_WON, result.roundStatus)
        assertEquals(RoundStatus.IN_PROGRESS, round.status)

        round.recordMove(move, result)
        assertEquals(RoundStatus.HUNTERS_WON, round.status)
    }
}
