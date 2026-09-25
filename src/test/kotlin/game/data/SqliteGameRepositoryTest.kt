package game.data

import game.domain.GameResult
import game.domain.MoveLog
import game.domain.Player
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class SqliteGameRepositoryTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `registered players persist without games and keep profile after deletion`() {
        val path = tempDir.resolve("registry.db").toString()
        val repository = SqliteGameRepository(path)
        repository.registerPlayer(" Анна ")
        assertEquals(listOf(PlayerStatistics("Анна", 0, 0, 0)), SqliteGameRepository(path).getStats())
        assertEquals(0.0, repository.getStats().single().averageScore)
        assertTrue(repository.getHistory().isEmpty())
        assertEquals("Игрок с таким именем уже зарегистрирован",
            assertFailsWith<IllegalArgumentException> { repository.registerPlayer("анна") }.message)
        assertEquals("Имя не может быть пустым",
            assertFailsWith<IllegalArgumentException> { repository.registerPlayer("   ") }.message)
        assertEquals("Имя не должно превышать 100 символов",
            assertFailsWith<IllegalArgumentException> { repository.registerPlayer("а".repeat(101)) }.message)
        val id = repository.writeGame(gameResult("Анна" to 3, "Борис" to 1, "Вера" to 0))
        assertEquals(PlayerStatistics("Анна", 1, 1, 3), repository.getStats().first { it.name == "Анна" })
        repository.deleteGame(id)
        assertEquals(PlayerStatistics("Анна", 0, 0, 0), SqliteGameRepository(path).getStats().first { it.name == "Анна" })
    }

    @Test
    fun `game history survives reopening repository`() {
        val databasePath = tempDir.resolve("history.db").toString()
        val repository = SqliteGameRepository(databasePath)
        val result = gameResult("Anna" to 3, "Boris" to 1, "Clara" to 1)

        val gameId = repository.writeGame(result)
        val stored = SqliteGameRepository(databasePath).getHistory().single()

        assertEquals(gameId, stored.id)
        assertEquals(listOf("Anna", "Boris", "Clara"), stored.players.map { it.name })
        assertEquals(listOf(3, 1, 1), stored.players.map { it.score })
        assertEquals(listOf("Anna"), stored.players.filter { it.winner }.map { it.name })
        assertEquals(result.history, stored.history)
    }

    @Test
    fun `statistics combine several games of the same players`() {
        val repository = SqliteGameRepository(tempDir.resolve("stats.db").toString())
        repository.writeGame(gameResult("Anna" to 3, "Boris" to 1, "Clara" to 0))
        repository.writeGame(gameResult("Boris" to 2, "Anna" to 1, "Denis" to 0))

        val statistics = repository.getStats().associateBy { it.name }

        assertEquals(PlayerStatistics("Anna", 2, 1, 4), statistics.getValue("Anna"))
        assertEquals(PlayerStatistics("Boris", 2, 1, 3), statistics.getValue("Boris"))
        assertEquals(PlayerStatistics("Clara", 1, 0, 0), statistics.getValue("Clara"))
        assertEquals(PlayerStatistics("Denis", 1, 0, 0), statistics.getValue("Denis"))
    }

    @Test
    fun `deleting game removes its moves and updates statistics`() {
        val databasePath = tempDir.resolve("delete.db").toString()
        val repository = SqliteGameRepository(databasePath)
        val firstId = repository.writeGame(gameResult("Anna" to 3, "Boris" to 0, "Clara" to 0))
        val secondId = repository.writeGame(gameResult("Boris" to 2, "Anna" to 1, "Clara" to 0))

        assertTrue(repository.deleteGame(firstId))
        assertFalse(repository.deleteGame(firstId))
        assertEquals(listOf(secondId), repository.getHistory().map { it.id })
        assertEquals(1, repository.getStats().first { it.name == "Anna" }.gamesPlayed)
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            listOf("games", "game_players", "moves").forEach { table ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT COUNT(*) FROM $table WHERE ${if (table == "games") "id" else "game_id"} = $firstId").use {
                        assertTrue(it.next())
                        assertEquals(0, it.getInt(1))
                    }
                }
            }
        }
    }

    @Test
    fun `history keeps newest game first and move order inside each round`() {
        val databasePath = tempDir.resolve("order.db").toString()
        val repository = SqliteGameRepository(databasePath)
        val first = gameResult("Anna" to 2, "Boris" to 2, "Clara" to 0).copy(
            history = listOf(
                listOf(MoveLog(1, 2, "Первая"), MoveLog(2, null, "Вторая")),
                listOf(MoveLog(3, 1, "Третья"))
            )
        )
        val firstId = repository.writeGame(first)
        val secondId = repository.writeGame(gameResult("Denis" to 3, "Eva" to 1, "Fedor" to 0))

        val history = SqliteGameRepository(databasePath).getHistory()

        assertEquals(listOf(secondId, firstId), history.map { it.id })
        assertEquals(first.history, history[1].history)
        assertEquals(listOf("Anna", "Boris"), history[1].players.filter { it.winner }.map { it.name })
        assertTrue(history.all { it.playedAt > 0 })
    }

    @Test
    fun `failed write rolls back game players and moves`() {
        val databasePath = tempDir.resolve("rollback.db").toString()
        val repository = SqliteGameRepository(databasePath)
        val duplicateNames = gameResult("Anna" to 3, "Anna" to 1, "Clara" to 0)

        assertFailsWith<Exception> { repository.writeGame(duplicateNames) }

        assertTrue(repository.getHistory().isEmpty())
        assertTrue(repository.getStats().isEmpty())
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            listOf("games", "players", "game_players", "moves").forEach { table ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT COUNT(*) FROM $table").use {
                        assertTrue(it.next())
                        assertEquals(0, it.getInt(1))
                    }
                }
            }
        }
        val id = repository.writeGame(gameResult("Anna" to 3, "Boris" to 1, "Clara" to 0))
        assertEquals(listOf(id), repository.getHistory().map { it.id })
        assertEquals(3, repository.getStats().size)
    }

    private fun gameResult(vararg scores: Pair<String, Int>): GameResult {
        val players = scores.mapIndexed { index, score -> Player(index + 1, score.first) }
        val scoreMap = players.zip(scores.map { it.second }).toMap()
        val maxScore = scoreMap.values.max()
        return GameResult(
            winners = scoreMap.filterValues { it == maxScore }.keys.toList(),
            history = listOf(
                listOf(
                    MoveLog(players[0].UID, players[1].UID, "Охотник"),
                    MoveLog(players[1].UID, null, "Укрытие")
                )
            ),
            scores = scoreMap
        )
    }
}
