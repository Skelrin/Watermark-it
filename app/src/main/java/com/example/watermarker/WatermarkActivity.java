package com.example.watermarker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

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
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Locale;

public class WatermarkActivity extends AppCompatActivity {

    private String watermarkImagePath;
    private String imagePath;
    private int radioButtonSelected = 0; // 1 = image & 2 = text
    private EditText watermarkTextInput;
    private EditText insertionKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.watermark_screen);

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        // Buttons to switch to other activities

        ImageView logoutButton = findViewById(R.id.logoutLogo);
        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
        });

        ImageView recoverWatermarkButton = findViewById(R.id.handLogo);
        recoverWatermarkButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), RecoverWatermarkActivity.class);
            startActivity(intent);
        });

        ImageView galleryButton = findViewById(R.id.galleryLogo);
        galleryButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), GalleryActivity.class);
            startActivity(intent);
        });

        // Radio Group Handler
        RadioGroup radioGroup = findViewById(R.id.radiogroup);
        Button watermarkImageButton = findViewById(R.id.watermarkImage);
        watermarkTextInput = findViewById(R.id.watermarkText);
        insertionKey = findViewById(R.id.watermarkPassword);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.radioImage:
                    watermarkImageButton.setVisibility(View.VISIBLE);
                    watermarkTextInput.setVisibility(View.GONE);
                    radioButtonSelected = 1;
                    break;
                case R.id.radioText:
                    watermarkImageButton.setVisibility(View.GONE);
                    watermarkTextInput.setVisibility(View.VISIBLE);
                    radioButtonSelected = 2;
                    break;
                default:
                    watermarkImageButton.setVisibility(View.GONE);
                    watermarkTextInput.setVisibility(View.GONE);
                    break;
            }
        });

        // open gallery to retrieve the image to watermark

        ActivityResultLauncher<Intent> galleryToRetrieveImage = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri selectedImageUri = data.getData();
                            imagePath = Utilities.getPath(getApplicationContext(), selectedImageUri);
                            GetWatermarkSizeTask getWatermarkSizeTask = new GetWatermarkSizeTask(WatermarkActivity.this);
                            getWatermarkSizeTask.execute();
                        }
                    }
                });

        ActivityResultLauncher<Intent> galleryToRetrieveWatermark = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri selectedImageUri = data.getData();
                            watermarkImagePath = Utilities.getPath(getApplicationContext(), selectedImageUri);
                        }
                    }
                });

        Button imageToWatermark = findViewById(R.id.imageToWatermark);

        imageToWatermark.setOnClickListener(v -> {
            Intent i = new Intent();
            i.setType("image/*");
            i.setAction(Intent.ACTION_GET_CONTENT);

            galleryToRetrieveImage.launch(i);
        });

        watermarkImageButton.setOnClickListener(v -> {
            Intent i = new Intent();
            i.setType("image/*");
            i.setAction(Intent.ACTION_GET_CONTENT);

            galleryToRetrieveWatermark.launch(i);
        });

        // Generate the watermarked image

        Button watermarker = findViewById(R.id.insertWatermark);
        watermarker.setOnClickListener(v -> {
            if (radioButtonSelected != 0){

                InsertWatermarkTask watermarkTask = new InsertWatermarkTask(WatermarkActivity.this);
                watermarkTask.execute();
            }
            else {
                Toast.makeText(WatermarkActivity.this, "Please choose a watermark type", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void hideLayout() {
        Button watermarkImage = findViewById(R.id.watermarkImage);
        EditText watermarkText = findViewById(R.id.watermarkText);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        if(progressBar.getVisibility() == View.GONE){
            setViewVisibility(View.GONE);
            if(radioButtonSelected == 0) {
                watermarkImage.setVisibility(View.GONE);
                watermarkText.setVisibility(View.GONE);
            } else if (radioButtonSelected == 1) {
                watermarkImage.setVisibility(View.GONE);
            }
            else {
                watermarkImage.setVisibility(View.GONE);
            }
            progressBar.setVisibility(View.VISIBLE);
        }
        else {
            setViewVisibility(View.VISIBLE);
            if(radioButtonSelected == 0) {
                watermarkImage.setVisibility(View.GONE);
                watermarkText.setVisibility(View.GONE);
            } else if (radioButtonSelected == 1) {
                watermarkImage.setVisibility(View.VISIBLE);
            }
            else {
                watermarkText.setVisibility(View.VISIBLE);
            }
            progressBar.setVisibility(View.GONE);
        }
    }

    private void setViewVisibility(int visibility) {
        LinearLayout watermarkLayout = findViewById(R.id.watermarkLayout);
        TextView imageToWatermarkText = findViewById(R.id.imageToWatermarkText);
        Button imageToWatermark = findViewById(R.id.imageToWatermark);
        EditText watermarkPassword = findViewById(R.id.watermarkPassword);
        Button insertWatermark = findViewById(R.id.insertWatermark);


        watermarkLayout.setVisibility(visibility);
        imageToWatermark.setVisibility(visibility);
        imageToWatermarkText.setVisibility(visibility);
        watermarkPassword.setVisibility(visibility);
        insertWatermark.setVisibility(visibility);
    }

    private class InsertWatermarkTask extends AsyncTask<Void, Void, Void> {

        private Activity mActivity;

        public InsertWatermarkTask(Activity activity) {
            mActivity = activity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            hideLayout();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Bitmap bitmap = null;
            try {
                Python py = Python.getInstance();
                PyObject module = py.getModule("watermark");
                if(radioButtonSelected == 1) {
                    byte[] bytes = module.callAttr("embeddedImage",
                                    imagePath,
                                    watermarkImagePath,
                                    insertionKey.getText().toString())
                            .toJava(byte[].class);
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    Log.d("DEBUG bitmap image", bitmap.toString());
                }
                else {
                    byte[] bytes = module.callAttr("embeddedText",
                                    imagePath,
                                    watermarkTextInput.getText().toString(),
                                    insertionKey.getText().toString())
                            .toJava(byte[].class);
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    Log.d("DEBUG bitmap text", bitmap.toString());
                }
            } catch (PyException e) {
                Log.d("DEBUG python", e.getMessage());
            }

            File directory = new File(Environment.getExternalStorageDirectory().getPath(), "watermark_it/watermarked_Images/");
            if (!directory.exists()) {
                try {
                    directory.mkdirs();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "watermarkedImage_" + timeStamp + ".png";
            File file = new File(directory, fileName);
            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
                outputStream.close();

                mActivity.runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Image saved successfully", Toast.LENGTH_SHORT).show());

                Uri photoUri = FileProvider.getUriForFile(getApplicationContext(), "com.example.watermarker.fileprovider", file);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(photoUri, "image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                mActivity.runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Error saving image : " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            hideLayout();
        }
    }

    private class GetWatermarkSizeTask extends AsyncTask<Void, Void, Void> {

        private Activity mActivity;

        public GetWatermarkSizeTask(Activity activity) {
            mActivity = activity;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            hideLayout();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Python py = Python.getInstance();
                PyObject module = py.getModule("watermark");
                String markSize = module.callAttr("getMarkSize",
                                imagePath)
                        .toJava(String.class);
                mActivity.runOnUiThread(() -> Toast.makeText(getApplicationContext(), markSize, Toast.LENGTH_LONG).show());
            } catch (PyException e) {
                Log.d("DEBUG python", e.getMessage());
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
