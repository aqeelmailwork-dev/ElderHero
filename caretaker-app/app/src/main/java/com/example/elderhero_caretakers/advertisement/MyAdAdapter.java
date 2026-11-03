package com.example.elderhero_caretakers.advertisement;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderhero_caretakers.R;
import com.squareup.picasso.Picasso;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class MyAdAdapter extends RecyclerView.Adapter<MyAdAdapter.VH> {

    public interface OnEditClickListener { void onEdit(Advertisement ad); }
    public interface OnDeleteClickListener { void onDelete(Advertisement ad); }

    private final Context context;
    private final List<Advertisement> list;
    private final OnEditClickListener editListener;
    private final OnDeleteClickListener deleteListener;

    public MyAdAdapter(Context context,
                       List<Advertisement> list,
                       OnEditClickListener editListener,
                       OnDeleteClickListener deleteListener) {
        this.context = context;
        this.list = list;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(context).inflate(R.layout.item_my_ad, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Advertisement ad = list.get(position);

        // Rate, availability, location
        h.tvRate.setText(ad.getDisplayRate());
        h.tvAvailability.setText("Availability: " + ad.getAdvertisementAvailability());
        h.tvLocation.setText("Location: " + ad.getAdvertisementLocation());

        // DATE + TIME
        h.tvDate.setText("Date: " + (ad.getAdvertisementDate() == null ? "-" : ad.getAdvertisementDate()));
        h.tvTime.setText("Time: " + (ad.getAdvertisementTime() == null ? "-" : ad.getAdvertisementTime()));

        // Image
        if (ad.getAdvertisementURL() != null && !ad.getAdvertisementURL().isEmpty()) {
            Picasso.get()
                    .load(ad.getAdvertisementURL())
                    .placeholder(R.drawable.ic_person_placeholder)
                    .into(h.imgAd);
        } else {
            h.imgAd.setImageResource(R.drawable.ic_person_placeholder);
        }

        // Fetch category name
        if (ad.getCategoriesID() != null) {
            FirebaseDatabase.getInstance()
                    .getReference("Categories")
                    .child(ad.getCategoriesID())
                    .child("CategoryName")
                    .get()
                    .addOnSuccessListener(s -> h.tvCategory.setText(s.getValue(String.class)))
                    .addOnFailureListener(e -> h.tvCategory.setText("-"));
        }

        // Buttons
        h.btnEdit.setOnClickListener(v -> editListener.onEdit(ad));
        h.btnDelete.setOnClickListener(v -> deleteListener.onDelete(ad));
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgAd;
        TextView tvCategory, tvRate, tvAvailability, tvLocation, tvDate, tvTime;
        Button btnEdit, btnDelete;

        VH(@NonNull View v) {
            super(v);
            imgAd = v.findViewById(R.id.imgAd);
            tvCategory = v.findViewById(R.id.tvCategory);
            tvRate = v.findViewById(R.id.tvRate);
            tvAvailability = v.findViewById(R.id.tvAvailability);
            tvLocation = v.findViewById(R.id.tvLocation);

            // NEW FIELDS
            tvDate = v.findViewById(R.id.tvDate);
            tvTime = v.findViewById(R.id.tvTime);

            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
