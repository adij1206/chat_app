package com.example.chatapp.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.chatapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {
    private CircleImageView user_image;
    private EditText userName,userStatus;
    private Button updateuserSettings;

    private String currentUserId;
    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;
    private StorageReference userProfileImageRef;

    private static final int GALLERY_REQUEST_CODE =1;

    private ProgressDialog loadingBar;
    private Toolbar mToolbar;
    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();
        userProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");

        user_image = (CircleImageView) findViewById(R.id.profile_image);
        userName = (EditText) findViewById(R.id.set_user_name);
        userStatus = (EditText) findViewById(R.id.set_status);
        updateuserSettings = (Button) findViewById(R.id.update_setting_btn);
        loadingBar =new ProgressDialog(this);

        mToolbar = (Toolbar) findViewById(R.id.settings_layout);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle("Account Settings");


        userName.setVisibility(View.INVISIBLE);

        updateuserSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String setUserName = userName.getText().toString();
                String setStatus = userStatus.getText().toString();

                if(TextUtils.isEmpty(setUserName)){
                    Toast.makeText(SettingsActivity.this,"Please write the user name",Toast.LENGTH_SHORT).show();
                }
                if(TextUtils.isEmpty(setStatus)){
                    Toast.makeText(SettingsActivity.this,"Please write the Status",Toast.LENGTH_SHORT).show();
                }
                else{
                    HashMap<String ,Object> profileMap = new HashMap<>();
                    profileMap.put("uid",currentUserId);
                    profileMap.put("name",setUserName);
                    profileMap.put("status",setStatus);
                    rootRef.child("Users").child(currentUserId).updateChildren(profileMap)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()){
                                        sendToMainPage();
                                        Toast.makeText(SettingsActivity.this, "Profile Update is Successful", Toast.LENGTH_SHORT).show();
                                    }
                                    else {
                                        String message = task.getException().toString();
                                        Toast.makeText(SettingsActivity.this, "Error :"+message, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }


            }
        });

        retrieveUserInfo();

        user_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent,GALLERY_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data!=null){

            Uri imageUri = data.getData();
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1,1)
                    .start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if(resultCode == RESULT_OK){
                loadingBar.setTitle("Set Profile Image");
                loadingBar.setMessage("Please wait,while updating your profile image...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                Uri cropImageUri = result.getUri();

                StorageReference filepath = userProfileImageRef.child(currentUserId + ".jpg");
//                filepath.putFile(cropImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
//                        if(task.isSuccessful()){
//                            Toast.makeText(SettingsActivity.this, "profile Image is Uploaded Successfully...", Toast.LENGTH_SHORT).show();
//                            String downloadUrl = task.getResult().getMetadata().getReference().getDownloadUrl().toString();
//                            Log.d(TAG, "onComplete: "+downloadUrl);
//                            rootRef.child("Users").child(currentUserId).child("images")
//                                    .setValue(downloadUrl)
//                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
//                                        @Override
//                                        public void onComplete(@NonNull Task<Void> task) {
//                                            if(task.isSuccessful()){
//                                                Toast.makeText(SettingsActivity.this, "Your Image is uploaded in database...", Toast.LENGTH_SHORT).show();
//                                                loadingBar.dismiss();
//                                            }
//                                            else {
//                                                String message = task.getException().toString();
//                                                Toast.makeText(SettingsActivity.this, "Error :"+ message, Toast.LENGTH_SHORT).show();
//                                                loadingBar.dismiss();
//                                            }
//                                        }
//                                    });
//                        }
//                        else {
//                            String message = task.getException().toString();
//                            Toast.makeText(SettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
//                            loadingBar.dismiss();
//                        }
//                    }
//                });
                filepath.putFile(cropImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(SettingsActivity.this, "profile Image is Uploaded Successfully...", Toast.LENGTH_SHORT).show();
                        Task<Uri> result = taskSnapshot.getMetadata().getReference().getDownloadUrl();
                        result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Uri downloadUrl = uri;
                                rootRef.child("Users").child(currentUserId).child("images")
                                   .setValue(downloadUrl.toString())
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                Toast.makeText(SettingsActivity.this, "Profile Image is Saved in Database", Toast.LENGTH_SHORT).show();
                                                loadingBar.dismiss();
                                            }
                                            else {
                                                String message = task.getException().toString();
                                                Toast.makeText(SettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                                                loadingBar.dismiss();
                                            }
                                        }
                                    });
                            }
                        });
                    }
                });
            }

        }

    }

    private void retrieveUserInfo() {
        rootRef.child("Users").child(currentUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if((dataSnapshot.exists()) && (dataSnapshot.hasChild("name") && (dataSnapshot.hasChild("images")))){
                            String retrieveUserName = dataSnapshot.child("name").getValue().toString();
                            String retrieveUserStatus = dataSnapshot.child("status").getValue().toString();
                            String retrieveUserImage = dataSnapshot.child("images").getValue().toString();

                            userName.setText(retrieveUserName);
                            userStatus.setText(retrieveUserStatus);
                            Picasso.get().load(retrieveUserImage).into(user_image);
                        }
                        else if((dataSnapshot.exists()) && (dataSnapshot.hasChild("name"))){

                            String retrieveUserName = dataSnapshot.child("name").getValue().toString();
                            String retrieveUserStatus = dataSnapshot.child("status").getValue().toString();

                            userName.setText(retrieveUserName);
                            userStatus.setText(retrieveUserStatus);
                        }
                        else
                        {
                            userName.setVisibility(View.VISIBLE);
                            Toast.makeText(SettingsActivity.this,"Please set and update your information",Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void sendToMainPage() {
        Intent mainIntent = new Intent(SettingsActivity.this,MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}
