package com.example.watermarker;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Date;
import java.util.Locale;

public class RecoverWatermarkActivity extends AppCompatActivity {

    private int radioButtonSelected = 0; // 1 = image & 2 = text
    private String watermarkedImagePath;
    private EditText insertionKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recover_watermark_screen);

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

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

        ImageView galleryButton = findViewById(R.id.galleryLogo);
        galleryButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), GalleryActivity.class);
            startActivity(intent);
        });

        // Radio Group Handler
        RadioGroup radioGroup = findViewById(R.id.radiogroup2);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // check which radio button was selected
            switch (checkedId) {
                case R.id.radioImage2:
                    radioButtonSelected = 1;
                    break;
                case R.id.radioText2:
                    radioButtonSelected = 2;
                    break;
                default:
                    break;
            }
        });

        ActivityResultLauncher<Intent> galleryToRetrieveImage = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri selectedImageUri = data.getData();
                            watermarkedImagePath = Utilities.getPath(getApplicationContext(), selectedImageUri);
                        }
                    }
                });

        Button watermarkedImageButton = findViewById(R.id.selectImageWatermarked);
        insertionKey = findViewById(R.id.watermarkPassword);

        watermarkedImageButton.setOnClickListener(v -> {
            Intent i = new Intent();
            i.setType("image/*");
            i.setAction(Intent.ACTION_GET_CONTENT);

            galleryToRetrieveImage.launch(i);
        });

        Button exportImageButton = findViewById(R.id.exportImage);

        exportImageButton.setOnClickListener(v -> {
            if (radioButtonSelected != 0 && watermarkedImagePath != null && !watermarkedImagePath.isEmpty()) {
                ExtractWatermarkTask extractWatermarkTask = new ExtractWatermarkTask(RecoverWatermarkActivity.this);
                extractWatermarkTask.execute();
            }
            else {
                Toast.makeText(getApplicationContext(), "Please choose a watermark type or a watermarked image.", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     *  Handle the display of the activity to show the loading circle or not
     */
    private void hideLayout(){
        LinearLayout linearLayout = findViewById(R.id.watermarkLayout);
        Button selectImageWatermarked = findViewById(R.id.selectImageWatermarked);
        Button exportImage = findViewById(R.id.exportImage);
        ProgressBar progressBar = findViewById(R.id.progressBar);

        if(progressBar.getVisibility() == View.GONE){
            linearLayout.setVisibility(View.GONE);
            selectImageWatermarked.setVisibility(View.GONE);
            exportImage.setVisibility(View.GONE);
            insertionKey.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }
        else {
            linearLayout.setVisibility(View.VISIBLE);
            selectImageWatermarked.setVisibility(View.VISIBLE);
            exportImage.setVisibility(View.VISIBLE);
            insertionKey.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

    /**
     * Task to extract and prompt the watermark.
     */
    private class ExtractWatermarkTask extends AsyncTask<Void, Void, Void> {

        private Activity mActivity;

        public ExtractWatermarkTask(Activity activity) {
            mActivity = activity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            hideLayout();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            File directory = new File(Environment.getExternalStorageDirectory().getPath(), "watermark_it/watermark/");
            if (!directory.exists()) {
                try {
                    directory.mkdirs();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Python py = Python.getInstance();
            PyObject module = py.getModule("watermark");

            // if the watermark extracted is an image
            if(radioButtonSelected == 1) {
                Bitmap bitmap = null;
                try {
                    byte[] bytes = module.callAttr("recoverWatermark",
                                    watermarkedImagePath,
                                    insertionKey.getText().toString())
                            .toJava(byte[].class);
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                } catch (PyException e) {
                    Log.d("DEBUG python", e.getMessage());
                }

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String fileName = "watermark_" + timeStamp + ".png";

                File file = new File(directory, fileName);
                try {
                    FileOutputStream outputStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputStream.flush();
                    outputStream.close();
                    mActivity.runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Watermark successfully extracted", Toast.LENGTH_SHORT).show());

                    // open the galley to prompt the watermark extracted
                    Uri photoUri = FileProvider.getUriForFile(getApplicationContext(), "com.example.watermarker.fileprovider", file);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(photoUri, "image/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.d("DEBUG", e.getMessage());
                }
            }
            // if the watermark extracted is a text
            else {
                String watermarkText = null;
                try {
                    watermarkText = module.callAttr("recoverText",
                                    watermarkedImagePath,
                                    insertionKey.getText().toString())
                            .toJava(String.class);
                } catch (PyException e) {
                    Log.d("DEBUG python", e.getMessage());
                }
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String fileName = "watermark_" + timeStamp + ".txt";
                File file = new File(directory, fileName);
                try {
                    FileWriter writer = new FileWriter(file);
                    writer.append(watermarkText);
                    writer.flush();
                    writer.close();
                    mActivity.runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Watermark successfully extracted", Toast.LENGTH_SHORT).show());

                    Uri uri = FileProvider.getUriForFile(getApplicationContext(), "com.example.watermarker.fileprovider", file);
                    Log.d("DEBUG", uri.getPath());
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "text/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.d("DEBUG", e.getMessage());
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            hideLayout();
        }
    }
}
