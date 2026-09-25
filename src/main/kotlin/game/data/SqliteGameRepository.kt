package game.data

import game.domain.GameResult
import game.domain.MoveLog
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Files
import java.nio.file.Path

class SqliteGameRepository(databasePath: String) : GameRepository {
    private val database: Database

    init {
        val databaseFile = Path.of(databasePath).toAbsolutePath()
        databaseFile.parent?.let { Files.createDirectories(it) }
        database = Database.connect("jdbc:sqlite:$databaseFile", driver = "org.sqlite.JDBC")
        transaction(database) {
            SchemaUtils.create(GamesTable, PlayersTable, GamePlayersTable, MovesTable)
        }
    }

    override fun registerPlayer(name: String) {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Имя не может быть пустым" }
        require(clean.length <= 100) { "Имя не должно превышать 100 символов" }
        transaction(database) {
            require(PlayersTable.selectAll().none { it[PlayersTable.name].equals(clean, ignoreCase = true) }) {
                "Игрок с таким именем уже зарегистрирован"
            }
            PlayersTable.insert { it[PlayersTable.name] = clean }
        }
    }

    override fun writeGame(result: GameResult): Int = transaction(database) {
        val gameId = GamesTable.insert {
            it[playedAt] = System.currentTimeMillis()
        } get GamesTable.id

        val winnerUids = result.winners.map { it.UID }.toSet()

        result.scores.forEach { (player, score) ->
            val playerId = findOrCreatePlayer(player.name)
            GamePlayersTable.insert {
                it[GamePlayersTable.gameId] = gameId
                it[GamePlayersTable.playerId] = playerId
                it[playerUid] = player.UID
                it[GamePlayersTable.score] = score
                it[winner] = player.UID in winnerUids
            }
        }

        result.history.forEachIndexed { roundIndex, moves ->
            moves.forEachIndexed { moveIndex, move ->
                MovesTable.insert {
                    it[MovesTable.gameId] = gameId
                    it[roundNumber] = roundIndex + 1
                    it[moveNumber] = moveIndex + 1
                    it[initiatorUid] = move.initiatorUID
                    it[targetUid] = move.targetUID
                    it[cardName] = move.cardName
                }
            }
        }
        gameId
    }

    override fun getHistory(): List<StoredGame> = transaction(database) {
        GamesTable.selectAll()
            .map(::readGame)
            .sortedByDescending { it.id }
    }

    override fun getStats(): List<PlayerStatistics> = transaction(database) {
        val namesById = PlayersTable.selectAll().associate { it[PlayersTable.id] to it[PlayersTable.name] }
        val records = GamePlayersTable.selectAll()
            .map { row ->
                PlayerGameRecord(
                    name = namesById.getValue(row[GamePlayersTable.playerId]),
                    score = row[GamePlayersTable.score],
                    winner = row[GamePlayersTable.winner]
                )
            }
        StatisticsCalculator.calculate(records, namesById.values)
    }

    override fun deleteGame(gameId: Int): Boolean = transaction(database) {
        MovesTable.deleteWhere { MovesTable.gameId eq gameId }
        GamePlayersTable.deleteWhere { GamePlayersTable.gameId eq gameId }
        GamesTable.deleteWhere { GamesTable.id eq gameId } > 0
    }

    private fun findOrCreatePlayer(name: String): Int {
        val existing = PlayersTable.selectAll()
            .where { PlayersTable.name eq name }
            .singleOrNull()
            ?.get(PlayersTable.id)
        if (existing != null)
            return existing

        return PlayersTable.insert { it[PlayersTable.name] = name } get PlayersTable.id
    }

    private fun readGame(row: ResultRow): StoredGame {
        val gameId = row[GamesTable.id]
        val namesById = PlayersTable.selectAll().associate { it[PlayersTable.id] to it[PlayersTable.name] }
        val players = GamePlayersTable.selectAll()
            .where { GamePlayersTable.gameId eq gameId }
            .map {
                StoredPlayerResult(
                    uid = it[GamePlayersTable.playerUid],
                    name = namesById.getValue(it[GamePlayersTable.playerId]),
                    score = it[GamePlayersTable.score],
                    winner = it[GamePlayersTable.winner]
                )
            }
            .sortedBy { it.uid }

        val moves = MovesTable.selectAll()
            .where { MovesTable.gameId eq gameId }
            .map {
                StoredMove(
                    round = it[MovesTable.roundNumber],
                    number = it[MovesTable.moveNumber],
                    log = MoveLog(
                        initiatorUID = it[MovesTable.initiatorUid],
                        targetUID = it[MovesTable.targetUid],
                        cardName = it[MovesTable.cardName]
                    )
                )
            }

        val history = moves.groupBy { it.round }
            .toSortedMap()
            .map { (_, roundMoves) -> roundMoves.sortedBy { it.number }.map { it.log } }

        return StoredGame(gameId, row[GamesTable.playedAt], players, history)
    }

    private data class StoredMove(val round: Int, val number: Int, val log: MoveLog)
}
