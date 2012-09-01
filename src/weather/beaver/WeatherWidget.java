/*
 * Copyright (C) 2009 The Android Open Source Project
 *
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

//TODO: set a timer and recieve via implicit broadcastreciever instead of update period
//TODO: methods to launch a notification with text, url, some kind of weather warning icon
//TODO: consider killing the service once its job is done
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

	public static final int agressiveUpdateInterval = 60000; // 1 minutes
	public static final int normalUpdateInterval = 300000; // 5 minutes
	public static final String ACTION_UPDATE_TIMER_EXPIRED = "WEATHER_UPDATE";


    //public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
	@Override
    public void onReceive(Context context, Intent intent){
        // To prevent any ANR timeouts, we perform the update in a service
        Toast.makeText(context, "WeatherWidget::onReceive, action: " + intent.getAction(), Toast.LENGTH_SHORT).show();
        //TODO: look at intent.getAction() and do something sensible there depending on the type
        if(intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE) || intent.getAction().equals(WeatherWidget.ACTION_UPDATE_TIMER_EXPIRED)){
        	if(UpdateService.weatherUpdate != null)
    	    {
    	    	//got a regular update. After the update, We can probably kill the service *setting UpdateService.weatherUpdate to null*
    	        String widgetUpdate = "Current Temp: " + Math.round(UpdateService.weatherUpdate.getCurrentTemp()) + ", " + UpdateService.weatherUpdate.getCurrentConditions();
    	        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.widget_message);
    	        updateViews.setTextViewText(R.id.message, widgetUpdate);
    	        ComponentName thisWidget = new ComponentName(context, WeatherWidget.class);
    	        
    	        //set the clicking behavior
    	        Intent activityIntent = new Intent(context, WeatherActivity.class);
    	        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	    	PendingIntent pendingIntent = PendingIntent.getActivity(context	, 0, activityIntent, 0);
    	    	//RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_weather);
    	        //RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.widget_message);
    	        updateViews.setOnClickPendingIntent(R.id.message, pendingIntent);         
    	        
    	        AppWidgetManager manager = AppWidgetManager.getInstance(context);
    	        manager.updateAppWidget(thisWidget, updateViews);
    	        startAlarm(context, WeatherWidget.normalUpdateInterval);
    	        
    	        UpdateService.weatherUpdate = null;
    	    }else{
    	    	//update not available, request short update.
    	        startAlarm(context, WeatherWidget.agressiveUpdateInterval);
    	        context.startService(new Intent(context, UpdateService.class));

    	    }
        }else if(intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_DISABLED)){
        	cancelAlarm(context);
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
    	alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + intervalMilliseconds, pendingIntent);
    	
    	
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
    	//TODO: add an enum(?) to track the state of this thing
    	public enum WeatherServiceStates{
    		INITIALIZED, GETTING_WEATHER, WEATHER_AVAILABLE
    	}
        //private Loader loader = new Loader();
    	private boolean finished = false;
    	private WeatherServiceStates state;
    	public static WeatherData weatherUpdate = null;
    	
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
                	weatherSource = WeatherHelperFactory.getWeatherHelper(true);//TODO:true means debug mode
                
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
		                    weatherUpdate = weather;
		                 
		                    /*
		                    String widgetUpdate = "Current Temp: " + Math.round(weather.getCurrentTemp()) + ", " + weather.getCurrentConditions();
		        	        RemoteViews updateViews = new RemoteViews(myContext.getPackageName(), R.layout.widget_message);
		        	        updateViews.setTextViewText(R.id.message, widgetUpdate);
		        	        ComponentName thisWidget = new ComponentName(myContext, WeatherWidget.class);
		        	        AppWidgetManager manager = AppWidgetManager.getInstance(myContext);
		        	        manager.updateAppWidget(thisWidget, updateViews);
		                    */
	                    }
	                    
	                }
                }catch(XMLWrapper.XMLCreationException ex){
                 StackTraceElement[] stacktrace = ex.getStackTrace();
                 for(int i = 0 ; i < stacktrace.length ; i++) {
                	 Log.d("WeatherBeaver", stacktrace[i].toString());
                 }
                	/*
                 String widgetUpdate = "could not find weather";
               	 RemoteViews updateViews = new RemoteViews(myContext.getPackageName(), R.layout.widget_message);
                 updateViews.setTextViewText(R.id.message, widgetUpdate);
                 ComponentName thisWidget = new ComponentName(myContext, WeatherWidget.class);
                 AppWidgetManager manager = AppWidgetManager.getInstance(myContext);
                 manager.updateAppWidget(thisWidget, updateViews);
                 */
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
            
            /*
            // Build the widget update for today
            RemoteViews updateViews = buildUpdate(this);
            // Push update for this widget to the home screen
            ComponentName thisWidget = new ComponentName(this, WeatherWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
            */
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
            }
            if (canUpdate()) {
                // Build an update that holds the updated widget contents
            	/*
            	 views = new RemoteViews(context.getPackageName(), R.layout.widget_weather);
                 Intent notificationIntent = new Intent(this, WeatherActivity.class);
             	PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
                 views.setOnClickPendingIntent(R.id.widget, pendingIntent); 
                
                String wordTitle = matcher.group(1);
                views.setTextViewText(R.id.word_title, wordTitle);
                views.setTextViewText(R.id.word_type, matcher.group(2));
                views.setTextViewText(R.id.definition, matcher.group(3).trim());
				
                // When user clicks on widget, launch to Wiktionary definition page
                String definePage = String.format("%s://%s/%s", ExtendedWikiHelper.WIKI_AUTHORITY,
                        ExtendedWikiHelper.WIKI_LOOKUP_HOST, wordTitle);
                */
                
                
                
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
