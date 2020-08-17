package com.example.chatapp.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.chatapp.R;
import com.example.chatapp.Adapters.TabsAccessorAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private ViewPager myViewPager;
    private TabLayout myTabLayout;
    private TabsAccessorAdapter myTabsAccessorAdapter;

    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;

    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    private EditText groupName;
    private Button createGroupBTN;
    private Button cancelGroupBTN;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //to create the toolbar manually
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Chat App");


        //Firebase authentication,recference
        mAuth = FirebaseAuth.getInstance();

        rootRef = FirebaseDatabase.getInstance().getReference();

        //this is for tab view in the main Activity
        myViewPager = (ViewPager) findViewById(R.id.main_tabs_pager);
        myTabsAccessorAdapter = new TabsAccessorAdapter(getSupportFragmentManager());
        myViewPager.setAdapter(myTabsAccessorAdapter);

        myTabLayout  =(TabLayout) findViewById(R.id.main_tab);
        myTabLayout.setupWithViewPager(myViewPager);
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser == null){
            sendToLoginPage();
        }
        else{
            updateUserStatus("online");
            verifyUserExist();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser != null){
            updateUserStatus("offline");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser != null){
            updateUserStatus("offline");
        }
    }

    private void verifyUserExist() {
        String currentUserID = mAuth.getCurrentUser().getUid();

        rootRef.child("Users").child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if((dataSnapshot.child("name").exists())){
                    Toast.makeText(MainActivity.this,"Welcome",Toast.LENGTH_SHORT).show();
                }
                else{
                    Intent settingintent = new Intent(MainActivity.this, SettingsActivity.class);
                    settingintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(settingintent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendToLoginPage() {
        Intent loginintent = new Intent(MainActivity.this, LoginActivity.class);
        loginintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginintent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
         super.onCreateOptionsMenu(menu);

         getMenuInflater().inflate(R.menu.option_menu,menu);
         return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
         super.onOptionsItemSelected(item);
         switch (item.getItemId()){
             case R.id.main_find_friend:
                 sendToFindFriendsActivity();
                 return true;

             case R.id.main_settings:
                 sendToSettings();
                 return true;

             case R.id.main_logout :
                 updateUserStatus("offline");
                 mAuth.signOut();
                 sendToLoginPage();
                 return true;
             case R.id.main_create_group:
                 requestNewGroup();

              default:
                  return false;

         }
    }

    private void sendToSettings() {
        Intent settingintent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(settingintent);
    }

    private void requestNewGroup() {
        builder = new AlertDialog.Builder(MainActivity.this);
        View view = getLayoutInflater().inflate(R.layout.create_group_popup,null);

        groupName = (EditText) view.findViewById(R.id.popup_group_name);
        createGroupBTN = (Button) view.findViewById(R.id.popup_create_grp_btn);
        cancelGroupBTN = (Button) view.findViewById(R.id.popup_cancel_btn);

        builder.setView(view);

        dialog = builder.create();
        dialog.show();

        createGroupBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String groupNameInfo = groupName.getText().toString();

                if(TextUtils.isEmpty(groupNameInfo)){
                    Toast.makeText(MainActivity.this, "Please Enter The Group Name", Toast.LENGTH_SHORT).show();
                }
                else {
                    createNewGroup(groupNameInfo);
                    dialog.dismiss();
                }
            }
        });

        cancelGroupBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    private void createNewGroup(final String groupNameInfo) {
        rootRef.child("Groups").child(groupNameInfo).setValue("")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(MainActivity.this,groupNameInfo + " group is created successfully...",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void sendToFindFriendsActivity() {
        Intent findFriendsIntent = new Intent(MainActivity.this, FindFriendsActivity.class);
        startActivity(findFriendsIntent);
    }

    private void updateUserStatus(String state){
        String saveCurrentDate,saveCurrentTime;

        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        saveCurrentTime = currentTime.format(calendar.getTime());

        HashMap<String , Object> onlineStatus = new HashMap<>();
        onlineStatus.put("time",saveCurrentTime);
        onlineStatus.put("date",saveCurrentDate);
        onlineStatus.put("state",state);

        currentUserId = mAuth.getCurrentUser().getUid();
        rootRef.child("Users").child(currentUserId).child("userState")
                .updateChildren(onlineStatus);
    }

}
