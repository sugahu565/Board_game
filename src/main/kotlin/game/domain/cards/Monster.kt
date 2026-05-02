package game.domain.cards

import game.domain.Card
import game.domain.Move
import game.domain.Round

abstract class Monster : Card() {
    override val winPoints = 2
    open fun canBeKilledByHunter(round: Round): Boolean = true // убежище в счёт не идёт
}

object VampireCard : Monster() {
    override val title: String = "Вампир"
    override val description: String = "Дает +1 ПО при победе (итого 3 ПО). Можно сыграть только на 4 ходе"
    override val winPoints: Int = 3

    override fun validate(move: Move, round: Round): String? {
        if (round.getCurrentTurn() < 4) {
            return "Вампира нельзя сыграть раньше 4 хода"
        }
        return null
    }

    override fun play(move: Move, round: Round) {
        move.initiator.playCard(this)
        round.finishWithMonsterWin()
    }
}

object KumihoCard : Monster() {
    override val title: String = "Кумихо"
    override val description: String = "Нельзя убить на 4 ходе. Можно сыграть только на 4 ходе"

    override fun validate(move: Move, round: Round): String? {
        if (round.getCurrentTurn() < 4) return "Нельзя сыграть раньше 4 хода"
        return null
    }

    override fun play(move: Move, round: Round) {
        move.initiator.playCard(this)
        round.finishWithMonsterWin()
    }

    override fun canBeKilledByHunter(round: Round): Boolean {
        return round.getCurrentTurn() != 4
    }
}

object WerewolfCard : Monster() {
    override val title: String = "Оборотень"
    override val description: String = "Можно сыграть на 2 и 3 ходе, если сыграно хотя бы два охотника. Можно сыграть на 4 ходе"

    override fun validate(move: Move, round: Round): String? {
        val currentTurn = round.getCurrentTurn()

        if (currentTurn >= 4) return null

        if (currentTurn >= 2) {
            val huntersPlayed = round.history.count { it.playerCard is Hunter }
            if (huntersPlayed >= 2) return null
            return "Для использования оборотня на данном ходу нужно не менее двух разыгранных охотников"
        }

        return "Нельзя сыграть на 1 ходе"
    }

    override fun play(move: Move, round: Round) {
        move.initiator.playCard(this)
        round.finishWithMonsterWin()
    }
}

object MandragoraCard : Monster() {
    override val title: String = "Мандрагора"
    override val description: String = "Можно сыграть на 2 и 3 ходе, если сыграно два мирных жителя. Можно сыграть на 4 ходе"

    override fun validate(move: Move, round: Round): String? {
        val currentTurn = round.getCurrentTurn()

        if (currentTurn >= 4) return null

        if (currentTurn >= 2) {
            val villagersPlayed = round.history.count { it.playerCard is Hide }
            if (villagersPlayed >= 2) return null
            return "Нужно чтобы до этого было сыграно 2 мирных жителя."
        }

        return "Нельзя сыграть на 1-м ходу!"
    }

    override fun play(move: Move, round: Round) {
        move.initiator.playCard(this)
        round.finishWithMonsterWin()
    }
}

object MermaidCard : Monster() {
    override val title: String = "Русалка"
    override val description: String = "Игнорирует эффект первой разыгранной карты охотника. Можно сыграть на 4 ходе"

    override fun validate(move: Move, round: Round): String? {
        if (round.getCurrentTurn() < 4) return "Нельзя сыграть раньше 4 хода"
        return null
    }

    override fun play(move: Move, round: Round) {
        move.initiator.playCard(this)
        round.finishWithMonsterWin()
    }
    override fun canBeKilledByHunter(round: Round): Boolean {
        val huntersPlayed = round.history.count { it.playerCard is Hunter }
        return huntersPlayed > 0
    }
}
