package com.example.universe;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.universe.Models.Event;
import com.example.universe.Models.Message;
import com.example.universe.Models.User;

import java.util.ArrayList;

public class MessageAdapter extends RecyclerView.Adapter {
    private final Context context;
    private ArrayList<Message> messageList;
    private static Util util;

    private final User me;

    private final IMessageListRecyclerAction mListener;

    int ITEM_SEND = 1;
    int ITEM_RECEIVE = 2;

    public MessageAdapter(Context context, ArrayList<Message> messageList, User me) {
        this.context = context;
        this.messageList = messageList;
        this.me = me;
        util = Util.getInstance();
        if(context instanceof IMessageListRecyclerAction){
            mListener = (IMessageListRecyclerAction) context;
        }else{
            throw new RuntimeException(context.toString()+ "must implement IEventListRecyclerAction");
        }
    }

    public ArrayList<Message> getMessages() {
        return messageList;
    }

    public void setMessages(ArrayList<Message> messages) {
        this.messageList = messages;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_SEND) {
            View view = LayoutInflater.from(context).inflate(R.layout.senderchat, parent, false);
            return new SenderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.receiverchat, parent, false);
            return new ReceiverViewHolder(view);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = this.getMessages().get(position);

        if (holder.getClass() == SenderViewHolder.class) {
            SenderViewHolder viewHolder = (SenderViewHolder) holder;
            //load user avatar
            util.getUser(message.getUserId(), user -> {
                if (user.getAvatarPath() != null) {
                    util.getDownloadUrlFromPath(user.getAvatarPath(), uri -> Glide.with(context)
                            .load(uri)
                            .centerCrop()
                            .into(viewHolder.getImageViewUserAvatar()), Util.DEFAULT_F_LISTENER);
                }
            }, Util.DEFAULT_F_LISTENER);

            if (message.getImagePath()!=null) {
                viewHolder.getImageViewPhoto().setVisibility(View.VISIBLE);
                viewHolder.getTextViewTimeOfMessage().setVisibility(View.GONE);
                viewHolder.getTextViewMessage().setVisibility(View.GONE);

                if (!message.getImagePath().startsWith("https")) {
                    util.getDownloadUrlFromPath(message.getImagePath(), uri -> Glide.with(context)
                            .load(uri)
                            .override(500, 500)
                            .into(viewHolder.getImageViewPhoto()), Util.DEFAULT_F_LISTENER);
                } else {
                    Glide.with(context)
                            .load(message.getImagePath())
                            .override(500, 500)
                            .into(viewHolder.getImageViewPhoto());
                }

            } else if (message.getFileURL()!=null) {
                viewHolder.getTextViewTimeOfMessage().setVisibility(View.GONE);
                viewHolder.getTextViewMessage().setVisibility(View.GONE);
                viewHolder.getImageViewPhoto().setVisibility(View.VISIBLE);
                viewHolder.getImageViewPhoto().setImageResource(R.drawable.filesmall);
                viewHolder.itemView.setOnClickListener(view -> mListener.openFile(message.getFileURL()));
            } else {
                viewHolder.getImageViewPhoto().setVisibility(View.GONE);
                viewHolder.getTextViewTimeOfMessage().setVisibility(View.VISIBLE);
                viewHolder.getTextViewMessage().setVisibility(View.VISIBLE);
                if (!message.getText().contains("Your friend share an event " +
                        "with you.Click Here to view the event!")) {
                    viewHolder.getTextViewMessage().setText(message.getText());
                } else {
                    viewHolder.getTextViewMessage().setText(message.getText().split("!")[0] + "!");
                    if (message.getText().split("!").length > 1) {
                        String eventId = message.getText().split("!")[1];
                        if (!eventId.isEmpty()) {
                            viewHolder.getTextViewMessage().setOnClickListener(v -> util.getEvent(eventId,
                                    event -> mListener.eventClickedFromRecyclerView(event, me),
                                    e -> Toast.makeText(context,"No Such Event!", Toast.LENGTH_SHORT).show()));
                        }
                    }
                }
                viewHolder.getTextViewTimeOfMessage().setText(message.getSimpleTime());
            }
        } else {
            ReceiverViewHolder viewHolder = (ReceiverViewHolder) holder;
            //load user avatar
            util.getUser(message.getUserId(), user -> {
                if (user.getAvatarPath() != null) {
                    util.getDownloadUrlFromPath(user.getAvatarPath(), uri -> Glide.with(context)
                            .load(uri)
                            .centerCrop()
                            .into(viewHolder.getImageViewUserAvatar()), Util.DEFAULT_F_LISTENER);
                }
            }, Util.DEFAULT_F_LISTENER);

            if (message.getImagePath()!=null) {
                viewHolder.getImageViewPhoto().setVisibility(View.VISIBLE);
                viewHolder.getTextViewTimeOfMessage().setVisibility(View.GONE);
                viewHolder.getTextViewMessage().setVisibility(View.GONE);

                if (!message.getImagePath().startsWith("https")) {
                    util.getDownloadUrlFromPath(message.getImagePath(), uri -> Glide.with(context)
                            .load(uri)
                            .override(500, 500)
                            .into(viewHolder.getImageViewPhoto()), Util.DEFAULT_F_LISTENER);
                } else {
                    Glide.with(context)
                            .load(message.getImagePath())
                            .override(500, 500)
                            .into(viewHolder.getImageViewPhoto());
                }

            } else if (message.getFileURL()!=null) {
                viewHolder.itemView.setOnClickListener(view -> mListener.openFile(message.getFileURL()));
                viewHolder.getImageViewPhoto().setVisibility(View.VISIBLE);
                viewHolder.getImageViewPhoto().setImageResource(R.drawable.filesmall);
                viewHolder.getTextViewTimeOfMessage().setVisibility(View.GONE);
                viewHolder.getTextViewMessage().setVisibility(View.GONE);
            } else {
                viewHolder.getImageViewPhoto().setVisibility(View.GONE);
                viewHolder.getTextViewTimeOfMessage().setVisibility(View.VISIBLE);
                viewHolder.getTextViewMessage().setVisibility(View.VISIBLE);
                if (!message.getText().contains("Your friend share an event " +
                        "with you.Click Here to view the event!")) {
                    viewHolder.getTextViewMessage().setText(message.getText());
                } else {
                    viewHolder.getTextViewMessage().setText(message.getText().split("!")[0] + "!");
                    if (message.getText().split("!").length > 1) {
                        String eventId = message.getText().split("!")[1];
                        if (!eventId.isEmpty()) {
                            viewHolder.getTextViewMessage().setOnClickListener(v -> util.getEvent(eventId,
                                    event -> mListener.eventClickedFromRecyclerView(event, me),
                                    e -> Toast.makeText(context,"No Such Event!", Toast.LENGTH_SHORT).show()));
                        }
                    }
                }
                viewHolder.getTextViewTimeOfMessage().setText(message.getSimpleTime());
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message message = this.getMessages().get(position);
        if (util.getCurrentUser().getUid().equals(message.getUserId())) {
            return ITEM_SEND;
        } else return ITEM_RECEIVE;
    }

    @Override
    public int getItemCount() {
        return this.getMessages().size();
    }

    public static class SenderViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewMessage;
        private final TextView timeOfMessage;
        private final ImageView imageViewPhoto;
        private final ImageView imageViewUserAvatar;

        public SenderViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.chatRoom_textView_messageContent);
            timeOfMessage = itemView.findViewById(R.id.chatRoom_textView_timeOfMessage);
            imageViewUserAvatar = itemView.findViewById(R.id.chatRoom_imageView_senderAvatar);
            imageViewPhoto = itemView.findViewById(R.id.chatRoom_imageView_message);
            imageViewPhoto.setVisibility(View.INVISIBLE);
        }

        public TextView getTextViewMessage() {
            return textViewMessage;
        }
        public TextView getTextViewTimeOfMessage() {
            return timeOfMessage;
        }
        public ImageView getImageViewPhoto() {
            return imageViewPhoto;
        }
        public ImageView getImageViewUserAvatar() {
            return imageViewUserAvatar;
        }
    }


    public static class ReceiverViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewMessage;
        private final TextView timeOfMessage;
        private final ImageView imageViewPhoto;
        private final ImageView imageViewUserAvatar;

        public ReceiverViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMessage = itemView.findViewById(R.id.chatRoom_textView_messageContent);
            timeOfMessage = itemView.findViewById(R.id.chatRoom_textView_timeOfMessage);
            imageViewUserAvatar = itemView.findViewById(R.id.chatRoom_imageView_senderAvatar);
            imageViewPhoto = itemView.findViewById(R.id.chatRoom_imageView_message);
            imageViewPhoto.setVisibility(View.INVISIBLE);
        }

        public TextView getTextViewMessage() {
            return textViewMessage;
        }

        public TextView getTextViewTimeOfMessage() {
            return timeOfMessage;
        }

        public ImageView getImageViewPhoto() {
            return imageViewPhoto;
        }
        public ImageView getImageViewUserAvatar() {
            return imageViewUserAvatar;
        }

    }

    public interface IMessageListRecyclerAction{
        void openFile(String url);
        void eventClickedFromRecyclerView(Event event, User user);
    }
}
