package com.divadvo.babbleboosternew.features.base;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.util.LongSparseArray;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import java.util.concurrent.atomic.AtomicLong;

import butterknife.ButterKnife;

import com.divadvo.babbleboosternew.MvpStarterApplication;
import com.divadvo.babbleboosternew.data.local.DbManager;
import com.divadvo.babbleboosternew.data.local.LocalUser;
import com.divadvo.babbleboosternew.data.local.Session;
import com.divadvo.babbleboosternew.features.lock.LockActivity;
import com.divadvo.babbleboosternew.injection.component.ActivityComponent;
import com.divadvo.babbleboosternew.injection.component.ConfigPersistentComponent;
import com.divadvo.babbleboosternew.injection.component.DaggerConfigPersistentComponent;
import com.divadvo.babbleboosternew.injection.module.ActivityModule;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * Abstract activity that every other Activity in this application must implement. It provides the
 * following functionality: - Handles creation of Dagger components and makes sure that instances of
 * ConfigPersistentComponent are kept across configuration changes. - Set up and handles a
 * GoogleApiClient instance that can be used to access the Google sign in api. - Handles signing out
 * when an authentication error event is received.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private static final String KEY_ACTIVITY_ID = "KEY_ACTIVITY_ID";
    private static final AtomicLong NEXT_ID = new AtomicLong(0);
    private static final LongSparseArray<ConfigPersistentComponent> componentsArray =
            new LongSparseArray<>();

    private long activityId;

    protected long startTime = 0;

    @Inject
    DbManager dbManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayout());
        ButterKnife.bind(this);

        // Create the ActivityComponent and reuses cached ConfigPersistentComponent if this is
        // being called after a configuration change.
        activityId =
                savedInstanceState != null
                        ? savedInstanceState.getLong(KEY_ACTIVITY_ID)
                        : NEXT_ID.getAndIncrement();
        ConfigPersistentComponent configPersistentComponent;
        if (componentsArray.get(activityId) == null) {
            Timber.i("Creating new ConfigPersistentComponent id=%d", activityId);
            configPersistentComponent =
                    DaggerConfigPersistentComponent.builder()
                            .appComponent(MvpStarterApplication.get(this).getComponent())
                            .build();
            componentsArray.put(activityId, configPersistentComponent);
        } else {
            Timber.i("Reusing ConfigPersistentComponent id=%d", activityId);
            configPersistentComponent = componentsArray.get(activityId);
        }
        ActivityComponent activityComponent = configPersistentComponent.activityComponent(new ActivityModule(this));
        inject(activityComponent);
        attachView();
    }

    protected abstract int getLayout();

    protected abstract void inject(ActivityComponent activityComponent);

    protected abstract void attachView();

    protected abstract void detachPresenter();

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_ACTIVITY_ID, activityId);
    }

    @Override
    protected void onDestroy() {
        if (!isChangingConfigurations()) {
            Timber.i("Clearing ConfigPersistentComponent id=%d", activityId);
            componentsArray.remove(activityId);
        }
        detachPresenter();
        super.onDestroy();
    }


    public static final int REQUEST_CODE = 1;
    public static final int REQUEST_CODE_START_ANOTHER = 67;
    protected boolean shouldCheckCredentials = false;

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected void onPause() {
        shouldCheckCredentials = true;
        if(LocalUser.getInstance() != null && LocalUser.getInstance().username != null) {
            Session s = new Session(startTime, System.currentTimeMillis() - startTime, this.getLocalClassName(), LocalUser.getInstance().username);
            dbManager.saveSessionLocal(s);
        }
        super.onPause();
    }


    @Override
    protected void onResume() {
        if (shouldCheckCredentials) {
            startActivityForResult(LockActivity.getStartIntent(this), REQUEST_CODE);
            finish();
        }
        startTime = System.currentTimeMillis();
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BaseActivity.REQUEST_CODE && resultCode == RESULT_OK) {
            shouldCheckCredentials = false;
        }

        if(requestCode == REQUEST_CODE_START_ANOTHER && resultCode == RESULT_CANCELED) {
            shouldCheckCredentials = false;
        }
    }

    @Override
    public void startActivity(Intent intent) {
        startActivityForResult(intent, REQUEST_CODE_START_ANOTHER);
    }
}
