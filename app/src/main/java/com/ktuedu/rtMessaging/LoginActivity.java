package com.ktuedu.rtMessaging;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    Button loginBtn;
    Button registerBtn;
    EditText userNameEDT;
    EditText passwordEDT;
    ProgressBar progressBar;

    String email;
    String password;
    FirebaseAuth firebaseAuth;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_login);

        loginBtn = findViewById(R.id.login);
        registerBtn = findViewById(R.id.login_register);
        userNameEDT = findViewById(R.id.login_username);
        passwordEDT = findViewById(R.id.login_password);
        progressBar = findViewById(R.id.login_loading);

        firebaseAuth = FirebaseAuth.getInstance();

        loginBtn.setOnClickListener(LogInBtnListener);
        registerBtn.setOnClickListener(RegisterBtnListener);

    }

    View.OnClickListener RegisterBtnListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            runRegisterActivity();
        }
    };

    View.OnClickListener LogInBtnListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            LogIn();
        }
    };

    private void runRegisterActivity() {
        Intent runIntent = new Intent(this, RegisterActivity.class);
        startActivity(runIntent);
    }

    private void LogIn(){
        email = userNameEDT.getText().toString();
        password = passwordEDT.getText().toString();


        if(TextUtils.isEmpty(email)){
            Toast.makeText(this, "Enter email", Toast.LENGTH_LONG).show();
            return;
        }
        if(TextUtils.isEmpty(password)){
            Toast.makeText(this, "Enter password", Toast.LENGTH_LONG).show();
            return;
        }
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){
                            user = firebaseAuth.getCurrentUser();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                        else{
                            Toast.makeText(LoginActivity.this, "Login unsuccessful.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

}
