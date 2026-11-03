package com.example.elderhero_caretakers;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.elderhero_caretakers.booking.CaretakerBookingListActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CompoundBarcodeView;

import java.io.InputStream;

public class ScanQRCodeActivity extends AppCompatActivity {

    private CompoundBarcodeView barcodeView;
    private Button btnGallery;
    private static final int CAMERA_REQUEST_CODE = 200;
    private boolean scanned = false;

    // Gallery Launcher
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    scanFromImage(imageUri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qrcode);

        barcodeView = findViewById(R.id.barcodeScannerView);
        btnGallery = findViewById(R.id.btnGallery);

        // Check Permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
        } else {
            startScanning();
        }

        // Set Gallery Button Listener
        btnGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });
    }

    private void startScanning() {
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() == null || scanned) return;
                processQrResult(result.getText());
            }
        });
    }

    // Logic to decode static image from Gallery
    private void scanFromImage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap == null) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                return;
            }

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            MultiFormatReader reader = new MultiFormatReader();

            Result result = reader.decode(binaryBitmap);
            processQrResult(result.getText());

        } catch (Exception e) {
            Toast.makeText(this, "No QR Code found in image", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    // Centralized processing for both Camera and Gallery results
    private void processQrResult(String qrCodeText) {
        if (scanned) return;
        scanned = true;

        // Pause camera if running
        barcodeView.pause();

        String[] parts = qrCodeText.split(":");

        if (parts.length != 4 || !parts[0].equals("confirm_job")) {
            Toast.makeText(ScanQRCodeActivity.this, "Invalid QR code format", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String bookingId = parts[1];


        markJobAsCompleted(bookingId);
    }

    private void markJobAsCompleted(String bookingId) {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();

        root.child("Bookings")
                .child(bookingId)
                .child("BookingStatus")
                .setValue("Completed")
                .addOnSuccessListener(a -> {
                    Toast.makeText(this,
                            "Booking marked as Completed",
                            Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(ScanQRCodeActivity.this, CaretakerBookingListActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}