package com.example.vladislove.testasyncreturnmethodwithrx;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subscribers.DisposableSubscriber;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    private FusedLocationProviderClient fusedLocationClient;
    CompositeDisposable disposables = new CompositeDisposable();

    private TextView tvCity;
    private ProgressBar pb;

    private String city;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvCity = findViewById(R.id.tv_city);
        pb = findViewById(R.id.pb);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkPermission();
    }

    public void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Camera permission granted
            getCity();
        } else {
            // Camera permission not granted
            Toast.makeText(this, "Permission not granted.", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    101);
        }
    }

    private void getCity() {
        Flowable<String> cityAcync = getCityAcync(fusedLocationClient);
        DisposableSubscriber<String> disposableSubscriber = cityAcync.subscribeWith(new DisposableSubscriber<String>() {
            @Override
            public void onNext(String s) {
                Log.d(TAG, "onNext: " + s);
                city = s;
            }

            @Override
            public void onError(Throwable t) {
                Log.d(TAG, "onError: " + t.toString());
                pb.setVisibility(View.GONE);
                tvCity.setText("Error");
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "onComplete: ");
                pb.setVisibility(View.GONE);
                tvCity.setText(city);
            }
        });
        disposables.add(disposableSubscriber);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)) {
            getCity();
        }
    }

    @SuppressLint("MissingPermission")
    public Flowable<String> getCityAcync(final FusedLocationProviderClient fusedLocationClient) {
        return Flowable.create(emitter -> {
            OnSuccessListener<Location> onSuccessListener = location -> {
                Geocoder geo = new Geocoder(getApplicationContext(), Locale.US);
                try {
                    List<Address> addresses = geo.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    Log.d(TAG, "onSuccess: " + (addresses.isEmpty() ? "empty" : "city " + addresses.get(0).getLocality()));
                    emitter.onNext(addresses.get(0).getLocality());
                    emitter.onComplete();
                } catch (IOException e) {
                    Log.d(TAG, "onSuccess: " + e.getMessage());
                    emitter.onError(e);
                }
            };

            fusedLocationClient.getLastLocation().addOnSuccessListener(onSuccessListener);
        }, BackpressureStrategy.BUFFER);
    }

    @Override
    protected void onPause() {
        super.onPause();
        disposables.dispose();
    }
}
