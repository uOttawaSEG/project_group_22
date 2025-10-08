package ca.SEG2105group22.otams;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SignUp extends AppCompatActivity {

    private EditText firstNameInput, lastNameInput, emailInput, passwordInput, phoneInput;
    private RadioGroup roleGroup;
    private RadioButton rbStudent, rbTutor;
    private Button btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use the filename you saved your XML as:
        setContentView(R.layout.activity_signup);

        // Bind views (IDs match your layout)
        firstNameInput = findViewById(R.id.input_first_name);
        lastNameInput  = findViewById(R.id.input_last_name);
        emailInput     = findViewById(R.id.input_email_username);
        passwordInput  = findViewById(R.id.input_account_password);
        phoneInput     = findViewById(R.id.input_phone_number);
        roleGroup      = findViewById(R.id.roleGroup);
        rbStudent      = findViewById(R.id.rb_role_student);
        rbTutor        = findViewById(R.id.rb_role_tutor);
        btnSignUp      = findViewById(R.id.btnSignUp);

        btnSignUp.setOnClickListener(v -> {
            boolean isTutor = rbTutor.isChecked();

            if (isTutor) {
                // Go to extra tutor info screen
                Intent i = new Intent(SignUp.this, TutorRegistration.class);
                i.putExtra("firstName", firstNameInput.getText().toString().trim());
                i.putExtra("lastName",  lastNameInput.getText().toString().trim());
                i.putExtra("email",     emailInput.getText().toString().trim());
                i.putExtra("password",  passwordInput.getText().toString());
                i.putExtra("phone",     phoneInput.getText().toString().trim());
                startActivity(i);
            } else {
                // Student flow can be handled here later
            }
        });
    }
}