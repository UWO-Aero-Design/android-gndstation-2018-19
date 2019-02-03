package com.source.aero.aerogroundstation;

import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;

public class MainActivity extends AppCompatActivity {
    private MapView mapView;

    //Ui Elements
    BottomNavigationView bottomNavigationView;
    SpeedDialView speedDialView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getResources().getString(R.string.mapboxToken));
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        //Initializing UI Elements
        initBottomNavigationBar();
        initSpeedDial();

    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    //Performs speed dial initialization
    private void initSpeedDial() {
        speedDialView = findViewById(R.id.mainActivitySpeedDial);
        speedDialView.addActionItem(
                new SpeedDialActionItem.Builder(R.id.mainActivitySpeedDialAction1, R.drawable.mapbox_compass_icon)
                        .setLabel("Option 1")
                        .create()
        );
        speedDialView.addActionItem(
                new SpeedDialActionItem.Builder(R.id.mainActivitySpeedDialAction2, R.drawable.mapbox_info_icon_default)
                        .setLabel("Option 2")
                        .create()
        );
        speedDialView.addActionItem(
                new SpeedDialActionItem.Builder(R.id.mainActivitySpeedDialAction3, R.drawable.ic_search_black_24dp)
                        .setLabel("Option 3")
                        .create()
        );
        speedDialView.addActionItem(
                new SpeedDialActionItem.Builder(R.id.mainActivitySpeedDialAction4, R.drawable.mapbox_marker_icon_default)
                        .setLabel("Option 4")
                        .create()
        );
        //On click listener for speed dial options
        speedDialView.setOnActionSelectedListener(new SpeedDialView.OnActionSelectedListener() {
            @Override
            public boolean onActionSelected(SpeedDialActionItem speedDialActionItem) {
                switch (speedDialActionItem.getId()) {
                    case R.id.mainActivitySpeedDialAction1:
                        speedDialView.close();
                        break;
                    case R.id.mainActivitySpeedDialAction2:
                        speedDialView.close();
                        break;
                    case R.id.mainActivitySpeedDialAction3:
                        speedDialView.close();
                        break;
                    case R.id.mainActivitySpeedDialAction4:
                        speedDialView.close();
                        break;
                    default:
                        return true;
                }
                return true;
            }
        });
    }

    //Initialize bottom navigation bar
    private void initBottomNavigationBar() {
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.mainActivityBottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return true;
            }
        });
    }
}