package com.example.seg2105_project_1_tutor_registration_form.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.seg2105_project_1_tutor_registration_form.R;
import com.google.android.material.textfield.TextInputEditText;

public class AdminRegistrationActivity extends AppCompatActivity {

    // For class demo: store in a secure place or fetch from DB/remote in real apps.
    private static final String ADMIN_INVITE_CODE = "SEG2105-ADMIN-ONLY";

    private TextInputEditText etName, etEmail, etPassword, etInvite;
    private AccountManager accountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_registration);
        accountManager = new AccountManager(this);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etInvite = findViewById(R.id.etInvite);

        findViewById(R.id.btnCreateAdmin).setOnClickListener(v -> tryCreateAdmin());
    }

    private void tryCreateAdmin() {
        String name = text(etName);
        String email = text(etEmail);
        String password = text(etPassword);
        String invite = text(etInvite);

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Enter a valid email."); return;
        }
        if (password.length() < 6) { toast("Password must be at least 6 characters."); return; }

        // Gate with invite code (comment this if you prefer first-admin bootstrap)
        if (!TextUtils.equals(invite, ADMIN_INVITE_CODE)) {
            toast("Invalid invite code."); return;
        }

        // Firebase version with invite code + callback (async)
        accountManager.registerAdmin(name, email, password, invite, (ok, msg) -> {
            runOnUiThread(() -> {
                toast(ok ? "Admin created." : (msg == null ? "Error creating admin." : msg));
                if (ok) finish();
            });
        });
    }

    private String text(TextInputEditText e) {
        return e.getText()==null ? "" : e.getText().toString().trim();
    }

    private void toast(String s){ Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
