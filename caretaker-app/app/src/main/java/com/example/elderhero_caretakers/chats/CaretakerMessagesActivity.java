package com.example.elderhero_caretakers.chats;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.elderhero_caretakers.CaretakerDrawer;
import com.example.elderhero_caretakers.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class CaretakerMessagesActivity extends AppCompatActivity {

    private ListView listChats;
    private ChatPreviewAdapter adapter;
    private List<ChatPreviewAdapter.ChatUser> chatUsers = new ArrayList<>();

    private String currentUid;
    private DatabaseReference chatThreadsRef;
    private DatabaseReference familyRef;

    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;

    private boolean isTabletMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caretaker_messages);

        drawerLayout = findViewById(R.id.drawerLayout);
        btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        new CaretakerDrawer(this, findViewById(R.id.leftDrawer), false);

        listChats = findViewById(R.id.listChats);

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        chatThreadsRef = FirebaseDatabase.getInstance().getReference("ChatThreads");
        familyRef = FirebaseDatabase.getInstance().getReference("FamilyElderly");

        adapter = new ChatPreviewAdapter(this, chatUsers);
        listChats.setAdapter(adapter);

        // Detect mobile vs tablet
        isTabletMode = isTablet();
        setupDeviceLayout();

        loadChatThreads();

        listChats.setOnItemClickListener((parent, view, position, id) -> {
            ChatPreviewAdapter.ChatUser selectedUser = chatUsers.get(position);

            if (isTabletMode) {
                // Show chat fragment inside the right pane
                findViewById(R.id.chatContainer).setVisibility(View.VISIBLE);

                ChatFragment fragment = ChatFragment.newInstance(
                        selectedUser.uid,
                        selectedUser.username
                );

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.chatContainer, fragment)
                        .commit();

            } else {
                // Phone: open ChatActivity full screen
                Intent i = new Intent(CaretakerMessagesActivity.this, ChatActivity.class);
                i.putExtra("receiverUid", selectedUser.uid);
                i.putExtra("receiverName", selectedUser.username);
                startActivity(i);
            }
        });
    }

    /** Detect tablet (8 inches and above) using dp width */
    private boolean isTablet() {
        float dpWidth = getResources().getConfiguration().smallestScreenWidthDp;
        return dpWidth >= 600;   // 600dp = typical 7–8 inch tablet
    }

    /** Adjust layout based on screen size */
    private void setupDeviceLayout() {
        View chatContainer = findViewById(R.id.chatContainer);

        if (isTabletMode) {
            // Tablet → Split screen visible
            chatContainer.setVisibility(View.VISIBLE);

            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) chatContainer.getLayoutParams();
            params.weight = 2; // make right side bigger
            chatContainer.setLayoutParams(params);

        } else {
            // Mobile → Hide right panel until chat opens
            chatContainer.setVisibility(View.GONE);
        }
    }

    private void loadChatThreads() {
        chatThreadsRef.orderByChild("caretakerId").equalTo(currentUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        chatUsers.clear();

                        for (DataSnapshot threadSnap : snapshot.getChildren()) {
                            ChatThread thread = threadSnap.getValue(ChatThread.class);
                            if (thread == null) continue;

                            String familyUid = thread.familyElderlyId;
                            String chatId = thread.chatId;
                            String lastMsg = thread.lastMessage != null ? thread.lastMessage : "-";

                            familyRef.child(familyUid).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snap) {

                                    String name =
                                            snap.child("FamilyElderlyName").getValue(String.class) != null
                                                    ? snap.child("FamilyElderlyName").getValue(String.class)
                                                    : snap.child("username").getValue(String.class);

                                    String img =
                                            snap.child("FamilyElderlyURL").getValue(String.class) != null
                                                    ? snap.child("FamilyElderlyURL").getValue(String.class)
                                                    : snap.child("imageUrl").getValue(String.class);

                                    chatUsers.add(new ChatPreviewAdapter.ChatUser(
                                            familyUid,
                                            name != null ? name : "Unknown",
                                            img != null ? img : "",
                                            lastMsg,
                                            chatId
                                    ));

                                    adapter.notifyDataSetChanged();
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(CaretakerMessagesActivity.this,
                                "Failed to load chats.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
