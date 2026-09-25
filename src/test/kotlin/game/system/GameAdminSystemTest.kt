package game.system

import game.data.PlayerStatistics
import game.data.SqliteGameRepository
import game.data.StoredGame
import game.domain.*
import game.domain.cards.SearchCard
import game.logic.GameAdmin
import game.logic.Input
import game.logic.Output
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameAdminSystemTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `finished game is saved and shown to user`() {
        val databasePath = tempDir.resolve("system.db").toString()
        val repository = SqliteGameRepository(databasePath)
        val output = RecordingOutput()

        val finishOnFirstMove = object : CardEffectHandler {
            override fun validate(move: Move, round: Round): String? = null
            override fun apply(move: Move, round: Round): MoveResult = MoveResult.Applied(RoundStatus.MONSTER_WON)
        }
        GameAdmin(repository, AutomaticInput(), output) { Game(it, finishOnFirstMove) }.runGame()

        val reopened = SqliteGameRepository(databasePath)
        val savedGame = reopened.getHistory().single()
        assertEquals(savedGame.id, output.savedGameId)
        assertTrue(output.gameResultShown)
        assertEquals(listOf(savedGame), output.shownHistory)
        assertEquals(reopened.getStats(), output.shownStats)
        assertEquals(3, savedGame.players.size)
        assertTrue(savedGame.history.single().isNotEmpty())
        assertTrue(savedGame.players.any { it.winner })
        assertEquals(setOf("Anna", "Boris", "Clara"), output.shownStats!!.map { it.name }.toSet())
    }

    private class AutomaticInput : Input {
        override fun inputPlayers(): List<Player> = listOf(
            Player(1, "Anna"), Player(2, "Boris"), Player(3, "Clara")
        )

        override fun inputMove(game: Game): Move {
            val player = game.getCurrentPlayer()
            val players = game.getAllPlayers()
            val hand = player.getCards()
            val card = when {
                SearchCard in hand -> SearchCard
                game.getCurrentTurn() >= 4 -> hand.firstOrNull { it.role == CardRole.MONSTER } ?: hand.first()
                else -> hand.first { it.role != CardRole.MONSTER }
            }
            val target = if (card.requiresTarget) players.first { it != player } else null
            val withoutPlayedCard = hand.toMutableList().also { it.remove(card) }

            val details = when (card.effect) {
                CardEffect.THEFT -> MoveDetails.Theft(withoutPlayedCard.firstOrNull())
                CardEffect.TRADE -> MoveDetails.Trade(
                    initiatorCards = withoutPlayedCard.take(1),
                    targetCards = target?.getCards()?.take(1) ?: emptyList()
                )
                CardEffect.FOG -> MoveDetails.Fog(
                    players.filter { it != player }.mapNotNull { other ->
                        other.getCards().firstOrNull()?.let { CardChoice(other, it) }
                    }
                )
                CardEffect.GOSSIP -> MoveDetails.Gossip(
                    players.mapNotNull { other ->
                        val available = other.getCards().toMutableList()
                        if (other == player) available.remove(card)
                        available.firstOrNull()?.let { CardChoice(other, it) }
                    }
                )
                CardEffect.HOUND -> MoveDetails.Hound(target?.getCards()?.firstOrNull())
                else -> MoveDetails.None
            }
            return Move(player, target, card, details)
        }
    }

    private class RecordingOutput : Output {
        var savedGameId: Int? = null
        var gameResultShown = false
        var shownHistory: List<StoredGame>? = null
        var shownStats: List<PlayerStatistics>? = null

        override fun showGameState(game: Game, roundNumber: Int) = Unit
        override fun showRoundResult(status: RoundStatus) = Unit
        override fun showMoveEvent(event: MoveEvent) = Unit
        override fun showError(message: String) = Unit
        override fun showGameResult(game: Game) { gameResultShown = true }
        override fun showGameSaved(gameId: Int) { savedGameId = gameId }
        override fun showHistory(history: List<StoredGame>) { shownHistory = history }
        override fun showStats(statistics: List<PlayerStatistics>) { shownStats = statistics }
    }
}
