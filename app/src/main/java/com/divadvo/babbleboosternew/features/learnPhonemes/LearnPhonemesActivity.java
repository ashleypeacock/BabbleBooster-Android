package com.divadvo.babbleboosternew.features.learnPhonemes;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.divadvo.babbleboosternew.R;
import com.divadvo.babbleboosternew.data.firebase.FirebaseSyncHelper;
import com.divadvo.babbleboosternew.data.local.DbManager;
import com.divadvo.babbleboosternew.data.local.Session;
import com.divadvo.babbleboosternew.data.local.StorageHelper;
import com.divadvo.babbleboosternew.features.base.BaseActivity;
import com.divadvo.babbleboosternew.features.choosePhonemes.ChoosePhonemesMvpView;
import com.divadvo.babbleboosternew.features.choosePhonemes.ChoosePhonemesPresenter;
import com.divadvo.babbleboosternew.features.recordVideo.RecordVideoActivity;
import com.divadvo.babbleboosternew.injection.component.ActivityComponent;
import com.snatik.storage.Storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;


/**
 * Learn the phenomes, this is the play section.
 */
public class LearnPhonemesActivity extends BaseActivity implements LearnPhonemesMvpView {

    public static final String EXTRA_PHONEME = "EXTRA_PHONEME";
    public static final String GO_TO_FINAL_VIDEO = "GO_TO_FINAL_VIDEO";

    @Inject
    LearnPhonemesPresenter learnPhonemesPresenter;

    @Inject
    DbManager dbManager;

    @Inject
    FirebaseSyncHelper firebaseSyncHelper;

    @BindView(R.id.image_phoneme)
    ImageView phonemeImage;

    @BindView(R.id.video_phoneme)
    VideoView phonemeVideo;

    @BindView(R.id.button_skip)
    Button btnSkip;

    @BindView(R.id.button_watch_video)
    Button btnWatchVideo;

    private MediaPlayer phonemeAudio = new MediaPlayer();

    private String phoneme;

    private List<String> imageFilesToDisplay;
    private List<String> videosToDisplay;
    private String audioPath;
    private int currentImageIndex = 0;
    private int currentVideoIndex = 0;

    Storage storage;

    String phonemeDirectoryPath;
    private String TAG = "LearningPhonemesActivity";


    public static Intent getStartIntent(Context context, String phoneme, boolean goToFinalVideo) {
        Intent intent = new Intent(context, LearnPhonemesActivity.class);
        intent.putExtra(EXTRA_PHONEME, phoneme);
        intent.putExtra(GO_TO_FINAL_VIDEO, goToFinalVideo);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        phoneme = getIntent().getStringExtra(EXTRA_PHONEME);
        if (phoneme == null) {
            throw new IllegalArgumentException("Learn Phonemes Activity requires a phoneme");
        }

        storage = new Storage(getApplicationContext());
        String path = storage.getInternalFilesDirectory();

        String subfolder = "phonemes" + File.separator + phoneme;
        phonemeDirectoryPath = path + File.separator + subfolder;

        initListeners();
        getImagesAndVideo();
    }



    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Goes to last video
        // After TRY_AGAIN button in the RecordVideoActivity was pressed
        if(getIntent().getBooleanExtra(GO_TO_FINAL_VIDEO, false)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(200);
                        playLastVideo();
                    }catch(Exception e) {

                    }
                }
            });
        } else {
            showCurrentImage(true);
        }
    }

    private void initListeners() {
        phonemeImage.setOnClickListener(v -> goToNextImage());

        phonemeVideo.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                goToNextVideo();
            }
            return true;
        });

//        phonemeVideo.setOnPreparedListener(mp -> mp.setLooping(true));

        btnSkip.setOnClickListener(v -> {
            phonemeVideo.stopPlayback();
            phonemeAudio.stop();
            startActivity(RecordVideoActivity.getStartIntent(this, phoneme, false));
            finish();
        });

        btnWatchVideo.setOnClickListener(v -> playLastVideo());
    }

    private void playLastVideo() {
        currentImageIndex = 0;
        phonemeVideo.setVisibility(View.VISIBLE);
        phonemeImage.setVisibility(View.INVISIBLE);
        currentVideoIndex = videosToDisplay.size() - 1;
        setCurrentVideo();
        playCurrentVideo();
        btnSkip.setVisibility(View.VISIBLE);
        btnWatchVideo.setVisibility(View.INVISIBLE);
    }

    private void showCurrentImage(boolean playSound) {
        try {
            String imagePath = imageFilesToDisplay.get(currentImageIndex);
            Glide.with(this).load(new File(imagePath)).into(phonemeImage);
        }catch(Exception e) {
            Toast.makeText(this, "Cannot find images on device. Please redownload", Toast.LENGTH_LONG).show();
        }
//        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
//        phonemeImage.setImageBitmap(bitmap);
        if(playSound) {
            phonemeAudio.seekTo(0);
            phonemeAudio.start();
        }
    }

    /**
     * Switches through all images until
     * the last image.
     * Then it goes to the videos
     */
    private void goToNextImage() {
        currentImageIndex++;
        if (currentImageIndex == imageFilesToDisplay.size()) {
            setCurrentVideo();
            currentImageIndex = 0;
            //showCurrentImage(false);
            // Show videos
            phonemeVideo.setVisibility(View.VISIBLE);
            phonemeImage.setVisibility(View.INVISIBLE);
            playCurrentVideo();
            showCurrentImage(false);
        } else {
            showCurrentImage(true);
        }

    }

    private void setCurrentVideo() {
        String path = videosToDisplay.get(currentVideoIndex);

        if (currentVideoIndex == videosToDisplay.size() - 1) {
            btnSkip.setVisibility(View.VISIBLE);
            btnWatchVideo.setVisibility(View.INVISIBLE);
        }

        phonemeVideo.setVideoPath(path);
        phonemeVideo.requestFocus();
    }

    private void playCurrentVideo() {
        phonemeVideo.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                Log.e(TAG, "onError: ");
                return false;
            }
        });

        phonemeVideo.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
                Log.d(TAG, "onInfo: " + i + ", " + i1);
                return false;
            }
        });
        // The videos will play in a loop
//        phonemeVideo.setOnPreparedListener(mp -> mp.setLooping(true));
        if(currentVideoIndex == videosToDisplay.size() - 1) {
            btnSkip.setEnabled(false);
            phonemeVideo.setOnCompletionListener(mp -> {
                btnSkip.setEnabled(true);
//                phonemeVideo.setOnPreparedListener(mp2 -> mp2.setLooping(true));
                phonemeVideo.start();

            });
        }
        else {
//            phonemeVideo.setOnPreparedListener(mp2 -> mp2.setLooping(true));
            phonemeVideo.setOnCompletionListener(mp -> {
                phonemeVideo.start();
            });
        }

        phonemeVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                Log.d(TAG, "onPrepared: " + phonemeVideo.getBufferPercentage() + "," + phonemeVideo.isActivated());
                runOnUiThread(() -> {
                    try {
                        Thread.sleep(700);
                        phonemeVideo.start();
                        Log.d(TAG, "onPrepared: " + phonemeVideo.getBufferPercentage() + "," + phonemeVideo.isActivated());
                    }catch(Exception e) {

                    }
                });
            }
        });
    }

    /**
     * Switches through all videos until
     * the last video.
     * Then it goes to the images
     */
    private void goToNextVideo() {
        currentVideoIndex++;
        if (currentVideoIndex == videosToDisplay.size()) {
            currentVideoIndex = 0;
            setCurrentVideo();
            phonemeVideo.start();

            // Show images
            phonemeVideo.setVisibility(View.INVISIBLE);
            phonemeImage.setVisibility(View.VISIBLE);

            btnWatchVideo.setVisibility(View.VISIBLE);
            btnSkip.setVisibility(View.INVISIBLE);
            showCurrentImage(true);

        } else {
            setCurrentVideo();
            playCurrentVideo();
        }
    }

    /**
     * Gets list of images and videos
     * Gets the audio file
     */
    private void getImagesAndVideo() {
        getImages();
        getVideos();
        getAudio();
    }

    private void getImages() {
//        imageFilesToDisplay = StorageUtils.getImagesListForPhoneme(phoneme);
        String regex = StorageHelper.IMAGE_REGEX;
        String path = phonemeDirectoryPath;
        List<File> files = storage.getFiles(path, regex);
        List<String> paths = new ArrayList<>();
        try {
            for (File file : files) {
                paths.add(file.getAbsolutePath());
            }
        }catch(Exception e) {
            Toast.makeText(this, "Please redownload images.", Toast.LENGTH_LONG).show();
        }

        imageFilesToDisplay = paths;

    }

    private void getVideos() {
//        videosToDisplay = StorageUtils.getVideosListForPhoneme(phoneme, true);
        String regex = StorageHelper.VIDEO_REGEX;
        String path = phonemeDirectoryPath;
        List<File> files = storage.getFiles(path, regex);

        if(files == null) {
            Toast.makeText(this, "Files are still downloading. Please retry", Toast.LENGTH_LONG).show();
            finish();
            return;
        }


        List<String> paths = new ArrayList<>();
        for (File file : files) {
            paths.add(file.getAbsolutePath());
        }

        // Add final video
        String pathFinalFolder = phonemeDirectoryPath + File.separator + "final";
        List<File> filesFinal = storage.getFiles(pathFinalFolder, regex);

        File folder = new File(pathFinalFolder);
        File[] listOfFiles = folder.listFiles();

        for (File file : filesFinal) {
            paths.add(file.getAbsolutePath());
        }

        videosToDisplay = paths;
    }

    private void getAudio() {
        String regex = "([^\\s]+(\\.(?i)(mp3|wav))$)";
        String path = phonemeDirectoryPath + File.separator + "final";

        List<File> files = storage.getFiles(path, regex);

        try {
            audioPath = files.get(0).getAbsolutePath();
            phonemeAudio.setDataSource(audioPath);
            phonemeAudio.prepare();
        } catch (Exception e) {
            Toast.makeText(this, "File not found or storage full. Trying to redownload.", Toast.LENGTH_LONG).show();
            if(!firebaseSyncHelper.isDownloading()) {
                firebaseSyncHelper.downloadFromFirebase();
            }
            finish();
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public int getLayout() {
        return R.layout.activity_learn_phonemes;
    }

    @Override
    protected void inject(ActivityComponent activityComponent) {
        activityComponent.inject(this);
    }

    @Override
    protected void attachView() {
        learnPhonemesPresenter.attachView(this);
    }

    @Override
    protected void detachPresenter() {
        learnPhonemesPresenter.detachView();
    }

}
