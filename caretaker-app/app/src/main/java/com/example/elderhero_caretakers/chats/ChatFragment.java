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
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.elderhero_caretakers.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.*;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatFragment extends Fragment {

    private static final int PICK_IMAGE = 1001;
    private static final int REQ_RECORD_AUDIO = 1002;

    private String caretakerUid, caretakerName, currentUid;
    private String chatId, familyUid;

    private DatabaseReference threadsRef, messagesRef;
    private StorageReference storageRef;

    private ListView listMessages;
    private EditText etMessage;
    private Button btnSend;
    private ImageButton btnImage, btnVoice;

    private final List<MessageModel> messages = new ArrayList<>();
    private ChatActivity.MessageAdapter messageAdapter;

    private MediaRecorder recorder;
    private String audioFilePath;
    private boolean isRecording = false;

    public ChatFragment() {}

    public static ChatFragment newInstance(String caretakerUid, String caretakerName) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString("caretakerUid", caretakerUid);
        args.putString("caretakerName", caretakerName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.activity_chat_fragment, container, false);

        caretakerUid = getArguments() != null ? getArguments().getString("caretakerUid") : "";
        caretakerName = getArguments() != null ? getArguments().getString("caretakerName") : "";

        TextView tvCaretakerName = view.findViewById(R.id.tvCaretakerName);
        listMessages = view.findViewById(R.id.listMessages);
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);
        btnImage = view.findViewById(R.id.btnImage);
        btnVoice = view.findViewById(R.id.btnVoice);

        tvCaretakerName.setText(!TextUtils.isEmpty(caretakerName) ? caretakerName : "Chat");

        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Determine family vs caretaker based on who opened chat
        familyUid = caretakerUid;

        // Deterministic ChatID
        chatId = currentUid.compareTo(familyUid) < 0
                ? currentUid + "_" + familyUid
                : familyUid + "_" + currentUid;

        threadsRef = FirebaseDatabase.getInstance().getReference("ChatThreads").child(chatId);
        messagesRef = FirebaseDatabase.getInstance().getReference("Messages").child(chatId);
        storageRef = FirebaseStorage.getInstance().getReference("ChatFiles");

        messageAdapter = new ChatActivity.MessageAdapter(requireContext(), messages, currentUid);
        listMessages.setAdapter(messageAdapter);

        ensureThread();
        loadMessages();

        btnSend.setOnClickListener(v -> sendMessage());
        btnImage.setOnClickListener(v -> openGallery());
        btnVoice.setOnClickListener(v -> toggleVoiceRecording());

        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);

        return view;
    }

    // CREATE CHAT THREAD (if not exists)
    private void ensureThread() {
        final String fChatId = chatId;
        final String fCaretaker = caretakerUid;
        final String fFamily = currentUid; // family-side fragment

        threadsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    ChatThread t = new ChatThread(
                            fChatId,
                            fCaretaker,
                            fFamily,
                            "", // lastMessage
                            System.currentTimeMillis()
                    );
                    threadsRef.setValue(t);
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // LOAD MESSAGES
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

    // SEND TEXT MESSAGE
    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        pushMessage("text", text, null);
        etMessage.setText("");
    }

    // SELECT IMAGE
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    // VOICE RECORDING toggle
    private void toggleVoiceRecording() {
        if (isRecording) {
            stopRecordingAndUpload();
            return;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
            return;
        }

        startRecording();
    }

    // START RECORDING (AAC / .m4a)
    private void startRecording() {
        audioFilePath = requireContext().getCacheDir().getAbsolutePath() +
                "/voice_" + System.currentTimeMillis() + ".m4a";

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

            Toast.makeText(requireContext(), "Recording...", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(requireContext(), "Recording failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void stopRecordingAndUpload() {
        try {
            recorder.stop();
            recorder.release();
            recorder = null;

            isRecording = false;
            btnVoice.setImageResource(android.R.drawable.ic_btn_speak_now);

            File audioFile = new File(audioFilePath);
            if (!audioFile.exists()) {
                Toast.makeText(requireContext(), "Recording failed.", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadAudio(Uri.fromFile(audioFile));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error stopping recording.", Toast.LENGTH_SHORT).show();
        }
    }

    // UPLOAD AUDIO (.m4a)
    private void uploadAudio(Uri uri) {
        StorageReference audioRef = storageRef.child("audio/voice_" + System.currentTimeMillis() + ".m4a");

        audioRef.putFile(uri)
                .addOnSuccessListener(s -> audioRef.getDownloadUrl().addOnSuccessListener(url -> {
                    pushMessage("voice", null, url.toString());
                }))
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Voice upload failed.", Toast.LENGTH_SHORT).show());
    }

    // UPLOAD IMAGE
    private void uploadImage(Uri uri) {
        StorageReference imgRef = storageRef.child("images/" + System.currentTimeMillis() + ".jpg");

        imgRef.putFile(uri)
                .addOnSuccessListener(s -> imgRef.getDownloadUrl().addOnSuccessListener(url -> {
                    pushMessage("image", null, url.toString());
                }))
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Image upload failed.", Toast.LENGTH_SHORT).show());
    }

    // SAVE MESSAGE TO DATABASE
    private void pushMessage(String type, String text, String fileUrl) {

        final String fChatId = chatId;
        final String fSender = currentUid;
        final String fReceiver = familyUid;

        String msgId = messagesRef.push().getKey();
        if (msgId == null) return;

        long ts = System.currentTimeMillis();

        MessageModel msg = new MessageModel(
                msgId,
                fChatId,
                fSender,
                fReceiver,
                type,
                ("text".equals(type) ? text : null),
                ("image".equals(type) ? fileUrl : null),
                ("voice".equals(type) ? fileUrl : null),
                ts
        );

        messagesRef.child(msgId).setValue(msg)
                .addOnSuccessListener(aVoid -> {

                    Map<String, Object> upd = new HashMap<>();
                    String preview = ("text".equals(type)) ? text :
                            ("image".equals(type) ? "[Photo]" : "[Voice]");
                    upd.put("lastMessage", preview);
                    upd.put("lastTimestamp", ts);

                    threadsRef.updateChildren(upd);
                });
    }

    // GET RESULT FROM GALLERY
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == requireActivity().RESULT_OK && data != null) {
            Uri uri = data.getData();
            uploadImage(uri);
        }
    }
}
