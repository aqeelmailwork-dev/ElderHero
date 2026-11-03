package com.example.elderhero_caretakers.chats;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.elderhero_caretakers.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class ChatPreviewAdapter extends BaseAdapter {

    private Context context;
    private List<ChatUser> users;

    public ChatPreviewAdapter(Context context, List<ChatUser> users) {
        this.context = context;
        this.users = users;
    }

    @Override public int getCount() { return users.size(); }
    @Override public Object getItem(int position) { return users.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.activity_chat_preview_adapter, parent, false);
        }

        ImageView imgProfile = convertView.findViewById(R.id.imgProfile);
        TextView tvName = convertView.findViewById(R.id.tvName);
        TextView tvLastMsg = convertView.findViewById(R.id.tvLastMsg);

        ChatUser user = users.get(position);

        tvName.setText(user.username != null ? user.username : "Unknown");
        tvLastMsg.setText(user.lastMessage != null ? user.lastMessage : "Tap to view chat");

        if (user.imageUrl != null && !user.imageUrl.isEmpty()) {
            Picasso.get()
                    .load(user.imageUrl)
                    .placeholder(R.drawable.ic_person_placeholder)
                    .into(imgProfile);
        } else {
            imgProfile.setImageResource(R.drawable.ic_person_placeholder);
        }

        return convertView;
    }

    public static class ChatUser {
        public String uid;
        public String username;
        public String imageUrl;
        public String lastMessage;
        public String chatId;

        public ChatUser(String uid, String username, String imageUrl, String lastMessage, String chatId) {
            this.uid = uid;
            this.username = username;
            this.imageUrl = imageUrl;
            this.lastMessage = lastMessage;
            this.chatId = chatId;
        }
    }
}
