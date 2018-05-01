package com.technexia.user.googlemapsampleapp;

import android.app.ActivityOptions;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.LoginFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.awareness.snapshot.PlacesResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.technexia.user.googlemapsampleapp.model.PlaceInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");

        mMap = googleMap;

        if (mLocationpermissionGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);

            init();
        }
    }

    public static final String TAG = "MapActivity";
    public static final String FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION;
    public static final String COARSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION;

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private Boolean mLocationpermissionGranted = false;
    private AutoCompleteTextView mSearchText;
    private GoogleMap mMap;
    private ImageView ic_gps;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private PlaceAutoCompleteAdapter mPlaceAutoCompleteAdapter;
    private GoogleApiClient mGoogleApiClient;

    public static final float DEFAULT_ZOOM = 15f;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40, -168), new LatLng(71, 136));
    private PlaceInfo mPlace;
    private Marker mMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mSearchText = (AutoCompleteTextView) findViewById(R.id.input_search);
        ic_gps = (ImageView) findViewById(R.id.ic_gps);

        getLocationPermission();
    }

    private void getLocationPermission() {
        Log.d(TAG, "initMap: initializing map");
        String[] permission = {FINE_LOCATION, COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationpermissionGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this, permission, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, permission, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void initMap() {
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(MapActivity.this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationpermissionGranted = false;
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationpermissionGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    mLocationpermissionGranted = true;
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    // initialize map here
                    initMap();
                }
            }
        }
    }


    private void getDeviceLocation() {

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (mLocationpermissionGranted) {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d("OnComplete", "Found Location");
                            Location currentLocation = (Location) task.getResult();

                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM, "My location");
//                            moveCamera(new LatLng(37.422,-122.084) ,DEFAULT_ZOOM);
                        } else {
                            Log.d("In Complete", "Found is Location null");
                            Toast.makeText(MapActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.d("Security Exception", "Exception : " + e.getMessage());
        }
    }
    private void moveCamera(LatLng latLng, float zoom, PlaceInfo placeInfo) {
        Log.d("move Camera", "Lat :  " + latLng.latitude + " Lng : " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        mMap.clear();
        if(placeInfo != null){
            try{
                String snippet = "Address : "+placeInfo.getAddress() + "\n"+
                        "Phone Number : "+placeInfo.getPhoneNumber() + "\n"+
                        "Website : "+placeInfo.getWebsiteUri() + "\n"+
                        "Price Rating : "+placeInfo.getRatting();
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng)
                        .title(placeInfo.getName())
                        .snippet(snippet);
                mMarker = mMap.addMarker(markerOptions);
                mMap.addMarker(markerOptions);geoLocation();
            }catch (NullPointerException e){
                Log.e(TAG,"move camera NullPointerException exe "+e.toString());
            }
        }else{
            mMap.addMarker(new MarkerOptions().position(latLng));
        }

        hideSoftKeyboard();

    }

    private void moveCamera(LatLng latLng, float zoom, String locationTitle) {
        Log.d("move Camera", "Lat :  " + latLng.latitude + " Lng : " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        if (!locationTitle.equalsIgnoreCase("My Location")) {
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(locationTitle);
            mMap.addMarker(options);
        }
        hideSoftKeyboard();

    }

    private void init() {
        Log.d(TAG, "init: initializing");

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();
        mSearchText.setOnItemClickListener(mAutoCompleteListner);
        mPlaceAutoCompleteAdapter = new PlaceAutoCompleteAdapter(this, mGoogleApiClient, LAT_LNG_BOUNDS, null);

        mSearchText.setAdapter(mPlaceAutoCompleteAdapter);

        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_SEARCH
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER) {
                    geoLocation();
                }
                return false;
            }
        });

        ic_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDeviceLocation();
            }
        });
        hideSoftKeyboard();
    }

    private void geoLocation() {
        Log.d(TAG, "geoLocation: geolocating");

        String searchString = mSearchText.getText().toString();

        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString, 1);

        } catch (IOException e) {
            Log.d(TAG, "geoLocation: IOEXCEPTION " + e);
        }
        if (list.size() > 0) {
            Address address = list.get(0);
            Log.d(TAG, "geoLocation: FOUND LOCTION " + address.toString());

            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM,
                    address.getAddressLine(0));
        }
    }

    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private AdapterView.OnItemClickListener mAutoCompleteListner = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            hideSoftKeyboard();

            final AutocompletePrediction item = mPlaceAutoCompleteAdapter.getItem(i);
            final String placeId = item.getPlaceId();

            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.d(TAG, "onResult: Place query did not complete successfully     " + places.getStatus().toString());
                places.release();
                return;
            }

            final Place place = places.get(0);
            try {
                mPlace = new PlaceInfo(place.getName().toString(), place.getAddress().toString(), place.getPhoneNumber().toString(),
                        place.getId().toString(), place.getWebsiteUri(), place.getLatLng(), place.getRating(), place.getAttributions().toString());
                Log.d(TAG, "onResult: Places details : " + mPlace);
            } catch (NullPointerException e) {
                Log.e(TAG, "onResult: NullPointerException" + e.toString());
            }

            moveCamera(new
                    LatLng(place.getViewport().getCenter().latitude,place.getViewport().getCenter().longitude),
                    DEFAULT_ZOOM,mPlace);
            places.release();
        }
    };
}
