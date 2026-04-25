package game.domain.cards
import game.domain.Move
import game.domain.Card
import game.domain.Round

abstract class Monster : Card() {
}

object Vamripe : Monster() {
    override val title = "Вампир"
    override val description: String = "В случае победы получите 1 дополнительное ПО (итого 3 ПО)"
    override fun validate(move: Move, round: Round) : String? {
        // проверить на то, что карта сущ у игрока
        // проверить на то, что кастуется не на себя
        // проверить на то, что сейчас нужный раунд
        // стоит проверить, что в Move лежит эта самая карта (но это лучше отдать Round)
        // валидация хода НУЖНОГО игрока (что должен ходить именно он) лежит на Round
        if (round.getCurrentRound() != 4) {
            return "Данную карту нельзя разыгрывать на данном ходе"
        }
        return null
    }

    override fun play(move: Move, round: Round) {
        
    }
}
