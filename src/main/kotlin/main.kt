import game.logic.GameAdmin
import game.ui.Console

fun main() {
    val console = Console()
    val gameAdmin = GameAdmin(input = console, output = console)

    try {
        gameAdmin.runGame()
    } catch (e: Exception) {
        println("\n💥 Произошла ошибка в приложении: ${e.message}")
        e.printStackTrace()
    }
}
