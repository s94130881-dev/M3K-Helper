package com.remtrik.m3khelper.ui

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.LinksScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ThemeEngineScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.utils.isRouteOnBackStackAsState
import com.ramcosta.composedestinations.utils.rememberDestinationsNavigator
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
import com.topjohnwu.superuser.Shell
import rikka.shizuku.Shizuku

private const val SHIZUKU_PERMISSION_CODE = 1001

class MainActivity : ComponentActivity() {

    private var shizukuGranted = false

    private val shizukuListener =
        object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(
                requestCode: Int,
                grantResult: Int
            ) {
                if (requestCode == SHIZUKU_PERMISSION_CODE) {
                    shizukuGranted =
                        grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED

                    setContent {
                        M3KHelperTheme {
                            InitDimens()
                            MainPermissionGate()
                        }
                    }
                }
            }
        }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        requestedOrientation = resolveOrientation()

        shizukuGranted = checkShizukuPermission()

        Shizuku.addRequestPermissionResultListener(shizukuListener)

        setContent {
            M3KHelperTheme {
                InitDimens()
                MainPermissionGate()
            }
        }
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(shizukuListener)
        super.onDestroy()
    }

    private fun checkShizukuPermission(): Boolean {
        return try {
            Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    private fun requestShizukuPermission() {
        try {
            if (!Shizuku.pingBinder()) {
                return
            }

            if (Shizuku.checkSelfPermission() !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_CODE)
            }
        } catch (_: Throwable) {
        }
    }

    private fun resolveOrientation(): Int {
        val forceRotation = prefs.getBoolean("force_rotation", false)
        val isNabu = Build.DEVICE == "nabu"
        val isDebugEmulator =
            BuildConfig.DEBUG && Build.DEVICE == "emu64xa"

        return if (
            isNabu ||
            isDebugEmulator ||
            forceRotation
        ) {
            SCREEN_ORIENTATION_FULL_USER
        } else {
            SCREEN_ORIENTATION_USER_PORTRAIT
        }
    }

    @Composable
    private fun MainPermissionGate() {

        val isA07 = isGalaxyA07()

        when {
            !isShizukuAvailable() -> {
                ShizukuUnavailable(
                    isA07 = isA07
                )
            }

            !shizukuGranted -> {
                ShizukuPermissionRequired(
                    onRequest = {
                        requestShizukuPermission()
                    }
                )
            }

            else -> {
                M3KRootContent(
                    isA07 = isA07
                )
            }
        }
    }

    private fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    private fun isGalaxyA07(): Boolean {
        val model = Build.MODEL.uppercase()
        val device = Build.DEVICE.lowercase()
        val product = Build.PRODUCT.lowercase()

        return model == "SM-A075M" ||
            model.contains("SM-A075") ||
            device == "a07" ||
            product.contains("a075")
    }
}

@Composable
private fun ShizukuUnavailable(
    isA07: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = buildString {
                append("M3K Helper\n\n")
                append("Shizuku não está disponível.\n\n")

                if (isA07) {
                    append(
                        "Dispositivo detectado: Samsung SM-A075M.\n\n"
                    )
                } else {
                    append(
                        "Dispositivo: ${Build.MODEL}.\n\n"
                    )
                }

                append(
                    "Inicie o serviço Shizuku e abra novamente o aplicativo."
                )
            }
        )
    }
}

@Composable
private fun ShizukuPermissionRequired(
    onRequest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        androidx.compose.foundation.layout.Column {

            Text(
                text = "Permissão Shizuku necessária"
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text =
                    "O M3K Helper precisa de acesso privilegiado " +
                    "fornecido pelo Shizuku para executar operações " +
                    "compatíveis com o sistema."
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Button(
                onClick = onRequest
            ) {
                Text("Conceder Shizuku")
            }
        }
    }
}

@Composable
internal fun InitDimens() {
    LineHeight = 20.ssp()
    FontSize = 15.ssp()
    PaddingValue = 10.sdp()
}

@Composable
internal fun M3KRootContent(
    isA07: Boolean
) {
    val navController = rememberNavController()
    val navigator = navController.rememberDestinationsNavigator()
    val orientation = LocalConfiguration.current.orientation

    var latestVersion by remember {
        mutableStateOf(
            LatestVersionInfo()
        )
    }

    LaunchedEffect(Unit) {
        if (prefs.getBoolean("check_update", true)) {
            latestVersion = checkNewVersion()
        }
    }

    val hasNewVersion =
        latestVersion.versionCode > BuildConfig.VERSION_CODE

    val bottomBarRoutes = remember {
        Destinations.entries
            .map { it.route.route }
            .toSet()
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible =
                    orientation !=
                        Configuration.ORIENTATION_LANDSCAPE,

                enter = slideInVertically { it },

                exit = slideOutVertically { it }
            ) {
                BottomNavigationBar(
                    navController,
                    navigator
                )
            }
        }
    ) { innerPadding ->

        Row {

            AnimatedVisibility(
                visible =
                    orientation ==
                        Configuration.ORIENTATION_LANDSCAPE,

                enter =
                    slideInHorizontally { -it },

                exit =
                    slideOutHorizontally { -it }
            ) {
                LeftNavigationBar(
                    navController,
                    navigator
                )
            }

            Box(
                modifier = Modifier
                    .padding(
                        bottom =
                            innerPadding.calculateBottomPadding()
                    )
                    .fillMaxSize()
            ) {

                DestinationsNavHost(
                    navGraph = NavGraphs.root,
                    navController = navController,

                    defaultTransitions =
                        object :
                            NavHostAnimatedDestinationStyle() {

                            override val enterTransition:
                                AnimatedContentTransitionScope
                                <NavBackStackEntry>.
                                () -> EnterTransition = {

                                if (
                                    targetState
                                        .destination
                                        .route !in
                                    bottomBarRoutes
                                ) {
                                    slideFromRightEnterTransition
                                } else {
                                    fadeEnterTransition
                                }
                            }

                            override val exitTransition:
                                AnimatedContentTransitionScope
                                <NavBackStackEntry>.
                                () -> ExitTransition = {

                                if (
                                    targetState
                                        .destination
                                        .route !in
                                    bottomBarRoutes
                                ) {

                                    if (
                                        targetState
                                            .destination
                                            .route ==
                                        SettingsScreenDestination
                                            .route ||

                                        targetState
                                            .destination
                                            .route ==
                                        ThemeEngineScreenDestination
                                            .route
                                    ) {
                                        slideToLeftExitTransition
                                    } else {
                                        slideToRightExitTransition
                                    }

                                } else {
                                    fadeExitTransition
                                }
                            }
                        }
                )

                val isWarningVisible
                    by showWarningCard
                        .collectAsStateWithLifecycle()

                if (isWarningVisible) {
                    UnknownDevice()
                }

                /*
                 * IMPORTANTE:
                 *
                 * O SM-A075M pode ser detectado aqui,
                 * mas NÃO é declarado como Windows-compatible.
                 *
                 * Detectar o aparelho != possuir suporte
                 * de boot para Windows.
                 */
                if (isA07) {
                    A07CompatibilityNotice()
                }
            }

            AnimatedVisibility(
                visible = hasNewVersion,

                enter = expandTransition,

                exit = collapseTransition
            ) {
                UpdateDialog(latestVersion)
            }
        }
    }
}

@Composable
private fun A07CompatibilityNotice() {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = "Galaxy A07 detectado"
            )
        },
        text = {
            Text(
                text =
                    "SM-A075M detectado.\n\n" +
                    "O aplicativo não declara suporte " +
                    "de Windows 11 neste aparelho porque " +
                    "a detecção do modelo não significa que " +
                    "exista um método de boot compatível.\n\n" +
                    "Não será utilizada uma UEFI de outro aparelho."
            )
        },
        confirmButton = {}
    )
}

@Composable
private fun getVisibleDestinations():
    List<Destinations> {

    val currentDeviceCard
        by device.currentDeviceCard
            .collectAsStateWithLifecycle()

    return remember(currentDeviceCard) {

        Destinations.entries.filter { destination ->

            !(
                currentDeviceCard.noLinks &&
                    destination.route ==
                    LinksScreenDestination
            )
        }
    }
}

@Composable
private fun BottomNavigationBar(
    navController: NavHostController,
    navigator: DestinationsNavigator
) {
    NavigationBar(
        tonalElevation = 12.dp,

        windowInsets =
            WindowInsets.systemBars
                .union(WindowInsets.displayCutout)
                .only(
                    WindowInsetsSides.Horizontal +
                        WindowInsetsSides.Bottom
                ),

        modifier =
            Modifier.height(120.sdp())
    ) {

        getVisibleDestinations()
            .filterNot {
                it.landscapeOnly
            }
            .forEach { destination ->

                val isCurrentDestOnBackStack
                    by navController
                        .isRouteOnBackStackAsState(
                            destination.route
                        )

                NavigationBarItem(

                    selected =
                        isCurrentDestOnBackStack,

                    onClick = {
                        navigateTo(
                            destination,
                            isCurrentDestOnBackStack,
                            navigator
                        )
                    },

                    icon = {
                        NavigationIcon(
                            destination,
                            isCurrentDestOnBackStack
                        )
                    },

                    label = {
                        Text(
                            text =
                                stringResource(
                                    destination.label
                                ),

                            fontSize =
                                10.ssp()
                        )
                    },

                    alwaysShowLabel = false
                )
            }
    }
}

@Composable
private fun LeftNavigationBar(
    navController: NavHostController,
    navigator: DestinationsNavigator
) {
    NavigationRail(
        modifier =
            Modifier.width(110.sdp()),

        windowInsets =
            WindowInsets.systemBars.only(
                WindowInsetsSides.Bottom +
                    WindowInsetsSides.Top
            )
    ) {

        getVisibleDestinations()
            .forEach { destination ->

                if (
                    destination.route ==
                    SettingsScreenDestination
                ) {
                    Spacer(
                        Modifier.weight(1f)
                    )
                }

                val isCurrentDestOnBackStack
                    by navController
                        .isRouteOnBackStackAsState(
                            destination.route
                        )

                NavigationRailItem(

                    selected =
                        isCurrentDestOnBackStack,

                    onClick = {
                        navigateTo(
                            destination,
                            isCurrentDestOnBackStack,
                            navigator
                        )
                    },

                    icon = {
                        NavigationIcon(
                            destination,
                            isCurrentDestOnBackStack
                        )
                    },

                    label = {
                        Text(
                            text =
                                stringResource(
                                    destination.label
                                ),

                            fontSize =
                                10.ssp()
                        )
                    },

                    alwaysShowLabel = false
                )
            }
    }
}

@Composable
private fun NavigationIcon(
    destination: Destinations,
    selected: Boolean
) {
    val icon =
        if (selected) {
            destination.iconSelected
        } else {
            destination.iconNotSelected
        }

    Icon(
        imageVector = icon,

        contentDescription =
            stringResource(
                destination.label
            ),

        modifier =
            Modifier.size(20.sdp())
    )
}

private fun navigateTo(
    destination: Destinations,
    isSelected: Boolean,
    navigator: DestinationsNavigator
) {
    if (isSelected) {
        navigator.popBackStack(
            destination.route,
            false
        )
    }

    navigator.navigate(
        destination.route
    ) {
        popUpTo(NavGraphs.root) {
            saveState = true
        }

        launchSingleTop = true
        restoreState = true
    }
}
