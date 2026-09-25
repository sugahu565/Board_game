package game.domain.cards

import game.domain.Card
import game.domain.CardEffect
import game.domain.CardRole

abstract class Monster : Card() {
    override val winPoints = 2
    final override val role = CardRole.MONSTER
    open fun canBeKilledByHunter(turn: Int, playedHunters: Int): Boolean = true
}

object VampireCard : Monster() {
    override val title = "Вампир"
    override val description = "Дает +1 ПО при победе (итого 3 ПО). Можно сыграть только на 4 ходе"
    override val winPoints = 3
    override val effect = CardEffect.VAMPIRE
}

object KumihoCard : Monster() {
    override val title = "Кумихо"
    override val description = "Нельзя убить на 4 ходе. Можно сыграть только на 4 ходе"
    override val effect = CardEffect.KUMIHO
    override fun canBeKilledByHunter(turn: Int, playedHunters: Int) = turn != 4
}

object WerewolfCard : Monster() {
    override val title = "Оборотень"
    override val description = "Можно сыграть на 2 и 3 ходе, если сыграно хотя бы два охотника. Можно сыграть на 4 ходе"
    override val effect = CardEffect.WEREWOLF
}

object MandragoraCard : Monster() {
    override val title = "Мандрагора"
    override val description = "Можно сыграть на 2 и 3 ходе, если сыграно два мирных жителя. Можно сыграть на 4 ходе"
    override val effect = CardEffect.MANDRAGORA
}

object MermaidCard : Monster() {
    override val title = "Русалка"
    override val description = "Игнорирует эффект первой разыгранной карты охотника. Можно сыграть на 4 ходе"
    override val effect = CardEffect.MERMAID
    override fun canBeKilledByHunter(turn: Int, playedHunters: Int): Boolean = playedHunters > 0
}
