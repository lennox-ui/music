package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.GenerativeViewModel
import com.google.ai.client.generativeai.GenerativeModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val generativeModel = GenerativeModel(
      modelName = "gemini-1.5-flash",
      apiKey = BuildConfig.GEMINI_API_KEY
    )

    setContent {
      MyApplicationTheme {
         AppEntry(generativeModel)
      }
    }
  }
}

@Composable
fun AppEntry(generativeModel: GenerativeModel) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            launcher.launch(permission)
        }
    }

    val viewModel: MainViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    // Trigger reload if permission becomes granted newly
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.loadLocalSongs()
        }
    }

    val shouldShowSplash = uiState.showSplash || uiState.isLoading || !hasPermission

    if (shouldShowSplash) {
        SplashScreen(
            hasPermission = hasPermission,
            isLoading = uiState.isLoading,
            onRetryPermission = {
                launcher.launch(permission)
            }
        )
    } else {
        MainApp(viewModel)
    }
}

@Composable
fun SplashScreen(
    hasPermission: Boolean,
    isLoading: Boolean,
    onRetryPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.my_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(32.dp)),
            contentScale = ContentScale.Crop
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp)
        ) {
            if (!hasPermission) {
                Text(
                    text = "Storage access is required to play your local music files.",
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onRetryPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1DB954)
                    )
                ) {
                    Text("Grant Permission", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF1DB954),
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp
                )
            }
        }
    }
}
