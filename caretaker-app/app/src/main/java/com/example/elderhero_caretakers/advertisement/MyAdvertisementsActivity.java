package com.example.elderhero_caretakers.advertisement;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderhero_caretakers.CaretakerDrawer;
import com.example.elderhero_caretakers.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class MyAdvertisementsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private MyAdAdapter adapter;
    private List<Advertisement> list = new ArrayList<>();

    private DatabaseReference adsRef;
    private ProgressDialog dialog;
    private String uid;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_my_advertisements);

        DrawerLayout drawer = findViewById(R.id.drawerLayout);
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> drawer.openDrawer(GravityCompat.START));
        new CaretakerDrawer(this, findViewById(R.id.leftDrawer), false);

        recyclerView = findViewById(R.id.recyclerMyAds);
        tvEmpty = findViewById(R.id.tvEmptyMessage);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        uid = FirebaseAuth.getInstance().getUid();
        adsRef = FirebaseDatabase.getInstance().getReference("Advertisements").child(uid);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Loading...");
        dialog.show();

        loadData();
    }

    private void loadData() {
        adsRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {

                list.clear();
                for (DataSnapshot s : snap.getChildren()) {
                    Advertisement ad = s.getValue(Advertisement.class);
                    if (ad != null) list.add(ad);
                }

                if (list.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    if (adapter == null) {
                        adapter = new MyAdAdapter(
                                MyAdvertisementsActivity.this,
                                list,
                                MyAdvertisementsActivity.this::editAd,
                                MyAdvertisementsActivity.this::deleteAd
                        );
                        recyclerView.setAdapter(adapter);
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                }
                dialog.dismiss();
            }

            @Override public void onCancelled(@NonNull DatabaseError e) {
                dialog.dismiss();
                Toast.makeText(MyAdvertisementsActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void editAd(Advertisement ad) {
        Intent i = new Intent(this, EditAdvertisementActivity.class);
        i.putExtra("adId", ad.getAdvertisementID());
        startActivity(i);
    }

    private void deleteAd(Advertisement ad) {

        new AlertDialog.Builder(this)
                .setTitle("Delete?")
                .setMessage("Are you sure?")
                .setPositiveButton("Yes", (d, w) -> {
                    ProgressDialog pd = new ProgressDialog(this);
                    pd.setMessage("Deleting...");
                    pd.show();

                    adsRef.child(ad.getAdvertisementID()).removeValue()
                            .addOnSuccessListener(v -> {
                                pd.dismiss();
                                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                pd.dismiss();
                                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
