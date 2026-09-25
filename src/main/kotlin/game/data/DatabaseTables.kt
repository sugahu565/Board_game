package game.data

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

internal object GamesTable : Table("games") {
    val id = integer("id").autoIncrement()
    val playedAt = long("played_at")
    override val primaryKey = PrimaryKey(id)
}

internal object PlayersTable : Table("players") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100).uniqueIndex()
    override val primaryKey = PrimaryKey(id)
}

internal object GamePlayersTable : Table("game_players") {
    val gameId = integer("game_id").references(GamesTable.id, onDelete = ReferenceOption.CASCADE)
    val playerId = integer("player_id").references(PlayersTable.id)
    val playerUid = integer("player_uid")
    val score = integer("score")
    val winner = bool("winner")
    override val primaryKey = PrimaryKey(gameId, playerId)
}

internal object MovesTable : Table("moves") {
    val id = integer("id").autoIncrement()
    val gameId = integer("game_id").references(GamesTable.id, onDelete = ReferenceOption.CASCADE)
    val roundNumber = integer("round_number")
    val moveNumber = integer("move_number")
    val initiatorUid = integer("initiator_uid")
    val targetUid = integer("target_uid").nullable()
    val cardName = varchar("card_name", 100)
    override val primaryKey = PrimaryKey(id)
}
