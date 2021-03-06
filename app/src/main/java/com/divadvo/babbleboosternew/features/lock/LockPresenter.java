package com.divadvo.babbleboosternew.features.lock;

import android.util.Log;

import com.divadvo.babbleboosternew.data.firebase.FirebaseSyncHelper;
import com.divadvo.babbleboosternew.data.local.PreferencesHelper;
import com.divadvo.babbleboosternew.data.local.User;
import com.divadvo.babbleboosternew.features.base.BasePresenter;
import com.divadvo.babbleboosternew.injection.ConfigPersistent;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import javax.inject.Inject;

import timber.log.Timber;

@ConfigPersistent
public class LockPresenter extends BasePresenter<LockMvpView> {

    private PreferencesHelper preferencesHelper;

    @Inject
    FirebaseSyncHelper firebaseSyncHelper;

    @Inject
    public LockPresenter(PreferencesHelper preferencesHelper) {
        this.preferencesHelper = preferencesHelper;
    }

    public void loginOffline(String password) {
        checkViewAttached();
        if (isCorrectPassword(password)) {
            getView().loginSuccessfulOffline(password);
        } else {
            getView().wrongPassword();
            getView().getLoginButton().setText("Login");
        }
    }

    private boolean isCorrectPassword(String password) {
        String usernameSaved = preferencesHelper.getString("username");

        if(password == null)
            Log.d("LockPresenter", "isCorrectPassword: ");

        if(doesLocalUserExist()) {
            return password.equals(usernameSaved);
        }
        else {
            // Nothing saved
            return false;
        }
    }

    public boolean doesLocalUserExist() {
        String usernameSaved = preferencesHelper.getString("username");
//        String usernameSaved = LocalUser.getInstance().username;
        return !usernameSaved.equals("");
    }

    private void signInAnonymously(String password) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        Task<AuthResult> resultTask = mAuth.signInAnonymously();

        resultTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Timber.i("Success");
                afterSignedInOnline(password);
            } else {
                Timber.i("Failed");
                getView().getLoginButton().setText("Login");
            }
        });
    }

    public void loginOnline(String password) {
        checkViewAttached();
        signInAnonymously(password);
    }

    public void savedUserInLocal(String password) {
        // Sync with firebase
//        textStatus.setText("Please wait");
        loadUser();
//        firebaseSyncHelper.setProgressView(this);
        Log.d("LoginTest", "savedUserInLocal: ");
        getView().getLoginButton().setEnabled(false);
        getView().getLoginButton().setText("Logging in...");
        loginOffline(password);
    }

    private void afterSignedInOnline(String password) {
        // Get the document from firestore under "users" collection
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("users").document(password);

        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                // If this doc exists, means that the user exists
                // ie the password was correct
                if (document != null && document.exists()) {
                    Timber.d("DocumentSnapshot data: " + task.getResult().getData());

                    try {
                        User user = document.toObject(User.class);
                        // Save user in local preferences
                        preferencesHelper.saveUser(user);
                    }catch (Exception e) {
                        getView().displayMessage("Error with user details");
//                        throw new UserDataCorrupt();
                    }

                    if(getView() != null) {
                        savedUserInLocal(password);
                    }
                } else {
                    Timber.d( "No such document");
                    getView().wrongPassword();
                }
            } else {
                Timber.d( "get failed with ", task.getException());
            }
        });
    }

    private static class UserDataCorrupt extends RuntimeException {
        UserDataCorrupt() {
            super("User data is corrupt on server");
        }
    }

    public void loginOnlineEmail(String password) {
        checkViewAttached();

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String email = password + "@babblebooster.com";

        mAuth.signInWithEmailAndPassword(email, email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
//                FirebaseUser user = mAuth.getCurrentUser();
                savedUserInLocal(password);
            } else {
                Timber.e(task.getException());
                getView().wrongPassword();
            }
        });

    }

    public void loadUser() {
        preferencesHelper.loadUser();
    }
}
