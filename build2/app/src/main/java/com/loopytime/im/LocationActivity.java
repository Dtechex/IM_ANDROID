package com.loopytime.im;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;


/**
 * Created by hitasoft on 25/6/18.
 */

public class LocationActivity extends BaseActivity implements View.OnClickListener, AdapterView.OnItemClickListener, TextWatcher,
        OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    final String TAG = "LocationActivity";
    /**
     * Declare Layout Elements
     **/
    MapView mapView;
    GoogleMap googleMap;
    TextView title, setLoc, apply;
    ImageView backbtn, cancelbtn, myLocation;
    AutoCompleteTextView address;
    Display display;
    RelativeLayout searchLay;
    ProgressDialog dialog;

    /**
     * Declare Varaibles
     **/
    final static int REQUEST_CHECK_SETTINGS_GPS = 0x1, REQUEST_ID_MULTIPLE_PERMISSIONS = 0x2;
    double lat, lon;
    String from = "";
    InputMethodManager imm;
    GoogleApiClient googleApiClient;
    LatLng center;
    Location mylocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Locale.setDefault(getResources().getConfiguration().locale);
        setContentView(R.layout.location_activity);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        backbtn = findViewById(R.id.backbtn);
        title = findViewById(R.id.title);
        setLoc = findViewById(R.id.apply);
        address = findViewById(R.id.address);
        cancelbtn = findViewById(R.id.cancelbtn);
        myLocation = findViewById(R.id.my_location);
        searchLay = findViewById(R.id.searchLay);
        apply = findViewById(R.id.apply);

        if(ApplicationClass.isRTL()){
            backbtn.setRotation(180);
        } else {
            backbtn.setRotation(0);
        }

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        display = this.getWindowManager().getDefaultDisplay();

        dialog = new ProgressDialog(LocationActivity.this);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        from = getIntent().getExtras().getString("from");
        if (from.equals("share")) {
            apply.setVisibility(View.VISIBLE);
        } else {
            apply.setVisibility(View.GONE);
            String sLat = getIntent().getExtras().getString("lat");
            String sLon = getIntent().getExtras().getString("lon");
            if (sLat != null && sLon != null) {
                lat = Double.parseDouble(sLat);
                lon = Double.parseDouble(sLon);
            }
            Log.v("lat", "lat==" + sLat);
            Log.v("lon", "lon==" + sLon);
        }

        int permissionLocation = ContextCompat.checkSelfPermission(LocationActivity.this,
                ACCESS_FINE_LOCATION);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(ACCESS_FINE_LOCATION);
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this,
                        listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            }
        } else {
            loadData();
        }

        backbtn.setVisibility(View.VISIBLE);
        title.setVisibility(View.VISIBLE);
        cancelbtn.setVisibility(View.GONE);
        backbtn.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.primarytext));

        title.setText(getString(R.string.location));
        backbtn.setOnClickListener(this);
        setLoc.setOnClickListener(this);
        cancelbtn.setOnClickListener(this);
        address.setOnItemClickListener(this);
        myLocation.setOnClickListener(this);
        apply.setOnClickListener(this);
        address.addTextChangedListener(this);

        // address.setAdapter(new PlacesAutoCompleteAdapter(LocationActivity.this, R.layout.dropdown_layout));

        // Gets to GoogleMap from the MapView and does initialization stuff
        mapView.getMapAsync(this);

        // Updates the location and zoom of the MapView
        //  address.setDropDownWidth(display.getWidth() - JoysaleApplication.dpToPx(this, 30));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.v("requestCode", "requestCode=" + requestCode);
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS:
                int permissionLocation = ContextCompat.checkSelfPermission(LocationActivity.this,
                        ACCESS_FINE_LOCATION);
                if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
                    loadData();
                } else {
                    makeToast(getString(R.string.location_permission_error));
                    finish();
                }
                break;
        }
    }

    @Override
    public void onNetworkChange(boolean isConnected) {

    }

    @Override
    public void onMapReady(final GoogleMap map) {
        Log.v(TAG, "map=" + map);
        googleMap = map;
        if (map != null) {
            map.getUiSettings().setMyLocationButtonEnabled(false);
            if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            } else {
                map.setMyLocationEnabled(true);
            }

            if (lat == 0 && lon == 0) {
                if (googleApiClient == null) {
                    setUpGClient();
                } else if (mylocation == null) {
                    getMyLocation();
                } else {
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(mylocation.getLatitude(), mylocation.getLongitude()), 15);
                    map.animateCamera(cameraUpdate);
                }
            } else {
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 15);
                map.animateCamera(cameraUpdate);
            }

            map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {

                @Override
                public void onCameraChange(CameraPosition arg0) {
                    // TODO Auto-generated method stub
                    center = map.getCameraPosition().target;
                    Log.v(TAG, "center-latitude=" + center.latitude + " &center-longitude=" + center.longitude);
                    lat = center.latitude;
                    lon = center.longitude;
                    map.clear();
                }
            });

            address.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        try {
                            imm.hideSoftInputFromWindow(address.getWindowToken(), 0);
                            Double latn[] = new Double[2];
                            if (address.getText().toString().trim().length() != 0) {
//                                latn = new GetLocationFromString().execute(address.getText().toString().trim()).get();
                                latn = getGeocodeLocation(address.getText().toString().trim());
                                double lat = latn[0];
                                double lon = latn[1];

                                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lon), 15);
                                map.animateCamera(cameraUpdate);
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        return true;
                    }
                    return false;
                }
            });
        }

        /*View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
        locationButton.setBackgroundResource(R.drawable.my_location);
        // and next place it, for exemple, on bottom right (as Google Maps loopytime)
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
        // position on right bottom
        rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
        rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        rlp.setMargins(0, 0, 30, 30);*/

        // Needs to call MapsInitializer before doing any CameraUpdateFactory calls
        try {
            MapsInitializer.initialize(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Function for get the lat, lon from gps
     **/

    private void loadData() {
        if (lat == 0 && lon == 0) {
            if (googleApiClient == null) {
                setUpGClient();
            } else if (mylocation == null) {
                getMyLocation();
            } else {
                lat = mylocation.getLatitude();
                lon = mylocation.getLongitude();
                Log.v("lat&lon", "lat = " + lat + "&lon=" + lon);
            }
        } else {

        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s.length() > 0) {
            cancelbtn.setVisibility(View.VISIBLE);
        } else {
            cancelbtn.setVisibility(View.GONE);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    private synchronized void setUpGClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    private void checkPermissions() {
        int permissionLocation = ContextCompat.checkSelfPermission(LocationActivity.this,
                ACCESS_FINE_LOCATION);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(ACCESS_FINE_LOCATION);
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this,
                        listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            }
        } else {
            getMyLocation();
        }

    }

    @Override
    public void onLocationChanged(Location location) {
        mylocation = location;
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new
                LatLng(mylocation.getLatitude(), mylocation.getLongitude()), 15));
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        checkPermissions();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private void getMyLocation() {
        if (googleApiClient != null) {
            if (googleApiClient.isConnected()) {
                int permissionLocation = ContextCompat.checkSelfPermission(LocationActivity.this,
                        ACCESS_FINE_LOCATION);
                if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
                    mylocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                    LocationRequest locationRequest = new LocationRequest();
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                            .addLocationRequest(locationRequest);
                    builder.setAlwaysShow(true);
                    LocationServices.FusedLocationApi
                            .requestLocationUpdates(googleApiClient, locationRequest, LocationActivity.this);
                    PendingResult result =
                            LocationServices.SettingsApi
                                    .checkLocationSettings(googleApiClient, builder.build());
                    result.setResultCallback(new ResultCallback<LocationSettingsResult>() {

                        @Override
                        public void onResult(LocationSettingsResult result) {
                            final Status status = result.getStatus();
                            switch (status.getStatusCode()) {
                                case LocationSettingsStatusCodes.SUCCESS:
                                    // All location settings are satisfied.
                                    // You can initialize location requests here.
                                    int permissionLocation = ContextCompat
                                            .checkSelfPermission(LocationActivity.this,
                                                    ACCESS_FINE_LOCATION);
                                    if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
                                        mylocation = LocationServices.FusedLocationApi
                                                .getLastLocation(googleApiClient);
                                        if (mylocation != null) {
                                            lat = mylocation.getLatitude();
                                            lon = mylocation.getLongitude();
                                        }
                                        Log.v("mylocation", "mylocation=" + mylocation);
                                    }
                                    break;
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    // Location settings are not satisfied.
                                    // But could be fixed by showing the user a dialog.
                                    try {
                                        // Show the dialog by calling startResolutionForResult(),
                                        // and check the result in onActivityResult().
                                        // Ask to turn on GPS automatically
                                        status.startResolutionForResult(LocationActivity.this,
                                                REQUEST_CHECK_SETTINGS_GPS);
                                    } catch (IntentSender.SendIntentException e) {
                                        // Ignore the error.
                                    }
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    // Location settings are not satisfied. However, we have no way to fix the
                                    // settings so we won't show the dialog.
                                    //finish();
                                    break;
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "onActivityResult");
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS_GPS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        getMyLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        cancelbtn.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onPause() {
        // chat.disconnect();
        super.onPause();
    }

    /**
     * Class for get the lat, lon from address
     **/

    class GetLocationFromString extends AsyncTask<String, Void, Double[]> {

        @Override
        protected Double[] doInBackground(String... params) {
            final Double latn[] = new Double[2];
            HttpURLConnection conn = null;
            StringBuilder jsonResults = new StringBuilder();
            try {
                StringBuilder sb = new StringBuilder("http://maps.google.com/maps/api/geocode/json");
                sb.append("?address=" + URLEncoder.encode(params[0], "utf8"));
                sb.append("&ka&sensor=false");
                sb.append("&language=" + getResources().getConfiguration().locale.getLanguage());
                sb.append("&key=" + getString(R.string.google_api_key));
                URL url = new URL(sb.toString());

                Log.v("MAP URL", "" + url);

                conn = (HttpURLConnection) url.openConnection();
                InputStreamReader in = new InputStreamReader(conn.getInputStream());

                // Load the results into a StringBuilder
                int read;
                char[] buff = new char[1024];
                while ((read = in.read(buff)) != -1) {
                    jsonResults.append(buff, 0, read);
                }
            } catch (MalformedURLException e) {
                Log.e("Error", "Error processing Places API URL", e);
                return latn;
            } catch (IOException e) {
                Log.e("Error", "Error connecting to Places API", e);
                return latn;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            try {
                JSONObject jsonObject = new JSONObject(jsonResults.toString());
                Log.v("jsonObject", "jsonObject=" + jsonObject);
                latn[1] = ((JSONArray) jsonObject.get("results")).getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location")
                        .getDouble("lng");
                latn[0] = ((JSONArray) jsonObject.get("results")).getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location")
                        .getDouble("lat");
                Log.v("lat & lon", " lat = " + latn[0] + " &lon = " + latn[1]);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return latn;
        }
    }

    private Double[] getGeocodeLocation(String params) {
        final Double latn[] = new Double[2];
        Geocoder gc = new Geocoder(getApplicationContext());
        if (Geocoder.isPresent()) {
            List<Address> list = new ArrayList<>();
            try {
                list = gc.getFromLocationName(params, 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (list.size() > 0) {
                Address address = list.get(0);
                latn[0] = address.getLatitude();
                latn[1] = address.getLongitude();
            }

            return latn;
        } else {
            Toast.makeText(getApplicationContext(), "No Location found", Toast.LENGTH_LONG).show();
            return latn;
        }
    }

    /**
     * Function for Onclick Events
     */

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.backbtn:
                finish();
                break;
            case R.id.apply:
                // new GetLocationAsync(center.latitude, center.longitude).execute().get();
                center = googleMap.getCameraPosition().target;
                lat = center.latitude;
                lon = center.longitude;
                if (lat == 0.0 || lon == 0.0) {
                    makeToast("Location not updated");
                } else {
                    Intent i = new Intent(LocationActivity.this, ChatActivity.class);
                    i.putExtra("lat", String.valueOf(lat));
                    i.putExtra("lon", String.valueOf(lon));
                    setResult(RESULT_OK, i);
                    finish();
                }
                break;
            case R.id.cancelbtn:
                address.setText("");
                cancelbtn.setVisibility(View.GONE);
                break;
            case R.id.my_location:
                if (googleApiClient == null) {
                    setUpGClient();
                } else if (mylocation == null) {
                    getMyLocation();
                } else {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new
                            LatLng(mylocation.getLatitude(), mylocation.getLongitude()), 15));
                }
                break;
        }
    }
}
