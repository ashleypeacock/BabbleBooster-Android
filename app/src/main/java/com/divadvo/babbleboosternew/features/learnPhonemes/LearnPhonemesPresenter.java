package com.divadvo.babbleboosternew.features.learnPhonemes;

import com.divadvo.babbleboosternew.data.DataManager;
import com.divadvo.babbleboosternew.data.local.DbManager;
import com.divadvo.babbleboosternew.data.local.Session;
import com.divadvo.babbleboosternew.features.base.BasePresenter;
import com.divadvo.babbleboosternew.injection.ConfigPersistent;

import javax.inject.Inject;

@ConfigPersistent
public class LearnPhonemesPresenter extends BasePresenter<LearnPhonemesMvpView> {

    private final DataManager dataManager;
    private final DbManager dbManager;


    @Inject
    public LearnPhonemesPresenter(DataManager dataManager, DbManager dbManager) {
        this.dataManager = dataManager;
        this.dbManager = dbManager;
    }

//    public void addSession(long startTime) {
//        dbManager.saveSession(new Session(startTime, System.currentTimeMillis() - startTime));
//    }
}
