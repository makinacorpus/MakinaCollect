package com.makina.collect.android.activities;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.makina.collect.android.R;
import com.makina.collect.android.dialog.DialogAboutUs;
import com.makina.collect.android.dialog.DialogExit;
import com.makina.collect.android.preferences.ActivityPreferences;
import com.makina.collect.android.theme.Theme;
import com.makina.collect.android.utilities.InfoLogger;
import com.makina.collect.android.utilities.StaticMethods;
import com.makina.collect.android.widgets.GeoPointWidget;

/**
 * GeoPointMapActivity is responsible for displaying the map used to get or
 * display a geopoint. It uses online google maps or mbTiles stored in
 * Android/data/com.makina.collect, depending on application settings.
 * 
 * @author Guillaume Salmon (guillaume.salmon.ext@makina-corpus.com)
 */

public class ActivityGeoPointMap extends SherlockFragmentActivity implements
		LocationListener, OnMarkerDragListener, OnMapLongClickListener {

	private static final String LOCATION_COUNT = "locationCount";


	private LocationManager mLocationManager;
	private GoogleMap mMap;
	private MarkerOptions mMarkerOption;
	private Marker mMarker = null;

	private Location mLocation;
	private Button mAcceptLocation;
	private Button mRefreshLocation;
	private LatLng mLatLng;

	private boolean mCaptureLocation = true;
	private boolean mIsDragged = false;
	private boolean mGPSOn = false;
	private boolean mNetworkOn = false;

	private double mLocationAccuracy;
	private int mLocationCount = 0;
	private final int RESULT_PREFERENCES=1;
	
    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getSupportMenuInflater();
        menu.clear();
        inflater.inflate(R.menu.menu_activity_dashboard, menu);
        
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
            // The action bar home/up action should open or close the drawer.
            // ActionBarDrawerToggle will take care of this.
            // Handle action buttons for all fragments
            switch (item.getItemId()) {
            case android.R.id.home:
            	finish();
            	return true;
            case R.id.menu_settings:
                    startActivityForResult((new Intent(this, ActivityPreferences.class)),RESULT_PREFERENCES);
                    return true;
             case R.id.menu_help:
                    Intent mIntent=new Intent(this, ActivityHelp.class);
            Bundle mBundle=new Bundle();
            mBundle.putInt("position", 1);
            mIntent.putExtras(mBundle);
            startActivity(mIntent);
                     return true;
            case R.id.menu_about_us:
                    DialogAboutUs.aboutUs(this);
                    return true;
            case R.id.menu_exit:
                   	DialogExit.show(this);
                    return true;
            default:
                    return super.onOptionsItemSelected(item);
            }
    }
    

    /*private void startPreloaderAnimation()
	{
		setSupportProgress(Window.PROGRESS_END);
		setSupportProgressBarIndeterminateVisibility(true);
	}

	private void stopPreloaderAnimation() {
		setSupportProgressBarIndeterminateVisibility(false);
	}*/
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		if (savedInstanceState != null) {
			mLocationCount = savedInstanceState.getInt(LOCATION_COUNT);
		}

		getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.actionbar_title_layout_edit_form, null);
        getSupportActionBar().setCustomView(v);
        
        //startPreloaderAnimation();
		boolean withLoc = true;

		//requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_geopoint);

		Intent intent = getIntent();

		mLocationAccuracy = GeoPointWidget.DEFAULT_LOCATION_ACCURACY;

		/* Set up the map and the marker */
		mMarkerOption = new MarkerOptions();
		mMap = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map)).getMap();
		mMap.setOnMarkerDragListener(this);
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
				0, 0), 2));

		mMap.setOnMapLongClickListener(this);
		if (intent != null && intent.getExtras() != null) {

			
			// No geolocation
			if (intent.hasExtra("noGPS")) {
				withLoc = false;
				mCaptureLocation = false;
			}

			// Show previous location
			if (intent.hasExtra(GeoPointWidget.LOCATION)) {
				//stopPreloaderAnimation();
				double[] location = intent
						.getDoubleArrayExtra(GeoPointWidget.LOCATION);
				mLatLng = new LatLng(location[0], location[1]);
				mMarkerOption.position(mLatLng);
				mMarker = mMap.addMarker(mMarkerOption);
				mMarker.setDraggable(true);
				mCaptureLocation = false;
				mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng,16));
			}


			// Get accuracy.
			if (intent.hasExtra(GeoPointWidget.ACCURACY_THRESHOLD)) {
				mLocationAccuracy = intent.getDoubleExtra(
						GeoPointWidget.ACCURACY_THRESHOLD,
						GeoPointWidget.DEFAULT_LOCATION_ACCURACY);
			}
		}

		// Use providers only if we want geolocation
		if (mCaptureLocation || withLoc) {

			// make sure we have a good location provider before continuing
			mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

			ConnectivityManager connectivityMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

			NetworkInfo[] nwInfos = connectivityMgr.getAllNetworkInfo();
			mNetworkOn = false;
			for (NetworkInfo nwInfo : nwInfos) {
				if (nwInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
					mNetworkOn = nwInfo.isAvailable();
				}
			}

			List<String> providers = mLocationManager.getProviders(true);
			for (String provider : providers) {
				if (provider.equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
					mGPSOn = true;
				}
			}
			if (!mGPSOn && !mNetworkOn) {
				Toast.makeText(getBaseContext(),
						getString(R.string.provider_disabled_error),
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}

		// Handles toasts to display when showing the map
		if (!mCaptureLocation && withLoc) {
			Toast.makeText(getApplicationContext(), R.string.marker_draggable,
					Toast.LENGTH_LONG).show();
		}
		if (!mCaptureLocation && !withLoc) {
			Toast.makeText(getApplicationContext(), R.string.marker_create,
					Toast.LENGTH_LONG).show();
		}

		mAcceptLocation = (Button) findViewById(R.id.accept_location);
		mAcceptLocation.setVisibility(View.VISIBLE);
		mAcceptLocation.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				returnResult();
			}
		});

		// Use GPS or network, depending on what's available

		if (mGPSOn) {
			Location loc = mLocationManager
					.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (loc != null) {
				InfoLogger.geolog("GeoPointMapActivity: "
						+ System.currentTimeMillis()
						+ " lastKnownLocation(GPS) lat: " + loc.getLatitude()
						+ " long: " + loc.getLongitude() + " acc: "
						+ loc.getAccuracy());
			} else {
				InfoLogger.geolog("GeoPointMapActivity: "
						+ System.currentTimeMillis()
						+ " lastKnownLocation(GPS) null location");
			}
		}

		if (mNetworkOn) {
			Location loc = mLocationManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (loc != null) {
				InfoLogger.geolog("GeoPointMapActivity: "
						+ System.currentTimeMillis()
						+ " lastKnownLocation(Network) lat: "
						+ loc.getLatitude() + " long: " + loc.getLongitude()
						+ " acc: " + loc.getAccuracy());
			} else {
				InfoLogger.geolog("GeoPointMapActivity: "
						+ System.currentTimeMillis()
						+ " lastKnownLocation(Network) null location");
			}
		}

		mRefreshLocation = ((Button) findViewById(R.id.refresh_location));
		mRefreshLocation.setVisibility(View.VISIBLE);
		mRefreshLocation.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getIntent().removeExtra(GeoPointWidget.LOCATION);
				onResume();
				if (mMarker!=null)
					mMarker.setDraggable(false);
			}
		});
		
	}

	// Used to return the location if we used the gps/network
	private void returnLocation() {
		if (mLocation != null) {
			Log.i(getClass().getName(),
					"returnLocation Lat : " + mLocation.getLatitude()
							+ " Long : " + mLocation.getLongitude() + " Alt : "
							+ mLocation.getAltitude() + " Acc : "
							+ mLocation.getAccuracy());
			Intent i = new Intent();
			i.putExtra(
					StaticMethods.LOCATION_RESULT,
					mLocation.getLatitude() + " " + mLocation.getLongitude()
							+ " " + mLocation.getAltitude() + " "
							+ mLocation.getAccuracy());
			setResult(RESULT_OK, i);
			
		}
		finish();
	}

	// Used to return the location if the user set a location himself with a
	// marker
	private void returnDragLocation() {
		Log.i(getClass().getName(), "returnDragLocation Lat : "
				+ mLatLng.latitude + " Long : " + mLatLng.longitude + " Alt : "
				+ 0 + " Acc : " + 0);
		Intent i = new Intent();
		i.putExtra(StaticMethods.LOCATION_RESULT, mLatLng.latitude + " "
				+ mLatLng.longitude + " " + 0 + " " + 0);
		setResult(RESULT_OK, i);
		finish();
	}


	@Override
	protected void onPause() {
		super.onPause();
		if (mLocationManager != null) {
			mLocationManager.removeUpdates(this);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mGPSOn) {
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 0, 0, this);
		}
		if (mNetworkOn) {
			mLocationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 0, 0, this);
		}

	}

	@Override
	public void onLocationChanged(Location location) {
		if (mCaptureLocation) {
			mLocation = location;
			if (mLocation != null) {
				// Bug report: cached GeoPoint is being returned as the first
				// value.
				// Wait for the 2nd value to be returned, which is hopefully not
				// cached?
				++mLocationCount;
				InfoLogger.geolog("GeoPointMapActivity: "
						+ System.currentTimeMillis() + " onLocationChanged("
						+ mLocationCount + ") lat: " + mLocation.getLatitude()
						+ " long: " + mLocation.getLongitude() + " acc: "
						+ mLocation.getAccuracy());
				if (mLocationCount > 1) {
					mLatLng = new LatLng(mLocation.getLatitude(),
							mLocation.getLongitude());
					mMap.animateCamera(CameraUpdateFactory.newLatLng(mLatLng));

					if (mLocation.getAccuracy() <= mLocationAccuracy) {
						// If the location is accurate enough, stop updating
						// location and allow user to drag the marker on the map
						Toast.makeText(getApplicationContext(),
								R.string.marker_draggable, Toast.LENGTH_LONG)
								.show();
						mLocationManager.removeUpdates(this);
						mRefreshLocation.setClickable(true);
						mMarker.setDraggable(true);
						mLatLng = new LatLng(mLocation.getLatitude(),
								mLocation.getLongitude());
					}
				}
				mLatLng = new LatLng(mLocation.getLatitude(),
						mLocation.getLongitude());

				// create a marker on the map or move the existing marker to the
				// new location
				if (mMarker == null) {
					mMarkerOption.position(mLatLng);
					mMarker = mMap.addMarker(mMarkerOption);
				} else {
					mMarker.setPosition(mLatLng);
				}
				mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng,
						16));
				
				//stopPreloaderAnimation();
			} else {
				InfoLogger.geolog("GeoPointMapActivity: "
						+ System.currentTimeMillis() + " onLocationChanged("
						+ mLocationCount + ") null location");
			}
		} else {
			Log.w(getClass().getName(),
					"onLocationChanged() : mCaptureLocation is "
							+ mCaptureLocation);
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onMarkerDrag(Marker marker) {
	}

	@Override
	public void onMarkerDragEnd(Marker marker) {
		// Allows the user to save a custom location by dragging the marker
		mLatLng = marker.getPosition();
		mAcceptLocation.setClickable(true);
		mIsDragged = true;
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, mMap.getCameraPosition().zoom));
	}

	@Override
	public void onMarkerDragStart(Marker marker) {
	}

	private void returnResult() {
		if (mIsDragged) {
			returnDragLocation();
		} else {
			returnLocation();
		}
	}

	@Override
	public void onMapLongClick(LatLng point) {
		// Allows the user to create or move a marker by long-clicking on the
		// map
		mMarkerOption.position(point);
		mLatLng = point;
		if (mMarker != null) {
			mMarker.remove();
		} else {
			Toast.makeText(getApplicationContext(), R.string.marker_draggable,
					Toast.LENGTH_LONG).show();
		}
		mMarker = mMap.addMarker(mMarkerOption);
		mMarker.setDraggable(true);
		mAcceptLocation.setClickable(true);
		mIsDragged = true;
		Log.i(getClass().getName(), "x = " + mLatLng.latitude + " y = "
				+ mLatLng.longitude);
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, mMap.getCameraPosition().zoom));
	}

	@Override
    public void onConfigurationChanged(Configuration newConfig) {
    	// TODO Auto-generated method stub
    	super.onConfigurationChanged(newConfig);
    	Theme.changeTheme(this);
    	LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View v;
    	if(newConfig.orientation==Configuration.ORIENTATION_LANDSCAPE)
    		v = inflator.inflate(R.layout.actionbar_title_layout_edit_form_land, null);
        else
        	v = inflator.inflate(R.layout.actionbar_title_layout_edit_form, null);
        getSupportActionBar().setCustomView(v);
    }
}
