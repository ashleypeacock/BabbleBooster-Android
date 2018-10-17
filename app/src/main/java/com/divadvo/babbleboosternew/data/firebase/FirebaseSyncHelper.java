package com.divadvo.babbleboosternew.data.firebase;


import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.divadvo.babbleboosternew.Constants;
import com.divadvo.babbleboosternew.data.local.Attempt;
import com.divadvo.babbleboosternew.data.local.DbManager;
import com.divadvo.babbleboosternew.data.local.LocalUser;
import com.divadvo.babbleboosternew.data.local.Session;
import com.divadvo.babbleboosternew.data.local.StorageHelper;
import com.divadvo.babbleboosternew.features.lock.LockMvpView;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.snatik.storage.Storage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

import static io.fabric.sdk.android.Fabric.TAG;

@Singleton
public class FirebaseSyncHelper {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    private Storage storage;

    StorageHelper storageHelper;
    DbManager dbManager;
    private LockMvpView progressView;
    Task<QuerySnapshot> ptask;

    public ArrayList<String> tasks = new ArrayList<>();
    public AtomicInteger tasksToF = new AtomicInteger(0);
    public AtomicInteger uploadingTaskCount = new AtomicInteger(0);
    private View progressBar;
    private TextView tvUpdate;

    @Inject
    public FirebaseSyncHelper(Context context, StorageHelper storageHelper, DbManager dbManager) {
        storage = new Storage(context);
        this.storageHelper = storageHelper;
        this.dbManager = dbManager;
    }

    public void downloadFromFirebase() {
        downloadFromFirebase(null, null);
    }

    public void downloadFromFirebase(View progressBar, TextView update) {
        if (isDownloading())
            return;

        this.progressBar = progressBar;
        this.tvUpdate = update;
        incrementAndLog();

        if(update != null)
            update.setText("Downloading Resources: " + tasksToF.get());

        ptask = db.collection("phonemeData").get();

        ptask.addOnFailureListener(command -> progressBar.setVisibility(View.GONE));

        ptask.addOnCompleteListener(task -> {
            decrementAndLog();
            if(update != null)
                update.setText("Downloading Resources: " + tasksToF.get());
            if (task.isSuccessful()) {
//                progressView.tryStartingHomeButWaitUntilFinished();
                for (DocumentSnapshot document : task.getResult()) {
                    Timber.d(document.getId() + " => " + document.getData());
                    Log.d(TAG, "downloadPhonemes: " + "data: " + document.getId()  + "," + document.getData());
                    String phonemeName = document.get("phoneme").toString();

                    if(LocalUser.getInstance().all_phonemes.contains(phonemeName)) {
                        downloadPhonemeData(document);
                    }
                }
                downloadReinforcement();
                if(!isDownloading())
                    progressBar.setVisibility(View.GONE);
                Log.d("LoginTest", "finished downloading phenomes: ");
            } else {
                progressBar.setVisibility(View.GONE);
                Timber.d("Error getting documents.", task.getException());
            }
        });
    }

    private void downloadPhonemeData(DocumentSnapshot phoneme) {
        Map<String, Object> data = phoneme.getData();
        String phonemeString = data.get("phoneme").toString();
        String finalVideo = data.get("finalVideo").toString();
        String sound = data.get("sound").toString();

        ArrayList<String> images = (ArrayList<String>) data.get("images");
        Timber.i(images.toString());

        String folderPhoneme = storageHelper.getPhonemeFolder(phonemeString);
        String folderFinal = storageHelper.getPhonemeFolderFinal(phonemeString);
        downloadFile(folderFinal, finalVideo);
        downloadFile(folderFinal, sound);

        List<String> filesToIgnore = storageHelper.getListOfFilesToIgnore(phonemeString);

        for(String image : images) {
            downloadFile(folderPhoneme, image, filesToIgnore);
        }

    }

    private void downloadFile(String folder, String gsFileLocation, List<String> filesToIgnore) {
        String fileName = gsFileLocation.substring(gsFileLocation.lastIndexOf('/') + 1, gsFileLocation.length());
        File f = new File(fileName);

        // Only download if not previously deleted by the user
        if(!filesToIgnore.contains(fileName) || f.length() == 0) {
            downloadFile(folder, gsFileLocation);
        }

    }


    ArrayList<FileDownloadTask> downloadTasks = new ArrayList<>();

    private void downloadFile(String folder, String gsFileLocation) {

        File folderF = new File(folder);
        String fileName = gsFileLocation.substring(gsFileLocation.lastIndexOf('/') + 1, gsFileLocation.length());

        File fileLocation = new File(folderF, fileName);

        StorageReference objectReference = firebaseStorage.getReferenceFromUrl(gsFileLocation);

        long length = fileLocation.length();
        // TODO: check if doesn't exist
        if(!fileLocation.exists() || (fileLocation.length() == 0)) {
            incrementAndLog();

            FileDownloadTask t = objectReference.getFile(fileLocation);

            downloadTasks.add(t);

            t.addOnSuccessListener(taskSnapshot -> {
                decrementAndLog();
                downloadTasks.remove(t);

                if(tvUpdate != null)
                    tvUpdate.setText("Downloading Resources: " + tasksToF.get());

                if (!isDownloading())
                    progressBar.setVisibility(View.GONE);
            }).addOnFailureListener(exception -> {
                Timber.e(exception);
                decrementAndLog();
                fileLocation.delete();
                Log.e("Error", "downloadFile: " + fileLocation + ", " + fileName, exception);
                if (!isDownloading())
                    progressBar.setVisibility(View.GONE);
            });
        }
    }

    public void incrementAndLog() {
        tasksToF.incrementAndGet();
        Log.d("Download", "incrementAndLog: " + tasksToF.get());
    }

    public void decrementAndLog() {
        tasksToF.decrementAndGet();
        Log.d("Download", "decrementAndLog: " + tasksToF.get());
//        if(!isDownloading())
//            progressBar.setVisibility(View.GONE);
    }


    public boolean isDownloading() {
        return tasksToF.get() > 0;
    }

    public boolean isUploading() {
        return uploadingTaskCount.get() > 0;
    }

    private void downloadReinforcement() {
        incrementAndLog();
        db.collection("users").document("default")
        .get().addOnCompleteListener(task -> {
            decrementAndLog();
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                downloadReinforcementFiles(document);
            } else {
                Timber.d( "Error getting documents.", task.getException());
            }
        });
    }

    private void downloadReinforcementFiles(DocumentSnapshot document) {
        Map<String, Object> data = document.getData();
        String videoYes = data.get("video_yes").toString();
        String videoGoodTry = data.get("video_good_try").toString();

        String folderReinforcement = storageHelper.getReinforcementFolder();
        downloadFile(folderReinforcement, videoYes);
        downloadFile(folderReinforcement, videoGoodTry);

        if (tasksToF.intValue() == 0 && !isDownloading())
            progressBar.setVisibility(View.GONE);
    }

    public void uploadEverything() {
        if(!isUploading()) {
            uploadAttempts();
            uploadTests();
            uploadDatabase();
            uploadMastered();
            uploadSessions();
        }
    }

    private void uploadAttempts() {
        List<File> attemptVideos = storageHelper.getAllAttemptVideos();

        StorageReference storageRef = firebaseStorage.getReference();

        String username = LocalUser.getInstance().username;
        String folderFirebase = "attempts/" + username;

        for(File file : attemptVideos) {
            String fileFirebase = folderFirebase + "/" + file.getName();
            StorageReference fileReference = storageRef.child(fileFirebase);

            uploadFile(fileReference, file, fileFirebase);
        }
    }


    private void uploadTests() {
        List<File> testVideos = storageHelper.getAllTestVideos();

        StorageReference storageRef = firebaseStorage.getReference();

        String username = LocalUser.getInstance().username;
        String folderFirebase = "tests/" + username;

        for(File file : testVideos) {
            String fileFirebase = folderFirebase + "/" + file.getName();
            StorageReference fileReference = storageRef.child(fileFirebase);

            uploadFile(fileReference, file, fileFirebase);
        }
    }


    private void uploadSessions() {

        CollectionReference collectionAttempts = db.collection("session").document(LocalUser.getInstance().username).collection("sessions");

        List<Session> sessionsInDatabase = new ArrayList<>();

        collectionAttempts.get().addOnCompleteListener(task -> {

            if (task.isSuccessful()) {
                List<Session> allAttemptsBefore = dbManager.getAllSessionsFromRealm();
                for (DocumentSnapshot document : task.getResult()) {

                    Session session = document.toObject(Session.class);

                    sessionsInDatabase.add(session);

                    // If not in local database, save it
                    if(!allAttemptsBefore.contains(session)) {
                        dbManager.saveSessionLocal(session);
                    }
                }


                List<Session> allAttempts = dbManager.getAllSessionsFromRealm();
                // Upload not existent
                for(Session session : allAttempts) {

                    // Skip if already uploaded
                    if(sessionsInDatabase.contains(session)) {
                        continue;
                    }

                    collectionAttempts.add(session);
                }
            } else {
                Timber.d("Error getting documents: ", task.getException());
            }
        });
    }

    private void uploadDatabase() {
        CollectionReference collectionAttempts = db.collection("results");
        List<Attempt> attemptsInDatabase = new ArrayList<>();

        collectionAttempts.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                List<Attempt> allAttemptsBefore = dbManager.getAllAttemptsFromRealm();
                for (DocumentSnapshot document : task.getResult()) {
                    Attempt attempt = document.toObject(Attempt.class);
                    attemptsInDatabase.add(attempt);

                    // If remote attempt is of the current user
                    // And it's not in the local database, then save it
                    if(attempt.getUsername().equals(LocalUser.getInstance().username) && !allAttemptsBefore.contains(attempt)) {
                        dbManager.saveAttemptLocal(attempt);
                    }
                }

                List<Attempt> allAttempts = dbManager.getAllAttemptsFromRealm();
                // Upload not existent
                for(Attempt attempt : allAttempts) {
                    // Skip if already uploaded
                    if(attemptsInDatabase.contains(attempt))
                        continue;

                    collectionAttempts.add(attempt);
                }
            } else {
                Timber.d("Error getting documents: ", task.getException());
            }
        });
    }

    private void uploadMastered() {
//        progressView.displayStatus("Uploading Mastered");

        Map<String, Object> data = new HashMap<>();
        data.put("mastered_phonemes", LocalUser.getInstance().mastered_phonemes);
//        data.put("mastered_phonemes", Arrays.asList("b", "d", "t"));

        db.collection("users").document(LocalUser.getInstance().username)
                .set(data, SetOptions.merge());
    }

    private void uploadFile(StorageReference fileReference, File localFile, String fileFirebase) {
        uploadingTaskCount.incrementAndGet();
        StorageReference storageRef = firebaseStorage.getReference();
        storageRef.child(fileFirebase).getDownloadUrl().addOnSuccessListener(uri -> {
            uploadingTaskCount.decrementAndGet();
        }).addOnFailureListener(exception -> {
            Timber.e(exception);
            uploadingTaskCount.decrementAndGet();
            Log.d(TAG, "uploadFile: " + uploadingTaskCount.get());
            doesntExistSoUpload(fileReference, localFile, fileFirebase);
        });
    }

    private void doesntExistSoUpload(StorageReference fileReference, File localFile, String fileFirebase) {
        // File doesn't exist
        Timber.i("File doesn't exist: " + localFile);
        Uri file = Uri.fromFile(localFile);
//        UploadTask uploadTask = fileReference.putFile(file);

//        tasksToFinish++;
//        tasks.add(fileFirebase);
        uploadingTaskCount.incrementAndGet();
        Timber.i("tasksToF: " + tasksToF.get());

        StorageReference storageRef = firebaseStorage.getReference();
        StorageReference firebaseFile = storageRef.child(fileFirebase);
        UploadTask uploadTask = firebaseFile.putFile(file);

//        progressView.displayStatus("Uploading file: " + localFile.getName());

        uploadTask.addOnFailureListener(exception2 -> {
            // Handle unsuccessful uploads
            Timber.e("Upload failed", exception2);
            uploadingTaskCount.decrementAndGet();
        }).addOnSuccessListener(taskSnapshot -> {
            // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
            uploadingTaskCount.decrementAndGet();
//            tasks.remove(fileFirebase);
            Uri downloadUrl = taskSnapshot.getDownloadUrl();
            Timber.i("Upload successful", downloadUrl);
            if(Constants.DELETE_UPLOADED_VIDEOS) {
                storageHelper.deleteFile(localFile.getAbsolutePath());
            }
        });
    }

    public void setProgressView(LockMvpView progressView) {
        this.progressView = progressView;
    }
}
