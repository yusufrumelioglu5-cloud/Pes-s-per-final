package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Goal
import com.example.model.Match
import com.example.model.Team
import com.example.model.UserRole
import com.example.viewmodel.TournamentViewModel

@Composable
fun KnockoutScreen(viewModel: TournamentViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val isAdmin = userRole == UserRole.ADMIN
    val knockoutMatches = state.matches.filter { it.isKnockout }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar("Çeyrek Final & Elemeler")
        
        if (!isAdmin && state.isKnockoutGenerated) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👁️", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = "Misafir Modu: Maç skorları ve golcüler sadece görüntülenebilir. Düzenleme yetkiniz yoktur.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        if (!state.isKnockoutGenerated) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Grup maçları henüz tamamlanmadı.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val grouped = knockoutMatches.groupBy { it.stage }
                
                listOf("QUARTER_FINAL" to "Çeyrek Final", 
                       "SEMI_FINAL" to "Yarı Final", 
                       "THIRD_PLACE" to "Üçüncülük Maçı", 
                       "FINAL" to "Final").forEach { (stage, title) ->
                    grouped[stage]?.let { matches ->
                        item {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(matches) { match ->
                            val t1 = state.teams.find { it.id == match.team1Id } ?: Team(id = "TBD", name = "TBD", flag = "❓", group = "")
                            val t2 = state.teams.find { it.id == match.team2Id } ?: Team(id = "TBD", name = "TBD", flag = "❓", group = "")
                            val matchGoals = state.goals.filter { it.matchId == match.id }
                            KnockoutMatchCard(
                                match = match,
                                team1 = t1,
                                team2 = t2,
                                goals = matchGoals,
                                isAdmin = isAdmin,
                                onScoreChanged = { s1, s2 -> viewModel.updateMatchScore(match.id, s1, s2) },
                                onPenaltiesChanged = { p1, p2 -> viewModel.updateMatchPenalties(match.id, p1, p2) },
                                onGoalAdded = { tId, name -> viewModel.addGoal(match.id, tId, name) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnockoutMatchCard(
    match: Match,
    team1: Team,
    team2: Team,
    goals: List<Goal>,
    isAdmin: Boolean,
    onScoreChanged: (Int?, Int?) -> Unit,
    onPenaltiesChanged: (Int?, Int?) -> Unit,
    onGoalAdded: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Standard Match UI
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(team1.flag, style = MaterialTheme.typography.headlineMedium)
                    Text(team1.name, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.5f), horizontalArrangement = Arrangement.Center) {
                    ScoreSelector(match.score1, enabled = isAdmin && team1.id != "TBD" && team2.id != "TBD") { onScoreChanged(it, match.score2) }
                    Text(" - ", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                    ScoreSelector(match.score2, enabled = isAdmin && team1.id != "TBD" && team2.id != "TBD") { onScoreChanged(match.score1, it) }
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(team2.flag, style = MaterialTheme.typography.headlineMedium)
                    Text(team2.name, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            
            // Penalties if drawn
            if (match.score1 != null && match.score1 == match.score2 && team1.id != "TBD" && team2.id != "TBD") {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(4.dp))
                Text("Penaltılar", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.secondary)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScoreSelector(match.penalty1, enabled = isAdmin) { onPenaltiesChanged(it, match.penalty2) }
                    Text(" - ", modifier = Modifier.padding(horizontal = 16.dp), fontWeight = FontWeight.Bold)
                    ScoreSelector(match.penalty2, enabled = isAdmin) { onPenaltiesChanged(match.penalty1, it) }
                }
            }

            // Display Goals if any
            if (goals.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val team1Goals = goals.filter { it.teamId == team1.id }
                    val team2Goals = goals.filter { it.teamId == team2.id }
                    
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        team1Goals.forEach { goal ->
                            Text("⚽ ${goal.scorerName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        team2Goals.forEach { goal ->
                            Text("${goal.scorerName} ⚽", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Goal Scorers Input for Knockouts (Optional but excellent quality)
            val totalGoals = (match.score1 ?: 0) + (match.score2 ?: 0)
            if (totalGoals > 0 && isAdmin && team1.id != "TBD" && team2.id != "TBD") {
                var showScorerInput by remember { mutableStateOf(false) }
                TextButton(
                    onClick = { showScorerInput = !showScorerInput },
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                ) {
                    Text(if (showScorerInput) "Golcüleri Gizle" else "Golcüleri Gir")
                }
                
                if (showScorerInput) {
                    var scorerName by remember { mutableStateOf("") }
                    var selectedTeamId by remember { mutableStateOf(team1.id) }
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = scorerName,
                            onValueChange = { scorerName = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Golcü Adı") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (scorerName.isNotBlank()) {
                                onGoalAdded(selectedTeamId, scorerName)
                                scorerName = ""
                            }
                        }) {
                            Text("Ekle")
                        }
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                        FilterChip(
                            selected = selectedTeamId == team1.id,
                            onClick = { selectedTeamId = team1.id },
                            label = { Text(team1.name) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        FilterChip(
                            selected = selectedTeamId == team2.id,
                            onClick = { selectedTeamId = team2.id },
                            label = { Text(team2.name) }
                        )
                    }
                }
            }
        }
    }
}
