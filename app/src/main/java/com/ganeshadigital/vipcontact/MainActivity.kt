package com.ganeshadigital.vipcontact

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ganeshadigital.vipcontact.ui.VipApp
import com.ganeshadigital.vipcontact.ui.VipTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VipTheme {
                VipApp()
            }
        }
    }
}
