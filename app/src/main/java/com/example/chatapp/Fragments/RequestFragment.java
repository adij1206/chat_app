package com.example.chatapp.Fragments;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatapp.Model.Contacts;
import com.example.chatapp.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 */
public class RequestFragment extends Fragment {

    private View requestView;
    private RecyclerView myChatRequestList;

    private DatabaseReference chatRequestRef,usersRef,contactsRef;
    private FirebaseAuth mAuth;
    private String currentUserId;

    public RequestFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        requestView =  inflater.inflate(R.layout.fragment_request, container, false);

        myChatRequestList = (RecyclerView) requestView.findViewById(R.id.chat_request_list);
        myChatRequestList.setLayoutManager(new LinearLayoutManager(getContext()));

        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        chatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

        return requestView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options = new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(chatRequestRef.child(currentUserId),Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts,RequestViewHolder> adapter = new FirebaseRecyclerAdapter<Contacts, RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final RequestViewHolder requestViewHolder, int i, @NonNull Contacts contacts) {
                requestViewHolder.acceptBtn.findViewById(R.id.chat_request_accept_btn).setVisibility(View.VISIBLE);
                requestViewHolder.declineBtn.findViewById(R.id.chat_request_decline_btn).setVisibility(View.VISIBLE);
                Log.d("Check","Reached");

                final String list_user_id = getRef(i).getKey();
                DatabaseReference getTypeRef = getRef(i).child("request_type").getRef();

                getTypeRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            String type = dataSnapshot.getValue().toString();
                            //Log.d("Check","Reached1");

                            if(type.equals("received")){
                                //Log.d("Check","Reached2");
                                usersRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.hasChild("images")){

                                            String requestUseImage = dataSnapshot.child("images").getValue().toString();
                                            String requestUserName = dataSnapshot.child("name").getValue().toString();
                                            String requestUserStatus = dataSnapshot.child("status").getValue().toString();

                                            requestViewHolder.userName.setText(requestUserName);
                                            requestViewHolder.userStatus.setText(requestUserStatus);

                                            Picasso.get().load(requestUseImage).placeholder(R.drawable.profile_image).into(requestViewHolder.userProfileImage);
                                        }
                                        else{
                                            String requestUserName = dataSnapshot.child("name").getValue().toString();
                                            String requestUserStatus = dataSnapshot.child("status").getValue().toString();

                                            requestViewHolder.userName.setText(requestUserName);
                                            requestViewHolder.userStatus.setText(requestUserStatus);
                                        }



                                        requestViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                CharSequence options[]= new CharSequence[]{
                                                        "Accept",
                                                        "Decline"
                                                };
                                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                                builder.setTitle("Chat Request");
                                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        if(which == 0){
                                                            contactsRef.child(currentUserId).child(list_user_id).child("contacts")
                                                                    .setValue("saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if(task.isSuccessful()){
                                                                        contactsRef.child(list_user_id).child(currentUserId).child("contacts")
                                                                                .setValue("saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                if(task.isSuccessful()){
                                                                                    chatRequestRef.child(currentUserId).child(list_user_id)
                                                                                            .removeValue()
                                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                @Override
                                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                                    if(task.isSuccessful()){
                                                                                                        chatRequestRef.child(list_user_id).child(currentUserId)
                                                                                                                .removeValue()
                                                                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                                    @Override
                                                                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                                                                        if(task.isSuccessful()){
                                                                                                                            Toast.makeText(getContext(), "New Contact Added", Toast.LENGTH_SHORT).show();
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
                                                        if(which == 1){
                                                            chatRequestRef.child(currentUserId).child(list_user_id)
                                                                    .removeValue()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if(task.isSuccessful()){
                                                                                chatRequestRef.child(list_user_id).child(currentUserId)
                                                                                        .removeValue()
                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                            @Override
                                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                                if(task.isSuccessful()){
                                                                                                    Toast.makeText(getContext(), "Request Removed", Toast.LENGTH_SHORT).show();
                                                                                                }
                                                                                            }
                                                                                        });
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                });
                                                builder.show();

                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }
                            else if(type.equals("sent")){
                                Button request_sent_btn = requestViewHolder.itemView.findViewById(R.id.chat_request_accept_btn);
                                request_sent_btn.setText("Request Sent");

                                requestViewHolder.itemView.findViewById(R.id.chat_request_decline_btn).setVisibility(View.INVISIBLE);

                                usersRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.hasChild("images")){

                                            String requestUseImage = dataSnapshot.child("images").getValue().toString();
                                            String requestUserName = dataSnapshot.child("name").getValue().toString();
                                            String requestUserStatus = dataSnapshot.child("status").getValue().toString();

                                            requestViewHolder.userName.setText(requestUserName);
                                            requestViewHolder.userStatus.setText("You Have Sent A Request " + requestUserName);

                                            Picasso.get().load(requestUseImage).placeholder(R.drawable.profile_image).into(requestViewHolder.userProfileImage);
                                        }
                                        else{
                                            String requestUserName = dataSnapshot.child("name").getValue().toString();
                                            String requestUserStatus = dataSnapshot.child("status").getValue().toString();

                                            requestViewHolder.userName.setText(requestUserName);
                                            requestViewHolder.userStatus.setText(requestUserStatus);
                                        }



                                        requestViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                CharSequence options[]= new CharSequence[]{
                                                        "Cancel Chat Request"
                                                };
                                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                                builder.setTitle("Already Sent Chat Request");
                                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        if(which == 0){
                                                            chatRequestRef.child(currentUserId).child(list_user_id)
                                                                    .removeValue()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if(task.isSuccessful()){
                                                                                chatRequestRef.child(list_user_id).child(currentUserId)
                                                                                        .removeValue()
                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                            @Override
                                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                                if(task.isSuccessful()){
                                                                                                    Toast.makeText(getContext(), "You have cancel the chat request", Toast.LENGTH_SHORT).show();
                                                                                                }
                                                                                            }
                                                                                        });
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                });
                                                builder.show();

                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });


                            }

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @NonNull
            @Override
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_request_display_list,parent,false);
                RequestViewHolder holder = new RequestViewHolder(view);
                return holder;
            }
        };

        myChatRequestList.setAdapter(adapter);
        adapter.startListening();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder{

        TextView userName,userStatus;
        Button acceptBtn,declineBtn;
        CircleImageView userProfileImage;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);

            userName = (TextView) itemView.findViewById(R.id.request_user_profile_name);
            userStatus = (TextView) itemView.findViewById(R.id.request_user_status);
            acceptBtn = (Button) itemView.findViewById(R.id.chat_request_accept_btn);
            declineBtn = (Button) itemView.findViewById(R.id.chat_request_decline_btn);
            userProfileImage = (CircleImageView) itemView.findViewById(R.id.request_user_profile_image);
        }
    }
}
