package com.source.aero.aerogroundstation;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import static java.lang.Thread.sleep;

public class FlightPathFragment extends Fragment implements OnMapReadyCallback {
    private final static String TAG = "FLIGHTPATH";

    private Polyline path;
    private MapView mapView;
    private MapboxMap map;
    protected Marker planeMarker;
    Marker Payload;
    Marker CDA;
    protected Marker lastPosition;
    protected Bitmap icon;
    protected IconFactory factory;

    //UI Elements
    ImageButton forwardButton;
    ImageButton backwardsButton;
    ImageButton playButton;
    Button displayButton;

    TextView fpWater;
    TextView fpWaterVal;
    TextView fpHabitat;
    TextView fpHabitatVal;
    TextView fpCDA;
    TextView fpCDAVal;

    TextView altitudeVal;
    TextView speedVal;
    TextView waterDropHeightVal;
    TextView habitatDropHeightVal;
    TextView gliderDropHeightVal;
    TextView rollVal;
    TextView pitchVal;
    TextView yawVal;

    TableLayout tb;

    //Data elements
    ArrayList<Waypoint> waypoints;
    ArrayList<LatLng> points;
    int[] showing; //0 = not showing, 1 = passed point (in polyline), 2 = passed point and showing black circle
    int currentPoint = 0;
    int lastPoint = 0;
    Bundle data;
    boolean endPath = false;
    boolean startPath = true;
    boolean playMode = false;

    boolean show;

    private TextView currentAltitude;
    private TextView currentPayload;
    private TextView currentDropAltitude;
    private TextView currentSpeed;
    private TextView currentTimeToTarget;
    private TextView currentDistanceToTarget;

    String waterHeight = "N/A";
    String habitatHeight = "N/A";
    String cdaHeight = "N/A";

    Handler handler;

    int playbackRate = 500;

    Thread thread;

    ArrayList<Marker> intermediatePoints;

    public FlightPathFragment() {
        //Empty constructor
    }

    //Required methods
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(getActivity(),getResources().getString(R.string.mapboxToken));
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                boolean run = (boolean) message.obj;
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_flight_path,parent,false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstance) {
        mapView = (MapView) view.findViewById(R.id.flightPathMapView);
        mapView.onCreate(savedInstance);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;
                //Initialize plane position and data view now that map is loaded
                updateData(waypoints.get(0));
                updatePlane(waypoints.get(0),currentPoint,lastPoint);
            }
        });

        show = false;

        //Get waypoints
        data = getArguments();
        //Retrieve data sent from activity
        try {
            waypoints = (ArrayList<Waypoint>) data.getSerializable("WAYPOINTS");
            //Test data
            //waypoints = populate();
        } catch (NullPointerException e) {
            Log.d(TAG,"Couldn't receive waypoints from main activity");
            getActivity().onBackPressed();
        } catch (ClassCastException e) {
            Log.d(TAG,"Data from main activity in wrong format");
            getActivity().onBackPressed();
        }

        //Set current point to first point;
        currentPoint = 0;
        lastPoint = 0;
        showing = new int[waypoints.size()];
        intermediatePoints = new ArrayList<Marker>();
        Arrays.fill(showing,0);

        points = new ArrayList<LatLng>();

        tb = (TableLayout) view.findViewById(R.id.tableFP);
        forwardButton = (ImageButton) view.findViewById(R.id.flightPathForwardButton);
        backwardsButton = (ImageButton) view.findViewById(R.id.flightPathBackwardsButton);
        playButton = (ImageButton) view.findViewById(R.id.flightPathPlayButton);
        displayButton = (Button) view.findViewById(R.id.displayInchButton);

        altitudeVal = (TextView) view.findViewById(R.id.flightPathAltitudeVal);
        speedVal = (TextView)  view.findViewById(R.id.flightPathSpeedVal);
        //TextView headingVal = (TextView)  view.findViewById(R.id.flightPathHeadingVal);

        waterDropHeightVal = (TextView)  view.findViewById(R.id.waterDropHeightVal);
        habitatDropHeightVal = (TextView)  view.findViewById(R.id.habitatDropHeightVal);
        gliderDropHeightVal  = (TextView)  view.findViewById(R.id.gliderDropHeightVal);

        rollVal = (TextView)  view.findViewById(R.id.flightPathRollVal);
        pitchVal = (TextView)  view.findViewById(R.id.flightPathPitchVal);
        yawVal = (TextView)  view.findViewById(R.id.flightPathYawVal);

        fpWater = (TextView)  view.findViewById(R.id.fpWater);
        fpWater.setVisibility(View.INVISIBLE);
        fpWaterVal = (TextView)  view.findViewById(R.id.fpWaterDropVal);
        fpWaterVal.setVisibility(View.INVISIBLE);
        fpHabitat = (TextView)  view.findViewById(R.id.fpHabitat);
        fpHabitat.setVisibility(View.INVISIBLE);
        fpHabitatVal = (TextView)  view.findViewById(R.id.fpHabitatDropVal);
        fpHabitatVal.setVisibility(View.INVISIBLE);
        fpCDA = (TextView)  view.findViewById(R.id.fpCDA);
        fpCDA.setVisibility(View.INVISIBLE);
        fpCDAVal = (TextView)  view.findViewById(R.id.fpCDADropVal);
        fpCDAVal.setVisibility(View.INVISIBLE);

        factory = IconFactory.getInstance(getActivity());
        icon = factory.fromResource(R.drawable.ic_plane).getBitmap();

        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                forward();
            }
        });
        backwardsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                backward();
            }
        });
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                play();
            }
        });

        displayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {

                if(show)
                {
                    tb.setVisibility(View.INVISIBLE);
                    fpWater.setVisibility(View.VISIBLE);
                    fpWaterVal.setVisibility(View.VISIBLE);
                    fpHabitat.setVisibility(View.VISIBLE);
                    fpHabitatVal.setVisibility(View.VISIBLE);
                    fpCDA.setVisibility(View.VISIBLE);
                    fpCDAVal.setVisibility(View.VISIBLE);

                    fpWaterVal.setText(waterHeight);
                    fpHabitatVal.setText(habitatHeight);
                    fpCDAVal.setText(cdaHeight);

                    altitudeVal.setVisibility(View.INVISIBLE);
                    speedVal.setVisibility(View.INVISIBLE);;
                    //TextView headingVal = (TextView)  view.findViewById(R.id.flightPathHeadingVal);

                    waterDropHeightVal.setVisibility(View.INVISIBLE);
                    habitatDropHeightVal.setVisibility(View.INVISIBLE);
                    gliderDropHeightVal.setVisibility(View.INVISIBLE);

                    rollVal.setVisibility(View.INVISIBLE);
                    pitchVal.setVisibility(View.INVISIBLE);
                    yawVal.setVisibility(View.INVISIBLE);
                }
                else
                {
                    tb.setVisibility(View.VISIBLE);
                    fpWater.setVisibility(View.INVISIBLE);
                    fpWaterVal.setVisibility(View.INVISIBLE);
                    fpHabitat.setVisibility(View.INVISIBLE);
                    fpHabitatVal.setVisibility(View.INVISIBLE);
                    fpCDA.setVisibility(View.INVISIBLE);
                    fpCDAVal.setVisibility(View.INVISIBLE);

                    altitudeVal.setVisibility(View.VISIBLE);
                    speedVal.setVisibility(View.VISIBLE);
                    //TextView headingVal = (TextView)  view.findViewById(R.id.flightPathHeadingVal);

                    waterDropHeightVal.setVisibility(View.VISIBLE);
                    habitatDropHeightVal.setVisibility(View.VISIBLE);
                    gliderDropHeightVal.setVisibility(View.VISIBLE);

                    rollVal.setVisibility(View.VISIBLE);
                    pitchVal.setVisibility(View.VISIBLE);
                    yawVal.setVisibility(View.VISIBLE);
                }


                show = !show;
            }
        });


    }

    public void updateData(Waypoint point) {
        //Initialize textview elements
        TextView altitudeVal = (TextView) getView().findViewById(R.id.flightPathAltitudeVal);
        TextView speedVal = (TextView)  getView().findViewById(R.id.flightPathSpeedVal);
        //TextView headingVal = (TextView)  getView().findViewById(R.id.flightPathHeadingVal);

        TextView waterDropHeightVal = (TextView)  getView().findViewById(R.id.waterDropHeightVal);
        TextView habitatDropHeightVal = (TextView)  getView().findViewById(R.id.habitatDropHeightVal);
        TextView gliderDropHeightVal  = (TextView)  getView().findViewById(R.id.gliderDropHeightVal);

        TextView rollVal = (TextView)  getView().findViewById(R.id.flightPathRollVal);
        TextView pitchVal = (TextView)  getView().findViewById(R.id.flightPathPitchVal);
        TextView yawVal = (TextView)  getView().findViewById(R.id.flightPathYawVal);

        //TextView locationVal = (TextView) getView().findViewById(R.id.flightPathLocationVal);

        //Update textviews for current point
        altitudeVal.setText(getString(R.string.flightPathAltitudeFormatString,point.getAltitude()));
        speedVal.setText(getString(R.string.flightPathSpeedFormatString,point.getSpeed()));
        //headingVal.setText(getString(R.string.flightPathHeadingFormatString,point.getHeading()));

        if(point.getWaterDrop() > 0)
        {
            waterDropHeightVal .setText(getString(R.string.flightPathAltitudeFormatString, point.getWaterDrop()));
        }

        if(point.getHabitatDrop() > 0)
        {
            habitatDropHeightVal .setText(getString(R.string.flightPathAltitudeFormatString, point.getHabitatDrop()));
        }

        if(point.getGliderDrop() > 0)
        {
            gliderDropHeightVal .setText(getString(R.string.flightPathAltitudeFormatString, point.getGliderDrop()));
        }


        rollVal.setText(getString(R.string.flightPathRollFormatString,point.getRoll()));
        pitchVal.setText(getString(R.string.flightPathPitchFormatString,point.getPitch()));
        yawVal.setText(getString(R.string.flightPathYawFormatString,point.getYaw()));
        //locationVal.setText(point.getLocation());
    }

    public void play() {
        if (playMode) {
            playButton.setImageResource(R.drawable.ic_play);
            playMode = false;
            thread.interrupt();
        }
        else {
            points.clear();
            //Remove intermediate points (marked by black circles)
            Iterator<Marker> removePoints = intermediatePoints.iterator();
            while (removePoints.hasNext()) {
                Marker temp = removePoints.next();
                temp.remove();
                removePoints.remove();
            }
            //Resetting path
            showing = new int[waypoints.size()];
            Arrays.fill(showing,0);
            updateData(waypoints.get(0));
            currentPoint = 0;
            lastPoint = 0;
            endPath = false;
            playButton.setImageResource(R.drawable.ic_pause_symbol);
            playMode = true;
            thread = new playThread();
            thread.start();
        }
    }

    public void forward() {
        if (currentPoint == waypoints.size()-1) {
            endPath = true;
        }
        if (currentPoint < waypoints.size()-1) {
            currentPoint += 1;
            updateData(waypoints.get(currentPoint));
            updatePlane(waypoints.get(currentPoint),lastPoint, currentPoint);
            updatePath(lastPoint,currentPoint);
            lastPoint = currentPoint;
        }
        else {
            Toast.makeText(getActivity(),"End of path reached",Toast.LENGTH_SHORT).show();
        }
    }

    //move marker backwards
    public void backward() {
        if (currentPoint == 0) {
            startPath = true;
        }
        if (currentPoint > 0) {
            currentPoint -= 1;
            updateData(waypoints.get(currentPoint));
            updatePlane(waypoints.get(currentPoint),lastPoint,currentPoint);
            updatePath(lastPoint,currentPoint);
            lastPoint = currentPoint;
        } else {
            Toast.makeText(getActivity(),"Start of path reached",Toast.LENGTH_SHORT).show();
        }
    }

    //Update plane marker
    public void updatePlane(Waypoint point,int last,int current) {
        if (planeMarker != null) {
            planeMarker.remove();
        }
        String locat = point.getLocation();
        LatLng location = convertToLatLng(locat);
        Matrix matrix = new Matrix();
        matrix.postRotate((float)point.getYaw());
        Bitmap rotatedBitmap = Bitmap.createBitmap(icon,0,0,icon.getWidth(),icon.getHeight(),matrix,true);
        rotatedBitmap = Bitmap.createScaledBitmap(rotatedBitmap,40,50,false);
        planeMarker = map.addMarker(new MarkerOptions().position(location).icon(factory.fromBitmap(rotatedBitmap)));
    }

    //Convert string location to LatLng
    public LatLng convertToLatLng(String locat) {
        Log.d("LatLngCnv", locat);
        String[] split = locat.split(",");
        double latitude = Double.parseDouble(split[0].substring(17,27));
        double longitude = Double.parseDouble(split[1].substring(11,21));
        LatLng location = new LatLng(latitude,longitude);
        return location;
    }

    //Update path for all previous points in case points were skipped
    public void updatePath(int last, int current) {
        if (path != null) {
            path.remove();
        }
        if (current > last) {
            for (int i = last; i <= current; i++) {
                if (showing[i] == 0) {
                    int showVal = 0;
                    if (points.size() > 2 && (points.size() % 10 ==0)) {
                        Bitmap circle = factory.fromResource(R.drawable.black_circle).getBitmap();
                        circle = Bitmap.createScaledBitmap(circle,10,10,false);
                        lastPosition = map.addMarker(new MarkerOptions().position(points.get(points.size()-2)).icon(factory.fromBitmap(circle)));
                        intermediatePoints.add(lastPosition);
                        showVal += 1;
                    }
                    Waypoint point = waypoints.get(i);
                    LatLng location = convertToLatLng(point.getLocation());
                    points.add(location);
                    showing[i] = showVal + 1;

                    if(point.getWaterDrop() != 0 )
                    {
                        Marker m = map.addMarker(new MarkerOptions()
                                .title("Water")
                                .position(location));
                        waterHeight = String.valueOf((int)point.getWaterDrop()) + " ft";
                    }

                    if(point.getHabitatDrop() != 0 )
                    {
                        Marker mm = map.addMarker(new MarkerOptions()
                                .title("Habitat")
                                .position(location));
                        habitatHeight = String.valueOf((int)point.getHabitatDrop()) + " ft";
                    }

                    if(point.getGliderDrop() != 0 )
                    {
                        Marker mmm = map.addMarker(new MarkerOptions()
                                .title("Glider")
                                .position(location));
                        String.valueOf((int)point.getAltitude());
                        cdaHeight = String.valueOf((int)point.getGliderDrop()) + " ft";
                    }
                }
            }
        }
        else {
            for (int i = current + 1 ; i <= last; i++) {
                if (showing[i] == 1) {
                    showing[i] = 0;
                }
                 if (showing[i] == 2) {
                    map.removeMarker(lastPosition);
                    showing[i] = 0;
                }
                points.remove(i);
            }
        }
        path = map.addPolyline(new PolylineOptions().addAll(points).color(getResources().getColor(R.color.colorSecondary)).width(3));
    }

    //Thread implements delay in between updating the position of the marker
    //Actual UI changes are made on the UI thread since only the main activity thread can access the UI
    private class playThread extends Thread {
        private static final String TAG = "Play Thread";
        @Override
        public void run() {
            Log.d(TAG,"Running thread");
            //Run forward until the end of the path is reached
            while (!endPath) {
                try {
                    sleep(playbackRate);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            forward();
                        }
                    });
                }catch (InterruptedException e) {
                    return;
                }

            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        if (thread != null) {
            thread.interrupt();
        }
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        if (thread != null) {
            thread.interrupt();
        }
        Toast.makeText(getActivity(),"Memory resources low, stopping playback",Toast.LENGTH_SHORT).show();
        super.onLowMemory();
    }

    @Override
    public void onDestroy() {
        if (thread != null) {
            thread.interrupt();
        }
        super.onDestroy();
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        this.map = mapboxMap;
    }

    public MapboxMap getMap() {
        if (map != null) {
            return this.map;
        }
        else {
            return null;
        }
    }

    //Close current fragment on back press
    //Status tab should always be closed


}

