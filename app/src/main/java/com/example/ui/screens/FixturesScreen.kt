package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Goal
import com.example.model.Match
import com.example.model.Team
import com.example.model.UserRole
import com.example.viewmodel.TournamentViewModel

@Composable
fun FixturesScreen(viewModel: TournamentViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val isAdmin = userRole == UserRole.ADMIN
    val groupMatches = state.matches.filter { !it.isKnockout }
    
    val allCompleted = groupMatches.all { it.isCompleted }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar("Fikstür & Skorlar")
        
        if (!isAdmin) {
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
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(groupMatches) { match ->
                val team1 = state.teams.find { it.id == match.team1Id }
                val team2 = state.teams.find { it.id == match.team2Id }
                val matchGoals = state.goals.filter { it.matchId == match.id }
                if (team1 != null && team2 != null) {
                    MatchCard(
                        match = match,
                        team1 = team1,
                        team2 = team2,
                        goals = matchGoals,
                        isAdmin = isAdmin,
                        onScoreChanged = { s1, s2 -> viewModel.updateMatchScore(match.id, s1, s2) },
                        onGoalAdded = { teamId, scorerName -> viewModel.addGoal(match.id, teamId, scorerName) }
                    )
                }
            }
            
            if (allCompleted && !state.isKnockoutGenerated && isAdmin) {
                item {
                    Button(
                        onClick = { viewModel.generateKnockoutBracket() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        Text("Elemeleri Oluştur (Çeyrek Final)")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchCard(
    match: Match,
    team1: Team,
    team2: Team,
    goals: List<Goal>,
    isAdmin: Boolean,
    onScoreChanged: (Int?, Int?) -> Unit,
    onGoalAdded: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${team1.group}. Grup",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Team 1
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(team1.flag, style = MaterialTheme.typography.headlineMedium)
                    Text(team1.name, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                
                // Scores
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1.5f)
                ) {
                    ScoreSelector(match.score1, enabled = isAdmin) { newScore -> onScoreChanged(newScore, match.score2) }
                    Text(" - ", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                    ScoreSelector(match.score2, enabled = isAdmin) { newScore -> onScoreChanged(match.score1, newScore) }
                }
                
                // Team 2
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(team2.flag, style = MaterialTheme.typography.headlineMedium)
                    Text(team2.name, fontWeight = FontWeight.Bold, maxLines = 1)
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

            // Goal Scorers Input
            val totalGoals = (match.score1 ?: 0) + (match.score2 ?: 0)
            if (totalGoals > 0 && isAdmin) {
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

@Composable
fun ScoreSelector(score: Int?, enabled: Boolean, onScoreChange: (Int?) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { if ((score ?: 0) > 0) onScoreChange((score ?: 0) - 1) },
            enabled = enabled
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease")
        }
        Text(
            text = score?.toString() ?: "-",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        IconButton(
            onClick = { onScoreChange((score ?: 0) + 1) },
            enabled = enabled
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(title: String) {
    androidx.compose.material3.TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.primary
        )
    )
}
