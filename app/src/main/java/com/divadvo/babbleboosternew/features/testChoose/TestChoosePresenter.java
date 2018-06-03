package com.divadvo.babbleboosternew.features.testChoose;

import com.divadvo.babbleboosternew.data.DataManager;
import com.divadvo.babbleboosternew.data.local.DbManager;
import com.divadvo.babbleboosternew.features.base.BasePresenter;
import com.divadvo.babbleboosternew.injection.ConfigPersistent;

import javax.inject.Inject;

@ConfigPersistent
public class TestChoosePresenter extends BasePresenter<TestChooseMvpView> {
    private final DataManager dataManager;
    private final DbManager dbManager;

    @Inject
    public TestChoosePresenter(DataManager dataManager, DbManager dbManager) {
        this.dataManager = dataManager;
        this.dbManager = dbManager;
    }

    public int calculateNumberOfAttemptsRemaining(String phoneme) {
        int numberOfAttemptsRemaining = dbManager.calculateNumberOfAttemptsRemaining(phoneme);

        if(numberOfAttemptsRemaining < 0) // we've changed this so we don't want incorrect numbers in the database
            numberOfAttemptsRemaining = 0;

        return numberOfAttemptsRemaining;
    }
}
