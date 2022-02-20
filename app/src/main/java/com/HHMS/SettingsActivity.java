package com.HHMS;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    private EditText EditNumber;
    private Button SaveBtn;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // calling the action bar
        ActionBar actionBar = getSupportActionBar();

        // showing the back button in action bar
        actionBar.setDisplayHomeAsUpEnabled(true);

        getSupportActionBar().setTitle("Settings");

        sharedPreferences = getSharedPreferences("hhmsdata", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();


        EditNumber = findViewById(R.id.settings_number);
        SaveBtn = findViewById(R.id.settings_submit);

        String number = sharedPreferences.getString("number","");

        if (!number.isEmpty()) {
            EditNumber.setText(number);
            SaveBtn.setText("Edit Number");
        }

        SaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String number = EditNumber.getText().toString();

                editor.putString("number", number);
                editor.commit();
                Toast.makeText(SettingsActivity.this, "Emergency contact saved!!!", Toast.LENGTH_SHORT).show();
                SaveBtn.setText("Edit Number");
            }
        });
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(SettingsActivity.this,HomeActivity.class));
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}