package game.ui

import game.data.SqliteGameRepository
import java.awt.Font
import java.awt.Insets
import java.awt.Toolkit
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.plaf.FontUIResource

fun main() {
    val databasePath = System.getenv("BOARD_GAME_DB") ?: "data/board_game.db"

    SwingUtilities.invokeLater {
        try {
            val repository = SqliteGameRepository(databasePath)
            val controller = DesktopGameController(repository)
            configureSwingAppearance()

            val window = DesktopWindow(controller)
            window.isVisible = true
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(null, e.message, "Ошибка запуска", JOptionPane.ERROR_MESSAGE)
            e.printStackTrace()
        }
    }
}

private fun configureSwingAppearance() {
    val height = Toolkit.getDefaultToolkit().screenSize.height
    val fontSize = when {
        height >= 1400 -> 26
        height >= 1000 -> 21
        else -> 18
    }

    val font = FontUIResource(Font.SANS_SERIF, Font.PLAIN, fontSize)
    val components = listOf(
        "Button", "CheckBox", "ComboBox", "Label", "List",
        "OptionPane", "TabbedPane", "TextArea", "TextField", "TextPane"
    )

    components.forEach { UIManager.put("$it.font", font) }
    UIManager.put("OptionPane.messageFont", font)
    UIManager.put("OptionPane.buttonFont", font)
    UIManager.put("TabbedPane.tabInsets", Insets(12, 20, 12, 20))
}
