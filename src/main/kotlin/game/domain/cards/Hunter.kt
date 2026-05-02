package game.domain.cards

import game.domain.Card
import game.domain.Move
import game.domain.Round
import game.domain.cards.Monster

abstract class Hunter : Card() {
    override val winPoints: Int = 2
}

object HunterCard : Hunter() {
    override val title: String = "Охотник"
    override val description: String = "Цель сбрасывает монстра. Нет эффекта на 1-м ходу."

    override fun validate(move: Move, round: Round): String? {
        if (move.target == null) return "Необходимо выбрать цель."
        if (move.target == move.initiator) return "Нельзя сыграть на себя."
        return null
    }

    override fun play(move: Move, round: Round) {
        move.initiator.playCard(this) // карта сыграна
        val targetPlayer = move.target!!

        if (round.getCurrentTurn() == 1 || move.target.hasHideCard()) { // 1 ход или у цели есть карта укрытия
            return
        }

        val targetMonster = targetPlayer.getCards().firstOrNull { it is Monster }

        // где-то добавить проверку на то, можно ли убить

        if (targetMonster != null) { // монстр есть => сбрасываем, мирные победили
            targetPlayer.discardCard(targetMonster)
            round.finishWithHuntersWin()
        }
    }
}
