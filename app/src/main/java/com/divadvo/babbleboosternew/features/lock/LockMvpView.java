package com.divadvo.babbleboosternew.features.lock;

import android.widget.Button;

import com.divadvo.babbleboosternew.features.base.MvpView;

public interface LockMvpView extends MvpView {

    void loginSuccessfulOffline(String password);

    void wrongPassword();

    Button getLoginButton();

    void tryStartingHomeButWaitUntilFinished();

    void displayMessage(String errorWithUserDetails);
}
