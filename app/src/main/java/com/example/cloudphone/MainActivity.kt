package com.example.cloudphone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cloudphone.data.model.CloudPhone
import com.example.cloudphone.data.repository.CloudPhoneRepository
import com.example.cloudphone.ui.home.HomeScreen
import com.example.cloudphone.ui.remote.RemoteControlScreen
import com.example.cloudphone.ui.token.TokenManagerScreen
import com.example.cloudphone.ui.theme.CloudPhoneTheme
import com.example.cloudphone.utils.TokenManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokenManager = TokenManager(this)

        setContent {
            CloudPhoneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(tokenManager)
                }
            }
        }
    }
}

@Composable
fun App(tokenManager: TokenManager) {
    val navController = rememberNavController()

    val startDestination = remember {
        runBlocking {
            val tokens = tokenManager.getTokensFlow().first()
            if (tokens.isEmpty()) "tokenManager" else "home"
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        // Token 管理页
        composable("tokenManager") {
            TokenManagerScreen(
                navController = navController,
                tokenManager = tokenManager,
                onHasToken = {
                    navController.navigate("home") {
                        popUpTo("tokenManager") { inclusive = true }
                    }
                }
            )
        }

        // 首页
        composable("home") {
            val repository = remember(tokenManager) {
                CloudPhoneRepository.create(
                    tokenProvider = {
                        runBlocking { tokenManager.getActiveTokenFlow().first() } ?: ""
                    },
                    deviceIdProvider = {
                        tokenManager.getActiveDeviceId()
                    }
                )
            }
            HomeScreen(
                navController = navController,
                tokenManager = tokenManager,
                repository = repository,
                onNavigateToTokens = { navController.navigate("tokenManager") },
                onNavigateToRemote = { phoneJson ->
                    navController.navigate("remote/${java.net.URLEncoder.encode(phoneJson, "UTF-8")}")
                }
            )
        }

        // 远程控制页
        composable(
            route = "remote/{phoneJson}",
            arguments = listOf(navArgument("phoneJson") { type = NavType.StringType })
        ) { backStackEntry ->
            val phoneJson = backStackEntry.arguments?.getString("phoneJson") ?: ""
            val phone = remember {
                Gson().fromJson(java.net.URLDecoder.decode(phoneJson, "UTF-8"), CloudPhone::class.java)
            }
            RemoteControlScreen(
                navController = navController,
                cloudPhone = phone,
                tokenManager = tokenManager
            )
        }
    }
}
