package game.domain

abstract class Card {
    open val winPoints: Int = 0
    abstract val title: String
    abstract val description: String

    // null если ход норм, иначе строка с ошибкой
    abstract fun validate(move: Move, round: Round): String?
    abstract fun play(move: Move, round: Round)
}
