package com.example.elderhero_familiyelderly;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatFragment extends Fragment {

    private static final int PICK_IMAGE = 1001;
    private static final int REQ_RECORD_AUDIO = 2001;

    private String caretakerUid, caretakerName, currentUid, chatId;

    private DatabaseReference threadsRef, messagesRef;
    private StorageReference storageRef;

    private ListView listMessages;
    private EditText etMessage;
    private Button btnSend;
    private ImageButton btnImage, btnVoice;

    private final List<MessageModel> messages = new ArrayList<>();
    private ChatActivity.MessageAdapter messageAdapter;

    private MediaRecorder recorder;
    private MediaPlayer activePlayer;

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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

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
        storageRef = FirebaseStorage.getInstance().getReference("ChatFiles");

        chatId = currentUid.compareTo(caretakerUid) < 0
                ? currentUid + "_" + caretakerUid
                : caretakerUid + "_" + currentUid;

        threadsRef = FirebaseDatabase.getInstance().getReference("ChatThreads").child(chatId);
        messagesRef = FirebaseDatabase.getInstance().getReference("Messages").child(chatId);

        messageAdapter = new ChatActivity.MessageAdapter(requireContext(), messages, currentUid);
        listMessages.setAdapter(messageAdapter);

        ensureThreadExists();
        loadMessages();

        btnSend.setOnClickListener(v -> sendMessage());
        btnImage.setOnClickListener(v -> openGallery());
        btnVoice.setOnClickListener(v -> toggleVoiceRecording());

        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);

        return view;
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
                if (!messages.isEmpty())
                    listMessages.setSelection(messages.size() - 1);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        pushMessage("text", text, null);
        etMessage.setText("");
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    private void toggleVoiceRecording() {

        if (isRecording) {
            stopRecordingAndUpload();
            return;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_RECORD_AUDIO);
            return;
        }

        startRecording();
    }

    private void startRecording() {

        audioFilePath = requireContext().getCacheDir().getAbsolutePath() +
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

            Toast.makeText(requireContext(), "Recording voice…", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Recording failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            if (!audioFile.exists() || audioFile.length() == 0) {
                Toast.makeText(requireContext(), "No valid audio", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadAudio(Uri.fromFile(audioFile));

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Stopping failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadAudio(Uri uri) {

        StorageReference audioRef =
                storageRef.child("audio/voice_" + System.currentTimeMillis() + ".m4a");

        audioRef.putFile(uri)
                .addOnSuccessListener(t ->
                        audioRef.getDownloadUrl()
                                .addOnSuccessListener(url ->
                                        pushMessage("voice", null, url.toString())))
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show());
    }

    private void uploadImage(Uri uri) {

        StorageReference imgRef =
                storageRef.child("images/" + System.currentTimeMillis() + ".jpg");

        imgRef.putFile(uri)
                .addOnSuccessListener(t ->
                        imgRef.getDownloadUrl().addOnSuccessListener(url ->
                                pushMessage("image", null, url.toString())))
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Image upload failed", Toast.LENGTH_SHORT).show());
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
                .addOnSuccessListener(aVoid -> {

                    String preview = type.equals("text") ? text :
                            type.equals("image") ? "[Photo]" : "[Voice]";

                    Map<String, Object> upd = new HashMap<>();
                    upd.put("lastChat", preview);
                    upd.put("lastTimestamp", ts);

                    threadsRef.updateChildren(upd);
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE &&
                resultCode == requireActivity().RESULT_OK &&
                data != null) {

            uploadImage(data.getData());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        releasePlayer();
    }

    private void releasePlayer() {
        if (activePlayer != null) {
            try { activePlayer.stop(); } catch (Exception ignored) {}
            try { activePlayer.release(); } catch (Exception ignored) {}
            activePlayer = null;
        }
    }
}