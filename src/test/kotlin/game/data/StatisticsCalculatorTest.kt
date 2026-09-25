package game.data

import kotlin.test.Test
import kotlin.test.assertEquals

class StatisticsCalculatorTest {
    @Test
    fun `statistics are grouped by player name`() {
        val statistics = StatisticsCalculator.calculate(
            listOf(
                PlayerGameRecord("Anna", score = 3, winner = true),
                PlayerGameRecord("Anna", score = 1, winner = false),
                PlayerGameRecord("Boris", score = 2, winner = false)
            )
        )

        assertEquals(PlayerStatistics("Anna", gamesPlayed = 2, wins = 1, totalScore = 4), statistics[0])
        assertEquals(2.0, statistics[0].averageScore)
        assertEquals(PlayerStatistics("Boris", gamesPlayed = 1, wins = 0, totalScore = 2), statistics[1])
    }

    @Test
    fun `empty history produces no statistics`() {
        assertEquals(emptyList(), StatisticsCalculator.calculate(emptyList()))
    }

    @Test
    fun `registered player without games has zero statistics`() {
        assertEquals(
            listOf(PlayerStatistics("Анна", 0, 0, 0)),
            StatisticsCalculator.calculate(emptyList(), listOf("Анна"))
        )
    }

    @Test
    fun `equal wins are sorted by total score then name`() {
        val statistics = StatisticsCalculator.calculate(
            listOf(
                PlayerGameRecord("Clara", 3, false),
                PlayerGameRecord("Boris", 3, false),
                PlayerGameRecord("Anna", 3, false),
                PlayerGameRecord("Clara", 1, true),
                PlayerGameRecord("Anna", 0, true),
                PlayerGameRecord("Boris", 0, true)
            )
        )

        assertEquals(listOf("Clara", "Anna", "Boris"), statistics.map { it.name })
        assertEquals(listOf(2.0, 1.5, 1.5), statistics.map { it.averageScore })
    }
}
