package game.domain

import game.domain.cards.SearchCard
import game.domain.cards.Villager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GameMoveTest {
    @Test
    fun `game validates applies and records a move`() {
        val players = (1..3).map { Player(it, "Player $it") }
        var validations = 0
        var applications = 0
        val handler = object : CardEffectHandler {
            override fun validate(move: Move, round: Round): String? {
                validations++
                return null
            }

            override fun apply(move: Move, round: Round): MoveResult {
                applications++
                move.initiator.playCard(move.playerCard)
                return MoveResult.Applied(RoundStatus.MONSTER_WON)
            }
        }
        val game = Game(players, handler)
        val currentPlayer = game.getCurrentPlayer()

        assertIs<GameMoveResult.Rejected>(game.processGameMove(Move(currentPlayer, null, Villager)))
        assertEquals(0, validations)
        assertEquals(0, applications)

        val result = game.processGameMove(Move(currentPlayer, null, SearchCard))

        assertIs<GameMoveResult.RoundFinished>(result)
        assertEquals(RoundStatus.MONSTER_WON, result.roundStatus)
        assertEquals(1, validations)
        assertEquals(1, applications)
        assertEquals(GameStatus.FINISHED, game.status)
        assertEquals(1, game.getResult().history.single().size)
    }

    @Test
    fun `handler validation rejects without changing round`() {
        val players = (1..3).map { Player(it, "Player $it") }
        var applied = false
        val handler = object : CardEffectHandler {
            override fun validate(move: Move, round: Round): String = "Ход запрещён правилом карты"
            override fun apply(move: Move, round: Round): MoveResult {
                applied = true
                return MoveResult.Applied(RoundStatus.IN_PROGRESS)
            }
        }
        val game = Game(players, handler)
        val currentPlayer = game.getCurrentPlayer()

        val result = game.processGameMove(Move(currentPlayer, null, SearchCard))

        assertIs<GameMoveResult.Rejected>(result)
        assertEquals("Ход запрещён правилом карты", result.message)
        assertEquals(currentPlayer, game.getCurrentPlayer())
        assertTrue(SearchCard in currentPlayer.getCards())
        assertEquals(false, applied)
    }

    @Test
    fun `game advances to next player after ordinary move`() {
        val players = (1..3).map { Player(it, "Player $it") }
        val handler = object : CardEffectHandler {
            override fun validate(move: Move, round: Round): String? = null
            override fun apply(move: Move, round: Round): MoveResult {
                move.initiator.playCard(move.playerCard)
                return MoveResult.Applied(RoundStatus.IN_PROGRESS)
            }
        }
        val game = Game(players, handler)
        val firstPlayer = game.getCurrentPlayer()

        val result = game.processGameMove(Move(firstPlayer, null, SearchCard))

        assertIs<GameMoveResult.MoveApplied>(result)
        assertEquals(GameStatus.IN_PROGRESS, game.status)
        assertNotEquals(firstPlayer, game.getCurrentPlayer())
    }
}
