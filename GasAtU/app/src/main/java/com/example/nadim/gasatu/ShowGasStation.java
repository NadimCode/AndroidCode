package com.example.nadim.gasatu;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ShowGasStation extends FragmentActivity {

    private   Double gpsMyLat;
    private   Double gpsMyLng;

    GoogleMap map;
    ArrayList<LatLng> markerPoints;

    Timer timer;
    TimerTask timerTask;
    int initialTime = 5000;
    int timerDelay = 10000;

    Polyline currentRoutes;

//we are going to use a handler to be able to run in our TimerTask
    final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_map_layout);

        // Initializing
        gpsMyLat = Double.parseDouble(MainActivity.gpsLat);
        gpsMyLng = Double.parseDouble(MainActivity.gpsLng);

//        gpsMyLat = 28.632808;
//        gpsMyLng = 77.218276;
//
//        MainActivity.nearestGPSLat = 28.635308;
//        MainActivity.nearestGPSLng = 77.22496;

        markerPoints = new ArrayList<LatLng>();

        // Getting reference to SupportMapFragment of the activity_main
        SupportMapFragment fm = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);

        // Getting Map for the SupportMapFragment
        map = fm.getMap();

        // Enable MyLocation Button in the Map
        map.setMyLocationEnabled(true);

        // Create a LatLng object for the current location
        LatLng latLng = new LatLng(gpsMyLat, gpsMyLng);

        CameraPosition myPosition = new CameraPosition.Builder()
                .target(latLng).zoom(30).bearing(0).tilt(0).build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(myPosition));

//        map.addMarker(new MarkerOptions().position(new LatLng(gpsMyLat, gpsMyLng)).title("You are here!").snippet("Consider yourself located"));

        showGasStation();
    }

    private void clearMap(){
        map.clear();
        if (currentRoutes != null) {
            currentRoutes.remove();
            currentRoutes = null;
        }
    }

    private void showGasStation(){

        LatLng origin = new LatLng(gpsMyLat, gpsMyLng);
        LatLng dest = new LatLng(MainActivity.nearestGPSLat, MainActivity.nearestGPSLng);

        MarkerOptions options = new MarkerOptions();
        options.position(origin);
        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        map.addMarker(options);

        options.position(dest);
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.gas_station_pin));
        map.addMarker(options);


        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(origin, dest);

        DownloadTask downloadTask = new DownloadTask();

        // Start downloading json data from Google Directions API
        downloadTask.execute(url);
    }

    private String getDirectionsUrl(LatLng origin,LatLng dest){

        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        // Destination of route
        String str_dest = "destination="+dest.latitude+","+dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        //api key
        String key = "key=" + MainActivity.APIKey;

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor + "&" + key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;


        return url;
    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            e.printStackTrace();
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(6);
                lineOptions.color(Color.RED);

            }

            // Drawing polyline in the Google Map for the i-th route
            currentRoutes = map.addPolyline(lineOptions);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_gas_station, menu);
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

    @Override
    public void onResume(){
        super.onResume();
        startTimer();
    }

    @Override
    public void onStop(){
        super.onStop();

        stoptimertask();
    }

    public void startTimer() {
        //set a new Timer
        timer = new Timer();
        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, initialTime, timerDelay); //
    }


    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {

            public void run() {
                //use a handler to run a toast that shows the current timestamp

                handler.post(new Runnable() {
                    public void run() {
//                        timerTest();
                        getCurrentLocation();
                    }
                });
            }
        };
    }

    private void timerTest(){
        //get the current timeStamp
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd:MMMM:yyyy HH:mm:ss a");
        final String strDate = simpleDateFormat.format(calendar.getTime());
        //show the toast
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(getApplicationContext(), strDate, duration);
        toast.show();
    }

    private void getCurrentLocation(){
        GPSTracker gps = new GPSTracker(this);

        // check if GPS enabled
        if(gps.canGetLocation()){

            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            double dAltitude = gps.getAltitude();

            gpsMyLat = gps.getLatitude();
            gpsMyLng = gps.getLongitude();

            // \n is for new line
            Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
            gps.stopUsingGPS();

            clearMap();

            showGasStation();

        }else{
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }
    }

}
