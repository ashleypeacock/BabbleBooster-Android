package com.divadvo.babbleboosternew.features.recordVideo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.afollestad.materialcamera.MaterialCamera;
import com.crashlytics.android.Crashlytics;
import com.divadvo.babbleboosternew.Constants;
import com.divadvo.babbleboosternew.R;
import com.divadvo.babbleboosternew.data.firebase.FirebaseSyncHelper;
import com.divadvo.babbleboosternew.data.local.LocalUser;
import com.divadvo.babbleboosternew.features.base.BaseActivity;
import com.divadvo.babbleboosternew.features.choosePhonemes.ChoosePhonemesActivity;
import com.divadvo.babbleboosternew.features.choosePhonemes.ChoosePhonemesPresenter;
import com.divadvo.babbleboosternew.features.learnPhonemes.LearnPhonemesActivity;
import com.divadvo.babbleboosternew.features.testChoose.TestChooseActivity;
import com.divadvo.babbleboosternew.injection.component.ActivityComponent;

import java.io.File;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

/**
 *
 */
public class RecordVideoActivity extends BaseActivity implements RecordVideoMvpView {
    public static final String EXTRA_PHONEME = "EXTRA_PHONEME";
    public static final String EXTRA_TEST = "EXTRA_TEST";


    private final static int CAMERA_RQ = 6969;

    @Inject
    FirebaseSyncHelper firebaseSyncHelper;

    @BindView(R.id.button_yes_new)
    Button btnYes;

    @BindView(R.id.button_attempt_new)
    Button btnAttempt;

    @BindView(R.id.button_no_new)
    Button btnNo;

    @BindView(R.id.button_review_video)
    Button btnReplay;

    @BindView(R.id.video_view_record)
    VideoView videoView;

    @Inject
    RecordVideoPresenter recordVideoPresenter;

    private String phoneme;
    private boolean isTest;
    private String TAG = "RecordVideoActivity";
    private Handler handler;
    private int cameraCrash = 0;

    public static Intent getStartIntent(Context context, String phoneme, boolean isTest) {
        Intent intent = new Intent(context, RecordVideoActivity.class);
        intent.putExtra(EXTRA_PHONEME, phoneme);
        intent.putExtra(EXTRA_TEST, isTest);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        phoneme = getIntent().getStringExtra(EXTRA_PHONEME);
        isTest = getIntent().getBooleanExtra(EXTRA_TEST, false);
        if (phoneme == null) {
            throw new IllegalArgumentException("Record Video Activity requires a phoneme");
        }

        ButtonClickListener buttonClickListener = new ButtonClickListener();
        btnYes.setOnClickListener(buttonClickListener);
        btnAttempt.setOnClickListener(buttonClickListener);
        btnNo.setOnClickListener(buttonClickListener);

        handler = new Handler(Looper.getMainLooper());

    }


    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        startCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @OnClick(R.id.button_review_video)
    void onReviewVideo() {
        videoView.start();

        findViewById(R.id.button_review_video).getBackground().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);


        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                findViewById(R.id.button_review_video).getBackground().setColorFilter(getResources().getColor(R.color.accent), PorterDuff.Mode.MULTIPLY);
            }
        });
    }

    /**
     * Uses material camera library to start a front facing camera
     * Video recording starts automatically and ends after max 10 seconds
     */
    private void startCamera() {
        shouldCheckCredentials = false;

        String folder = recordVideoPresenter.getVideoFolder(isTest);

        new MaterialCamera(this)
                .allowRetry(false)
                .autoSubmit(true)
                .saveDir(folder)
//                .qualityProfile(Constants.CAMERA_QUALITY)
                .showPortraitWarning(false)
                .defaultToFrontFacing(true)
                .videoPreferredAspect(16f / 9f)
                .iconStop(R.drawable.ic_stop_recording)
                .retryExits(false)
                .autoRecordWithDelayMs(10) //100
                .countdownSeconds(90f) // max length of video is 90 seconds
                .countdownImmediately(true) // recording starts automatically
                .start(CAMERA_RQ);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        shouldCheckCredentials = false;
        super.onActivityResult(requestCode, resultCode, data);
        shouldCheckCredentials = false;


        // Received recording or error from MaterialCamera
        if (requestCode == CAMERA_RQ) {

            if (resultCode == RESULT_OK) {
                saveAttemptVideo(data.getDataString());
                findViewById(R.id.layout_main).setVisibility(View.VISIBLE);
            } else if (data != null) {
                Exception e = (Exception) data.getSerializableExtra(MaterialCamera.ERROR_EXTRA);
                Log.e(TAG, "onActivityResult: ", e);
                startCamera();
                cameraCrash++;
                if(cameraCrash == 3) {
                    Toast.makeText(this, "Something wrong with camera", Toast.LENGTH_LONG).show();
                }

                Crashlytics.logException(e);
            }
        }
    }

    private String originalFilePath;

    private void saveAttemptVideo(String videoPath) {
        originalFilePath = videoPath;
        showRecordedVideo(videoPath);
    }

    private void showRecordedVideo(String videoPath) {
        videoView.setVideoPath(videoPath);
        videoView.start();
    }


    private void runSaveAttemptInDatabase(String response) {
//        AttemptLocal attemptLocal = new AttemptLocal(user.getUserid(), attemptNumberFull, CommonUtils.getCurrentTimeStamp(), phoneme, response);
//
//        LocalDatabaseHelper databaseHelper = LocalDatabaseHelper.getInstance(this);
//        databaseHelper.addAttempt(attemptLocal);

        recordVideoPresenter.saveAttemptInDatabase(phoneme, isTest, originalFilePath, response);

        Toast.makeText(this, response, Toast.LENGTH_SHORT).show();

        // Hide all buttons
        btnYes.setVisibility(View.GONE);
        btnAttempt.setVisibility(View.GONE);
        btnNo.setVisibility(View.GONE);
        btnReplay.setVisibility(View.GONE);

        performActionAfterResponse(response);
    }

    private void performActionAfterResponse(String response) {
        if ("YES".equals(response)) {
            if (!isTest)
                playVideo("YES");
            else {
                startActivity(TestChooseActivity.getStartIntent(this));
                finish();
            }
        }
        if ("GOOD TRY".equals(response)) {
            if (!isTest)
                playVideo("GOOD_TRY");
            else {
                startActivity(TestChooseActivity.getStartIntent(this));
                finish();
            }
        }
        if ("TRY AGAIN".equals(response)) {
            if (!isTest)
                startActivity(LearnPhonemesActivity.getStartIntent(this, phoneme, true));
            else
                startActivity(TestChooseActivity.getStartIntent(this));

            finish();
        }
    }

    private void playVideo(String name) {
        String videoPath = recordVideoPresenter.getReinforcementVideo(name);
        if (videoPath != null) {
            // Play until the end of the video
            // Then go back to the menu where one can
            // choose the phonemes
            videoView.setOnCompletionListener(mp -> startChoosePhoneme());

            videoView.setVideoPath(videoPath);
            videoView.start();
        }
    }


    private void startChoosePhoneme() {
        if (!isTest)
            startActivity(ChoosePhonemesActivity.getStartIntent(this));
        else
            startActivity(TestChooseActivity.getStartIntent(this));
        finish();
    }


    @Override
    public int getLayout() {
        return R.layout.activity_record_video;
    }

    @Override
    protected void inject(ActivityComponent activityComponent) {
        activityComponent.inject(this);
    }

    @Override
    protected void attachView() {
        recordVideoPresenter.attachView(this);
    }

    @Override
    protected void detachPresenter() {
        recordVideoPresenter.detachView();
    }

    public class ButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Button btn = (Button) v;
            String response = btn.getText().toString();
            runSaveAttemptInDatabase(response);
        }
    }

}
