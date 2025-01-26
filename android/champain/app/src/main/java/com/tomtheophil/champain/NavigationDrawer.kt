package com.tomtheophil.champain

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
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
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            Box(modifier = Modifier.fillMaxHeight()) {
                ModalDrawerSheet(
                    drawerContainerColor = Color.Black.copy(alpha = 0.5f),
                    windowInsets = DrawerDefaults.windowInsets,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    NavigationDrawerItem(
                        label = { 
                            Text(
                                "Game",
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White
                            )
                        },
                        selected = selectedPage == 0,
                        onClick = {
                            onPageSelected(0)
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        label = { 
                            Text(
                                "Settings",
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White
                            )
                        },
                        selected = selectedPage == 1,
                        onClick = {
                            onPageSelected(1)
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        label = { 
                            Text(
                                "Calibrate",
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White
                            )
                        },
                        selected = false,
                        onClick = {
                            onCalibrate()
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        content()
    }
} 