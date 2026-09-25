package game.domain.cards

import game.domain.Card
import game.domain.CardEffect

abstract class Action : Card()

object SearchCard : Card() {
    override val title = "Розыск"
    override val description = "Вы начинаете раун"
    override val effect = CardEffect.SEARCH
}

object Villager : Action() {
    override val title = "Мирный житель"
    override val description = "Без эффектов"
    override val effect = CardEffect.VILLAGER
}

object RumorsCard : Action() {
    override val title = "Слухи"
    override val description = "Каждый игрок получает случайную карту от соседа справа. Не работает на 4 ходе"
    override val effect = CardEffect.RUMORS
}

object TheftCard : Action() {
    override val title = "Кража"
    override val description = "Заберите случайную карту у цели, затем отдайте ей одну свою. Не работает на 4 ходе."
    override val effect = CardEffect.THEFT
    override val requiresTarget = true
}

object InvestigationCard : Card() {
    override val title = "Расследование"
    override val description = "Посмотрите карты выбранного игрока."
    override val effect = CardEffect.INVESTIGATION
    override val requiresTarget = true
}

object GossipCard : Card() {
    override val title = "Сплетни"
    override val description = "Каждый игрок получает выбранную карту от соседа слева. Не работает на 4 ходе"
    override val effect = CardEffect.GOSSIP
}

object FogCard : Card() {
    override val title = "Туман"
    override val description = "Все остальные сдают по 1 карте. Карты перемешиваются и раздаются обратно. Не работает на 4 ходе"
    override val effect = CardEffect.FOG
}

object TradeCard : Card() {
    override val title = "Обмен"
    override val description = "Обменяйтесь 1 или 2 картами с выбранным игроком. Не работает на 4 ходу."
    override val effect = CardEffect.TRADE
    override val requiresTarget = true
}

object HoundCard : Card() {
    override val title = "Пёс"
    override val description = "Цель сбрасывает выбранную карту, а Пёс переходит к ней в руку"
    override val effect = CardEffect.HOUND
    override val requiresTarget = true
}
