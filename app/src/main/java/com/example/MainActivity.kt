package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.NoteDatabase
import com.example.data.repository.LabelRepositoryImpl
import com.example.data.repository.NoteRepositoryImpl
import com.example.domain.usecase.*
import com.example.presentation.screens.HomeScreen
import com.example.presentation.screens.NoteEditScreen
import com.example.presentation.viewmodel.NoteViewModel
import com.example.presentation.viewmodel.NoteViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    // 🔥 Modern permission launcher (BEST PRACTICE)
    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                // optional fallback logic
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        askNotificationPermission()

        // =========================
        // DB INIT
        // =========================
        val database = NoteDatabase.getDatabase(applicationContext)
        val noteDao = database.noteDao()
        val labelDao = database.labelDao()

        val noteRepository = NoteRepositoryImpl(noteDao)
        val labelRepository = LabelRepositoryImpl(labelDao)

        val noteUseCases = NoteUseCases(
            getAllNotes = GetAllNotesUseCase(noteRepository),
            getNote = GetNoteUseCase(noteRepository),
            saveNote = SaveNoteUseCase(noteRepository),
            deleteNote = DeleteNoteUseCase(noteRepository),
            searchNotes = SearchNotesUseCase(noteRepository)
        )

        val labelUseCases = LabelUseCases(
            getAllLabels = GetAllLabelsUseCase(labelRepository),
            saveLabel = SaveLabelUseCase(labelRepository),
            deleteLabel = DeleteLabelUseCase(labelRepository)
        )

        val factory = NoteViewModelFactory(noteUseCases, labelUseCases)
        val viewModel = ViewModelProvider(this, factory)[NoteViewModel::class.java]

        // =========================
        // UI
        // =========================
        setContent {
            MyApplicationTheme {

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {

                    composable("home") {
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToEdit = { noteId ->
                                navController.navigate("note_edit/$noteId")
                            }
                        )
                    }

                    composable(
                        route = "note_edit/{noteId}",
                        arguments = listOf(navArgument("noteId") {
                            type = NavType.LongType
                        })
                    ) { backStackEntry ->

                        val noteId = backStackEntry.arguments?.getLong("noteId") ?: -1L

                        NoteEditScreen(
                            noteId = noteId,
                            viewModel = viewModel,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    // 🔥 MODERN PERMISSION HANDLING
    private fun askNotificationPermission() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val permission = Manifest.permission.POST_NOTIFICATIONS

        val granted = ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(permission)
        }
    }
}
