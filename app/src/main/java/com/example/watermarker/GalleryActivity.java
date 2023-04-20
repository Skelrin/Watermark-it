package com.example.watermarker;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;

public class GalleryActivity extends AppCompatActivity {

    private LinearLayout linearLayoutImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery_screen);

        ImageView logoutButton = findViewById(R.id.logoutLogo);
        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
        });

        ImageView watermarkButton = findViewById(R.id.watermarkLogo);
        watermarkButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), WatermarkActivity.class);
            startActivity(intent);
        });

        ImageView recoverWatermarkButton = findViewById(R.id.handLogo);
        recoverWatermarkButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), RecoverWatermarkActivity.class);
            startActivity(intent);
        });

        linearLayoutImages = findViewById(R.id.linearLayoutImages);
        try {
            // get every files from the app directory
            File folder = new File(Environment.getExternalStorageDirectory().getPath(), "watermark_it/watermarked_Images/");
            File[] files = folder.listFiles((dir, name) -> name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"));

            // create an imageView to be added to the layout
            for (File file : files) {
                ImageView imageView = new ImageView(getApplicationContext());
                imageView.setImageURI(Uri.fromFile(file));
                imageView.setAdjustViewBounds(true);
                int maxHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 600, getResources().getDisplayMetrics());
                int maxWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 390, getResources().getDisplayMetrics());
                imageView.setMaxHeight(maxHeight);
                imageView.setMaxWidth(maxWidth);
                imageView.setPadding(0,0,0,20);
                imageView.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setDataAndType(FileProvider.getUriForFile(getApplicationContext(), "com.example.watermarker.fileprovider", file), "image/*");
                    startActivity(intent);
                });
                linearLayoutImages.addView(imageView);
            }
        }
        catch (Exception e) {
            Log.d("DEBUG", e.getMessage());
        }
    }
}
