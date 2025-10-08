package ca.SEG2105group22.otams;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // your login XML

        Button btnCreate = findViewById(R.id.btnTEST);
        btnCreate.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SignUp.class))
        );
    }
}