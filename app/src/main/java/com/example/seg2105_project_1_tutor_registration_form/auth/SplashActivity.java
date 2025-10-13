package com.example.seg2105_project_1_tutor_registration_form.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.seg2105_project_1_tutor_registration_form.R;

public class SplashActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView roleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        roleTextView = findViewById(R.id.role_text_view);
        roleTextView.setText("Loading…");

        // Navigate after 2 seconds (adjust as you like)
        handler.postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 2000);
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null); // avoid leaks
        super.onDestroy();
    }
}
