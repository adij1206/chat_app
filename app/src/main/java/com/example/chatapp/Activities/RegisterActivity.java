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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class RegisterActivity extends AppCompatActivity {
    private Button registerBtn;
    private EditText registerEmail,registerPwd;
    private TextView alreadyAcclink;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;
    private DatabaseReference rootRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();

        // Intialization of all fields in register.xml file
        registerBtn = (Button) findViewById(R.id.register_button);
        registerEmail = (EditText) findViewById(R.id.register_email);
        registerPwd = (EditText) findViewById(R.id.register_password);
        alreadyAcclink = (TextView) findViewById(R.id.Already_have_account_link);

        loadingBar = new ProgressDialog(this);

        alreadyAcclink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               sendToLoginActivity();
            }
        });
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createnewAccount();
            }
        });
    }

    private void sendToLoginActivity() {
        Intent registerIntent = new Intent(RegisterActivity.this,LoginActivity.class);
        startActivity(registerIntent);
    }

    private void createnewAccount() {
        String userEmail = registerEmail.getText().toString();
        String userPassword= registerPwd.getText().toString();

        if(!TextUtils.isEmpty(userEmail)&& !TextUtils.isEmpty(userPassword)){

            loadingBar.setTitle("Creating New Account");
            loadingBar.setMessage("Please wait, while we are creating your new account...");
            loadingBar.setCanceledOnTouchOutside(true);
            loadingBar.show();

            mAuth.createUserWithEmailAndPassword(userEmail,userPassword)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){

                                String deviceToken = FirebaseInstanceId.getInstance().getToken();

                                String currentUserID = mAuth.getCurrentUser().getUid();
                                rootRef.child("Users").child(currentUserID).setValue(" ");
                                //This is done for notification which is not completed yet
                                //TODO: Learn Node.js for Notification
                                rootRef.child(currentUserID).child("device-token")
                                        .setValue(deviceToken);

                                sendToMainActivity();
                                Toast.makeText(RegisterActivity.this,"Account is created Successfully...",Toast.LENGTH_LONG).show();
                                loadingBar.dismiss();
                            }
                            else {
                                String message = task.getException().toString();
                                Toast.makeText(RegisterActivity.this,"Error :" + message,Toast.LENGTH_LONG).show();
                                loadingBar.dismiss();
                            }
                        }
                    });
        }
        else
        {
            Toast.makeText(RegisterActivity.this,"Please Fill the blank column",Toast.LENGTH_LONG).show();
        }
    }

    private void sendToMainActivity() {
        Intent mainIntent = new Intent(RegisterActivity.this,MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}
