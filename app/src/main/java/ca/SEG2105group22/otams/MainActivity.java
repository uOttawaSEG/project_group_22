package ca.SEG2105group22.otams;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("test")
                .add(new TestMessage("Hello Firebase!"))
                .addOnSuccessListener(ref ->
                        Log.d("FIREBASE", "Document added with ID: " + ref.getId()))
                .addOnFailureListener(e ->
                        Log.w("FIREBASE", "Error adding document", e));
    }
}


class TestMessage {
    public String text;

    public TestMessage() { }
    public TestMessage(String text) { this.text = text; }
}