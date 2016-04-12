package com.example.nadim.gasatu;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static String gpsLat="";
    public static String gpsLng="";
    public static String gpsAltitude="";

    public  static Double nearestGPSLat;
    public  static Double nearestGPSLng;

    public static String APIKey = "Replace your own API Key";

    TextView findGasButton, viewMapButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getCurrentLocation();

        findGasButton = getFindGasButton();
        viewMapButton = getViewMapButton();
        getViewMapButton().setVisibility(View.GONE);

    }

    private class GetPlaces extends AsyncTask<Void, Void, ArrayList<Place>> {

        private ProgressDialog dialog;
        private Context context;
        private String places;

        public GetPlaces(Context context, String places) {
            this.context = context;
            this.places = places;
        }

        @Override
        protected void onPostExecute(ArrayList<Place> result) {
            super.onPostExecute(result);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            findNearestPoint(result);
//            for (int i = 0; i < result.size(); i++) {
//                mMap.addMarker(new MarkerOptions()
//                        .title(result.get(i).getName())
//                        .position(
//                                new LatLng(result.get(i).getLatitude(), result
//                                        .get(i).getLongitude()))
//                        .icon(BitmapDescriptorFactory
//                                .fromResource(R.drawable.pin))
//                        .snippet(result.get(i).getVicinity()));
//            }
//            CameraPosition cameraPosition = new CameraPosition.Builder()
//                    .target(new LatLng(result.get(0).getLatitude(), result
//                            .get(0).getLongitude())) // Sets the center of the map to
//                            // Mountain View
//                    .zoom(14) // Sets the zoom
//                    .tilt(30) // Sets the tilt of the camera to 30 degrees
//                    .build(); // Creates a CameraPosition from the builder
//            mMap.animateCamera(CameraUpdateFactory
//                    .newCameraPosition(cameraPosition));
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(context);
            dialog.setCancelable(false);
            dialog.setMessage("Loading..");
            dialog.isIndeterminate();
            dialog.show();
        }

        @Override
        protected ArrayList<Place> doInBackground(Void... arg0) {
            PlacesService service = new PlacesService(
                    APIKey);

//            gpsLat = "28.632808";
//            gpsLng = "77.218276";

//            gpsLat = "38.910950";
//            gpsLng = "121.618968";

            ArrayList<Place> findPlaces = service.findPlaces(Double.parseDouble(gpsLat), // 28.632808
                    Double.parseDouble(gpsLng), places); // 77.218276

            for (int i = 0; i < findPlaces.size(); i++) {

                Place placeDetail = findPlaces.get(i);
                Log.e("post", "places : " + placeDetail.getName());
            }
            return findPlaces;
        }

    }

    private void findNearestPoint(ArrayList<Place> pointList){
        if (pointList.size() == 0)
            return;

        Double myLat = Double.parseDouble(gpsLat);
        Double myLng = Double.parseDouble(gpsLng);
        Location hereLoc = new Location("from");
        hereLoc.setLatitude(myLat);
        hereLoc.setLongitude(myLng);

        float min = 0.f;
        List<Integer> minList = new ArrayList<>();

        for (int i=0; i<pointList.size(); i++){
            Location toLoc = new Location("to");
            toLoc.setLongitude(pointList.get(i).getLongitude());
            toLoc.setLatitude(pointList.get(i).getLatitude());
            float distance = hereLoc.distanceTo(toLoc);
            if (i==0) {
                min = distance;
                minList.add(i);
            }
            else {
                if (distance <= min) {
                    min = distance;
                    minList.add(i);
                }
            }
        }

        if (minList.size() == 0)
            return;

        int idxOfNearest = minList.get(minList.size()-1).intValue();
        nearestGPSLat = pointList.get(idxOfNearest).getLatitude();
        nearestGPSLng = pointList.get(idxOfNearest).getLongitude();

        getViewMapButton().setVisibility(View.VISIBLE);
        getViewMapButton().setText(pointList.get(idxOfNearest).getName());

    }


    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }
    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }


    private void getCurrentLocation(){
        GPSTracker gps = new GPSTracker(this);

        // check if GPS enabled
        if(gps.canGetLocation()){

            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            double dAltitude = gps.getAltitude();

            gpsLat = String.format("%f", latitude);
            gpsLng = String.format("%f", longitude);
            gpsAltitude = String.format("%f", dAltitude);


            // \n is for new line
            Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
            gps.stopUsingGPS();
        }else{
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }
    }

    private TextView getFindGasButton(){
        if (findGasButton == null){
            findGasButton = (TextView)findViewById(R.id.findGasButton);
            findGasButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    findGasStation();
                }
            });
        }
        return findGasButton;
    }

    private void findGasStation(){
        new GetPlaces(MainActivity.this,"gas_station").execute();
    }

    private TextView getViewMapButton(){
        if (viewMapButton == null){
            viewMapButton = (TextView)findViewById(R.id.viewMapButton);
            viewMapButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewGasStationOnMap();
                }
            });
        }
        return viewMapButton;
    }

    private void viewGasStationOnMap(){
        Intent intent = new Intent();
        intent.setClass(this, ShowGasStation.class);
        startActivity(intent);
    }

    private  class  GetNearByGasStation extends AsyncTask<String, Void,  List<String>> {

        String strRespond;
        protected void onPreExecute() {
            // TODO Auto-generated method stub
        }

        @Override
        protected List<String> doInBackground(String... params) {
            // TODO Auto-generated method stub
            List<String> result = null;
            return result;
        }
        @Override
        protected void onPostExecute(List<String> result) {
            super.onPostExecute(result);

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
