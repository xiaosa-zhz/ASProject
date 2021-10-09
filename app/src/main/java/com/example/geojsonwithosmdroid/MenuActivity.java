package com.example.geojsonwithosmdroid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.example.geojsonwithosmdroid.util.NavigationManager;

import java.util.List;

public class MenuActivity extends AppCompatActivity {

    Boolean isVoiceEnabled;
    double[] alertSettings;
    EditText text1, text2, text3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Menu");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        Bundle oldSettings = getIntent().getExtras();
        isVoiceEnabled = oldSettings.getBoolean("IsVoiceEnabled");
        //TODO: complete menu
        Button voiceSettingButton = findViewById(R.id.voice_setting_button);
        if(isVoiceEnabled) {
            voiceSettingButton.setText(R.string.menu_voice_setting_button_on);
        } else {
            voiceSettingButton.setText(R.string.menu_voice_setting_button_off);
        }
        voiceSettingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isVoiceEnabled = !isVoiceEnabled;
                if(isVoiceEnabled) {
                    voiceSettingButton.setText(R.string.menu_voice_setting_button_on);
                } else {
                    voiceSettingButton.setText(R.string.menu_voice_setting_button_off);
                }
            }
        });
        //alert option configuration
        alertSettings = oldSettings.getDoubleArray("AlertSettings");
        text1 = findViewById(R.id.alert_setting1);
        text1.setText(String.valueOf(alertSettings[0]));
        text2 = findViewById(R.id.alert_setting2);
        text2.setText(String.valueOf(alertSettings[1]));
        text3 = findViewById(R.id.alert_setting3);
        text3.setText(String.valueOf(alertSettings[2]));
    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent intent = new Intent();
        Bundle newSettings = new Bundle();
        String result;
        result = text1.getText().toString();
        if(result.length() != 0) {
            alertSettings[0] = Double.parseDouble(result);
        }
        result = text2.getText().toString();
        if(result.length() != 0) {
            alertSettings[1] = Double.parseDouble(result);
        }
        result = text3.getText().toString();
        if(result.length() != 0) {
            alertSettings[2] = Double.parseDouble(result);
        }
        newSettings.putDoubleArray("AlertSettings", alertSettings);
        newSettings.putBoolean("IsVoiceEnabled", isVoiceEnabled);
        intent.putExtras(newSettings);
        setResult(RESULT_OK, intent);
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Intent intent = new Intent();
        Bundle newSettings = new Bundle();
        String result;
        result = text1.getText().toString();
        if(result.length() != 0) {
            alertSettings[0] = Double.parseDouble(result);
        }
        result = text2.getText().toString();
        if(result.length() != 0) {
            alertSettings[1] = Double.parseDouble(result);
        }
        result = text3.getText().toString();
        if(result.length() != 0) {
            alertSettings[2] = Double.parseDouble(result);
        }
        newSettings.putDoubleArray("AlertSettings", alertSettings);
        newSettings.putBoolean("IsVoiceEnabled", isVoiceEnabled);
        intent.putExtras(newSettings);
        setResult(RESULT_OK, intent);
    }
}