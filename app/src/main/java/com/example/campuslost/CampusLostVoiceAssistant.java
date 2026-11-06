
package com.example.campuslost;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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

    // HomeActivity-কে recognized command পাঠানোর interface
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

    // Text-to-Speech তৈরি
    private void initializeTextToSpeech() {

        textToSpeech = new TextToSpeech(
                activity,
                status -> {

                    if (status == TextToSpeech.SUCCESS) {

                        int result = textToSpeech.setLanguage(
                                Locale.US
                        );

                        ttsReady =
                                result != TextToSpeech.LANG_MISSING_DATA
                                        && result != TextToSpeech.LANG_NOT_SUPPORTED;

                    } else {
                        Toast.makeText(
                                activity,
                                "Voice output is not available",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    // Speech Recognizer তৈরি
    private void initializeSpeechRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {

            Toast.makeText(
                    activity,
                    "Speech recognition is not available",
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

                        speak("Sorry, I could not understand. Please try again.");
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
                            speak("I could not understand your command.");
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

    // Assistant চালু করা
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
                    "Speech recognition is unavailable",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        speakAndListen();
    }

    // প্রথমে কথা বলবে, তারপর শুনবে
    private void speakAndListen() {

        if (!ttsReady) {
            startSpeechRecognition();
            return;
        }

        textToSpeech.setOnUtteranceProgressListener(
                new UtteranceProgressListener() {

                    @Override
                    public void onStart(String utteranceId) {
                    }

                    @Override
                    public void onDone(String utteranceId) {

                        activity.runOnUiThread(
                                () -> startSpeechRecognition()
                        );
                    }

                    @Override
                    public void onError(String utteranceId) {
                    }
                }
        );

        textToSpeech.speak(
                "Hello! How can I help you?",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "assistant_greeting"
        );
    }

    // Microphone listening শুরু
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

    // Permission result HomeActivity থেকে গ্রহণ
    public void onPermissionResult(
            int requestCode,
            int[] grantResults
    ) {

        if (requestCode == REQUEST_AUDIO_PERMISSION) {

            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

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

    // Assistant-এর voice response
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

    // Resource বন্ধ করা
    public void destroy() {

        if (speechRecognizer != null) {
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
