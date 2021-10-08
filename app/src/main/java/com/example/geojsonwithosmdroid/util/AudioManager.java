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

    public void sendVoiceAlert(NavigationManager.AlertDistance distance) {
        if(isVoiceEnabled) {
            CharSequence speakText;
            String start = mainActivity.getResources().getString(R.string.alert_speak_text_start);
            String end = mainActivity.getResources().getString(R.string.alert_speak_text_end);
            speakText = start + distance.toValue() + end;
            tts.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, "normal_alert");
        }
    }
}
