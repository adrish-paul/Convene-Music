package com.example.convenemusic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.convenemusic.ui.MainUI
import com.example.convenemusic.ui.MusicViewModel
import com.example.convenemusic.ui.theme.ConveneMusicTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        


        enableEdgeToEdge()
        setContent {
            ConveneMusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainUI(viewModel = viewModel)
                }
            }
        }
    }
}