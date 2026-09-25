import game.logic.GameAdmin
import game.data.SqliteGameRepository
import game.ui.Console

fun main() {
    val console = Console()
    val repository = SqliteGameRepository(System.getenv("BOARD_GAME_DB") ?: "data/board_game.db")

    val gameAdmin = GameAdmin(
        gameRepository = repository,
        input = console,
        output = console
    )
    try {
        gameAdmin.runGame()
    } catch (e: Exception) {
        println("\n💥 Произошла ошибка в приложении: ${e.message}")
        e.printStackTrace()
    }
}
