package com.example.geojsonwithosmdroid;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

//TODO: todo
public class AudioManager {
    TextToSpeech tts;
    MainActivity mainActivity;

    public AudioManager(MainActivity main) {
        mainActivity = main;
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

    public int speak(CharSequence text, int queueMode, Bundle params, String utteranceId) {
        return tts.speak(text, queueMode, params, utteranceId);
    }
}
