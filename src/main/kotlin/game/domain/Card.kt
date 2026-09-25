package game.domain

abstract class Card {
    open val winPoints: Int = 0
    open val role: CardRole = CardRole.NEUTRAL
    abstract val title: String
    abstract val description: String
    abstract val effect: CardEffect
    open val requiresTarget: Boolean = false
}

enum class CardRole { NEUTRAL, MONSTER, FRIEND, HUNTER, HIDE }

enum class CardEffect {
    SEARCH, VILLAGER, RUMORS, THEFT, INVESTIGATION, GOSSIP, FOG, TRADE, HOUND,
    FRIEND, HIDE, HUNTER, VAMPIRE, KUMIHO, WEREWOLF, MANDRAGORA, MERMAID
}
