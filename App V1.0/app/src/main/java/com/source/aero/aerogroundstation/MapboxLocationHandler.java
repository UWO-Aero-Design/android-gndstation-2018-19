package com.source.aero.aerogroundstation;

import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.support.annotation.NonNull;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.geometry.LatLng;



public class MapboxLocationHandler extends Fragment implements PermissionsListener{
    // Permission handling object
    private PermissionsManager permissionsManager;
    private MapboxMap map;

    //UI Objects
    private Button currentLocationButton;

    // Storing last location of this device, received from location engine
    private Location lastKnownLocation;


    public MapboxLocationHandler() {
        //Empty fragment constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            this.map = ((MainActivity) getActivity()).passMap(); //Get reference to map object from main
        } catch (NullPointerException e) {
            Log.d("MyLocation Fragment", "Couldn't get map from main activity");
        }

        MainActivity activity = (MainActivity)getActivity();

    }

    //Return inflater with location handler layout
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_mapboxlocationhandler, parent, false);
    }

    //Initialize UI elements
    @Override
    public void onViewCreated(View view, Bundle savedInstance) {
//        currentLocationButton = (Button) view.findViewById(R.id.locationButton);
//        currentLocationButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                currentLocation();
//            }
//        });
        currentLocation();
    }

    // Gets current LatLng from location engine and updates last known location. Sets camera to track location.
    @SuppressWarnings( {"MissingPermission"})
    public void cameraToCurrentLocation() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(getActivity())) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            LocationComponent locationComponent = map.getLocationComponent();
            locationComponent.activateLocationComponent(getActivity());
            locationComponent.setLocationComponentEnabled(true);
            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
            lastKnownLocation = locationComponent.getLastKnownLocation();

        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(getActivity());
        }
    }

    // Gets current LatLng from location engine and updates last known location. Does not set camera to track location.
    @SuppressWarnings( {"MissingPermission"})
    public void updateCurrentLocation() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(getActivity())) {
            // Activate the MapboxMap LocationComponent to show user location
            // Adding in LocationComponentOptions is also an optional parameter
            LocationComponent locationComponent = map.getLocationComponent();
            locationComponent.activateLocationComponent(getActivity());
            locationComponent.setLocationComponentEnabled(true);
            // Set the component's camera mode
            lastKnownLocation = locationComponent.getLastKnownLocation();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(getActivity());
        }
    }

    // Updates last known location LatLng value.
    public void cameraToLocation() {
        CameraPosition position = new CameraPosition.Builder()
                .target(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude())) // Sets the new camera position
                .zoom(17) // Sets the zoom
                .tilt(30)
                .build(); // Creates a CameraPosition from the builder

        map.animateCamera(CameraUpdateFactory
                .newCameraPosition(position), 7000);
    }

    // Handling permission requests
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Prompt user with location services
    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(getActivity(), R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    // Handle permission result
    @Override
    public void onPermissionResult(boolean granted) {
        // If we have permission, zoom camera to location and store latest LatLng. Else, Toast out the not granted string
        if (granted) {
            cameraToCurrentLocation();
        } else {
            Toast.makeText(getActivity(), R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
        }
    }

    //Updates map to current gps location
    public void currentLocation() {
        updateCurrentLocation();
        cameraToLocation();
        Toast.makeText(getActivity(),Location.convert(lastKnownLocation.getLatitude(), Location.FORMAT_DEGREES) + " " + Location.convert(lastKnownLocation.getLongitude(), Location.FORMAT_DEGREES), Toast.LENGTH_LONG).show();


        // Dialog builder
        MainActivity activity = (MainActivity)getActivity();
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View textView = inflater.inflate(R.layout.location_builder, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setView(textView)
                .setTitle("Save Target?")
                .setPositiveButton("Save",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        EditText editText = (EditText) textView.findViewById(R.id.nameBuild);
                        String name = editText.getText().toString();
                        Log.d("Builder Loc0", name);
                        if(name.equals(""))
                        {
                            Toast.makeText(getActivity(), "Empty name, try again", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Log.d("Builder Loc2", name);
                            activity.mDatabaseHelper.addTarget(name, lastKnownLocation.toString());
                            dialog.cancel();
                        }

                    }
                })
                .setNegativeButton("Close",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        builder.create();
        builder.show();
    }
}