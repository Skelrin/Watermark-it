package com.example.watermarker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_screen);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        EditText editTextEmail = findViewById(R.id.emailAddress);
        EditText editTextPassword = findViewById(R.id.editTextTextPassword);
        Button login = findViewById(R.id.loginButton2);
        Button register = findViewById(R.id.registerButton2);

        login.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        });

        register.setOnClickListener(v -> {
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

            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(getApplicationContext(), "Account created.",
                                Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(getApplicationContext(), "Authentication failed.",
                                Toast.LENGTH_SHORT).show();

                    }
                });
        });
    }
}