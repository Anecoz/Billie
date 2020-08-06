package com.anecoz.billie;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class Statistics {
    private final static String TAG = "Statistics";

    private final static String COLLECTION = "trips";
    private static String _document;

    private static double _lastLat;
    private static double _lastLng;
    private static String _lastBatt;
    private static String _lastSpeed;

    private static boolean _enabled = false;
    private static boolean _initialized = false;

    public static void initialize() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US);
        df.setTimeZone(tz);
        _document = df.format(new Date());

        Map<String, Object> topEntry = new HashMap<>();
        List<Map<String, Object>> array = new ArrayList<>();
        topEntry.put("data", array);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(COLLECTION).document(_document)
                .set(topEntry);

        _initialized = true;
    }

    public static void cleanup() {
        writeToDb();
    }

    public static void setEnabled(boolean enabled) {
        _enabled = enabled;
        if (_enabled && !_initialized) {
            initialize();
        }
    }

    public static void writeToDb() {
        if (!_enabled) return;
        double timestamp = System.currentTimeMillis();

        Map<String, Object> entry = new HashMap<>();
        entry.put("timestamp", timestamp);
        entry.put("lat", _lastLat);
        entry.put("lng", _lastLng);
        entry.put("batt", _lastBatt);
        entry.put("speed", _lastSpeed);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(COLLECTION).document(_document)
                .update("data", FieldValue.arrayUnion(entry))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
    }

    public static void addLatLngEntry(double latitude, double longitude) {
        _lastLat = latitude;
        _lastLng = longitude;
    }

    public static void addBattEntry(String percent) {
        _lastBatt = percent;
    }

    public static void addSpeedEntry(String speed) {
        _lastSpeed = speed;
    }
}
