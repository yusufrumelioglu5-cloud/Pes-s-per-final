package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.UserRole
import com.example.viewmodel.TournamentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: TournamentViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val isAdmin = userRole == UserRole.ADMIN

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var jsonText by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize()) {
        androidx.compose.material3.TopAppBar(
            title = { Text("Ayarlar & Turnuva Yönetimi", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.primary
            ),
            actions = {
                IconButton(onClick = { viewModel.logout() }) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Çıkış Yap",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Session Info Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isAdmin) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) 
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAdmin) Icons.Default.LockOpen else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Aktif Oturum",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isAdmin) "Yönetici (Admin)" else "Misafir (Gözlemci)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Çıkış Yap", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Warning banner for Guests
            if (!isAdmin) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Misafir oturumlarında veri içe aktarma, turnuva sıfırlama ve takım bilgisi güncelleme işlemleri kısıtlanmıştır.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // Backup / Restore Data
            item {
                Text(
                    text = "Veri Yedekleme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Dışa Aktar")
                    }
                    Button(
                        onClick = { showImportDialog = true },
                        enabled = isAdmin,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("İçe Aktar")
                    }
                }
            }
            
            // Reset tournament action (only admin)
            if (isAdmin) {
                item {
                    Button(
                        onClick = { viewModel.resetTournament() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Turnuvayı Sıfırla", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // Edit Teams Section
            item {
                Text(
                    text = "Takımları Düzenle",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            
            items(state.teams, key = { it.id }) { team ->
                var name by remember { mutableStateOf(team.name) }
                var flag by remember { mutableStateOf(team.flag) }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = flag,
                            onValueChange = { 
                                flag = it
                                viewModel.updateTeamInfo(team.id, name, flag) 
                            },
                            enabled = isAdmin,
                            modifier = Modifier.width(72.dp),
                            label = { Text("Bayrak") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { 
                                name = it
                                viewModel.updateTeamInfo(team.id, name, flag) 
                            },
                            enabled = isAdmin,
                            modifier = Modifier.weight(1f),
                            label = { Text("Takım Adı") },
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
    
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Dışa Aktar (JSON)") },
            text = {
                OutlinedTextField(
                    value = viewModel.exportState(),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Kapat") }
            }
        )
    }
    
    if (showImportDialog && isAdmin) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("İçe Aktar") },
            text = {
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    placeholder = { Text("JSON verisini buraya yapıştırın") }
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.importState(jsonText)
                    showImportDialog = false
                    jsonText = ""
                }) { Text("İçe Aktar") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("İptal") }
            }
        )
    }
}
