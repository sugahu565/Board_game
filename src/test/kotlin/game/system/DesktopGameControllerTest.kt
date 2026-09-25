package game.system

import game.data.SqliteGameRepository
import game.domain.*
import game.ui.DesktopGameController
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.*

class DesktopGameControllerTest {
    @TempDir lateinit var tempDir: Path

    @Test
    fun `gui controller hides hand and persists completed game`() {
        val path = tempDir.resolve("desktop.db").toString()
        val handler = object : CardEffectHandler {
            override fun validate(move: Move, round: Round): String? = null
            override fun apply(move: Move, round: Round): MoveResult =
                MoveResult.Applied(RoundStatus.MONSTER_WON)
        }
        val controller = DesktopGameController(SqliteGameRepository(path)) { Game(it, handler) }
        listOf("Анна", "Борис", "Вера").forEach(controller::addPlayer)
        assertEquals(3, controller.players.size)
        assertTrue(controller.players.all { it.gamesPlayed == 0 })
        controller.startGame()
        assertTrue(controller.visibleCards().isEmpty())
        assertFailsWith<IllegalStateException> { controller.submitMove(0, null, MoveDetails.None) }
        controller.revealHand()
        assertTrue(controller.visibleCards().isNotEmpty())
        val search = controller.visibleCards().indexOfFirst { it.effect == CardEffect.SEARCH }
        assertTrue(search >= 0)
        assertIs<GameMoveResult.RoundFinished>(controller.submitMove(search, null, MoveDetails.None))
        assertTrue(controller.visibleCards().isEmpty())
        assertNotNull(controller.savedGameId)
        val reopened = SqliteGameRepository(path)
        assertEquals(1, reopened.getHistory().size)
        assertEquals(1, reopened.getHistory().single().history.single().size)
        assertTrue(reopened.getStats().all { it.gamesPlayed == 1 && it.wins == 1 })
        controller.returnToLobby()
        assertNull(controller.game)
    }

    @Test
    fun `adding names reuses registered player and removing keeps profile`() {
        val path = tempDir.resolve("names.db").toString()
        val controller = DesktopGameController(SqliteGameRepository(path))
        controller.addPlayer(" Анна ")
        assertEquals(listOf("Анна"), controller.selectedNames.toList())
        assertFailsWith<IllegalArgumentException> { controller.addPlayer("анна") }
        controller.removePlayer("Анна")
        controller.addPlayer("анна")
        assertEquals(listOf("Анна"), controller.selectedNames.toList())
        assertEquals(listOf("Анна"), SqliteGameRepository(path).getStats().map { it.name })
    }
}
