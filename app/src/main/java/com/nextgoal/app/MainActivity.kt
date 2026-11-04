package com.nextgoal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nextgoal.app.ui.navigation.NextGoalNavGraph
import com.nextgoal.app.ui.theme.BgDeep
import com.nextgoal.app.ui.theme.NextGoalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NextGoalTheme {
                Surface(modifier = Modifier.fillMaxSize().background(BgDeep)) {
                    NextGoalNavGraph()
                }
            }
        }
    }
}
