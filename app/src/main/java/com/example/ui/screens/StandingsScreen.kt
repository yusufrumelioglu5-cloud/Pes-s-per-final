package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.TeamStats
import com.example.viewmodel.TournamentViewModel

@Composable
fun StandingsScreen(viewModel: TournamentViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar("Puan Durumu")
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            listOf("A", "B", "C").forEach { group ->
                item {
                    StandingsTable(title = "Grup $group", standings = viewModel.getStandings(group))
                }
            }
            
            item {
                StandingsTable(title = "En İyi 3.ler (İlk 2 Çıkar)", standings = viewModel.getBestThirdPlaces())
            }
        }
    }
}

@Composable
fun StandingsTable(title: String, standings: List<TeamStats>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Takım", modifier = Modifier.weight(3f), fontWeight = FontWeight.Bold)
                Text("O", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Text("Av", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Text("P", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            
            standings.forEachIndexed { index, stat ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(3f), verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}. ", fontWeight = FontWeight.Bold)
                        Text(stat.flag, modifier = Modifier.padding(horizontal = 4.dp))
                        Text(stat.teamName, maxLines = 1)
                    }
                    Text(stat.played.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(stat.goalDifference.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text(stat.points.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
