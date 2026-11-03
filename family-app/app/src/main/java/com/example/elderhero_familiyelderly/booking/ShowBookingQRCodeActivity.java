package com.example.elderhero_familiyelderly.booking;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.elderhero_familiyelderly.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.io.OutputStream;

public class ShowBookingQRCodeActivity extends AppCompatActivity {

    private ImageView imgQRCode;
    private TextView tvBookingInfo;
    private Button btnDownloadQR;
    private Bitmap currentQrBitmap; // Class-level variable to hold the bitmap for saving

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_booking_qrcode);

        // Initialize Views
        imgQRCode = findViewById(R.id.imgQRCode);
        tvBookingInfo = findViewById(R.id.tvBookingInfo);
        btnDownloadQR = findViewById(R.id.btnDownloadQR);

        // Retrieve Intent Data
        String bookingId = getIntent().getStringExtra("bookingId");
        String familyElderlyID = getIntent().getStringExtra("familyUid");
        String caretakerID = getIntent().getStringExtra("caretakerUid");

        if (bookingId == null || familyElderlyID == null || caretakerID == null) {
            Toast.makeText(this, "Missing data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvBookingInfo.setText("Booking ID: " + bookingId);

        // Generate QR Code Data string
        String qrData = "confirm_job:" + bookingId + ":" + familyElderlyID + ":" + caretakerID;

        generateQRCode(qrData);

        // Set Download Button Listener
        btnDownloadQR.setOnClickListener(v -> {
            if (currentQrBitmap != null) {
                saveQRCodeToGallery(currentQrBitmap, "BookingQR_" + bookingId);
            } else {
                Toast.makeText(this, "QR Code not ready yet", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateQRCode(String text) {
        QRCodeWriter writer = new QRCodeWriter();

        try {
            int size = 512;
            com.google.zxing.common.BitMatrix bitMatrix =
                    writer.encode(text, BarcodeFormat.QR_CODE, size, size);

            // Create Bitmap
            currentQrBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    currentQrBitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            imgQRCode.setImageBitmap(currentQrBitmap);

        } catch (WriterException e) {
            Toast.makeText(this, "QR generation failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void saveQRCodeToGallery(@NonNull Bitmap bitmap, String fileName) {
        OutputStream fos;
        try {
            ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName + ".jpg");
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

            // Organize file in the Pictures/BookingQR directory
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BookingQR");
            }

            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

            if (imageUri != null) {
                fos = resolver.openOutputStream(imageUri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                if (fos != null) {
                    fos.close();
                }
                Toast.makeText(this, "QR Code saved to Gallery", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to create file directory", Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}