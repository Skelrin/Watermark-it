package com.example.watermarker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_screen);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        EditText editTextEmail = findViewById(R.id.emailAddress);
        EditText editTextPassword = findViewById(R.id.editTextTextPassword);
        Button login = findViewById(R.id.loginButton);
        Button register = findViewById(R.id.registerButton);

        register.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(intent);
            finish();
        });

        // handle connection to the app
        login.setOnClickListener(view -> {
            String email, password;
            email = editTextEmail.getText().toString();
            password = editTextPassword.getText().toString();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(getApplicationContext(), "Enter email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(getApplicationContext(), "Enter password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getApplicationContext(), "Login Successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplicationContext(), WatermarkActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
        });

    }
}