package com.shadabshaikh.networth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shadabshaikh.networth.ui.NetworthApp
import com.shadabshaikh.networth.ui.NetworthViewModel
import com.shadabshaikh.networth.ui.theme.NetworthTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: NetworthViewModel = viewModel()
            val state by vm.state.collectAsState()
            NetworthTheme(dark = state.theme != "light") {
                NetworthApp(vm)
            }
        }
    }
}
