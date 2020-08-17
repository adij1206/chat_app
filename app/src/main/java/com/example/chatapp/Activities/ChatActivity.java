package com.example.chatapp.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapp.Adapters.MessagesAdapter;
import com.example.chatapp.Model.Messages;
import com.example.chatapp.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private String messageReceiverId,messageReceiverName,messageReceiverImage;

    private TextView userName,userLastSeen;
    private CircleImageView userImage;
    private androidx.appcompat.widget.Toolbar chatToolBar;
    private EditText messageInputText;
    private ImageButton sendMessageBtn,sendFileBtn;
    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;
    private String messageSenderId;

    private final List<Messages> messagesList = new ArrayList<>();
    private RecyclerView userMessageList;
    private LinearLayoutManager manager;
    private MessagesAdapter adapter;

    private String saveCurrentDate,saveCurrentTime;
    private String checker = "",myUrl = "";
    private StorageTask uploadTask;
    private Uri fileUri;

    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messageReceiverId = getIntent().getExtras().get("visit_user_id").toString();
        messageReceiverName = getIntent().getExtras().get("visit_user_name").toString();
        messageReceiverImage = getIntent().getExtras().get("visit_user_image").toString();

        mAuth = FirebaseAuth.getInstance();
        messageSenderId = mAuth.getCurrentUser().getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();

        initialize();

        userName.setText(messageReceiverName);
        Picasso.get().load(messageReceiverImage).into(userImage);

        sendMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        displayUserLastSeen();

        sendFileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence options[] = new CharSequence[]{
                        "Images",
                        "PDF Files",
                        "Documents"
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                builder.setTitle("Select The Files");

                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        if(i == 0){
                            checker = "image";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent.createChooser(intent,"Select Image"),438);
                        }
                        if(i ==1){
                            checker = "pdf";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/pdf");
                            startActivityForResult(intent.createChooser(intent,"Select PDF File"),438);
                        }
                        if(i == 2){
                            checker = "docx";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/msword");
                            startActivityForResult(intent.createChooser(intent,"Select Document"),438);
                        }
                    }
                });
                builder.show();
            }
        });
    }

    private void initialize() {

        chatToolBar = (Toolbar) findViewById(R.id.chat_toolbar);
        setSupportActionBar(chatToolBar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = layoutInflater.inflate(R.layout.custom_chat_bar,null);

        actionBar.setCustomView(actionBarView);

        userName = (TextView) findViewById(R.id.custom_user_name);
        userLastSeen  = (TextView) findViewById(R.id.custom_user_status);
        userImage = (CircleImageView) findViewById(R.id.custom_profile_image);

        messageInputText = (EditText) findViewById(R.id.chat_input_message);
        sendMessageBtn = (ImageButton) findViewById(R.id.chat_send_message);
        sendFileBtn = (ImageButton) findViewById(R.id.chat_send_file);

        adapter = new MessagesAdapter(messagesList);
        userMessageList = (RecyclerView) findViewById(R.id.private_chat_of_user);
        manager = new LinearLayoutManager(this);
        userMessageList.setLayoutManager(manager);
        userMessageList.setAdapter(adapter);

        loadingBar = new ProgressDialog(this);

        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        saveCurrentTime = currentTime.format(calendar.getTime());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 438 && resultCode == RESULT_OK && data!=null && data.getData()!=null){

            loadingBar.setTitle("Sending File");
            loadingBar.setMessage("Please wait,while we are sending image");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            fileUri = data.getData();

            if(!checker.equals("image")){
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Document File");

                final String messageSendeRef = "Message/" + messageSenderId + "/" + messageReceiverId;
                final String messageReceiverRef = "Message/" + messageReceiverId + "/" + messageSenderId;

                DatabaseReference userMessageKeyRef = rootRef.child("Messages")
                        .child(messageSenderId).child(messageReceiverId).push();

                final String messagePushId = userMessageKeyRef.getKey();

                final StorageReference filePath = storageReference.child(messagePushId + "." + checker);

                filePath.putFile(fileUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Map messageTextBody = new HashMap();
                        messageTextBody.put("message",taskSnapshot.getMetadata().getReference().getDownloadUrl().toString());
                        messageTextBody.put("name",fileUri.getLastPathSegment());
                        messageTextBody.put("type",checker);
                        messageTextBody.put("from",messageSenderId);
                        messageTextBody.put("to",messageReceiverId);
                        messageTextBody.put("messageId",messagePushId);
                        messageTextBody.put("time",saveCurrentTime);
                        messageTextBody.put("date",saveCurrentDate);

                        Map messageBodyDetails = new HashMap();
                        messageBodyDetails.put(messageSendeRef + "/" + messagePushId,messageTextBody);
                        messageBodyDetails.put(messageReceiverRef + "/" + messagePushId,messageTextBody);
                        rootRef.updateChildren(messageBodyDetails);
                        loadingBar.dismiss();
                    }
                });

            }
            else if(checker.equals("image")){
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image File");

                final String messageSendeRef = "Message/" + messageSenderId + "/" + messageReceiverId;
                final String messageReceiverRef = "Message/" + messageReceiverId + "/" + messageSenderId;

                DatabaseReference userMessageKeyRef = rootRef.child("Messages")
                        .child(messageSenderId).child(messageReceiverId).push();

                final String messagePushId = userMessageKeyRef.getKey();

                final StorageReference filePath = storageReference.child(messagePushId + ".jpg");
                uploadTask = filePath.putFile(fileUri);

                uploadTask.continueWithTask(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) throws Exception {
                        if(!task.isSuccessful()){
                            throw task.getException();
                        }
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        Uri downloadUrl = task.getResult();
                        myUrl = downloadUrl.toString();

                        Map messageTextBody = new HashMap();
                        messageTextBody.put("message",myUrl);
                        messageTextBody.put("name",fileUri.getLastPathSegment());
                        messageTextBody.put("type",checker);
                        messageTextBody.put("from",messageSenderId);
                        messageTextBody.put("to",messageReceiverId);
                        messageTextBody.put("messageId",messagePushId);
                        messageTextBody.put("time",saveCurrentTime);
                        messageTextBody.put("date",saveCurrentDate);

                        Map messageBodyDetails = new HashMap();
                        messageBodyDetails.put(messageSendeRef + "/" + messagePushId,messageTextBody);
                        messageBodyDetails.put(messageReceiverRef + "/" + messagePushId,messageTextBody);

                        rootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                            @Override
                            public void onComplete(@NonNull Task task) {
                                if(task.isSuccessful()){
                                    loadingBar.dismiss();
                                    Toast.makeText(ChatActivity.this, "Message Sent ", Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    loadingBar.dismiss();
                                    Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                }
                                messageInputText.setText("");
                            }
                        });
                    }
                });

            }
            else {
                loadingBar.dismiss();
                Toast.makeText(this, "Nothing Selected or Error ", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("Checking","Ok1");

        rootRef.child("Message").child(messageSenderId).child(messageReceiverId)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Log.d("Checking1","Ok1");
                        Messages msg = dataSnapshot.getValue(Messages.class);
                        Log.d("Checking2","Ok3");
                        messagesList.add(msg);
                        adapter.notifyDataSetChanged();

                        userMessageList.smoothScrollToPosition(userMessageList.getAdapter().getItemCount());

                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void sendMessage() {
        final String messageInput = messageInputText.getText().toString();
        if(TextUtils.isEmpty(messageInput)){
            Toast.makeText(this, "Please Write A Message", Toast.LENGTH_SHORT).show();
        }
        else {
            String messageSendeRef = "Message/" + messageSenderId + "/" + messageReceiverId;
            String messageReceiverRef = "Message/" + messageReceiverId + "/" + messageSenderId;

            DatabaseReference userMessageKeyRef = rootRef.child("Messages")
                    .child(messageSenderId).child(messageReceiverId).push();

            String messagePushId = userMessageKeyRef.getKey();

            Map messageTextBody = new HashMap();
            messageTextBody.put("message",messageInput);
            messageTextBody.put("type","text");
            messageTextBody.put("from",messageSenderId);
            messageTextBody.put("to",messageReceiverId);
            messageTextBody.put("messageId",messagePushId);
            messageTextBody.put("time",saveCurrentTime);
            messageTextBody.put("date",saveCurrentDate);

            Map messageBodyDetails = new HashMap();
            messageBodyDetails.put(messageSendeRef + "/" + messagePushId,messageTextBody);
            messageBodyDetails.put(messageReceiverRef + "/" + messagePushId,messageTextBody);

            rootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(task.isSuccessful()){
                        Toast.makeText(ChatActivity.this, "Message Sent ", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                    messageInputText.setText("");
                }
            });

        }
    }

    private void displayUserLastSeen(){
        rootRef.child("Users").child(messageReceiverId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.child("userState").hasChild("state")){
                    String state = dataSnapshot.child("userState").child("state").getValue().toString();
                    String date = dataSnapshot.child("userState").child("date").getValue().toString();
                    String time = dataSnapshot.child("userState").child("time").getValue().toString();

                    if(state.equals("online")){
                        userLastSeen.setText("Online");
                    }
                    else if(state.equals("offline")){
                        userLastSeen.setText("Last Seen : " + date + "  " + time);
                    }
                }
                else {
                    userLastSeen.setText("Offline");
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

}
