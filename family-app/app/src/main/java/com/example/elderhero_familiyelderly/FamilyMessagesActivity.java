package com.example.elderhero_familiyelderly;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class FamilyMessagesActivity extends AppCompatActivity {

    private ListView listChats;
    private ChatPreviewAdapter adapter;
    private List<ChatPreviewAdapter.ChatUser> chatUsers = new ArrayList<>();

    private String currentUid;

    private DatabaseReference chatThreadsRef;
    private DatabaseReference caretakerRef;

    private boolean isTablet;
    private FrameLayout chatContainer;

    private DrawerLayout drawerLayout;
    private ImageButton btnDrawer; // unified drawer button

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family_messages);

        drawerLayout = findViewById(R.id.drawerLayout);
        btnDrawer = findViewById(R.id.btnDrawer);
        listChats = findViewById(R.id.listChats);
        chatContainer = findViewById(R.id.chatContainer);

        // Setup drawer globally
        DrawerHelper.setupDrawer(this, drawerLayout, false);

        // Open drawer on click
        btnDrawer.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        chatThreadsRef = FirebaseDatabase.getInstance().getReference("ChatThreads");
        caretakerRef = FirebaseDatabase.getInstance().getReference("Caretakers");

        adapter = new ChatPreviewAdapter(this, chatUsers);
        listChats.setAdapter(adapter);

        // Detect if device is tablet
        isTablet = isTabletDevice();
        configureLayoutForDevice();

        loadChatThreads();

        listChats.setOnItemClickListener((parent, view, position, id) -> {
            ChatPreviewAdapter.ChatUser selected = chatUsers.get(position);

            if (isTablet) {
                // Tablet → open side panel
                ChatFragment fragment = ChatFragment.newInstance(
                        selected.uid,
                        selected.username
                );
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.chatContainer, fragment)
                        .commit();
            } else {
                // Phone → open full screen chat
                Intent i = new Intent(FamilyMessagesActivity.this, ChatActivity.class);
                i.putExtra("caretakerUid", selected.uid);
                i.putExtra("caretakerName", selected.username);
                startActivity(i);
            }
        });
    }

    /** Detect tablets using smallest width dp */
    private boolean isTabletDevice() {
        float dpWidth = getResources().getConfiguration().smallestScreenWidthDp;
        return dpWidth >= 600; // 8-inch tablets and above
    }

    /** Configure chatContainer visibility and weights */
    private void configureLayoutForDevice() {
        if (isTablet) {
            chatContainer.setVisibility(View.VISIBLE);

            android.widget.LinearLayout.LayoutParams params =
                    (android.widget.LinearLayout.LayoutParams) chatContainer.getLayoutParams();
            params.weight = 2;
            chatContainer.setLayoutParams(params);

        } else {
            chatContainer.setVisibility(View.GONE);
        }
    }

    private void loadChatThreads() {
        chatThreadsRef.orderByChild("familyElderlyId").equalTo(currentUid)
                .addValueEventListener(new ValueEventListener() {

                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        chatUsers.clear();

                        for (DataSnapshot threadSnap : snapshot.getChildren()) {
                            ChatThread thread = threadSnap.getValue(ChatThread.class);
                            if (thread == null) continue;

                            fetchCaretakerDetails(
                                    thread.caretakerId,
                                    thread.lastChat,
                                    thread.chatId
                            );
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(FamilyMessagesActivity.this,
                                "Failed to load chats", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchCaretakerDetails(String uid, String lastChat, String chatId) {
        caretakerRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String name = snapshot.child("CaretakerName").getValue(String.class);
                String img = snapshot.child("CaretakerProfileUrl").getValue(String.class);

                chatUsers.add(new ChatPreviewAdapter.ChatUser(
                        uid,
                        name != null ? name : "Unknown",
                        img != null ? img : "",
                        lastChat != null ? lastChat : "",
                        chatId
                ));

                adapter.notifyDataSetChanged();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
