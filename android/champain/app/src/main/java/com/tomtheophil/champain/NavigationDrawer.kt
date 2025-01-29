package com.tomtheophil.champain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun NavigationDrawer(
    drawerState: DrawerState,
    selectedPage: Int,
    onPageSelected: (Int) -> Unit,
    onCalibrate: () -> Unit,
    scope: CoroutineScope,
    isConnected: Boolean,
    content: @Composable () -> Unit
) {
    var settingsExpanded by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.Black.copy(alpha = 0.80f),
                modifier = Modifier.width(350.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // App Icon
                        Icon(
                            painter = painterResource(id = R.drawable.champagne_party_svgrepo_com),
                            contentDescription = "Champagne Icon",
                            modifier = Modifier.size(150.dp),
                            tint = Color.Unspecified
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // App Name
                        Text(
                            text = "Champain",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Navigation items
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NavigationDrawerItem(
                                label = { Text("Game", color = Color.White) },
                                selected = selectedPage == 0,
                                onClick = {
                                    onPageSelected(0)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent,
                                    selectedContainerColor = Color.White.copy(alpha = 0.2f)
                                )
                            )

                            // Settings section
                            NavigationDrawerItem(
                                label = { 
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Settings", color = Color.White)
                                        Icon(
                                            imageVector = if (settingsExpanded) 
                                                Icons.Default.KeyboardArrowUp 
                                            else 
                                                Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (settingsExpanded) 
                                                "Collapse" 
                                            else 
                                                "Expand",
                                            tint = Color.White
                                        )
                                    }
                                },
                                selected = selectedPage in 1..3,
                                onClick = { settingsExpanded = !settingsExpanded },
                                modifier = Modifier.padding(horizontal = 12.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent,
                                    selectedContainerColor = Color.White.copy(alpha = 0.2f)
                                )
                            )

                            if (settingsExpanded) {
                                NavigationDrawerItem(
                                    label = { Text("Shake", color = Color.White) },
                                    selected = selectedPage == 1,
                                    onClick = {
                                        onPageSelected(1)
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 0.dp, bottom = 0.dp),
                                    colors = NavigationDrawerItemDefaults.colors(
                                        unselectedContainerColor = Color.Transparent,
                                        selectedContainerColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )

                                NavigationDrawerItem(
                                    label = { Text("Orientation", color = Color.White) },
                                    selected = selectedPage == 2,
                                    onClick = {
                                        onPageSelected(2)
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 0.dp, bottom = 0.dp),
                                    colors = NavigationDrawerItemDefaults.colors(
                                        unselectedContainerColor = Color.Transparent,
                                        selectedContainerColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )

                                NavigationDrawerItem(
                                    label = { Text("Connection", color = Color.White) },
                                    selected = selectedPage == 3,
                                    onClick = {
                                        onPageSelected(3)
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 0.dp, bottom = 0.dp),
                                    colors = NavigationDrawerItemDefaults.colors(
                                        unselectedContainerColor = Color.Transparent,
                                        selectedContainerColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                            }

                            NavigationDrawerItem(
                                label = { Text("Calibrate", color = if (isConnected) Color.White else UIConstants.CONTENT_DISABLED) },
                                selected = false,
                                onClick = {
                                    if (isConnected) {
                                        onCalibrate()
                                        scope.launch { drawerState.close() }
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    unselectedContainerColor = Color.Transparent,
                                    selectedContainerColor = Color.White.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }

                    // Instructions at the bottom
                    Text(
                        "How to use:\n\n" +
                        "1. Connect in Settings\n" +
                        "2. Hold phone upright, press Calibrate\n" +
                        "3. Go to Game to shake & tap",
                        color = UIConstants.CONTENT_SECONDARY,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(
                                start = 8.dp,
                                end = 4.dp,
                                bottom = 96.dp
                            )
                    )
                }
            }
        }
    ) {
        content()
    }
} 