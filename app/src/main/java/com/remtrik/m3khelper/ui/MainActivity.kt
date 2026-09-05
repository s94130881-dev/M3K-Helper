package com.remtrik.m3khelper.ui

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

import com.remtrik.m3khelper.BuildConfig
import com.remtrik.m3khelper.prefs

import com.remtrik.m3khelper.ui.component.NoRoot
import com.remtrik.m3khelper.ui.component.UnknownDevice
import com.remtrik.m3khelper.ui.component.UpdateDialog
import com.remtrik.m3khelper.ui.theme.M3KHelperTheme

import com.remtrik.m3khelper.util.collapseTransition
import com.remtrik.m3khelper.util.expandTransition
import com.remtrik.m3khelper.util.fadeEnterTransition
import com.remtrik.m3khelper.util.fadeExitTransition
import com.remtrik.m3khelper.util.funcs.Download.checkNewVersion
import com.remtrik.m3khelper.util.funcs.LatestVersionInfo
import com.remtrik.m3khelper.util.slideFromRightEnterTransition
import com.remtrik.m3khelper.util.slideToLeftExitTransition
import com.remtrik.m3khelper.util.slideToRightExitTransition
import com.remtrik.m3khelper.util.variables.FontSize
import com.remtrik.m3khelper.util.variables.LineHeight
import com.remtrik.m3khelper.util.variables.PaddingValue
import com.remtrik.m3khelper.util.variables.device
import com.remtrik.m3khelper.util.variables.sdp
import com.remtrik.m3khelper.util.variables.showWarningCard
import com.remtrik.m3khelper.util.variables.ssp

import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.LinksScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ThemeEngineScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator

import com.topjohnwu.superuser.Shell

import kotlinx.coroutines.delay

import rikka.shizuku.Shizuku

private const val SHIZUKU_REQUEST_CODE = 1001

class MainActivity : ComponentActivity() {

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->

            if (requestCode != SHIZUKU_REQUEST_CODE) {
                return@OnRequestPermissionResultListener
            }

            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                recreate()
            }
        }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Shizuku.addRequestPermissionResultListener(
            shizukuPermissionListener
        )

        enableEdgeToEdge()

        window.isNavigationBarContrastEnforced = false

        requestedOrientation = resolveOrientation()

        setContent {
            M3KHelperTheme {

                InitDimens()

                PrivilegedAccessContent(
                    onRequestShizuku = {
                        requestShizukuPermission()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(
            shizukuPermissionListener
        )

        super.onDestroy()
    }

    private fun requestShizukuPermission() {

        if (Shizuku.isPreV11()) {
            return
        }

        val granted =
            try {
                Shizuku.checkSelfPermission() ==
                    PackageManager.PERMISSION_GRANTED
            } catch (_: Throwable) {
                false
            }

        if (granted) {
            return
        }

        val canRequest =
            try {
                !Shizuku.shouldShowRequestPermissionRationale()
            } catch (_: Throwable) {
                true
            }

        if (!canRequest) {
            return
        }

        try {
            Shizuku.requestPermission(
                SHIZUKU_REQUEST_CODE
            )
        } catch (_: Throwable) {
        }
    }

    private fun resolveOrientation(): Int {

        val forceRotation =
            prefs.getBoolean(
                "force_rotation",
                false
            )

        val isNabu =
            Build.DEVICE == "nabu"

        val isDebugEmulator =
            BuildConfig.DEBUG &&
                Build.DEVICE == "emu64xa"

        return if (
            isNabu ||
            isDebugEmulator ||
            forceRotation
        ) {
            ActivityInfo.SCREEN_ORIENTATION_FULL_USER
        } else {
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }
    }
}

@Composable
private fun PrivilegedAccessContent(
    onRequestShizuku: () -> Unit
) {

    var rootAvailable by remember {
        mutableStateOf(false)
    }

    var shizukuRunning by remember {
        mutableStateOf(false)
    }

    var shizukuGranted by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {

        while (true) {

            rootAvailable =
                try {
                    Shell.isAppGrantedRoot() == true
                } catch (_: Throwable) {
                    false
                }

            shizukuRunning =
                try {
                    Shizuku.pingBinder()
                } catch (_: Throwable) {
                    false
                }

            shizukuGranted =
                if (shizukuRunning) {
                    try {
                        Shizuku.checkSelfPermission() ==
                            PackageManager.PERMISSION_GRANTED
                    } catch (_: Throwable) {
                        false
                    }
                } else {
                    false
                }

            delay(1000)
        }
    }

    when {

        rootAvailable -> {
            M3KRootContent()
        }

        shizukuRunning && shizukuGranted -> {
            M3KRootContent()
        }

        shizukuRunning -> {
            ShizukuPermissionScreen(
                onRequestPermission = onRequestShizuku
            )
        }

        else -> {
            NoRoot()
        }
    }
}

@Composable
private fun ShizukuPermissionScreen(
    onRequestPermission: () -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),

            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = "Shizuku",
                style =
                    MaterialTheme.typography.headlineSmall
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text =
                    "O M3K Helper encontrou o Shizuku, mas a permissão ainda não foi concedida.",

                style =
                    MaterialTheme.typography.bodyLarge
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxSize()
            ) {
                Text("CONCEDER PERMISSÃO")
            }
        }
    }
}
