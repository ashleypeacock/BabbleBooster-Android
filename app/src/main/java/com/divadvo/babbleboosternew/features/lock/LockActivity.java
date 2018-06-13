package com.divadvo.babbleboosternew.features.lock;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.divadvo.babbleboosternew.R;
import com.divadvo.babbleboosternew.data.firebase.FirebaseSyncHelper;
import com.divadvo.babbleboosternew.data.local.LocalUser;
import com.divadvo.babbleboosternew.data.local.PermissionsUtils;
import com.divadvo.babbleboosternew.data.local.User;
import com.divadvo.babbleboosternew.features.base.BaseActivity;
import com.divadvo.babbleboosternew.features.home.HomeActivity;
import com.divadvo.babbleboosternew.injection.component.ActivityComponent;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import timber.log.Timber;

public class LockActivity extends BaseActivity implements LockMvpView {

    @Inject
    LockPresenter lockPresenter;

    @Inject
    FirebaseSyncHelper firebaseSyncHelper;

    @BindView(R.id.editText_password)
    EditText editTextPassword;

    @BindView(R.id.button_login)
    Button buttonLogin;

    @BindView(R.id.button_login_online)
    Button buttonLoginOnline;

    @BindView(R.id.text_status)
    TextView textStatus;

    private String enteredPassword;

//    @BindView(R.id.button_clear_data)
//    Button buttonClearData;


    public static Intent getStartIntent(Context context) {
        Intent intent = new Intent(context, LockActivity.class);
//        intent.putExtra(EXTRA_POKEMON_NAME, pokemonName);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionsUtils.requestAllPermissions(this);


        buttonLogin.setOnClickListener(v -> {
            buttonLogin.setEnabled(false);
            enteredPassword = editTextPassword.getText().toString();
            if(!enteredPassword.isEmpty()) {
                if (isNetworkConnected(this))
                    lockPresenter.loginOnline(enteredPassword);
                else
                    lockPresenter.loginOffline(enteredPassword);
            }
//            buttonLogin.setEnabled(true);
        });

        buttonLoginOnline.setOnClickListener(v -> {
            enteredPassword = editTextPassword.getText().toString();
            if (enteredPassword.isEmpty())
                Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show();
            else
                lockPresenter.loginOnline(enteredPassword);
        });

        if(savedInstanceState != null) {
            enteredPassword = savedInstanceState.getString("enteredpass");
            editTextPassword.setText(enteredPassword);
        }

        Log.d("Test", "onCreate: ");
        
//        buttonLogin.setEnabled(lockPresenter.doesLocalUserExist());

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("enteredpass", editTextPassword.getText().toString());
    }

    public Button getLoginButton() {
        return buttonLogin;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(this, "Need all permissions", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean isNetworkConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

//    @OnClick(R.id.button_clear_data)
//    void clearData() {
//        if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
//            ((ActivityManager)getSystemService(ACTIVITY_SERVICE))
//                    .clearApplicationUserData(); // note: it has a return value!
//        } else {
//            // use old hacky way, which can be removed
//            // once minSdkVersion goes above 19 in a few years.
//        }
//    }

    @Override
    public int getLayout() {
        return R.layout.activity_lock;
    }

    @Override
    protected void inject(ActivityComponent activityComponent) {
        activityComponent.inject(this);
    }

    @Override
    protected void attachView() {
        lockPresenter.attachView(this);
    }

    @Override
    protected void detachPresenter() {
        lockPresenter.detachView();
    }

    @Override
    public void wrongPassword() {
        Timber.e("Wrong Password");
        Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show();
        buttonLogin.setEnabled(true);
    }

    @Override
    public void loginSuccessfulOffline(String password) {
        // Load user from local prefs
//        LocalUser.setInstance();
        lockPresenter.loadUser();
        buttonLogin.setEnabled(true);
        startHomeActivity();
    }

    private void startHomeActivity() {
        startActivity(HomeActivity.getStartIntent(this));
        finish();
    }


    @Override
    protected void onPause() {
        super.onPause();
        firebaseSyncHelper.setProgressView(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        firebaseSyncHelper.setProgressView(this);
    }

    @Override
    public void tryStartingHomeButWaitUntilFinished() {
//        try {
//            Thread.sleep(300);
//            while (firebaseSyncHelper.tasksToF.get() > 0) {
////                textStatus.setText(firebaseSyncHelper.tasksToF.toString());
//                textStatus.append(firebaseSyncHelper.tasksToF.toString());
//                Thread.sleep(300);
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        startHomeActivity();
    }

    @Override
    public void displayMessage(String errorWithUserDetails) {
        Toast.makeText(this, "Error with user details", Toast.LENGTH_LONG).show();
    }

    @Override
    public void displayStatus(int numberRemaining) {
        textStatus.setText("Please wait until 0. Remaining: " + numberRemaining);
        Timber.i("Remaining: " + numberRemaining);
        buttonLogin.setEnabled(numberRemaining == 0);
        if(numberRemaining == 0) {
            lockPresenter.loginOffline(enteredPassword);
        }
    }
}
