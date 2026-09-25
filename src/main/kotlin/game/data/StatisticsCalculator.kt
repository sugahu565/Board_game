package game.data

internal data class PlayerGameRecord(
    val name: String,
    val score: Int,
    val winner: Boolean
)

internal object StatisticsCalculator {
    fun calculate(
        records: List<PlayerGameRecord>,
        playerNames: Collection<String> = records.map { it.name }
    ): List<PlayerStatistics> {
        val byName = records.groupBy { it.name }

        return playerNames.distinct().map { name ->
            val games = byName[name].orEmpty()

            PlayerStatistics(
                name = name,
                gamesPlayed = games.size,
                wins = games.count { it.winner },
                totalScore = games.sumOf { it.score }
            )
        }
            .sortedWith(
                compareByDescending<PlayerStatistics> { it.wins }
                    .thenByDescending { it.totalScore }
                    .thenBy { it.name }
            )
    }
}
