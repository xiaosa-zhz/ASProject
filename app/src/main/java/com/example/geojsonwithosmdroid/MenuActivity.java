package com.example.geojsonwithosmdroid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.example.geojsonwithosmdroid.util.NavigationManager;

import java.util.List;

public class MenuActivity extends AppCompatActivity {

    Boolean isVoiceEnabled;
    NavigationManager.AlertDistance alertDistance;

    Spinner alertOptions;

    public static class MySpinnerAdapter extends ArrayAdapter<String> {

        String[] mStringArray;

        public MySpinnerAdapter(@NonNull Context context, int resource) {
            super(context, resource);
        }

        public MySpinnerAdapter(@NonNull Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);
        }

        public MySpinnerAdapter(@NonNull Context context, int resource, @NonNull String[] objects) {
            super(context, resource, objects);
            mStringArray = objects;
        }

        public MySpinnerAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull String[] objects) {
            super(context, resource, textViewResourceId, objects);
        }

        public MySpinnerAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
            super(context, resource, objects);
        }

        public MySpinnerAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull List<String> objects) {
            super(context, resource, textViewResourceId, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(super.getContext());
                convertView = inflater.inflate(R.layout.spinner_item, parent, false);
            }
//            //此处text1是Spinner默认的用来显示文字的TextView
//            TextView tv = (TextView) convertView.findViewById(android.R.id.text1);
//            tv.setText(mStringArray[position]);
//            tv.setTextSize(44f);
//            tv.setTextColor(Color.BLACK);
            return super.getView(position, convertView, parent);
        }
    }

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
        alertDistance = (NavigationManager.AlertDistance) oldSettings.getSerializable("AlertLevel");
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
        alertOptions = findViewById(R.id.alert_setting_spinner);
        String[] options = getResources().getStringArray(R.array.alert_options);
        MySpinnerAdapter adapter = new MySpinnerAdapter(
                this, R.layout.support_simple_spinner_dropdown_item, options);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        alertOptions.setAdapter(adapter);
        for(int count = 0; count < NavigationManager.options.length; ++count) {
            if(alertDistance == NavigationManager.options[count]) {
                alertOptions.setSelection(count);
            }
        }
        alertOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                alertDistance = NavigationManager.options[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                alertOptions.setSelection(1);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent intent = new Intent();
        Bundle newSettings = new Bundle();
        newSettings.putSerializable("AlertLevel", alertDistance);
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
        newSettings.putSerializable("AlertLevel", alertDistance);
        newSettings.putBoolean("IsVoiceEnabled", isVoiceEnabled);
        intent.putExtras(newSettings);
        setResult(RESULT_OK, intent);
    }
}