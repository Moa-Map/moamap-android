package com.example.moamap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.moamap.core.designsystem.theme.MoaMapTheme
import com.example.moamap.core.navigation.MoaMapNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoaMapTheme {
                MoaMapNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
