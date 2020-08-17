package com.example.chatapp.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private CircleImageView vUserProfileImage;
    private TextView vUserName,vUserstatus;
    private Button sendMeassageRequest,declineMessageRequest;
    private String receiverUserId;

    private String current_state,senderUserId;

    private DatabaseReference userRef,chatRequestRef,contactsRef,notificationRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        chatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        notificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");

        vUserProfileImage = (CircleImageView) findViewById(R.id.visit_profile_image);
        vUserName = (TextView) findViewById(R.id.visit_user_name);
        vUserstatus = (TextView) findViewById(R.id.visit_user_status);
        sendMeassageRequest = (Button) findViewById(R.id.visit_send_message);
        declineMessageRequest =(Button) findViewById(R.id.visit_decline_message);

        senderUserId = mAuth.getCurrentUser().getUid();
        receiverUserId = getIntent().getExtras().get("visit_user_id").toString();

        //Toast.makeText(this, "User Id :" + receiverUserId, Toast.LENGTH_SHORT).show();

        current_state = "new";
        retrieveUserInfo();

    }

    private void retrieveUserInfo() {
        // here we are retriveing the data of the users.
        userRef.child(receiverUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if((dataSnapshot.exists()) && (dataSnapshot.hasChild("images"))){
                    String user_image = dataSnapshot.child("images").getValue().toString();
                    String user_name = dataSnapshot.child("name").getValue().toString();
                    String user_status = dataSnapshot.child("status").getValue().toString();

                    Picasso.get().load(user_image).placeholder(R.drawable.profile_image).into(vUserProfileImage);
                    vUserName.setText(user_name);
                    vUserstatus.setText(user_status);

                    manageChatRequest();
                }
                else {
                    String user_name = dataSnapshot.child("name").getValue().toString();
                    String user_status = dataSnapshot.child("status").getValue().toString();

                    vUserName.setText(user_name);
                    vUserstatus.setText(user_status);

                    manageChatRequest();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void manageChatRequest() {

        chatRequestRef.child(senderUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if( dataSnapshot.hasChild(receiverUserId)){
                   String request_type = dataSnapshot.child(receiverUserId).child("request_type").getValue().toString();

                   if(request_type.equals("sent")){
                       // If the request is sent then we are enabling the cancel chat request button
                       current_state = "request_sent";
                       sendMeassageRequest.setText("Cancel Chat Request");
                   }
                   else if(request_type.equals("received")){
                       // If the request is received by the reciver then we are enabling the acept and decline button for the reciever
                       current_state=  "request_recieved";
                       sendMeassageRequest.setText("Accept Chat Request");

                       declineMessageRequest.setVisibility(View.VISIBLE);
                       declineMessageRequest.setEnabled(true);

                       declineMessageRequest.setOnClickListener(new View.OnClickListener() {
                           @Override
                           public void onClick(View v) {
                               cancelChatRequest();
                           }
                       });
                   }
                }
                // this is done so that when use accepts the request, the chat request has been removed.
                // so now no reciveris is present , so when we open the reciver user it has to show the status of friends.
                else {
                    contactsRef.child(senderUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.hasChild(receiverUserId)){
                                current_state = "friends";
                                sendMeassageRequest.setText("Remove this contact");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if(!senderUserId.equals(receiverUserId)){
            sendMeassageRequest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendMeassageRequest.setEnabled(false);

                    //We have given 3 choices either to send,accept or decline the chat request

                    if(current_state.equals("new")){
                        sendChatRequest();
                    }
                    if(current_state.equals("request_sent")){
                        cancelChatRequest();
                    }
                    if(current_state.equals("request_recieved")){
                        acceptChatRequest();
                    }
                    if(current_state.equals("friends")){
                        removeSpecificContact();
                    }
                }
            });
        }
        else {
            //If we open our own profile then IT is obvious that we cannt sent request to ourself for chatting
            sendMeassageRequest.setVisibility(View.INVISIBLE);
        }
    }

    private void removeSpecificContact() {
        contactsRef.child(senderUserId).child(receiverUserId)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            contactsRef.child(receiverUserId).child(senderUserId)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                //After the request is removed we are enabling the send request button and disabling the decline chatrequest button
                                                current_state= "new";
                                                sendMeassageRequest.setEnabled(true);
                                                sendMeassageRequest.setText("Send Message");

                                                declineMessageRequest.setVisibility(View.INVISIBLE);
                                                declineMessageRequest.setEnabled(false);

                                            }
                                        }
                                    });
                        }
                    }
                });

    }

    private void acceptChatRequest() {
        // Here the request is accepted by the receiver of the request . SO we are updating the data of both the user that request has been accepted
        contactsRef.child(senderUserId).child(receiverUserId)
                .child("contacts").setValue("saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                      if (task.isSuccessful()) {
                          contactsRef.child(receiverUserId).child(senderUserId)
                                  .child("contacts").setValue("saved")
                                  .addOnCompleteListener(new OnCompleteListener<Void>() {
                                      @Override
                                      public void onComplete(@NonNull Task<Void> task) {
                                          //After the chat request has been accepted, The chat request has been removed from the chatrequest section in the below code.
                                          if (task.isSuccessful()) {
                                                chatRequestRef.child(senderUserId).child(receiverUserId)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful()){
                                                                    chatRequestRef.child(receiverUserId).child(senderUserId)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    if(task.isSuccessful()){
                                                                                        sendMeassageRequest.setEnabled(true);
                                                                                        current_state = "friends";
                                                                                        sendMeassageRequest.setText("Remove this contact");

                                                                                        declineMessageRequest.setVisibility(View.INVISIBLE);
                                                                                        declineMessageRequest.setEnabled(false);
                                                                                    }
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                          }
                                      }
                                  });
                      }
                    }
                });
    }

    private void cancelChatRequest() {
    //Here we are removing the chatrequest from the chatrequest section
        chatRequestRef.child(senderUserId).child(receiverUserId)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            chatRequestRef.child(receiverUserId).child(senderUserId)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                //After the request is removed we are enabling the send request button and disabling the decline chatrequest button
                                                current_state= "new";
                                                sendMeassageRequest.setEnabled(true);
                                                sendMeassageRequest.setText("Send Message");

                                                declineMessageRequest.setVisibility(View.INVISIBLE);
                                                declineMessageRequest.setEnabled(false);

                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void sendChatRequest() {
        //Here the request is being send to the receiver for chatiing
        chatRequestRef.child(senderUserId).child(receiverUserId)
                .child("request_type").setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            chatRequestRef.child(receiverUserId).child(senderUserId)
                                    .child("request_type").setValue("received")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){

                                                HashMap<String,String> chatNotificationMap = new HashMap<>();
                                                chatNotificationMap.put("from",senderUserId);
                                                chatNotificationMap.put("type","request");

                                                notificationRef.child(receiverUserId).push()
                                                        .setValue(chatNotificationMap)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if(task.isSuccessful()){
                                                                    //After the request is sent to reciever , cancel request option is being enabled
                                                                    sendMeassageRequest.setEnabled(true);
                                                                    current_state = "request_sent";
                                                                    sendMeassageRequest.setText("Cancel Chat Request");
                                                                }
                                                            }
                                                        });

                                                sendMeassageRequest.setEnabled(true);
                                                current_state = "request_sent";
                                                sendMeassageRequest.setText("Cancel Chat Request");
                                            }
                                        }
                                    });
                        }
                    }
                });
    }
}
