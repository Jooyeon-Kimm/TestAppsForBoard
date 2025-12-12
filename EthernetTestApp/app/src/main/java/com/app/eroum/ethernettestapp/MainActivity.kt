package com.app.eroum.ethernettestapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.app.eroum.ethernettestapp.ui.ethernet.EthernetTestScreen


class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** {onCreate} */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                EthernetTestScreen(viewModel = viewModel)
            }
        }
    }
}

