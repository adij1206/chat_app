package com.example.chatapp.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class LoginActivity extends AppCompatActivity {


    private FirebaseAuth mAuth;
    private Button loginBtn,phoneLoginBtn;
    private EditText email,password;
    private TextView needNewAcclink,forgetPasswordLink;
    private ProgressDialog loadingBar;
    private DatabaseReference usersRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth= FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        //intialization of all fields of login.xml file
        loginBtn = (Button) findViewById(R.id.login_button);
        phoneLoginBtn = (Button) findViewById(R.id.phone_login_button);
        email= (EditText) findViewById(R.id.login_email);
        password = (EditText) findViewById(R.id.login_password);
        needNewAcclink = (TextView) findViewById(R.id.need_new_acount_link);
        forgetPasswordLink = (TextView) findViewById(R.id.forget_password_link);
        loadingBar = new ProgressDialog(this);

        needNewAcclink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent registerIntent = new Intent(LoginActivity.this,RegisterActivity.class);
                startActivity(registerIntent);
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginToMAin();
            }
        });
        phoneLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent phoneNoLoginIntent = new Intent(LoginActivity.this,PhoneLoginActivity.class);
                startActivity(phoneNoLoginIntent);
                finish();
            }
        });
    }

    private void LoginToMAin() {
        String userEmail = email.getText().toString();
        final String userPassword= password.getText().toString();

        if(!TextUtils.isEmpty(userEmail)&& !TextUtils.isEmpty(userPassword)){

            loadingBar.setTitle("Sign In");
            loadingBar.setMessage("Please wait...");
            loadingBar.setCanceledOnTouchOutside(true);
            loadingBar.show();

            mAuth.signInWithEmailAndPassword(userEmail,userPassword)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){

                                String currentUserId = mAuth.getCurrentUser().getUid();
                                String deviceToken = FirebaseInstanceId.getInstance().getToken();
                                usersRef.child(currentUserId).child("device-token")
                                        .setValue(deviceToken).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            sendToMainPage();
                                            Toast.makeText(LoginActivity.this,"Login is Successful...",Toast.LENGTH_LONG).show();
                                            loadingBar.dismiss();
                                        }
                                    }
                                });


                            }
                            else {
                                String message = task.getException().toString();
                                Toast.makeText(LoginActivity.this,"Error :" + message,Toast.LENGTH_LONG).show();
                                loadingBar.dismiss();
                            }
                        }
                    });
        }
        else {
            Toast.makeText(LoginActivity.this,"Please fill the blank column",Toast.LENGTH_LONG).show();
        }
    }


    private void sendToMainPage() {
        Intent mainIntent = new Intent(LoginActivity.this,MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}
