package com.example.model

import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class Team(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var flag: String,
    val group: String
)

@JsonClass(generateAdapter = true)
data class Match(
    val id: String = UUID.randomUUID().toString(),
    val team1Id: String,
    val team2Id: String,
    var score1: Int? = null,
    var score2: Int? = null,
    val isKnockout: Boolean = false,
    var penalty1: Int? = null,
    var penalty2: Int? = null,
    val stage: String = "GROUP",
    var isCompleted: Boolean = false,
    val nextMatchId: String? = null
)

@JsonClass(generateAdapter = true)
data class Goal(
    val id: String = UUID.randomUUID().toString(),
    val matchId: String,
    val teamId: String,
    var scorerName: String
)

@JsonClass(generateAdapter = true)
data class TournamentState(
    val teams: List<Team>,
    val matches: List<Match>,
    val goals: List<Goal>,
    val isKnockoutGenerated: Boolean = false
)

data class TeamStats(
    val teamId: String,
    val teamName: String,
    val flag: String,
    val group: String,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val points: Int
) {
    val goalDifference: Int get() = goalsFor - goalsAgainst
}

enum class UserRole {
    NONE, GUEST, ADMIN
}
