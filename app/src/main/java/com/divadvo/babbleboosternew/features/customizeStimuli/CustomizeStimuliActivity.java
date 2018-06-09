package com.divadvo.babbleboosternew.features.customizeStimuli;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.divadvo.babbleboosternew.R;
import com.divadvo.babbleboosternew.features.base.BaseActivity;
import com.divadvo.babbleboosternew.injection.component.ActivityComponent;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;
import butterknife.BindView;
import butterknife.OnClick;

public class CustomizeStimuliActivity extends BaseActivity implements CustomizeStimuliMvpView {
    public static final String EXTRA_PHONEME = "EXTRA_PHONEME";
    private static int FILE_IMAGE = 98;
    private static int FILE_VIDEO = 99;
    static final int REQUEST_IMAGE_CAPTURE = 5;


    @Inject
    CustomizeStimuliPresenter customizeStimuliPresenter;

    @BindView(R.id.textViewPhoneme_to_customize)
    TextView phonemeTextView;

    @BindView(R.id.layout_images)
    LinearLayout linearLayoutImages;

//    @BindView(R.id.layout_videos)
//    LinearLayout linearLayoutVideos;

    private String phoneme;
    private List<File> imageFilesToDisplay;
    private List<File> videosToDisplay;
    private String mCurrentPhotoPath;

    public static Intent getStartIntent(Context context, String phoneme) {
        Intent intent = new Intent(context, CustomizeStimuliActivity.class);
        intent.putExtra(EXTRA_PHONEME, phoneme);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        phoneme = getIntent().getStringExtra(EXTRA_PHONEME);
        if (phoneme == null) {
            throw new IllegalArgumentException("Customize Stimuli Activity requires a phoneme");
        }

        phonemeTextView.setText("Phoneme " + phoneme);

        reloadImagesAndVideos();
    }

    private void reloadImagesAndVideos() {
        imageFilesToDisplay = customizeStimuliPresenter.getAllImagesForPhoneme(phoneme);
        videosToDisplay = customizeStimuliPresenter.getAllVideosForPhoneme(phoneme);

        if(imageFilesToDisplay.isEmpty() && videosToDisplay.isEmpty()) {
            Toast.makeText(this, "No stimuli for " + phoneme, Toast.LENGTH_SHORT).show();
            finish();
        }

        updateLayout();
    }

    private void updateLayout() {
        linearLayoutImages.removeAllViews(); // Clear the layout

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);


        int i = 0;


        for(File videoFile : videosToDisplay) {
            VideoView phonemeVideo = new VideoView(this);
            String path = videoFile.getAbsolutePath();


            phonemeVideo.setVideoPath(path);
            phonemeVideo.seekTo(1);

            phonemeVideo.setOnTouchListener(new View.OnTouchListener() {

                private static final int MIN_CLICK_DURATION = 500;
                private long startClickTime;
                private boolean longClickActive = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_UP:
                            longClickActive = false;
                            break;
                        case MotionEvent.ACTION_DOWN:
                            if (longClickActive == false) {
                                longClickActive = true;
                                startClickTime = Calendar.getInstance().getTimeInMillis();
                            }
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (longClickActive == true) {
                                long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                                if (clickDuration >= MIN_CLICK_DURATION) {
                                    phonemeVideo.start();
                                    String pathVideo = path;
                                    askRemoveFile(pathVideo);
                                    longClickActive = false;
                                }
                            }
                            break;
                    }
                    return true;
                }
            });

//            phonemeVideo.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            int width = 700;

            phonemeVideo.setLayoutParams(new android.view.ViewGroup.LayoutParams(width, width));

            linearLayoutImages.addView(phonemeVideo);

        }

        for(File imageFile : imageFilesToDisplay) {
            ImageView phonemeImage = new ImageView(this);
            String imagePath = imageFile.getAbsolutePath();
//            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
//            phonemeImage.setImageBitmap(bitmap);


//            phonemeImage.setMinimumWidth(400);

//            int width = linearLayoutImages.getWidth();
//            linearLayoutImages.measure(0,0);
            int width = 700;

            phonemeImage.setLayoutParams(new android.view.ViewGroup.LayoutParams(width, width));


            phonemeImage.setOnLongClickListener(v -> {
                String pathVideo = imagePath;
                askRemoveFile(pathVideo);
                return false;
            });
//
//            phonemeImage.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            Random rnd = new Random();
            int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
            phonemeImage.setBackgroundColor(color);


            linearLayoutImages.addView(phonemeImage); i++;


            Glide.with(this)
                    .load(imageFile)
                    .into(phonemeImage);

//            phonemeImage.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        }
    }

    private void askRemoveFile(String path) {
        new MaterialDialog.Builder(this)
                .title("Remove this file?")
                .positiveText("Yes, delete")
                .negativeText("No, don't delete")
                .onPositive((dialog, which) -> {
                    removeFile(path);
                })
                .show();

    }

    private void removeFile(String path) {
        customizeStimuliPresenter.addToIgnoreFile(phoneme, path);
        customizeStimuliPresenter.deleteFile(path);
        reloadImagesAndVideos();
    }

    void addImageFromCamera() {
        dispatchTakePictureIntent();
    }

    @OnClick(R.id.buttonAddImage)
    void addNewImageClick() {
//        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        startActivityForResult(Intent.createChooser(intent, "Select Image"), FILE_IMAGE);
//        intent.setType("image/* video/*");
        showDialog();
    }

    @OnClick(R.id.buttonAddVideo)
    void addNewVideoClick() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/* video/*");
        intent.setType("video/mp4");
        startActivityForResult(Intent.createChooser(intent, "Select Video"), FILE_IMAGE);
    }



    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        shouldCheckCredentials = false;
        super.onActivityResult(requestCode, resultCode, data);
        shouldCheckCredentials = false;

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            File photoFile = new File(mCurrentPhotoPath);
            File stimuliFile = customizeStimuliPresenter.getStimuliFileFromExternal(phoneme, photoFile);
            customizeStimuliPresenter.copyFile(photoFile, stimuliFile);
            reloadImagesAndVideos();
        } else if (resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData();
            String selectedPath = getPath(selectedImageUri);
            File file = new File(selectedPath);

            if (selectedPath != null) {
                if (requestCode == FILE_VIDEO) {
                    addNewVideo(file);
                } else if (requestCode == FILE_IMAGE) {
                    addNewImage(file);
                }
            }
        }
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();

            } catch (IOException ex) {
                Toast.makeText(this, "Failed to take photo", Toast.LENGTH_LONG).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.divadvo.babbleboosternew.provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        mCurrentPhotoPath = image.getAbsolutePath();

        // Save a file: path for use with ACTION_VIEW intents
        return image;
    }

    private void addNewImage(File file) {
        File stimuliFile = customizeStimuliPresenter.getStimuliFileFromExternal(phoneme, file);
        customizeStimuliPresenter.copyFile(file, stimuliFile);
        reloadImagesAndVideos();
    }

    private void addNewVideo(File file) {
        File stimuliFile = customizeStimuliPresenter.getStimuliFileFromExternal(phoneme, file);
        customizeStimuliPresenter.copyFile(file, stimuliFile);
        reloadImagesAndVideos();

    }

    @Override
    public int getLayout() {
        return R.layout.activity_customize_stimuli;
    }

    @Override
    protected void inject(ActivityComponent activityComponent) {
        activityComponent.inject(this);
    }

    @Override
    protected void attachView() {
        customizeStimuliPresenter.attachView(this);
    }

    @Override
    protected void detachPresenter() {
        customizeStimuliPresenter.detachView();
    }



    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Video.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            // HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
            // THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else
            return null;
    }

    public void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.pick_photo_method)
                .setItems(R.array.photo_method, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == 0) {
                            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                            intent.setType("image/* video/*");
                            intent.setType("video/mp4");
                            startActivityForResult(Intent.createChooser(intent, "Select Video"), FILE_IMAGE);
                        } else {
                            dispatchTakePictureIntent();
                        }
                    }
                });
        builder.create();
        builder.show();
    }

}
