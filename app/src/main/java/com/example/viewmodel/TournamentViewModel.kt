package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class TournamentViewModel(application: Application) : AndroidViewModel(application) {

    private val _userRole = MutableStateFlow(UserRole.NONE)
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    fun loginAsAdmin(password: String): Boolean {
        return if (password == "admin123") {
            _userRole.value = UserRole.ADMIN
            true
        } else {
            false
        }
    }

    fun loginAsGuest() {
        _userRole.value = UserRole.GUEST
    }

    fun logout() {
        _userRole.value = UserRole.NONE
    }

    fun isAdmin(): Boolean = _userRole.value == UserRole.ADMIN

    private val prefs = application.getSharedPreferences("tournament_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val stateAdapter = moshi.adapter(TournamentState::class.java)

    private val _state = MutableStateFlow(loadState() ?: generateInitialState())
    val state: StateFlow<TournamentState> = _state.asStateFlow()

    init {
        // Autosave when state changes
        viewModelScope.launch {
            _state.collect { newState ->
                saveState(newState)
            }
        }
    }

    private fun saveState(state: TournamentState) {
        val json = stateAdapter.toJson(state)
        prefs.edit().putString("state_json", json).apply()
    }

    private fun loadState(): TournamentState? {
        val json = prefs.getString("state_json", null) ?: return null
        return try {
            stateAdapter.fromJson(json)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportState(): String {
        return stateAdapter.toJson(_state.value)
    }

    fun importState(json: String): Boolean {
        if (!isAdmin()) return false
        return try {
            val importedState = stateAdapter.fromJson(json)
            if (importedState != null) {
                _state.value = importedState
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun resetTournament() {
        if (!isAdmin()) return
        _state.value = generateInitialState()
    }

    fun updateTeamInfo(teamId: String, name: String, flag: String) {
        if (!isAdmin()) return
        val currentState = _state.value
        val updatedTeams = currentState.teams.map { 
            if (it.id == teamId) it.copy(name = name, flag = flag) else it 
        }
        _state.value = currentState.copy(teams = updatedTeams)
    }

    fun updateMatchScore(matchId: String, score1: Int?, score2: Int?) {
        if (!isAdmin()) return
        val currentState = _state.value
        val match = currentState.matches.find { it.id == matchId } ?: return
        
        val isCompleted = score1 != null && score2 != null && (!match.isKnockout || score1 != score2 || (match.penalty1 != null && match.penalty2 != null))
        
        val updatedMatches = currentState.matches.map {
            if (it.id == matchId) it.copy(score1 = score1, score2 = score2, isCompleted = isCompleted) else it
        }
        
        // Remove goals if new score is less than existing goals
        val updatedGoals = currentState.goals.filter { goal ->
            if (goal.matchId == matchId) {
                val teamGoalsCount = currentState.goals.count { it.matchId == matchId && it.teamId == goal.teamId }
                val newMax = if (goal.teamId == match.team1Id) score1 ?: 0 else score2 ?: 0
                // For simplicity, we just keep all existing if possible, or we could trim. We'll leave it simple for now.
                true
            } else true
        }

        _state.value = currentState.copy(matches = updatedMatches, goals = updatedGoals)
        
        if (isCompleted && match.isKnockout) {
            propagateKnockoutWin(matchId)
        }
    }

    fun updateMatchPenalties(matchId: String, pen1: Int?, pen2: Int?) {
         if (!isAdmin()) return
         val currentState = _state.value
         val match = currentState.matches.find { it.id == matchId } ?: return
         
         val isCompleted = match.score1 != null && match.score2 != null && pen1 != null && pen2 != null && pen1 != pen2
         val updatedMatches = currentState.matches.map {
             if (it.id == matchId) it.copy(penalty1 = pen1, penalty2 = pen2, isCompleted = isCompleted) else it
         }
         _state.value = currentState.copy(matches = updatedMatches)
         
         if (isCompleted && match.isKnockout) {
             propagateKnockoutWin(matchId)
         }
    }

    fun addGoal(matchId: String, teamId: String, scorerName: String) {
        if (!isAdmin()) return
        val currentState = _state.value
        val newGoal = Goal(matchId = matchId, teamId = teamId, scorerName = scorerName)
        _state.value = currentState.copy(goals = currentState.goals + newGoal)
    }

    fun removeGoal(goalId: String) {
        if (!isAdmin()) return
        val currentState = _state.value
        _state.value = currentState.copy(goals = currentState.goals.filter { it.id != goalId })
    }
    
    fun getScorers(): List<Pair<String, Int>> {
        val allGoals = _state.value.goals
        return allGoals.groupBy { it.scorerName }
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }
    }

    private fun propagateKnockoutWin(matchId: String) {
        val st = _state.value
        val match = st.matches.find { it.id == matchId } ?: return
        
        val s1 = match.score1 ?: 0; val s2 = match.score2 ?: 0
        val p1 = match.penalty1 ?: 0; val p2 = match.penalty2 ?: 0
        
        val winnerId = if (s1 > s2 || (s1 == s2 && p1 > p2)) match.team1Id else match.team2Id
        val loserId = if (s1 > s2 || (s1 == s2 && p1 > p2)) match.team2Id else match.team1Id
        
        val qfs = st.matches.filter { it.stage == "QUARTER_FINAL" }
        val sfs = st.matches.filter { it.stage == "SEMI_FINAL" }
        val third = st.matches.find { it.stage == "THIRD_PLACE" }
        val fin = st.matches.find { it.stage == "FINAL" }
        
        if (qfs.size == 4 && sfs.size == 2 && fin != null && third != null) {
            val updatedMatches = st.matches.toMutableList()
            
            fun updateMatchTeam(targetId: String, isTeam1: Boolean, newTeamId: String) {
                val idx = updatedMatches.indexOfFirst { it.id == targetId }
                if (idx != -1) {
                    updatedMatches[idx] = if (isTeam1) updatedMatches[idx].copy(team1Id = newTeamId)
                                          else updatedMatches[idx].copy(team2Id = newTeamId)
                }
            }
            
            when (match.id) {
                qfs[0].id -> updateMatchTeam(sfs[0].id, true, winnerId)
                qfs[1].id -> updateMatchTeam(sfs[0].id, false, winnerId)
                qfs[2].id -> updateMatchTeam(sfs[1].id, true, winnerId)
                qfs[3].id -> updateMatchTeam(sfs[1].id, false, winnerId)
                sfs[0].id -> {
                    updateMatchTeam(fin.id, true, winnerId)
                    updateMatchTeam(third.id, true, loserId)
                }
                sfs[1].id -> {
                    updateMatchTeam(fin.id, false, winnerId)
                    updateMatchTeam(third.id, false, loserId)
                }
            }
            _state.value = st.copy(matches = updatedMatches)
        }
    }

    fun getStandings(group: String? = null): List<TeamStats> {
        val st = _state.value
        val targetTeams = if (group == null) st.teams else st.teams.filter { it.group == group }
        val groupMatches = st.matches.filter { !it.isKnockout && it.isCompleted }

        return targetTeams.map { team ->
            var p = 0; var w = 0; var d = 0; var l = 0; var gf = 0; var ga = 0
            groupMatches.forEach { m ->
                if (m.team1Id == team.id) {
                    val s1 = m.score1 ?: 0; val s2 = m.score2 ?: 0
                    gf += s1; ga += s2; p++
                    when {
                        s1 > s2 -> w++
                        s1 == s2 -> d++
                        else -> l++
                    }
                } else if (m.team2Id == team.id) {
                    val s1 = m.score1 ?: 0; val s2 = m.score2 ?: 0
                    gf += s2; ga += s1; p++
                    when {
                        s2 > s1 -> w++
                        s2 == s1 -> d++
                        else -> l++
                    }
                }
            }
            TeamStats(
                teamId = team.id, teamName = team.name, flag = team.flag, group = team.group,
                played = p, won = w, drawn = d, lost = l, goalsFor = gf, goalsAgainst = ga, points = (w * 3) + (d * 1)
            )
        }.sortedWith(compareByDescending<TeamStats> { it.points }
            .thenByDescending { it.goalDifference }
            .thenByDescending { it.goalsFor }
            .thenBy { it.teamName })
    }

    fun getBestThirdPlaces(): List<TeamStats> {
        val thirds = listOf("A", "B", "C").mapNotNull { g ->
            getStandings(g).getOrNull(2)
        }
        return thirds.sortedWith(compareByDescending<TeamStats> { it.points }
            .thenByDescending { it.goalDifference }
            .thenByDescending { it.goalsFor }
            .thenBy { it.teamName })
    }

    fun generateKnockoutBracket() {
        if (!isAdmin()) return
        val st = _state.value
        if (st.isKnockoutGenerated) return
        
        val a = getStandings("A")
        val b = getStandings("B")
        val c = getStandings("C")
        
        val bestThirds = getBestThirdPlaces().take(2)
        
        // Ensure we have 8 teams
        val qualifiers = mutableListOf<TeamStats>()
        if (a.size >= 2) qualifiers.addAll(a.take(2))
        if (b.size >= 2) qualifiers.addAll(b.take(2))
        if (c.size >= 2) qualifiers.addAll(c.take(2))
        qualifiers.addAll(bestThirds)
        
        if (qualifiers.size < 8) return // Cannot generate
        
        // QF Pairings (Seeded roughly)
        // QF1: 1A vs Best 3rd
        // QF2: 1B vs 2nd Best 3rd
        // QF3: 1C vs 2A
        // QF4: 2B vs 2C
        val qf1 = Match(team1Id = a[0].teamId, team2Id = bestThirds[0].teamId, isKnockout = true, stage = "QUARTER_FINAL")
        val qf2 = Match(team1Id = b[0].teamId, team2Id = bestThirds.getOrNull(1)?.teamId ?: c[1].teamId, isKnockout = true, stage = "QUARTER_FINAL") // fallback if less thirds
        val qf3 = Match(team1Id = c[0].teamId, team2Id = a[1].teamId, isKnockout = true, stage = "QUARTER_FINAL")
        val qf4 = Match(team1Id = b[1].teamId, team2Id = c[1].teamId, isKnockout = true, stage = "QUARTER_FINAL")
        
        // SF Pairings
        val sf1 = Match(team1Id = "TBD", team2Id = "TBD", isKnockout = true, stage = "SEMI_FINAL")
        val sf2 = Match(team1Id = "TBD", team2Id = "TBD", isKnockout = true, stage = "SEMI_FINAL")
        
        // 3rd & Final
        val thirdPlace = Match(team1Id = "TBD", team2Id = "TBD", isKnockout = true, stage = "THIRD_PLACE")
        val finalMatch = Match(team1Id = "TBD", team2Id = "TBD", isKnockout = true, stage = "FINAL")
        
        val nextMatches = st.matches + listOf(qf1, qf2, qf3, qf4, sf1, sf2, thirdPlace, finalMatch)
        _state.value = st.copy(matches = nextMatches, isKnockoutGenerated = true)
    }

    private fun generateInitialState(): TournamentState {
        val teams = listOf(
            Team(id = "ESP", name = "İspanya", flag = "🇪🇸", group = "A"),
            Team(id = "GER", name = "Almanya", flag = "🇩🇪", group = "A"),
            Team(id = "SUI", name = "İsviçre", flag = "🇨🇭", group = "A"),
            Team(id = "SWE", name = "İsveç", flag = "🇸🇪", group = "A"),
            Team(id = "BRA", name = "Brezilya", flag = "🇧🇷", group = "B"),
            Team(id = "FRA", name = "Fransa", flag = "🇫🇷", group = "B"),
            Team(id = "NED", name = "Hollanda", flag = "🇳🇱", group = "B"),
            Team(id = "NOR", name = "Norveç", flag = "🇳🇴", group = "B"),
            Team(id = "ENG", name = "İngiltere", flag = "🏴󠁧󠁢󠁥󠁮󠁧󠁿", group = "C"),
            Team(id = "POR", name = "Portekiz", flag = "🇵🇹", group = "C"),
            Team(id = "BEL", name = "Belçika", flag = "🇧🇪", group = "C"),
            Team(id = "TUR", name = "Türkiye", flag = "🇹🇷", group = "C")
        )
        
        val matches = mutableListOf<Match>()
        listOf("A", "B", "C").forEach { group ->
            val gTeams = teams.filter { it.group == group }
            val t1 = gTeams[0]; val t2 = gTeams[1]; val t3 = gTeams[2]; val t4 = gTeams[3]
            matches.add(Match(team1Id = t1.id, team2Id = t2.id, stage = "GROUP"))
            matches.add(Match(team1Id = t3.id, team2Id = t4.id, stage = "GROUP"))
            matches.add(Match(team1Id = t1.id, team2Id = t3.id, stage = "GROUP"))
            matches.add(Match(team1Id = t2.id, team2Id = t4.id, stage = "GROUP"))
            matches.add(Match(team1Id = t1.id, team2Id = t4.id, stage = "GROUP"))
            matches.add(Match(team1Id = t2.id, team2Id = t3.id, stage = "GROUP"))
        }
        return TournamentState(teams = teams, matches = matches, goals = emptyList())
    }
}
