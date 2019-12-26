package com.ktuedu.rtMessaging;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.internal.firebase_auth.zzew;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.auth.zzy;
import com.google.firebase.auth.zzz;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.ktuedu.rtMessaging.Models.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    FirebaseUser user;
    String userName;
    String password;
    String email;

    FirebaseAuth firebaseAuth;
    FirebaseDatabase database;
    ProgressBar progressBar;
    Button registerBtn;
    EditText userNameText;
    EditText emailText;
    EditText passwordText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_register);

        database = FirebaseDatabase.getInstance();
        progressBar = findViewById(R.id.register_loading);
        userNameText= findViewById(R.id.register_username);
        emailText = findViewById(R.id.register_email);
        passwordText = findViewById(R.id.register_password);
        registerBtn = findViewById(R.id.register_btn);
        registerBtn.setOnClickListener(registerListener);
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();

    }

    View.OnClickListener registerListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            registerUser();
        }
    };




    public void addUserToDatabase() {
        FirebaseFirestore database = FirebaseFirestore.getInstance(); //Initialize cloud firestore
        String key = user.getUid(); //retrieve the user id so we can later use as key in the database

        HashMap<String, String> userData = new HashMap<>();
        userData.put("username", userName);
        userData.put("imageUrl", "none");

        Map<String, Object> update = new HashMap<>();
        update.put(key, userData);

        database.collection("users").document(key).set(update); //update user's details to Firestore
}

    private void registerUser() {

        userName = userNameText.getText().toString();
        email = emailText.getText().toString();
        password = passwordText.getText().toString();

        if(TextUtils.isEmpty(email)){
            Toast.makeText(this, "Enter email", Toast.LENGTH_LONG).show();
            return;
        }
        if(TextUtils.isEmpty(password)){
            Toast.makeText(this, "Enter password", Toast.LENGTH_LONG).show();
            return;
        }

        if(TextUtils.isEmpty(userName)){
            Toast.makeText(this, "Enter username", Toast.LENGTH_LONG).show();
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            user = firebaseAuth.getCurrentUser();
                            UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(userName)
                                    .build();
                            user.updateProfile(profileUpdate);

                            addUserToDatabase();
                            Toast.makeText(getApplicationContext(), "Registration successful.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(getApplicationContext(), "Registration error. Check your details", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }



}
