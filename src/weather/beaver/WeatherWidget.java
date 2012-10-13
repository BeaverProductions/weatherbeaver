/*
 * Copyright (C) 2009 The Android Open Source Project
 * Copyright (C) 2012 Marek Laskowski
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package weather.beaver;
//import com.example.android.beaver.SimpleWikiHelper.ApiException;
//import com.example.android.beaver.SimpleWikiHelper.ParseException;


//TODO: methods to launch a notification with text, url, some kind of weather warning icon
//TODO: on widget removal, kill the service if there are no more widgets left

import java.util.Set;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.vanaltj.canweather.*;
import com.vanaltj.canweather.data.Place;
import com.vanaltj.canweather.data.WeatherData;
import com.vanaltj.canweather.envcan.XMLWrapper;

/**
 * Define a simple widget that shows the weather according to environment canada
 * an update we spawn a background {@link Service} to perform the API queries.
 */


public class WeatherWidget extends BroadcastReceiver {

	//public static final int agressiveUpdateInterval = 60000; // 1 minutes - not used anymore: now using an intent broadcast, rather than polling a shared object
	public static final int defaultUpdateInterval = 300000; // 5 minutes
	public static final String ACTION_UPDATE_TIMER_EXPIRED = "WEATHERBEAVER_TIMER_EXPIRED";
	public static final String ACTION_WEATHER_UPDATE_AVAILABLE = "WEATHERBEAVER_WEATHER_AVAILABLE";
	public static final String INTENT_PAYLOAD_CURRENT_TEMPERATURE = "WEATHERBEAVER_CURRENT_TEMP";
	public static final String INTENT_PAYLOAD_CURRENT_CONDITIONS = "WEATHERBEAVER_CURRENT_CONDITIONS";



    //public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
	@Override
    public void onReceive(Context context, Intent intent){
        // To prevent any ANR timeouts, we perform the update in a service
        //Toast.makeText(context, "WeatherWidget::onReceive, action: " + intent.getAction(), Toast.LENGTH_SHORT).show();
        if(intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE) || intent.getAction().equals(WeatherWidget.ACTION_UPDATE_TIMER_EXPIRED)){
        	
        	//TODO: what should happen now is it starts a repeating timer with the update interval
        	//also start the service
        }else if(intent.getAction().equals(WeatherWidget.ACTION_WEATHER_UPDATE_AVAILABLE)){
        	Toast.makeText(context, "WeatherWidget::onReceive, action: " + intent.getAction(), Toast.LENGTH_SHORT).show();
        	
        	String widgetUpdate = "Current Temp: " + Math.round(intent.getDoubleExtra(INTENT_PAYLOAD_CURRENT_TEMPERATURE, -274.0)) + ", " + intent.getStringExtra(INTENT_PAYLOAD_CURRENT_CONDITIONS);
	        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_message);
	        updateViews.setTextViewText(R.id.message, widgetUpdate);
	        ComponentName thisWidget = new ComponentName(context, WeatherWidget.class);
	        
	        //set the clicking behavior
	        Intent activityIntent = new Intent(context, WeatherActivity.class);
	        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	    	PendingIntent pendingIntent = PendingIntent.getActivity(context	, 0, activityIntent, 0);
	    	
	        updateViews.setOnClickPendingIntent(R.id.message, pendingIntent);         
	        
	        AppWidgetManager manager = AppWidgetManager.getInstance(context);
	        manager.updateAppWidget(thisWidget, updateViews);
	        
	        //TODO: decide what to do about the timer
        	
        }else if(intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_DISABLED)){
        	cancelAlarm(context);        	
        	//TODO: also kill the service!
        	
        }
        //Log.d("WeatherBeaver", "WeatherWidget::OnUpdate");
    }
	
    private void startAlarm(Context context, long intervalMilliseconds)
    {
    	AlarmManager alarmManager = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);
    	Intent updateIntent = new Intent(context, WeatherWidget.class);
    	updateIntent.setAction(WeatherWidget.ACTION_UPDATE_TIMER_EXPIRED);
        //activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, updateIntent, 0);
    	//alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + intervalMilliseconds, pendingIntent);
    	alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + intervalMilliseconds, intervalMilliseconds, pendingIntent);
    }
    
    private void cancelAlarm(Context context)
    {
    	AlarmManager alarmManager = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);
    	Intent updateIntent = new Intent(context, WeatherWidget.class);
    	updateIntent.setAction(WeatherWidget.ACTION_UPDATE_TIMER_EXPIRED);
        //activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, updateIntent, 0);
    	alarmManager.cancel(pendingIntent);
    	
    	
    	
    }
    public static class UpdateService extends Service {
    	
    	//TODO: what is the state of this thing once finished=true?
    	public enum WeatherServiceStates{
    		INITIALIZED, GETTING_WEATHER, WEATHER_AVAILABLE, UPDATE_COMPLETED
    	}
        //private Loader loader = new Loader();
    	private boolean finished = false;
    	private WeatherServiceStates state;
    	
    	//No longer used.
    	//public static WeatherData weatherUpdate = null;
    	
    	public synchronized WeatherServiceStates getState(){
    	
    		return state;

    	}
    	
    	private synchronized void setState(WeatherServiceStates newState){
    		state = newState;
    	}
    	
        private class Loader implements Runnable {
        	private Context myContext = null;
        	
            public Loader(Context c){
            	myContext = c;
            	
            }

            public void run() {
                // TODO do the full initialization.

            	setState(WeatherServiceStates.GETTING_WEATHER);
            	Log.d("WeatherBeaver", "Getting weatherhelper.");
                WeatherHelper weatherSource = null;
                
                try{
                	//TODO:true means debug mode
                	weatherSource = WeatherHelperFactory.getWeatherHelper(true);
                
	                Log.d("WeatherBeaver", "Getting places.");
	                Set<Place> places = weatherSource.getPlaces();
	                Log.d("WeatherBeaver", "Got places.");
	                for (Place place : places) {
	                    Log.d("WeatherBeaver", place.getName());
	                    WeatherData weather = weatherSource.getWeather(place);
	                    if(weather != null){
		                    Log.d("WeatherBeaver","Current Temp: " + weather.getCurrentTemp());
		                    Log.d("WeatherBeaver","Current Conditions: " + weather.getCurrentConditions());
		                    Log.d("WeatherBeaver","High: " + weather.getTodayHigh());
		                    Log.d("WeatherBeaver","Low: " + weather.getTodayLow());
		                    Log.d("WeatherBeaver","Upcoming Conditions: " + weather.getUpcomingConditions());
		                    //no longer used:
		                    //weatherUpdate = weather;
		                    //this is our new way to do it. Send an intent with the update, and then we can probably terminate the service
		                    Intent updateIntent = new Intent(myContext, WeatherWidget.class);
		                	updateIntent.setAction(WeatherWidget.ACTION_WEATHER_UPDATE_AVAILABLE);
		                	updateIntent.putExtra(WeatherWidget.INTENT_PAYLOAD_CURRENT_CONDITIONS, weather.getCurrentConditions());
		                	updateIntent.putExtra(WeatherWidget.INTENT_PAYLOAD_CURRENT_TEMPERATURE, weather.getCurrentTemp());
		                    myContext.sendBroadcast(updateIntent);
		                	setState(WeatherServiceStates.UPDATE_COMPLETED);

		                    //kill the service
		                	stopSelf();//that was easy
		 
	                    }
	                    
	                }
                }catch(XMLWrapper.XMLCreationException ex){
                 StackTraceElement[] stacktrace = ex.getStackTrace();
                 for(int i = 0 ; i < stacktrace.length ; i++) {
                	 Log.d("WeatherBeaver", stacktrace[i].toString());
                 }
                
                 //Toast.makeText(myContext, "WeatherWidget exception during update from server", Toast.LENGTH_SHORT).show();

                }
                finished = true;
                setState(WeatherServiceStates.WEATHER_AVAILABLE);
            }

            public boolean finished() {
                return finished;
            }
            
        }
        
        @Override
        public void onCreate() {
        	//TODO: check whether the thread is running (maybe even in onStart())
        	finished=false;
        	setState(WeatherServiceStates.INITIALIZED);
            
        	
            Log.d("WeatherBeaver", "WeatherWidget::UpdateService::onCreate");
            
            
        }
        
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.d("WeatherBeaver", "WeatherWidget::UpdateService::onStartCommand");
            if(getState() != WeatherServiceStates.GETTING_WEATHER){
            	new Thread(new Loader(this)).start();
            }
        	//TODO: determine whether it should be sticky or not!
			return Service.START_NOT_STICKY;
        }
        
        @Override
        public void onDestroy() {
            
            // Tell the user we stopped.
            Toast.makeText(this, R.string.update_service_stopped, Toast.LENGTH_SHORT).show();
        }
        
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        
        /**
         * Build a widget update to show the current weather
         *  Will block until the online API returns.
         */
        public RemoteViews buildUpdate(Context context) {
            RemoteViews views = null;
            // TODO build default thingy.  Maybe this is the loading thing.
            if (isLoading()) {
                // TODO build "loading" view.
            	//currently the loading view is the default one loaded
            }
            if (canUpdate()) {
                // Build an update that holds the updated widget contents
                
                
            } else {
                // Didn't find weather, so show error message
                views = new RemoteViews(context.getPackageName(), R.layout.widget_message);
                //views.setTextViewText(R.id.message, context.getString(R.string.widget_error));
                views.setTextViewText(R.id.message, "WTF");
                sendNotification("robots!", "whatever details", "http://google.com");
            }
            return views;
        }
        public void sendNotification(String ticker, String details, String uri){
        	NotificationManager nm = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        	int icon = R.drawable.star_logo;
        	long when = System.currentTimeMillis();
        	Notification currentNotification = new Notification(icon, ticker, when);
        	
        	//now set a bunch of bullcrap
        	Context context = getApplicationContext();
        	CharSequence contentTitle = context.getString(R.string.notification_title);
        	
        	CharSequence contentText = "Weatherbeaver weather warning";
        	Intent notificationIntent = new Intent(this, WeatherActivity.class);
        	PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        	currentNotification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        	
        	nm.notify(1, currentNotification);
        	
        }

        private boolean canUpdate() {
            return false;
        }

        private boolean isLoading() {
            return true;
        }

    }//updateService   

	

	   
}
