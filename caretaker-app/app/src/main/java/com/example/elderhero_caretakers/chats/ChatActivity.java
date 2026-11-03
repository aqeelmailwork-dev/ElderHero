package com.example.elderhero_caretakers.chats;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.elderhero_caretakers.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.*;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1001;
    private static final int REQ_RECORD_AUDIO = 1002;

    private String receiverUid, receiverName, currentUid;
    private String chatId;

    private DatabaseReference threadsRef;
    private DatabaseReference messagesRef;
    private StorageReference storageRef;

    private ListView listMessages;
    private EditText etMessage;
    private Button btnSend;
    private ImageButton btnBack, btnImage, btnVoice;

    private final List<MessageModel> messages = new ArrayList<>();
    private MessageAdapter messageAdapter;

    private MediaRecorder recorder;
    private String audioFilePath;
    private boolean isRecording = false;

    private final SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());


    // onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        receiverUid = getIntent().getStringExtra("receiverUid");
        receiverName = getIntent().getStringExtra("receiverName");

        if (receiverUid == null || receiverUid.trim().isEmpty()) {
            Toast.makeText(this, "Chat unavailable: missing receiver ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (receiverName == null || receiverName.trim().isEmpty()) {
            receiverName = "Unknown User";
        }

        TextView tvName = findViewById(R.id.tvCaretakerName);
        listMessages = findViewById(R.id.listMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        btnImage = findViewById(R.id.btnImage);
        btnVoice = findViewById(R.id.btnVoice);

        tvName.setText(receiverName);

        btnBack.setOnClickListener(view -> finish());

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Generate deterministic ChatID
        chatId = (currentUid.compareTo(receiverUid) < 0)
                ? currentUid + "_" + receiverUid
                : receiverUid + "_" + currentUid;

        threadsRef = FirebaseDatabase.getInstance().getReference("ChatThreads").child(chatId);
        messagesRef = FirebaseDatabase.getInstance().getReference("Messages").child(chatId);
        storageRef = FirebaseStorage.getInstance().getReference("ChatFiles");

        messageAdapter = new MessageAdapter(this, messages, currentUid);
        listMessages.setAdapter(messageAdapter);

        ensureThreadExists();
        loadMessages();

        btnSend.setOnClickListener(view -> sendTextMessage());
        btnImage.setOnClickListener(view -> pickImage());
        btnVoice.setOnClickListener(view -> toggleVoiceRecording());

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQ_RECORD_AUDIO
        );
    }


    // Ensure ChatThread Exists
    private void ensureThreadExists() {

        final String fChatId = chatId;
        final String fCaretaker = currentUid;
        final String fFamily = receiverUid;

        threadsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {

                    ChatThread thread = new ChatThread(
                            fChatId,
                            fCaretaker,
                            fFamily,
                            null,
                            System.currentTimeMillis()
                    );

                    threadsRef.setValue(thread);
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    // Load Messages
    private void loadMessages() {

        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {

                messages.clear();

                for (DataSnapshot msgSnap : snapshot.getChildren()) {
                    MessageModel m = msgSnap.getValue(MessageModel.class);
                    if (m != null) messages.add(m);
                }

                Collections.sort(messages, (a, b) ->
                        Long.compare(a.messageTimeStamp, b.messageTimeStamp));

                messageAdapter.notifyDataSetChanged();
                listMessages.setSelection(messages.size() - 1);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    // Send Text Message
    private void sendTextMessage() {
        String text = etMessage.getText().toString().trim();
        if (!TextUtils.isEmpty(text)) {
            pushMessage("text", text, null);
        }
        etMessage.setText("");
    }


    // Pick Image
    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, PICK_IMAGE);
    }


    // Upload Image
    private void uploadImage(Uri uri) {

        StorageReference fileRef = storageRef.child("images/" + System.currentTimeMillis() + ".jpg");

        fileRef.putFile(uri)
                .addOnSuccessListener(t -> fileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    pushMessage("image", null, downloadUri.toString());
                }))
                .addOnFailureListener(e -> Toast.makeText(this, "Image upload failed.", Toast.LENGTH_SHORT).show());
    }


    // Toggle Voice Recording
    private void toggleVoiceRecording() {

        if (isRecording) {
            stopRecordingAndUpload();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQ_RECORD_AUDIO
            );
            return;
        }

        startRecording();
    }


    // START RECORDING (AAC .m4a)
    private void startRecording() {
        audioFilePath = getCacheDir().getAbsolutePath() + "/voice_" + System.currentTimeMillis() + ".m4a";

        try {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(128000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(audioFilePath);

            recorder.prepare();
            recorder.start();

            isRecording = true;
            btnVoice.setImageResource(android.R.drawable.ic_media_pause);

            Toast.makeText(this, "Recording...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Recording failed.", Toast.LENGTH_SHORT).show();
        }
    }


    private void stopRecordingAndUpload() {

        try {
            recorder.stop();
            recorder.release();
            recorder = null;
            isRecording = false;

            btnVoice.setImageResource(android.R.drawable.ic_btn_speak_now);

            File f = new File(audioFilePath);
            if (!f.exists() || f.length() == 0) {
                Toast.makeText(this, "Empty recording.", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadAudio(Uri.fromFile(f));

        } catch (Exception e) {
            Toast.makeText(this, "Failed to stop recording.", Toast.LENGTH_SHORT).show();
        }
    }


    // Upload Audio (.m4a)
    private void uploadAudio(Uri uri) {

        StorageReference audioRef =
                storageRef.child("audio/voice_" + System.currentTimeMillis() + ".m4a");

        audioRef.putFile(uri)
                .addOnSuccessListener(t ->
                        audioRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                            pushMessage("voice", null, downloadUri.toString());
                        }))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Audio upload failed.", Toast.LENGTH_SHORT).show());
    }


    // Push message to DB
    private void pushMessage(String type, String text, String fileUrl) {

        final String fChatId = chatId;
        final String fSender = currentUid;
        final String fReceiver = receiverUid;

        String msgId = messagesRef.push().getKey();
        if (msgId == null) return;

        long ts = System.currentTimeMillis();

        String msgText = (type.equals("text")) ? text : null;
        String msgImg = (type.equals("image")) ? fileUrl : null;
        String msgAudio = (type.equals("voice")) ? fileUrl : null;

        MessageModel message = new MessageModel(
                msgId,
                fChatId,
                fSender,
                fReceiver,
                type,
                msgText,
                msgImg,
                msgAudio,
                ts
        );

        messagesRef.child(msgId).setValue(message)
                .addOnSuccessListener(unused -> {
                    Map<String, Object> upd = new HashMap<>();
                    upd.put("lastTimestamp", ts);

                    if (msgText != null) upd.put("lastMessage", msgText);
                    else if (msgImg != null) upd.put("lastMessage", "[Photo]");
                    else upd.put("lastMessage", "[Voice]");

                    threadsRef.updateChildren(upd);
                });
    }


    // Activity Result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) uploadImage(uri);
        }
    }


    // Adapter
    public static class MessageAdapter extends BaseAdapter {

        private final android.content.Context ctx;
        private final List<MessageModel> list;
        private final String currentUid;
        private final SimpleDateFormat sdf =
                new SimpleDateFormat("hh:mm a", Locale.getDefault());

        public MessageAdapter(android.content.Context c, List<MessageModel> l, String uid) {
            ctx = c;
            list = l;
            currentUid = uid;
        }

        @Override public int getCount() { return list.size(); }
        @Override public Object getItem(int i) { return list.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View recycled, ViewGroup parent) {

            MessageModel msg = list.get(pos);
            boolean isMe = msg.senderId.equals(currentUid);

            LinearLayout layout = new LinearLayout(ctx);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(12, 6, 12, 6);

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
            params.setMargins(10, 8, 10, 8);
            params.gravity = isMe ? Gravity.END : Gravity.START;

            // Text
            if (msg.messageType.equals("text")) {

                TextView tv = new TextView(ctx);
                tv.setText(msg.messageText + "\n" + sdf.format(new Date(msg.messageTimeStamp)));
                tv.setTextColor(isMe ? 0xFFFFFFFF : 0xFF000000);
                tv.setBackgroundResource(isMe ? R.drawable.bg_chat_me : R.drawable.bg_chat_other);
                tv.setPadding(20, 14, 20, 14);
                tv.setLayoutParams(params);
                layout.addView(tv);
            }

            // Image
            else if (msg.messageType.equals("image")) {

                ImageView img = new ImageView(ctx);
                img.setLayoutParams(params);
                img.setAdjustViewBounds(true);
                img.setMaxWidth(600);

                Picasso.get().load(msg.messageImageURL).into(img);
                layout.addView(img);
            }

            // Voice
            else if (msg.messageType.equals("voice")) {

                Button play = new Button(ctx);
                play.setText("Play Voice");
                play.setLayoutParams(params);

                play.setOnClickListener(view -> {
                    try {
                        MediaPlayer p = new MediaPlayer();
                        p.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        p.setDataSource(msg.messageAudioURL);
                        p.setOnPreparedListener(MediaPlayer::start);
                        p.setOnCompletionListener(mp -> mp.release());
                        p.prepareAsync();

                    } catch (Exception e) {
                        Toast.makeText(ctx, "Cannot play audio.", Toast.LENGTH_SHORT).show();
                    }
                });

                layout.addView(play);
            }

            return layout;
        }
    }
}
