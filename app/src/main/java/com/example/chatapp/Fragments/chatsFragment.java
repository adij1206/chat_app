package com.example.chatapp.Fragments;


import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.chatapp.Activities.ChatActivity;
import com.example.chatapp.Model.Contacts;
import com.example.chatapp.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 */
public class chatsFragment extends Fragment {

    private View chatsView;
    private RecyclerView privateChatList;
    private DatabaseReference chatsRef,usersRef;
    private FirebaseAuth mAuth;
    private String currentUserId;


    public chatsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
         chatsView = inflater.inflate(R.layout.fragment_chats, container, false);


         mAuth = FirebaseAuth.getInstance();
         currentUserId = mAuth.getCurrentUser().getUid();
         chatsRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserId);
         usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        privateChatList = (RecyclerView) chatsView.findViewById(R.id.private_chat_list);
        privateChatList.setLayoutManager(new LinearLayoutManager(getContext()));

        return chatsView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options = new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(chatsRef,Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts,ChatsViewHolder> adapter = new FirebaseRecyclerAdapter<Contacts, ChatsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ChatsViewHolder chatsViewHolder, int i, @NonNull Contacts contacts) {
                final String usersId = getRef(i).getKey();
                final String[] priUserImage = {"default_image"};
                usersRef.child(usersId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            if(dataSnapshot.hasChild("images")){
                                 priUserImage[0] = dataSnapshot.child("images").getValue().toString();
                                Picasso.get().load(priUserImage[0]).placeholder(R.drawable.profile_image).into(chatsViewHolder.privateUserImage);
                            }

                            final String priUserName = dataSnapshot.child("name").getValue().toString();
                            String priUserStatus = dataSnapshot.child("status").getValue().toString();

                            chatsViewHolder.privateUserName.setText(priUserName);


                            if(dataSnapshot.child("userState").hasChild("state")){
                                String state = dataSnapshot.child("userState").child("state").getValue().toString();
                                String date = dataSnapshot.child("userState").child("date").getValue().toString();
                                String time = dataSnapshot.child("userState").child("time").getValue().toString();

                                if(state.equals("online")){
                                    chatsViewHolder.privateUserStatus.setText("Online");
                                }
                                else if(state.equals("offline")){
                                    chatsViewHolder.privateUserStatus.setText("Last Seen : " + date + "  " + time);
                                }
                            }
                            else {
                                chatsViewHolder.privateUserStatus.setText("Offline");
                            }

                            chatsViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                    chatIntent.putExtra("visit_user_id",usersId);
                                    chatIntent.putExtra("visit_user_name",priUserName);
                                    chatIntent.putExtra("visit_user_image", priUserImage[0]);
                                    startActivity(chatIntent);
                                }
                            });
                           }
                        }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @NonNull
            @Override
            public ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(getContext()).inflate(R.layout.user_list_layout,parent,false);
                ChatsViewHolder holder = new ChatsViewHolder(view);

                return holder;
            }

        };
        privateChatList.setAdapter(adapter);
        adapter.startListening();
    }

    public static class ChatsViewHolder extends RecyclerView.ViewHolder{
        CircleImageView privateUserImage;
        TextView privateUserName,privateUserStatus;

        public ChatsViewHolder(@NonNull View itemView) {
            super(itemView);

            privateUserImage = (CircleImageView) itemView.findViewById(R.id.user_profile_image);
            privateUserName = (TextView) itemView.findViewById(R.id.user_profile_name);
            privateUserStatus = (TextView) itemView.findViewById(R.id.user_status);
        }
    }
}
