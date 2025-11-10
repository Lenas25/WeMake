package com.utp.wemake;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.utp.wemake.models.Board;
import com.utp.wemake.viewmodels.MainViewModel;
import com.utp.wemake.viewmodels.VoiceTaskViewModel;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceTaskActivity extends AppCompatActivity {

    private LottieAnimationView lottieAnimationView;
    private TextView tvStatusListening, tvRecognizedText;
    private FloatingActionButton fabStartListening;
    private LinearLayout btnStop, btnCancel;

    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private MainViewModel mainViewModel;
    private VoiceTaskViewModel viewModel;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    fabStartListening.performClick(); // Simula un clic para empezar a escuchar
                } else {
                    Toast.makeText(this, "Permiso de micrófono denegado.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_voice_task);

        // Obtener el MainViewModel para acceder al board seleccionado
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel = new ViewModelProvider(this).get(VoiceTaskViewModel.class);
        observeViewModel();

        initializeViews();
        setupToolbar();
        setupListeners();
        setupSpeechRecognizer();

        // La UI empieza en estado de reposo (Idle)
        updateUiToIdleState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // --- MUY IMPORTANTE: Liberar los recursos del SpeechRecognizer ---
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    private void initializeViews() {
        lottieAnimationView = findViewById(R.id.lottie_animation_view);
        tvStatusListening = findViewById(R.id.tv_status_listening);
        tvRecognizedText = findViewById(R.id.tv_recognized_text);
        fabStartListening = findViewById(R.id.fab_start_listening);
        btnStop = findViewById(R.id.btn_stop);
        btnCancel = findViewById(R.id.btn_cancel);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.top_app_bar);
        toolbar.setTitle(R.string.title_voice_creation);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Configura el reconocedor de voz que se ejecutará DENTRO de la app.
     */
    private void setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true); // Para resultados en tiempo real

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                // El reconocedor está listo, actualizamos la UI
                updateUiToListeningState();
                tvStatusListening.setText("Escuchando...");
            }

            @Override
            public void onBeginningOfSpeech() {
                // El usuario ha empezado a hablar
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // ¡Magia! Hacemos que la animación reaccione al volumen de la voz.
                float progress = (rmsdB + 2f) / 12f;
                lottieAnimationView.setProgress(Math.max(0, Math.min(1, progress)));
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    tvRecognizedText.setText(matches.get(0));
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String finalText = matches.get(0);
                    tvRecognizedText.setText(finalText);

                    // Procesar con IA
                    String boardId = getCurrentBoardId();
                    viewModel.updateRecognizedText(finalText);
                    viewModel.processVoiceText(finalText, boardId);

                    updateUiToProcessingState();
                }
            }

            @Override
            public void onError(int error) {
                // Ocurrió un error.
                Toast.makeText(VoiceTaskActivity.this, getErrorText(error), Toast.LENGTH_SHORT).show();
                updateUiToIdleState();
            }

            @Override
            public void onEndOfSpeech() {
                tvStatusListening.setText("Procesando...");
            }

            @Override
            public void onBufferReceived(byte[] buffer) { }
            @Override
            public void onEvent(int eventType, Bundle params) {  }
        });
    }

    private void setupListeners() {
        fabStartListening.setOnClickListener(v -> checkAndRequestPermission());
        btnStop.setOnClickListener(v -> speechRecognizer.stopListening());
        btnCancel.setOnClickListener(v -> {
            speechRecognizer.cancel();
            updateUiToIdleState();
        });
    }

    private void checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            speechRecognizer.startListening(speechRecognizerIntent);
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    /**
     * Muestra la animación de escucha y los botones relevantes.
     */
    private void updateUiToListeningState() {
        lottieAnimationView.setVisibility(View.VISIBLE);
        tvStatusListening.setVisibility(View.VISIBLE);
        tvRecognizedText.setText("");

        fabStartListening.setVisibility(View.INVISIBLE);
        btnStop.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.VISIBLE);
    }

    /**
     * Muestra el botón de inicio y oculta la animación.
     */
    private void updateUiToIdleState() {
        lottieAnimationView.setVisibility(View.INVISIBLE);
        lottieAnimationView.cancelAnimation();
        tvStatusListening.setVisibility(View.INVISIBLE);

        fabStartListening.setVisibility(View.VISIBLE);
        btnStop.setVisibility(View.INVISIBLE);
        btnCancel.setVisibility(View.VISIBLE); // Dejamos Cancelar visible
    }

    private void updateUiToProcessingState() {
        tvStatusListening.setText("Procesando con IA...");
        lottieAnimationView.setVisibility(View.VISIBLE);
        fabStartListening.setVisibility(View.INVISIBLE);
        btnStop.setVisibility(View.INVISIBLE);
        btnCancel.setVisibility(View.VISIBLE);
    }

    private String getCurrentBoardId() {
        Board selectedBoard = mainViewModel.getSelectedBoard().getValue();
        if (selectedBoard != null) {
            Log.d("VoiceTaskActivity", "Using board ID: " + selectedBoard.getId());
            return selectedBoard.getId();
        }
        Log.e("VoiceTaskActivity", "No board selected!");
        return null;
    }

    private void observeViewModel() {
        viewModel.getRecognizedText().observe(this, text -> {
            tvRecognizedText.setText(text);
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                updateUiToProcessingState();
            } else {
                updateUiToIdleState();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getTaskCreated().observe(this, event -> {
            VoiceTaskViewModel.CreationResult result = event.getContentIfNotHandled();
            if (result != null && result.isSuccess()) {
                // Mostrar mensaje diferente según si es admin o no
                if (result.isAdmin()) {
                    Toast.makeText(this, "Tarea creada correctamente", Toast.LENGTH_LONG).show();
                    finish(); // Volver al tablero
                } else {
                    // Usuario no-admin: mostrar diálogo con opción de ver propuestas
                    showProposalSubmittedDialog();
                }
            }
        });

        viewModel.getAiResponse().observe(this, response -> {
            if (response != null && response.isSuccess()) {
                tvStatusListening.setText("Tarea generada: " + response.getTitle());
            }
        });
    }

    /**
     * Muestra un diálogo informando que la propuesta fue enviada
     */
    private void showProposalSubmittedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Propuesta enviada")
                .setMessage("Tu propuesta de tarea ha sido enviada al administrador del tablero para su aprobación.")
                .setPositiveButton("Entendido", (dialog, which) -> {
                    finish(); // Volver al tablero
                })
                .setCancelable(false)
                .show();
    }

    // Método de ayuda para traducir códigos de error a texto
    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO: return "Error de audio";
            case SpeechRecognizer.ERROR_CLIENT: return "Error del cliente";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "Permisos insuficientes";
            case SpeechRecognizer.ERROR_NETWORK: return "Error de red";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: return "Tiempo de espera de red";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No se entendió, intenta de nuevo";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: return "El servicio está ocupado";
            case SpeechRecognizer.ERROR_SERVER: return "Error del servidor";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No se detectó voz";
            default: return "Error desconocido";
        }
    }
}