
package com.example.campuslost;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public class CampusLostVoiceAssistant {

    private static final int REQUEST_AUDIO_PERMISSION = 501;

    private Activity activity;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;

    private boolean ttsReady = false;

    public interface VoiceAssistantListener {
        void onCommandRecognized(String command);
    }

    private VoiceAssistantListener listener;

    public CampusLostVoiceAssistant(
            Activity activity,
            VoiceAssistantListener listener
    ) {
        this.activity = activity;
        this.listener = listener;

        initializeTextToSpeech();
        initializeSpeechRecognizer();
    }

    // Initialize Text-to-Speech
    private void initializeTextToSpeech() {

        textToSpeech = new TextToSpeech(activity, status -> {

            if (status == TextToSpeech.SUCCESS) {

                int result = textToSpeech.setLanguage(Locale.US);

                ttsReady =
                        result != TextToSpeech.LANG_MISSING_DATA
                                && result != TextToSpeech.LANG_NOT_SUPPORTED;

            } else {
                Toast.makeText(
                        activity,
                        "Voice output unavailable",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    // Initialize Speech Recognizer
    private void initializeSpeechRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {

            Toast.makeText(
                    activity,
                    "Speech recognition unavailable",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        speechRecognizer =
                SpeechRecognizer.createSpeechRecognizer(activity);

        speechRecognizer.setRecognitionListener(
                new RecognitionListener() {

                    @Override
                    public void onReadyForSpeech(Bundle params) {
                    }

                    @Override
                    public void onBeginningOfSpeech() {
                    }

                    @Override
                    public void onRmsChanged(float rmsdB) {
                    }

                    @Override
                    public void onBufferReceived(byte[] buffer) {
                    }

                    @Override
                    public void onEndOfSpeech() {
                    }

                    @Override
                    public void onError(int error) {

                        Toast.makeText(
                                activity,
                                "Could not recognize speech. Try shaking again.",
                                Toast.LENGTH_SHORT
                        ).show();

                        // Do not automatically restart listening
                    }

                    @Override
                    public void onResults(Bundle results) {

                        ArrayList<String> matches =
                                results.getStringArrayList(
                                        SpeechRecognizer.RESULTS_RECOGNITION
                                );

                        if (matches != null && !matches.isEmpty()) {

                            String command = matches.get(0);

                            if (listener != null) {
                                listener.onCommandRecognized(command);
                            }

                        } else {

                            Toast.makeText(
                                    activity,
                                    "No command recognized",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }

                    @Override
                    public void onPartialResults(Bundle partialResults) {
                    }

                    @Override
                    public void onEvent(int eventType, Bundle params) {
                    }
                }
        );
    }

    // Start Assistant
    public void startListening() {

        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    REQUEST_AUDIO_PERMISSION
            );

            return;
        }

        if (speechRecognizer == null) {
            Toast.makeText(
                    activity,
                    "Speech recognition unavailable",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        speakAndListen();
    }

    // Speak greeting, then listen once
    private void speakAndListen() {

        if (!ttsReady) {
            startSpeechRecognition();
            return;
        }

        textToSpeech.speak(
                "Hello! How can I help you?",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "assistant_greeting"
        );

        textToSpeech.setOnUtteranceProgressListener(
                new UtteranceProgressListener() {

                    @Override
                    public void onStart(String utteranceId) {
                    }

                    @Override
                    public void onDone(String utteranceId) {

                        // Only greeting completion starts listening
                        if ("assistant_greeting".equals(utteranceId)) {

                            activity.runOnUiThread(
                                    () -> startSpeechRecognition()
                            );
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                    }
                }
        );
    }

    // Start microphone listening
    private void startSpeechRecognition() {

        if (speechRecognizer == null) {
            return;
        }

        Intent intent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.US.toLanguageTag()
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                1
        );

        speechRecognizer.startListening(intent);
    }

    // Handle Microphone Permission
    public void onPermissionResult(
            int requestCode,
            int[] grantResults
    ) {

        if (requestCode == REQUEST_AUDIO_PERMISSION) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                speakAndListen();

            } else {

                Toast.makeText(
                        activity,
                        "Microphone permission is required",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    // Speak Response
    public void speak(String message) {

        if (textToSpeech != null && ttsReady) {

            textToSpeech.speak(
                    message,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "assistant_response"
            );
        }
    }

    // Destroy Resources
    public void destroy() {

        if (speechRecognizer != null) {
            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }
}
