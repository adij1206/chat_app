package com.example.chatapp.Adapters;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.Activities.ImageViewerActivity;
import com.example.chatapp.Activities.MainActivity;
import com.example.chatapp.Model.Messages;
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

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessagesViewHolder> {

    private List<Messages> userMessageList;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    public MessagesAdapter(@NonNull List<Messages> userMessageList) {
        this.userMessageList = userMessageList;
    }


    @NonNull
    @Override
    public MessagesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.custom_message_layout,parent,false);

        mAuth = FirebaseAuth.getInstance();

        return new MessagesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MessagesViewHolder holder, final int position) {
        String messageSenderId = mAuth.getCurrentUser().getUid();
        Messages messages = userMessageList.get(position);

        String fromUserId  = messages.getFrom();
        String fromMessageType = messages.getType();

        userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUserId);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild("images")){
                    String receiversImage = dataSnapshot.child("images").getValue().toString();
                    Picasso.get().load(receiversImage).placeholder(R.drawable.profile_image).into(holder.receiverImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        holder.receiverMessageText.setVisibility(View.GONE);
        holder.receiverImage.setVisibility(View.GONE);
        holder.senderMessageText.setVisibility(View.GONE);
        holder.messageSenderPicture.setVisibility(View.GONE);
        holder.messageReceiverPicture.setVisibility(View.GONE);

        if(fromMessageType.equals("text")){
            if(fromUserId.equals(messageSenderId)){
                holder.senderMessageText.setVisibility(View.VISIBLE);
                holder.senderMessageText.setBackgroundResource(R.drawable.sender_messages_layout);
                holder.senderMessageText.setText(messages.getMessage()+ "\n \n" + messages.getTime() + " - " + messages.getDate());
            }
            else {

                holder.receiverMessageText.setVisibility(View.VISIBLE);
                holder.receiverImage.setVisibility(View.VISIBLE);

                holder.receiverMessageText.setBackgroundResource(R.drawable.receiver_messages_layout);
                holder.receiverMessageText.setText(messages.getMessage()+ "\n \n" + messages.getTime() + " - " + messages.getDate());
            }
        }
        else if(fromMessageType.equals("image")){
            if(fromUserId.equals(messageSenderId)){
                holder.messageSenderPicture.setVisibility(View.VISIBLE);

                Picasso.get().load(messages.getMessage()).into(holder.messageSenderPicture);
            }
            else {
                holder.receiverImage.setVisibility(View.VISIBLE);
                holder.messageReceiverPicture.setVisibility(View.VISIBLE);

                Picasso.get().load(messages.getMessage()).into(holder.messageReceiverPicture);
            }
        }
        else if(fromMessageType.equals("pdf") ||fromMessageType.equals("docx") ){
            if(fromUserId.equals(messageSenderId)){
                holder.messageSenderPicture.setVisibility(View.VISIBLE);
                //holder.messageSenderPicture.setBackgroundResource(R.drawable.fii);
                Picasso.get().load("https://firebasestorage.googleapis.com/v0/b/chat-database-995bd.appspot.com/o/Image%20File%2Ffii.png?alt=media&token=e143270f-827b-496b-8a0c-8e809ea11c94")
                        .into(holder.messageSenderPicture);

            }
            else {
                holder.receiverImage.setVisibility(View.VISIBLE);
                holder.messageReceiverPicture.setVisibility(View.VISIBLE);
                //holder.messageReceiverPicture.setBackgroundResource(R.drawable.fii);
                Picasso.get().load("https://firebasestorage.googleapis.com/v0/b/chat-database-995bd.appspot.com/o/Image%20File%2Ffii.png?alt=media&token=e143270f-827b-496b-8a0c-8e809ea11c94")
                        .into(holder.messageReceiverPicture);

            }
        }

        if(fromUserId.equals(messageSenderId)){
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (userMessageList.get(position).getType().equals("pdf") || userMessageList.get(position).getType().equals("pdf")) {
                        CharSequence options[] = new CharSequence[]{
                                "Delete for Me",
                                "Download and View File",
                                "Cancel",
                                "Delete for Everyone"
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(which == 0){
                                    deleteSentMessage(position,holder);
                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                                else if(which == 1){
                                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            //TODO: Error occurs ,unable to open file
                                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(userMessageList.get(position).getMessage()));
                                            holder.itemView.getContext().startActivity(intent);
                                        }
                                    });
                                }
                                else if(which == 3){
                                    deleteMessageForEveryone(position,holder);
                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }

                            }
                        });
                        builder.show();

                    }

                    else if (userMessageList.get(position).getType().equals("text")) {
                        CharSequence options[] = new CharSequence[]{
                                "Delete for Me",
                                "Cancel",
                                "Delete for Everyone"
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(which == 0){
                                    deleteSentMessage(position,holder);
                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }

                                else if(which == 2){
                                    deleteMessageForEveryone(position,holder);
                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }

                            }
                        });
                        builder.show();
                    }

                    else if (userMessageList.get(position).getType().equals("image")) {
                        CharSequence options[] = new CharSequence[]{
                                "Delete for Me",
                                "View This Image",
                                "Cancel",
                                "Delete for Everyone"
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(which == 0){
                                    deleteSentMessage(position,holder);
                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                                else if(which == 1){
                                    Intent intent = new Intent(holder.itemView.getContext(), ImageViewerActivity.class);
                                    intent.putExtra("url",userMessageList.get(position).getMessage());
                                    holder.itemView.getContext().startActivity(intent);
                                }
                                else if(which == 3){
                                    deleteMessageForEveryone(position,holder);
                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }

                            }
                        });
                        builder.show();
                    }
                }
            });
        }
        else {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (userMessageList.get(position).getType().equals("pdf") || userMessageList.get(position).getType().equals("pdf")) {
                        CharSequence options[] = new CharSequence[]{
                                "Delete for Me",
                                "Download and View File",
                                "Cancel"
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(which == 0){
                                    deleteReceivedMessage(position,holder);
                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                                else if(which == 1){
                                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            //TODO: Error occurs ,unable to open file
                                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(userMessageList.get(position).getMessage()));
                                            holder.itemView.getContext().startActivity(intent);
                                        }
                                    });
                                }

                            }
                        });
                        builder.show();

                    }

                    else if (userMessageList.get(position).getType().equals("text")) {
                        CharSequence options[] = new CharSequence[]{
                                "Delete for Me",
                                "Cancel"
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(which == 0){
                                    deleteReceivedMessage(position,holder);
                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }

                            }
                        });
                        builder.show();
                    }

                    else if (userMessageList.get(position).getType().equals("image")) {
                        CharSequence options[] = new CharSequence[]{
                                "Delete for Me",
                                "View This Image",
                                "Cancel"
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(which == 0){
                                    deleteReceivedMessage(position,holder);
                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                                else if(which == 1){
                                    Intent intent = new Intent(holder.itemView.getContext(), ImageViewerActivity.class);
                                    intent.putExtra("url",userMessageList.get(position).getMessage());
                                    holder.itemView.getContext().startActivity(intent);

                                }

                            }
                        });
                        builder.show();
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return userMessageList.size();
    }

    public void deleteSentMessage(final int positions,final MessagesViewHolder holder){
        DatabaseReference RootRef = FirebaseDatabase.getInstance().getReference();
        RootRef.child("Message")
                .child(userMessageList.get(positions).getFrom())
                .child(userMessageList.get(positions).getTo())
                .child(userMessageList.get(positions).getMessageId())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(holder.itemView.getContext(),"Message is deleted",Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(holder.itemView.getContext(),"Error Occurred",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void deleteReceivedMessage(final int positions,final MessagesViewHolder holder){
        DatabaseReference RootRef = FirebaseDatabase.getInstance().getReference();
        RootRef.child("Message")
                .child(userMessageList.get(positions).getTo())
                .child(userMessageList.get(positions).getFrom())
                .child(userMessageList.get(positions).getMessageId())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){

                    Toast.makeText(holder.itemView.getContext(),"Message is deleted",Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(holder.itemView.getContext(),"Error Occurred",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void deleteMessageForEveryone(final int positions,final MessagesViewHolder holder){
        final DatabaseReference RootRef = FirebaseDatabase.getInstance().getReference();
        RootRef.child("Message")
                .child(userMessageList.get(positions).getTo())
                .child(userMessageList.get(positions).getFrom())
                .child(userMessageList.get(positions).getMessageId())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    RootRef.child("Message")
                            .child(userMessageList.get(positions).getFrom())
                            .child(userMessageList.get(positions).getTo())
                            .child(userMessageList.get(positions).getMessageId())
                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                Toast.makeText(holder.itemView.getContext(),"Message is deleted",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                }
                else {
                    Toast.makeText(holder.itemView.getContext(),"Error Occurred",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public class MessagesViewHolder extends RecyclerView.ViewHolder{

        TextView senderMessageText,receiverMessageText;
        CircleImageView receiverImage;
        ImageView messageSenderPicture,messageReceiverPicture;

        public MessagesViewHolder(@NonNull View itemView) {
            super(itemView);

            senderMessageText = (TextView) itemView.findViewById(R.id.message_sender_text);
            receiverMessageText = (TextView) itemView.findViewById(R.id.message_receiver_text);
            receiverImage = (CircleImageView) itemView.findViewById(R.id.message_profile_image);
            messageSenderPicture = (ImageView) itemView.findViewById(R.id.message_sender_image_view);
            messageReceiverPicture = (ImageView) itemView.findViewById(R.id.message_receiver_image_view);
        }
    }

}
