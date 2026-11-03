package com.example.elderhero_familiyelderly;

import android.app.Application;
import android.net.Uri;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Logger;
import com.google.firebase.storage.FirebaseStorage;

/**
 * Points the Firebase SDKs at the self-hosted Firebase Local Emulator Suite.
 * The database URL is rewritten at init time because useEmulator() mutates the
 * RepoInfo cache key in place, making later getInstance() calls create production instances.
 */
public class ElderHeroApp extends Application {

    private static final int DATABASE_PORT = 9010;
    private static final int AUTH_PORT = 9099;
    private static final int STORAGE_PORT = 9199;

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseOptions options = FirebaseOptions.fromResource(this);
        if (options == null) {
            FirebaseApp.initializeApp(this);
            return;
        }
        if (BuildConfig.USE_EMULATOR && options.getDatabaseUrl() != null) {
            String namespace = Uri.parse(options.getDatabaseUrl()).getHost().split("\\.")[0];
            options = new FirebaseOptions.Builder(options)
                    .setDatabaseUrl("http://" + BuildConfig.EMULATOR_HOST + ":" + DATABASE_PORT
                            + "?ns=" + namespace)
                    .build();
        }
        FirebaseApp.initializeApp(this, options);
        if (BuildConfig.DEBUG) {
            FirebaseDatabase.getInstance().setLogLevel(Logger.Level.DEBUG);
        }
        if (BuildConfig.USE_EMULATOR) {
            FirebaseAuth.getInstance().useEmulator(BuildConfig.EMULATOR_HOST, AUTH_PORT);
            FirebaseStorage.getInstance().useEmulator(BuildConfig.EMULATOR_HOST, STORAGE_PORT);
        }
    }
}
