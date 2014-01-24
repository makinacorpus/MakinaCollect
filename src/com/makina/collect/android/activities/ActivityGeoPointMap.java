
package com.makina.collect.android.activities;

import java.text.DecimalFormat;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
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
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.makina.collect.android.R;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.provider.MapBoxOfflineTileProvider;
import com.makina.collect.android.utilities.InfoLogger;
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

	private TextView mLocationStatus;

	private LocationManager mLocationManager;
	private GoogleMap mMap;
	private MarkerOptions mMarkerOption;
	private Marker mMarker = null;

	private Location mLocation;
	private Button mAcceptLocation;
	private Button mCancelLocation;
	private Button mRefreshLocation;
	private LatLng mLatLng;

	private boolean mCaptureLocation = true;
	private boolean mIsDragged = false;
	private Button mShowLocation;

	private boolean mGPSOn = false;
	private boolean mNetworkOn = false;

	private double mLocationAccuracy;
	private int mLocationCount = 0;

	
    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getSupportMenuInflater();
        menu.clear();
        
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       switch(item.getItemId())
       {
       		case android.R.id.home:
        	finish();
        	return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void startPreloaderAnimation()
	{
		setSupportProgress(Window.PROGRESS_END);
		setSupportProgressBarIndeterminateVisibility(true);
	}

	private void stopPreloaderAnimation() {
		setSupportProgressBarIndeterminateVisibility(false);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		if (savedInstanceState != null) {
			mLocationCount = savedInstanceState.getInt(LOCATION_COUNT);
		}

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        startPreloaderAnimation();
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

		if (intent != null && intent.getExtras() != null) {

			
			// No geolocation
			if (intent.hasExtra("noGPS")) {
				withLoc = false;
				mMap.setOnMapLongClickListener(this);
				mCaptureLocation = false;
			}

			// Show previous location
			if (intent.hasExtra(GeoPointWidget.LOCATION)) {
				stopPreloaderAnimation();
				double[] location = intent
						.getDoubleArrayExtra(GeoPointWidget.LOCATION);
				mLatLng = new LatLng(location[0], location[1]);
				mMarkerOption.position(mLatLng);
				mMarker = mMap.addMarker(mMarkerOption);
				mMarker.setDraggable(true);
				mCaptureLocation = false;
				mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng,
						10));
			}

			// Set offline mode or not according to settings
			if (intent.hasExtra("offLine")) {
				mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
				TileOverlayOptions options = new TileOverlayOptions();
				String filePath = Environment.getExternalStorageDirectory()
						.toString()
						+ "/odk/tiles8bits.mbtiles";
				Log.e(getClass().getName(), filePath);
				options.tileProvider(new MapBoxOfflineTileProvider(filePath));
				mMap.addTileOverlay(options);
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

		mCancelLocation = (Button) findViewById(R.id.cancel_location);
		mCancelLocation.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Exit activity without saving location.
				Collect.getInstance().getActivityLogger()
						.logInstanceAction(this, "cancelLocation", "cancel");
				finish();
			}
		});

		mAcceptLocation = (Button) findViewById(R.id.accept_location);
		mAcceptLocation.setVisibility(View.VISIBLE);
		mAcceptLocation.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger()
						.logInstanceAction(this, "acceptLocation", "OK");
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

		// Focuses on marked location
		mShowLocation = ((Button) findViewById(R.id.show_location));
		mShowLocation.setVisibility(View.VISIBLE);
		mShowLocation.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger()
						.logInstanceAction(this, "showLocation", "onClick");
				mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng,
						16));
			}
		});
		mShowLocation.setClickable(mMarker != null);

		if (mCaptureLocation) {
			// Case where we show the current location according to the provider
			mLocationStatus = (TextView) findViewById(R.id.location_status);
			mRefreshLocation = ((Button) findViewById(R.id.refresh_location));
			mRefreshLocation.setVisibility(View.VISIBLE);
			mRefreshLocation.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Collect.getInstance()
							.getActivityLogger()
							.logInstanceAction(this, "refreshLocation",
									"onClick");
					onResume();
					mMarker.setDraggable(false);
				}
			});
			mRefreshLocation.setClickable(false);

		} else {
			// Case where we only show the saved location
			((TextView) findViewById(R.id.location_status))
					.setVisibility(View.GONE);
			mAcceptLocation.setClickable(false);

		}

	}

	@Override
	protected void onStart() {
		super.onStart();
		Collect.getInstance().getActivityLogger().logOnStart(this);
	}

	@Override
	protected void onStop() {
		Collect.getInstance().getActivityLogger().logOnStop(this);
		super.onStop();
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
					ActivityForm.LOCATION_RESULT,
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
		i.putExtra(ActivityForm.LOCATION_RESULT, mLatLng.latitude + " "
				+ mLatLng.longitude + " " + 0 + " " + 0);
		setResult(RESULT_OK, i);
		finish();
	}

	private String truncateFloat(float f) {
		return new DecimalFormat("#.##").format(f);
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
					mLocationStatus.setText(getString(
							R.string.location_provider_accuracy,
							mLocation.getProvider(),
							truncateFloat(mLocation.getAccuracy())));
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
					mShowLocation.setClickable(true);
				} else {
					mMarker.setPosition(mLatLng);
				}
				mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng,
						16));
				
				stopPreloaderAnimation();
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
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, 11));
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
		mShowLocation.setClickable(true);
		mIsDragged = true;
		Log.i(getClass().getName(), "x = " + mLatLng.latitude + " y = "
				+ mLatLng.longitude);
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, 11));
	}

}
