package com.example.geojsonwithosmdroid.util;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import com.example.geojsonwithosmdroid.MainActivity;
import com.example.geojsonwithosmdroid.R;

import java.util.Locale;

//TODO: todo
public class AudioManager {
    private TextToSpeech tts;
    private MainActivity mainActivity;
    private boolean isVoiceEnabled;

    public boolean isVoiceEnabled() {
        return isVoiceEnabled;
    }

    public void setVoiceEnabled(boolean b) {
        String speakText;
        if(isVoiceEnabled == b) {
            return;
        }
        if(b) {
            speakText = mainActivity.getString(R.string.voice_on);
        } else {
            speakText = mainActivity.getString(R.string.voice_off);
        }
        tts.speak(speakText, TextToSpeech.QUEUE_ADD, null, "voice_on_off");
        this.isVoiceEnabled = b;
    }

    public AudioManager(MainActivity main) {
        mainActivity = main;
        isVoiceEnabled = true;
        tts = new TextToSpeech(main, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS) {
                    int supported = tts.setLanguage(Locale.CHINESE);
                    if ((supported != TextToSpeech.LANG_AVAILABLE) && (supported != TextToSpeech.LANG_COUNTRY_AVAILABLE)) {
                        Toast.makeText(mainActivity, "不支持的语言 CODE: " + supported, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(mainActivity, "初始化语音失败 CODE: " + i, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void sendVoiceAlert(double distance, String name) {
        if(isVoiceEnabled) {
            CharSequence speakText;
            String start = mainActivity.getResources().getString(R.string.alert_speak_text_start);
            String mid = mainActivity.getResources().getString(R.string.alert_speak_text_mid);
            String end = mainActivity.getResources().getString(R.string.alert_speak_text_end);
            speakText = start + distance + mid + name + end;
            tts.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, "normal_alert");
        }
    }
}
