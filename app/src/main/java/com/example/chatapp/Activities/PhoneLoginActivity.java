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
import android.widget.Toast;

import com.example.chatapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;

public class PhoneLoginActivity extends AppCompatActivity {

    private EditText phoneNumberInput,verificationCode;
    private Button sendVerifyCodeBTN,verifyBTN;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private FirebaseAuth mAuth;
    private String mVerificationId;
    private ProgressDialog loadingBar;
    private PhoneAuthProvider.ForceResendingToken mResendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);

        mAuth = FirebaseAuth.getInstance();

        phoneNumberInput = (EditText) findViewById(R.id.phone_number_input);
        verificationCode = (EditText) findViewById(R.id.verification_code_input);
        sendVerifyCodeBTN = (Button) findViewById(R.id.send_verification_code);
        verifyBTN = (Button) findViewById(R.id.verify_button);

        loadingBar = new ProgressDialog(this);

        sendVerifyCodeBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String phoneNumber = phoneNumberInput.getText().toString();

                if(TextUtils.isEmpty(phoneNumber)){
                    Toast.makeText(PhoneLoginActivity.this, "Please Enter The Valid Phone Number", Toast.LENGTH_SHORT).show();
                }
                else{
                    loadingBar.setTitle("Phone Authentication");
                    loadingBar.setMessage("Please Wait,While your phone is authenticating");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                            phoneNumber,        // Phone number to verify
                            60,                 // Timeout duration
                            TimeUnit.SECONDS,   // Unit of timeout
                            PhoneLoginActivity.this,               // Activity (for callback binding)
                            callbacks);        // OnVerificationStateChangedCallbacks
                }
            }
        });

        verifyBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendVerifyCodeBTN.setVisibility(View.INVISIBLE);
                phoneNumberInput.setVisibility(View.INVISIBLE);

                String verificationCodeKey = verificationCode.getText().toString();

                if(TextUtils.isEmpty(verificationCodeKey)){
                    Toast.makeText(PhoneLoginActivity.this, "Please Enter the code...", Toast.LENGTH_SHORT).show();
                }
                else{
                    loadingBar.setTitle("Code Verification");
                    loadingBar.setMessage("Please Wait,While we are verifying verification code");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, verificationCodeKey);
                    signInWithPhoneAuthCredential(credential);
                }

            }
        });

        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                loadingBar.dismiss();
                Toast.makeText(PhoneLoginActivity.this, "Please enter the valid phone number with your country code...", Toast.LENGTH_SHORT).show();
                sendVerifyCodeBTN.setVisibility(View.VISIBLE);
                phoneNumberInput.setVisibility(View.VISIBLE);

                verifyBTN.setVisibility(View.INVISIBLE);
                verificationCode.setVisibility(View.INVISIBLE);


            }

            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                loadingBar.dismiss();

                Toast.makeText(PhoneLoginActivity.this,"Code has been sent,please check your inbox...",Toast.LENGTH_LONG).show();

                sendVerifyCodeBTN.setVisibility(View.INVISIBLE);
                phoneNumberInput.setVisibility(View.INVISIBLE);

                verifyBTN.setVisibility(View.VISIBLE);
                verificationCode.setVisibility(View.VISIBLE);

            }
        };

    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            loadingBar.dismiss();
                            Toast.makeText(PhoneLoginActivity.this, "Congratulations,You are logged in...", Toast.LENGTH_SHORT).show();
                            Intent mainAcitivityIntent = new Intent(PhoneLoginActivity.this,MainActivity.class);
                            startActivity(mainAcitivityIntent);
                            finish();
                        }
                        else {
                            String errorMessage = task.getException().toString();
                            Toast.makeText(PhoneLoginActivity.this,"Error :"+errorMessage , Toast.LENGTH_SHORT).show();
                        }
                    }

                });
    }



}
