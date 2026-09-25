package game.ui

import game.domain.*
import java.awt.Dimension
import java.awt.Font
import java.awt.Insets
import java.awt.Toolkit
import javax.swing.*

class DesktopWindow(private val controller: DesktopGameController) : JFrame("Board game") {
    private val tabs = JTabbedPane()
    private val lobby = JPanel()
    private val table = JPanel()
    private val stats = JTextArea()
    private val history = JTextArea()
    private val nameField = JTextField(20)

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(760, 560)

        val screen = Toolkit.getDefaultToolkit().screenSize
        setSize(maxOf(760, screen.width * 3 / 4), maxOf(560, screen.height * 3 / 4))
        setLocationRelativeTo(null)

        tabs.addTab("Игроки", JScrollPane(lobby))
        tabs.addTab("Игра", JScrollPane(table))
        tabs.addTab("Статистика", JScrollPane(stats))
        tabs.addTab("История", JScrollPane(history))

        stats.isEditable = false
        history.isEditable = false
        stats.margin = Insets(20, 20, 20, 20)
        history.margin = Insets(20, 20, 20, 20)

        add(tabs)
        tabs.addChangeListener {
            if (tabs.selectedIndex != 1 && controller.handVisible) {
                controller.hideHand()
                render()
            }
        }
        nameField.addActionListener {
            perform {
                addPlayerFromField()
                nameField.requestFocusInWindow()
            }
        }

        render()
    }

    private fun column(panel: JPanel) {
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = BorderFactory.createEmptyBorder(24, 24, 24, 24)
    }

    private fun line(panel: JPanel, component: JComponent) {
        component.alignmentX = LEFT_ALIGNMENT
        panel.add(component)
        panel.add(Box.createVerticalStrut(12))
    }

    private fun button(text: String, action: () -> Unit): JButton = JButton(text).apply {
        margin = Insets(12, 18, 12, 18)

        addActionListener { perform(action) }
    }

    private fun perform(action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            showError(e.message ?: "Ошибка")
        } finally {
            render()
        }
    }

    private fun showError(message: String) =
        JOptionPane.showMessageDialog(this, message, "Ошибка", JOptionPane.ERROR_MESSAGE)

    private fun addPlayerFromField() {
        controller.addPlayer(nameField.text)
        nameField.text = ""
    }

    private fun render() {
        renderLobby()
        renderGame()

        stats.text = controller.players.joinToString("\n") {
            "${it.name}: игр ${it.gamesPlayed}, побед ${it.wins}, " +
                "очков ${it.totalScore}, среднее %.2f".format(it.averageScore)
        }.ifEmpty { "Игроков пока нет" }

        history.text = controller.history.joinToString("\n\n") { game ->
            "Партия #${game.id}: победители ${game.players.filter { it.winner }.joinToString { it.name }}\n" +
                game.history.flatMap { it }.joinToString("\n") { move ->
                    "${game.players.firstOrNull { it.uid == move.initiatorUID }?.name}: ${move.cardName}"
                }
        }.ifEmpty { "Партий пока нет" }

        revalidate()
        repaint()
    }

    private fun renderLobby() {
        lobby.removeAll()
        column(lobby)

        val editable = controller.game == null
        line(lobby, JLabel("Добавьте игроков (от 3 до 8):").apply {
            font = font.deriveFont(Font.BOLD, (font.size + 4).toFloat())
        })

        val row = JPanel()
        row.maximumSize = Dimension(Int.MAX_VALUE, 66)
        nameField.isEnabled = editable
        row.add(nameField)
        row.add(button("Добавить") {
            addPlayerFromField()
            nameField.requestFocusInWindow()
        }.apply { isEnabled = editable })

        line(lobby, row)
        line(lobby, JLabel("Участники игры: ${controller.selectedNames.size}/8"))

        controller.selectedNames.forEach { name ->
            val participant = JPanel()
            participant.maximumSize = Dimension(Int.MAX_VALUE, 66)
            participant.add(JLabel(name))
            participant.add(button("Убрать") {
                controller.removePlayer(name)
            }.apply { isEnabled = editable })

            line(lobby, participant)
        }

        line(lobby, button("Начать игру") {
            controller.startGame()
            tabs.selectedIndex = 1
        }.apply { isEnabled = editable })
    }

    private fun renderGame() {
        table.removeAll()
        column(table)
        val game = controller.game
        if (game == null) {
            line(table, JLabel("Выберите игроков и начните игру."))
            return
        }

        if (game.status == GameStatus.FINISHED) {
            line(table, JLabel("Игра #${controller.savedGameId} завершена."))
            line(table, JLabel("Победители: ${game.getResult().winners.joinToString { it.name }}"))

            game.getScores().forEach { (player, score) ->
                line(table, JLabel("${player.name}: $score очков"))
            }
            line(table, button("В лобби") {
                controller.returnToLobby()
                tabs.selectedIndex = 0
            })
            return
        }

        val current = game.getCurrentPlayer()
        line(table, JLabel("Раунд ${game.getRoundNumber()}, ход ${game.getCurrentTurn()}").apply {
            font = font.deriveFont(Font.BOLD, (font.size + 4).toFloat())
        })
        line(table, JLabel("Передайте устройство игроку ${current.name}"))

        if (!controller.handVisible) {
            line(table, button("Я ${current.name}. Показать руку") {
                controller.revealHand()
            })
            return
        }

        line(table, JLabel("Карты игрока ${current.name}:"))
        controller.visibleCards().forEachIndexed { index, card ->
            line(table, button("${index + 1}. ${card.title}") { play(index) })
            line(table, JLabel("<html><div style='width:750px'>${card.description}</div></html>"))
        }
    }

    private fun play(index: Int) {
        val game = controller.game ?: return
        val actor = game.getCurrentPlayer()
        val card = actor.getCards()[index]
        val target = if (card.requiresTarget) {
            val possible = game.getAllPlayers().filter {
                it != actor || card.effect == CardEffect.FRIEND || card.effect == CardEffect.HOUND
            }
            choosePlayer("Выберите цель", possible) ?: return
        } else null

        table.removeAll()
        table.add(JLabel("Передайте устройство указанному игроку."))
        table.revalidate()
        table.repaint()

        val details = chooseDetails(
            card,
            actor,
            target,
            game.getAllPlayers(),
            game.getCurrentTurn()
        ) ?: run {
            controller.hideHand()
            return
        }

        when (val result = controller.submitMove(index, target?.UID, details)) {
            is GameMoveResult.Rejected -> showError(result.message)
            is GameMoveResult.MoveApplied -> showEvent(result.event, actor)
            is GameMoveResult.RoundFinished -> {
                showEvent(result.event, actor)
                JOptionPane.showMessageDialog(this, "Раунд завершён: ${result.roundStatus}")
            }
        }
    }

    private fun showEvent(event: MoveEvent?, actor: Player) {
        if (event is MoveEvent.CardsRevealed) {
            JOptionPane.showMessageDialog(this, "Передайте устройство игроку ${actor.name}")
            JOptionPane.showMessageDialog(this, "Карты ${event.player.name}: ${event.cards.joinToString { it.title }}")
        }
    }

    private fun choosePlayer(prompt: String, players: List<Player>): Player? {
        val names = players.map { it.name }.toTypedArray()
        val chosen = JOptionPane.showInputDialog(
            this,
            prompt,
            "Выбор цели",
            JOptionPane.QUESTION_MESSAGE,
            null,
            names,
            names.first()
        ) as? String ?: return null

        return players.first { it.name == chosen }
    }

    private fun chooseCards(player: Player, played: Card? = null, max: Int = 1): List<Card>? {
        val available = player.getCards().toMutableList()
        if (played != null) available.remove(played)
        if (available.isEmpty()) return emptyList()

        val handoff = JOptionPane.showConfirmDialog(
            this,
            "Передайте устройство игроку ${player.name}. Готовы?",
            "Передача",
            JOptionPane.OK_CANCEL_OPTION
        )
        if (handoff != JOptionPane.OK_OPTION) return null

        val list = JList(available.mapIndexed { index, card -> "${index + 1}. ${card.title}" }.toTypedArray())
        list.selectionMode = if (max == 1) {
            ListSelectionModel.SINGLE_SELECTION
        } else {
            ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        }

        val answer = JOptionPane.showConfirmDialog(
            this,
            JScrollPane(list),
            "${player.name}: выберите ${if (max == 1) "карту" else "1–2 карты"}",
            JOptionPane.OK_CANCEL_OPTION
        )
        if (answer != JOptionPane.OK_OPTION) return null

        val indices = list.selectedIndices
        if (indices.isEmpty() || indices.size > max) {
            showError("Выберите ${if (max == 1) "одну карту" else "1–2 карты"}")
            return null
        }

        return indices.map { available[it] }
    }

    private fun chooseDetails(
        card: Card,
        actor: Player,
        target: Player?,
        players: List<Player>,
        turn: Int
    ): MoveDetails? {
        val disabled = setOf(CardEffect.THEFT, CardEffect.TRADE, CardEffect.FOG, CardEffect.GOSSIP)
        if (turn >= 4 && card.effect in disabled) return MoveDetails.None

        return when (card.effect) {
            CardEffect.THEFT -> {
                val chosen = chooseCards(actor, card)?.singleOrNull() ?: return null
                MoveDetails.Theft(chosen)
            }
            CardEffect.TRADE -> {
                val offered = chooseCards(actor, card, 2) ?: return null
                val received = chooseCards(target!!, max = offered.size) ?: return null

                if (offered.size != received.size) {
                    showError("Выберите одинаковое количество карт")
                    return null
                }

                MoveDetails.Trade(offered, received)
            }
            CardEffect.HOUND -> {
                val played = if (target == actor) card else null
                val chosen = chooseCards(target!!, played)?.singleOrNull() ?: return null
                MoveDetails.Hound(chosen)
            }
            CardEffect.FOG, CardEffect.GOSSIP -> {
                val participants = players.filter { card.effect == CardEffect.GOSSIP || it != actor }
                val choices = mutableListOf<CardChoice>()

                for (player in participants) {
                    val played = if (player == actor) card else null
                    val chosen = chooseCards(player, played) ?: return null
                    chosen.singleOrNull()?.let { choices.add(CardChoice(player, it)) }
                }

                if (card.effect == CardEffect.FOG) MoveDetails.Fog(choices) else MoveDetails.Gossip(choices)
            }
            else -> MoveDetails.None
        }
    }
}
