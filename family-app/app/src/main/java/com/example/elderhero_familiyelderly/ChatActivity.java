package com.example.elderhero_familiyelderly;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1001;
    private static final int REQ_RECORD_AUDIO = 2001;

    private String caretakerUid, caretakerName, currentUid, chatId;

    private DatabaseReference threadsRef, messagesRef;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        caretakerUid = getIntent().getStringExtra("receiverUid");
        caretakerName = getIntent().getStringExtra("receiverName");

        if (caretakerUid == null) {
            Toast.makeText(this, "Error: User ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView tvName = findViewById(R.id.tvCaretakerName);
        tvName.setText(caretakerName != null ? caretakerName : "Chat");

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        listMessages = findViewById(R.id.listMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnImage = findViewById(R.id.btnImage);
        btnVoice = findViewById(R.id.btnVoice);

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        chatId = currentUid.compareTo(caretakerUid) < 0
                ? currentUid + "_" + caretakerUid
                : caretakerUid + "_" + currentUid;

        threadsRef = FirebaseDatabase.getInstance().getReference("ChatThreads").child(chatId);
        messagesRef = FirebaseDatabase.getInstance().getReference("Messages").child(chatId);
        storageRef = FirebaseStorage.getInstance().getReference("ChatFiles");

        messageAdapter = new MessageAdapter(this, messages, currentUid);
        listMessages.setAdapter(messageAdapter);

        ensureThreadExists();
        loadMessages();

        btnSend.setOnClickListener(v -> sendTextMessage());
        btnImage.setOnClickListener(v -> pickImage());
        btnVoice.setOnClickListener(v -> toggleVoiceRecording());

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQ_RECORD_AUDIO
        );
    }

    private void ensureThreadExists() {
        threadsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    ChatThread t = new ChatThread(
                            chatId,
                            caretakerUid,
                            currentUid,
                            "",
                            System.currentTimeMillis()
                    );
                    threadsRef.setValue(t);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    private void loadMessages() {
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {

                messages.clear();

                for (DataSnapshot s : snapshot.getChildren()) {
                    MessageModel m = s.getValue(MessageModel.class);
                    if (m != null) messages.add(m);
                }

                messages.sort(Comparator.comparingLong(a -> a.messageTimeStamp));
                messageAdapter.notifyDataSetChanged();
                listMessages.setSelection(messages.size() - 1);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    private void sendTextMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        pushMessage("text", text, null);
        etMessage.setText("");
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, PICK_IMAGE);
    }

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

    private void startRecording() {

        audioFilePath = getCacheDir().getAbsolutePath() +
                "/voice_" + System.currentTimeMillis() + ".m4a";

        try {
            recorder = new MediaRecorder();

            recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            recorder.setAudioEncodingBitRate(192000);
            recorder.setAudioSamplingRate(44100);

            recorder.setOutputFile(audioFilePath);
            recorder.prepare();
            recorder.start();

            isRecording = true;
            btnVoice.setImageResource(android.R.drawable.ic_media_pause);

            Toast.makeText(this, "Recording voice…", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Recording failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "No audio detected.", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadAudio(Uri.fromFile(f));

        } catch (Exception e) {
            Toast.makeText(this, "Stop failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadAudio(Uri uri) {

        StorageReference audioRef =
                storageRef.child("audio/voice_" + System.currentTimeMillis() + ".m4a");

        audioRef.putFile(uri)
                .addOnSuccessListener(t ->
                        audioRef.getDownloadUrl()
                                .addOnSuccessListener(downloadUri ->
                                        pushMessage("voice", null, downloadUri.toString())))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show());
    }


    private void uploadImage(Uri uri) {
        StorageReference fileRef =
                storageRef.child("images/" + System.currentTimeMillis() + ".jpg");

        fileRef.putFile(uri)
                .addOnSuccessListener(t ->
                        fileRef.getDownloadUrl().addOnSuccessListener(downloadUri ->
                                pushMessage("image", null, downloadUri.toString())))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Image upload failed.", Toast.LENGTH_SHORT).show());
    }


    private void pushMessage(String type, String text, String fileUrl) {

        String msgId = messagesRef.push().getKey();
        if (msgId == null) return;

        long ts = System.currentTimeMillis();

        MessageModel msg = new MessageModel(
                msgId,
                chatId,
                currentUid,
                caretakerUid,
                type,
                type.equals("text") ? text : null,
                type.equals("image") ? fileUrl : null,
                type.equals("voice") ? fileUrl : null,
                ts
        );

        messagesRef.child(msgId).setValue(msg)
                .addOnSuccessListener(unused -> {

                    String preview = type.equals("text") ? text :
                            type.equals("image") ? "[Photo]" : "[Voice]";

                    Map<String, Object> upd = new HashMap<>();
                    upd.put("lastChat", preview);
                    upd.put("lastTimestamp", ts);

                    threadsRef.updateChildren(upd);
                });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) uploadImage(uri);
        }
    }

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

            if (msg.messageType.equals("text")) {

                TextView tv = new TextView(ctx);
                tv.setText(msg.messageText + "\n" + sdf.format(new Date(msg.messageTimeStamp)));
                tv.setTextColor(isMe ? 0xFFFFFFFF : 0xFF000000);
                tv.setBackgroundResource(isMe ? R.drawable.bg_chat_me : R.drawable.bg_chat_other);
                tv.setPadding(20, 14, 20, 14);
                tv.setLayoutParams(params);
                layout.addView(tv);
            }

            else if (msg.messageType.equals("image")) {

                ImageView img = new ImageView(ctx);
                img.setLayoutParams(params);
                img.setAdjustViewBounds(true);
                img.setMaxWidth(600);
                Picasso.get().load(msg.messageImageURL).into(img);
                layout.addView(img);
            }

            else if (msg.messageType.equals("voice")) {

                Button play = new Button(ctx);
                play.setText("Play Voice");
                play.setLayoutParams(params);

                play.setOnClickListener(view -> {
                    try {

                        MediaPlayer p = new MediaPlayer();
                        p.setAudioAttributes(new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build());

                        p.setDataSource(msg.messageAudioURL);
                        p.prepareAsync();

                        p.setOnPreparedListener(MediaPlayer::start);
                        p.setOnCompletionListener(MediaPlayer::release);

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