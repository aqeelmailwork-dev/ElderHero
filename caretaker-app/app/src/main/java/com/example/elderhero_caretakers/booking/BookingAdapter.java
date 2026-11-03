package com.example.elderhero_caretakers.booking;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderhero_caretakers.R;
import com.example.elderhero_caretakers.ReportBookingActivity;
import com.example.elderhero_caretakers.ScanQRCodeActivity;
import com.example.elderhero_caretakers.chats.ChatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    private final List<Booking> bookings;
    private boolean caretakerMode = false;
    private Context context;

    public BookingAdapter(List<Booking> bookings) {
        this.bookings = bookings;
    }

    public void setCaretakerMode(boolean mode) {
        this.caretakerMode = mode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View v = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking b = bookings.get(position);

        // 1. Basic Details
        String date = b.BookingDate != null ? b.BookingDate : "-";
        String time = b.BookingTime != null ? b.BookingTime : "-";
        holder.tvDateTime.setText(date + " • " + time);

        // BIND DURATION
        holder.tvDuration.setText("Est. Duration: " + (b.BookingDuration != null ? b.BookingDuration : "Not Specified"));

        holder.tvNotes.setText("Notes: " + (b.BookingNote != null && !b.BookingNote.isEmpty() ? b.BookingNote : "No notes provided."));

        // 2. Fetch User Info
        fetchFamilyName(b.FamiliyElderlyID, holder);
        fetchCaretakerName(b.CaretakerID, holder);

        // 3. Fetch Service Details
        fetchRate(b, holder);
        fetchCategoryAndSub(b, holder);

        // 4. Status Styling
        String status = b.BookingStatus != null ? b.BookingStatus : "Pending";
        holder.tvStatus.setText("Status: " + status);
        setStatusStyle(status, holder);

        // 5. Reset Visibility (Hide decision/action buttons by default)
        holder.layoutDecisionButtons.setVisibility(View.GONE); // The container for Accept/Reject
        holder.btnScanQR.setVisibility(View.GONE);
        holder.btnViewReview.setVisibility(View.GONE);

        // 6. General Action Button Logic
        holder.btnChat.setOnClickListener(v -> {
            if (b.FamiliyElderlyID == null) return;
            Intent chatIntent = new Intent(context, ChatActivity.class);
            chatIntent.putExtra("receiverUid", b.FamiliyElderlyID);
            chatIntent.putExtra("receiverName", holder.tvFamilyName.getText().toString());
            context.startActivity(chatIntent);
        });

        holder.btnViewProfile.setOnClickListener(v -> showFamilyProfilePopup(b.FamiliyElderlyID));

        holder.btnReport.setOnClickListener(v -> {
            Intent intent = new Intent(context, ReportBookingActivity.class);
            intent.putExtra("bookingId", b.BookingID);
            intent.putExtra("familyId", b.FamiliyElderlyID);
            context.startActivity(intent);
        });

        // 7. Caretaker Specific Workflow
        if (caretakerMode) {
            if ("Pending".equalsIgnoreCase(status)) {
                // Show the specific container for Accept/Reject
                holder.layoutDecisionButtons.setVisibility(View.VISIBLE);

                holder.btnAccept.setOnClickListener(v -> showAcceptWarning(b, holder));
                holder.btnReject.setOnClickListener(v -> showRejectDialog(b, holder));

            } else if ("Accepted".equalsIgnoreCase(status)) {
                holder.btnScanQR.setVisibility(View.VISIBLE);
                holder.btnScanQR.setOnClickListener(v -> {
                    Intent i = new Intent(context, ScanQRCodeActivity.class);
                    i.putExtra("bookingId", b.BookingID);
                    context.startActivity(i);
                });
            } else if ("Completed".equalsIgnoreCase(status)) {
                checkIfReviewed(b, holder);
            }
        }
    }

    // --- Data Fetching Methods ---

    private void fetchFamilyName(String familyUid, ViewHolder holder) {
        if (familyUid == null) { holder.tvFamilyName.setText("Unknown Family"); return; }
        FirebaseDatabase.getInstance().getReference("FamilyElderly").child(familyUid)
                .child("FamilyElderlyName").get().addOnSuccessListener(snap -> {
                    String name = snap.getValue(String.class);
                    holder.tvFamilyName.setText(name != null ? name : "Unknown");
                });
    }

    private void fetchCaretakerName(String caretakerId, ViewHolder holder) {
        if (caretakerId == null) { holder.tvCaretakerName.setText("Caretaker: Unknown"); return; }
        FirebaseDatabase.getInstance().getReference("Caretakers").child(caretakerId)
                .child("CaretakerName").get().addOnSuccessListener(snap -> {
                    String name = snap.getValue(String.class);
                    holder.tvCaretakerName.setText("Caretaker: " + (name != null ? name : "Unknown"));
                }).addOnFailureListener(e -> holder.tvCaretakerName.setText("Caretaker: -"));
    }

    private void fetchRate(Booking b, ViewHolder holder) {
        if (b.AdvertismentID == null || b.CaretakerID == null) { holder.tvRate.setText("Rate: N/A"); return; }
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Advertisements").child(b.CaretakerID).child(b.AdvertismentID);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String min = snapshot.child("AdvertisementMinRate").getValue(String.class);
                    String max = snapshot.child("AdvertisementMaxRate").getValue(String.class);
                    if (min != null && max != null) holder.tvRate.setText("RM " + min + "–" + max + "/hr");
                    else if (min != null) holder.tvRate.setText("RM " + min + "/hr");
                    else holder.tvRate.setText("Rate: N/A");
                } else { holder.tvRate.setText("Rate: N/A"); }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchCategoryAndSub(Booking b, ViewHolder holder) {
        if (b.AdvertismentID == null || b.CaretakerID == null) {
            holder.tvCategory.setText("Category: Unknown");
            holder.tvSubCategory.setText("Services: Unknown");
            return;
        }

        DatabaseReference adRef = FirebaseDatabase.getInstance().getReference("Advertisements").child(b.CaretakerID).child(b.AdvertismentID);
        adRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot adSnap) {
                if (!adSnap.exists()) return;

                String categoryID = adSnap.child("CategoriesID").getValue(String.class);
                List<String> subList = new ArrayList<>();
                for (DataSnapshot s : adSnap.child("SubCategoriesID").getChildren()) {
                    subList.add(s.getValue(String.class));
                }

                if (categoryID != null) {
                    FirebaseDatabase.getInstance().getReference("Categories").child(categoryID)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot catSnap) {
                                    String cName = catSnap.child("CategoryName").getValue(String.class);
                                    holder.tvCategory.setText("Category: " + (cName != null ? cName : "-"));

                                    DataSnapshot subsSnap = catSnap.child("SubCategories");
                                    StringBuilder sb = new StringBuilder();
                                    for (String subID : subList) {
                                        if (subsSnap.hasChild(subID)) {
                                            String sName = subsSnap.child(subID).child("SubCategoryName").getValue(String.class);
                                            if (sb.length() > 0) sb.append(", ");
                                            sb.append(sName);
                                        }
                                    }
                                    holder.tvSubCategory.setText(sb.length() == 0 ? "Services: None" : "Services: " + sb.toString());
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // --- Helper UI Methods ---

    private void setStatusStyle(String status, ViewHolder holder) {
        int bgColor, textColor;
        if ("Accepted".equalsIgnoreCase(status)) {
            bgColor = Color.parseColor("#E8F5E9"); textColor = Color.parseColor("#2E7D32");
        } else if ("Rejected".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status)) {
            bgColor = Color.parseColor("#FFEBEE"); textColor = Color.parseColor("#C62828");
        } else if ("Completed".equalsIgnoreCase(status)) {
            bgColor = Color.parseColor("#E3F2FD"); textColor = Color.parseColor("#1565C0");
        } else {
            bgColor = Color.parseColor("#FFF3E0"); textColor = Color.parseColor("#E65100");
        }
        holder.cvStatusContainer.setCardBackgroundColor(bgColor);
        holder.tvStatus.setTextColor(textColor);
    }

    private void showFamilyProfilePopup(String familyUid) {
        if (familyUid == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_family_profile, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView tvName = view.findViewById(R.id.tvName);
        TextView tvAge = view.findViewById(R.id.tvAge);
        TextView tvGender = view.findViewById(R.id.tvGender);
        TextView tvPhone = view.findViewById(R.id.tvPhone);
        TextView tvType = view.findViewById(R.id.tvType);
        ImageView imgProfile = view.findViewById(R.id.imgProfile);
        Button btnClose = view.findViewById(R.id.btnCloseProfile);

        FirebaseDatabase.getInstance().getReference("FamilyElderly").child(familyUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            tvName.setText(snapshot.child("FamilyElderlyName").getValue(String.class));
                            tvAge.setText("Age: " + snapshot.child("FamilyElderlyAge").getValue());
                            tvGender.setText("Gender: " + snapshot.child("FamilyElderlyGender").getValue(String.class));
                            tvPhone.setText("Phone: " + snapshot.child("FamilyElderlyPhone").getValue(String.class));
                            tvType.setText(snapshot.child("FamilyElderlyType").getValue(String.class));
                            String imgUrl = snapshot.child("FamilyElderlyURL").getValue(String.class);
                            if (imgUrl != null && !imgUrl.isEmpty()) Picasso.get().load(imgUrl).placeholder(R.drawable.ic_profile).into(imgProfile);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showReviewDialog(Booking b) {
        FirebaseDatabase.getInstance().getReference("Reviews").child(b.BookingID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(context, "No review found", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        View view = LayoutInflater.from(context).inflate(R.layout.dialog_review_popup, null);
                        builder.setView(view);
                        AlertDialog dialog = builder.create();
                        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                        TextView tvDetails = view.findViewById(R.id.tvReviewDetails);
                        TextView tvText = view.findViewById(R.id.tvReviewText);
                        Button btnClose = view.findViewById(R.id.btnCloseReview);

                        Float rating = snapshot.child("ReviewRating").getValue(Float.class);
                        String text = snapshot.child("ReviewDetails").getValue(String.class);

                        tvDetails.setText((rating != null ? rating : "0.0") + " / 5.0");
                        tvText.setText(text != null ? text : "No comment.");
                        btnClose.setOnClickListener(v -> dialog.dismiss());
                        dialog.show();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void checkIfReviewed(Booking b, ViewHolder holder) {
        FirebaseDatabase.getInstance().getReference("Reviews").child(b.BookingID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            holder.btnViewReview.setVisibility(View.VISIBLE);
                            holder.btnViewReview.setOnClickListener(v -> showReviewDialog(b));
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showAcceptWarning(Booking b, ViewHolder holder) {
        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Confirm Accept")
                .setMessage("Are you sure you want to accept this booking?")
                .setPositiveButton("Accept", (d, w) -> updateStatus(b, "Accepted", holder))
                .setNegativeButton("Cancel", null).show();
    }

    private void showRejectDialog(Booking b, ViewHolder holder) {
        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle("Reject Booking")
                .setMessage("Are you sure you want to reject this booking?")
                .setPositiveButton("Reject", (d, w) -> updateStatus(b, "Rejected", holder))
                .setNegativeButton("Cancel", null).show();
    }

    private void updateStatus(Booking b, String newStatus, ViewHolder holder) {
        FirebaseDatabase.getInstance().getReference("Bookings").child(b.BookingID)
                .child("BookingStatus").setValue(newStatus)
                .addOnSuccessListener(a -> {
                    b.BookingStatus = newStatus;
                    holder.tvStatus.setText("Status: " + newStatus);
                    setStatusStyle(newStatus, holder);
                    if("Accepted".equals(newStatus)) {
                        holder.layoutDecisionButtons.setVisibility(View.GONE); // Hide decision buttons
                        holder.btnScanQR.setVisibility(View.VISIBLE);
                    } else if ("Rejected".equals(newStatus)) {
                        holder.layoutDecisionButtons.setVisibility(View.GONE);
                    }
                    notifyItemChanged(holder.getAdapterPosition());
                });
    }

    @Override
    public int getItemCount() { return bookings.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFamilyName, tvCaretakerName, tvDateTime, tvNotes, tvStatus, tvRate, tvCategory, tvSubCategory, tvDuration;
        MaterialCardView cvStatusContainer;
        MaterialButton btnViewProfile, btnChat, btnReport, btnAccept, btnReject;
        Button btnScanQR, btnViewReview;
        LinearLayout layoutDecisionButtons; // New Container Reference

        ViewHolder(View itemView) {
            super(itemView);
            tvFamilyName = itemView.findViewById(R.id.tvFamilyName);
            tvCaretakerName = itemView.findViewById(R.id.tvCaretakerName);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvDuration = itemView.findViewById(R.id.tvDuration); // New binding
            tvRate = itemView.findViewById(R.id.tvRate);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvSubCategory = itemView.findViewById(R.id.tvSubCategory);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            cvStatusContainer = itemView.findViewById(R.id.cvStatusContainer);

            btnViewProfile = itemView.findViewById(R.id.btnViewProfile);
            btnChat = itemView.findViewById(R.id.btnChat);
            btnReport = itemView.findViewById(R.id.btnReport);

            // Buttons inside the new container
            layoutDecisionButtons = itemView.findViewById(R.id.layoutDecisionButtons);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);

            btnScanQR = itemView.findViewById(R.id.btnScanQR);
            btnViewReview = itemView.findViewById(R.id.btnViewReview);
        }
    }
}