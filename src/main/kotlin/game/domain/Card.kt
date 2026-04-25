package game.domain

abstract class Card {
    abstract val title: String
    abstract val description: String
    abstract fun validate(move: Move, round: Round) : String?
    abstract fun play(move: Move, round: Round)
}