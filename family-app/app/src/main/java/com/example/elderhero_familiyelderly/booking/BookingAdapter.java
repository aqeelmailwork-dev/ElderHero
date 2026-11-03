package com.example.elderhero_familiyelderly.booking;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderhero_familiyelderly.FamilyLeaveReviewActivity;
import com.example.elderhero_familiyelderly.R;
import com.example.elderhero_familiyelderly.ReportBookingActivity;
import com.example.elderhero_familiyelderly.ChatActivity;
import com.example.elderhero_familiyelderly.booking.ShowBookingQRCodeActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    private final List<Booking> bookings;
    private Map<String, Boolean> reviewStatusMap = new HashMap<>();
    private boolean caretakerMode = false;
    private Context context;

    public BookingAdapter(List<Booking> bookings) {
        this.bookings = bookings;
    }

    public void setCaretakerMode(boolean mode) {
        this.caretakerMode = mode;
    }

    public void setReviewStatusMap(Map<String, Boolean> map) {
        this.reviewStatusMap = map;
        notifyDataSetChanged();
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

        holder.tvDateTime.setText((b.BookingDate != null ? b.BookingDate : "-") + " • " + (b.BookingTime != null ? b.BookingTime : "-"));

        // BIND DURATION
        holder.tvDuration.setText("Est. Duration: " + (b.BookingDuration != null ? b.BookingDuration : "Not Specified"));

        holder.tvNotes.setText("Notes: " + (b.BookingNote != null ? b.BookingNote : "-"));

        // STATUS LOGIC WITH COLORS
        String status = b.BookingStatus != null ? b.BookingStatus : "Unknown";
        holder.tvStatus.setText("Status: " + status);

        int bgColor, textColor;

        if ("Accepted".equalsIgnoreCase(status)) {
            bgColor = Color.parseColor("#E8F5E9"); // Light Green
            textColor = Color.parseColor("#2E7D32"); // Dark Green
        } else if ("Rejected".equalsIgnoreCase(status)) {
            bgColor = Color.parseColor("#FFEBEE"); // Light Red
            textColor = Color.parseColor("#C62828"); // Dark Red
        } else if ("Completed".equalsIgnoreCase(status)) {
            bgColor = Color.parseColor("#E3F2FD"); // Light Blue
            textColor = Color.parseColor("#1565C0"); // Dark Blue
        } else {
            // Pending or Default
            bgColor = Color.parseColor("#FFF3E0"); // Light Orange
            textColor = Color.parseColor("#E65100"); // Dark Orange
        }

        holder.cvStatusContainer.setCardBackgroundColor(bgColor);
        holder.tvStatus.setTextColor(textColor);

        // Fetch other data
        fetchFamilyName(b.FamiliyElderlyID, holder);
        fetchCaretakerName(b.CaretakerID, holder);
        fetchRate(b, holder);
        fetchCategoryAndSub(b, holder);

        holder.btnAccept.setVisibility(View.GONE);
        holder.btnReject.setVisibility(View.GONE);
        holder.btnGenerateQR.setVisibility(View.GONE);
        holder.btnLeaveReview.setVisibility(View.GONE);

        holder.btnProfile.setOnClickListener(v -> {
            if (caretakerMode) showProfilePopup(b.FamiliyElderlyID, "FamilyElderly");
            else showProfilePopup(b.CaretakerID, "Caretakers");
        });

        holder.btnChat.setOnClickListener(v -> {
            if (b.CaretakerID == null || b.CaretakerID.isEmpty()) {
                Toast.makeText(context, "Error: Caretaker not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("Caretakers")
                    .child(b.CaretakerID)
                    .child("CaretakerName");

            ref.get().addOnSuccessListener(snapshot -> {
                String name = snapshot.exists() ? snapshot.getValue(String.class) : "Caretaker";
                Intent chatIntent = new Intent(context, ChatActivity.class);
                chatIntent.putExtra("receiverUid", b.CaretakerID);
                chatIntent.putExtra("receiverName", name);
                context.startActivity(chatIntent);
            }).addOnFailureListener(e ->
                    Toast.makeText(context, "Failed to load caretaker info.", Toast.LENGTH_SHORT).show()
            );
        });

        if (caretakerMode && "Pending".equalsIgnoreCase(b.BookingStatus)) {
            holder.btnAccept.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);
            holder.btnAccept.setOnClickListener(v -> updateStatus(b, "Accepted", holder));
            holder.btnReject.setOnClickListener(v -> updateStatus(b, "Rejected", holder));
        }

        if (!caretakerMode) {
            if ("Accepted".equalsIgnoreCase(b.BookingStatus)) {
                holder.btnGenerateQR.setVisibility(View.VISIBLE);
                holder.btnGenerateQR.setOnClickListener(v -> {
                    Intent i = new Intent(context, ShowBookingQRCodeActivity.class);
                    i.putExtra("bookingId", b.BookingID);
                    i.putExtra("familyUid", b.FamiliyElderlyID);
                    i.putExtra("caretakerUid", b.CaretakerID);
                    context.startActivity(i);
                });
            }

            if ("Completed".equalsIgnoreCase(b.BookingStatus)) {
                holder.btnLeaveReview.setVisibility(View.VISIBLE);

                boolean isReviewed = reviewStatusMap.containsKey(b.BookingID) && Boolean.TRUE.equals(reviewStatusMap.get(b.BookingID));

                if (isReviewed) {
                    holder.btnLeaveReview.setText("Edit Review");
                } else {
                    holder.btnLeaveReview.setText("Leave a Review");
                }

                holder.btnLeaveReview.setOnClickListener(v -> {
                    Intent i = new Intent(context, FamilyLeaveReviewActivity.class);
                    i.putExtra("bookingId", b.BookingID);
                    i.putExtra("caretakerUid", b.CaretakerID);
                    context.startActivity(i);
                });
            }
        }

        holder.btnReport.setOnClickListener(v -> {
            if (b.BookingID == null || b.CaretakerID == null || b.FamiliyElderlyID == null) {
                Toast.makeText(context, "Error: Missing data", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(context, ReportBookingActivity.class);
            i.putExtra("bookingId", b.BookingID);
            i.putExtra("caretakerId", b.CaretakerID);
            i.putExtra("familyId", b.FamiliyElderlyID);
            context.startActivity(i);
        });
    }

    private void showProfilePopup(String userId, String nodeName) {
        if (userId == null) {
            Toast.makeText(context, "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View popupView = LayoutInflater.from(context).inflate(R.layout.dialog_profile_popup, null);
        builder.setView(popupView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvName = popupView.findViewById(R.id.tvName);
        TextView tvType = popupView.findViewById(R.id.tvType);
        TextView tvGender = popupView.findViewById(R.id.tvGender);
        TextView tvAge = popupView.findViewById(R.id.tvAge);
        TextView tvPhone = popupView.findViewById(R.id.tvPhone);
        ImageView imgProfile = popupView.findViewById(R.id.imgProfile);
        Button btnClose = popupView.findViewById(R.id.btnCloseProfile);

        tvType.setText(nodeName.equals("Caretakers") ? "Caretaker Profile" : "Family Member Profile");

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(nodeName).child(userId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    tvName.setText("User not found");
                    return;
                }
                String name, gender, phone, imgUrl, age;

                if (nodeName.equals("Caretakers")) {
                    name = snapshot.child("CaretakerName").getValue(String.class);
                    gender = snapshot.child("CaretakerGender").getValue(String.class);
                    phone = snapshot.child("CaretakerPhoneNo").getValue(String.class);
                    imgUrl = snapshot.child("CaretakerProfileUrl").getValue(String.class);
                    Object ageObj = snapshot.child("CaretakerAge").getValue();
                    age = (ageObj != null) ? String.valueOf(ageObj) : "-";
                } else {
                    name = snapshot.child("FamilyElderlyName").getValue(String.class);
                    gender = snapshot.child("FamilyElderlyGender").getValue(String.class);
                    phone = snapshot.child("FamilyElderlyPhone").getValue(String.class);
                    imgUrl = snapshot.child("FamilyElderlyURL").getValue(String.class);
                    Object ageObj = snapshot.child("FamilyElderlyAge").getValue();
                    age = (ageObj != null) ? String.valueOf(ageObj) : "-";
                }

                tvName.setText(name != null ? name : "Unknown");
                tvGender.setText("Gender: " + (gender != null ? gender : "-"));
                tvPhone.setText("Phone: " + (phone != null ? phone : "-"));
                tvAge.setText("Age: " + age);

                if (imgUrl != null && !imgUrl.isEmpty()) {
                    Picasso.get().load(imgUrl).placeholder(R.drawable.ic_profile).error(R.drawable.ic_profile).into(imgProfile);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void fetchFamilyName(String id, ViewHolder holder) {
        if (id == null) { holder.tvFamilyName.setText("Booked By: Unknown"); return; }
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("FamilyElderly").child(id);
        ref.child("FamilyElderlyName").get().addOnSuccessListener(snap ->
                holder.tvFamilyName.setText("Booked By: " + (snap.getValue(String.class) != null ? snap.getValue(String.class) : "Unknown"))
        );
    }

    private void fetchCaretakerName(String id, ViewHolder holder) {
        if (id == null) { holder.tvCaretakerName.setText("Caretaker: Unknown"); return; }
        FirebaseDatabase.getInstance().getReference("Caretakers").child(id).child("CaretakerName").get()
                .addOnSuccessListener(snap -> holder.tvCaretakerName.setText("Caretaker: " + (snap.getValue(String.class) != null ? snap.getValue(String.class) : "Unknown")));
    }

    private void fetchRate(Booking b, ViewHolder holder) {
        if (b.AdvertismentID == null || b.CaretakerID == null) { holder.tvRate.setText("Rate: N/A"); return; }
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Advertisements").child(b.CaretakerID).child(b.AdvertismentID);
        ref.get().addOnSuccessListener(s -> {
            if (s.exists()) {
                String min = s.child("AdvertisementMinRate").getValue(String.class);
                String max = s.child("AdvertisementMaxRate").getValue(String.class);
                String rate = "N/A";
                if (min != null && max != null) rate = "RM" + min + "–RM" + max + "/Session";
                else if (min != null) rate = "RM" + min + "/Session";
                holder.tvRate.setText("Rate: " + rate);
            } else { holder.tvRate.setText("Rate: N/A"); }
        });
    }

    private void fetchCategoryAndSub(Booking b, ViewHolder holder) {
        if (b.AdvertismentID == null || b.CaretakerID == null) {
            holder.tvCategory.setText("Category: Unknown");
            holder.tvSubCategory.setText("Subcategories: Unknown");
            return;
        }

        DatabaseReference adRef = FirebaseDatabase.getInstance().getReference("Advertisements").child(b.CaretakerID).child(b.AdvertismentID);
        adRef.get().addOnSuccessListener(adSnap -> {
            if (!adSnap.exists()) {
                holder.tvCategory.setText("Category: Unknown");
                holder.tvSubCategory.setText("Subcategories: Unknown");
                return;
            }

            String categoryID = adSnap.child("CategoriesID").getValue(String.class);
            List<String> subList = new ArrayList<>();
            for (DataSnapshot s : adSnap.child("SubCategoriesID").getChildren()) {
                subList.add(s.getValue(String.class));
            }

            FirebaseDatabase.getInstance().getReference("Categories").child(categoryID).child("CategoryName").get()
                    .addOnSuccessListener(catSnap -> holder.tvCategory.setText("Category: " + (catSnap.getValue(String.class) != null ? catSnap.getValue(String.class) : "-")));

            FirebaseDatabase.getInstance().getReference("Categories").child(categoryID).child("SubCategories").get()
                    .addOnSuccessListener(subSnap -> {
                        StringBuilder sb = new StringBuilder();
                        for (String subID : subList) {
                            if (subSnap.hasChild(subID)) {
                                String name = subSnap.child(subID).child("SubCategoryName").getValue(String.class);
                                sb.append("• ").append(name).append("\n");
                            }
                        }
                        holder.tvSubCategory.setText(sb.length() == 0 ? "Subcategories: None" : "Subcategories:\n" + sb.toString().trim());
                    });
        });
    }

    private void updateStatus(Booking b, String newStatus, ViewHolder holder) {
        FirebaseDatabase.getInstance().getReference("Bookings").child(b.BookingID).child("BookingStatus").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    b.BookingStatus = newStatus;
                    holder.tvStatus.setText("Status: " + newStatus);
                    notifyItemChanged(holder.getAdapterPosition());
                    Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFamilyName, tvCaretakerName, tvDateTime, tvNotes, tvStatus, tvRate, tvDuration;
        TextView tvCategory, tvSubCategory;
        MaterialButton btnAccept, btnReject, btnGenerateQR, btnLeaveReview, btnReport, btnProfile, btnChat;
        MaterialCardView cvStatusContainer;

        ViewHolder(View itemView) {
            super(itemView);
            tvFamilyName = itemView.findViewById(R.id.tvFamilyName);
            tvCaretakerName = itemView.findViewById(R.id.tvCaretakerName);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvDuration = itemView.findViewById(R.id.tvDuration); // Bound new ID
            tvRate = itemView.findViewById(R.id.tvRate);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvSubCategory = itemView.findViewById(R.id.tvSubCategory);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnGenerateQR = itemView.findViewById(R.id.btnGenerateQR);
            btnLeaveReview = itemView.findViewById(R.id.btnLeaveReview);
            btnReport = itemView.findViewById(R.id.btnReport);
            btnProfile = itemView.findViewById(R.id.btnProfile);
            btnChat = itemView.findViewById(R.id.btnChat);
            cvStatusContainer = itemView.findViewById(R.id.cvStatusContainer);
        }
    }
}