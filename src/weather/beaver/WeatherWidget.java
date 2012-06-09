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
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import com.vanaltj.canweather.*;
import com.vanaltj.canweather.data.Place;
import com.vanaltj.canweather.data.WeatherData;

/**
 * Define a simple widget that shows the weather according to environment canada
 * an update we spawn a background {@link Service} to perform the API queries.
 */
public class WeatherWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // To prevent any ANR timeouts, we perform the update in a service
        context.startService(new Intent(context, UpdateService.class));
        Log.d("WeatherBeaver", "WeatherWidget::OnUpdate");
        
    }

    public static class UpdateService extends Service {
        @Override
        public void onStart(Intent intent, int startId) {
        	
            Log.d("WeatherBeaver", "WeatherWidget::UpdateService::onStart");

        	
        	Intent activityIntent = new Intent(this, WeatherActivity.class);
        	
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);
        	RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.widget_weather);
            //RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.widget_message);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent); 
            // Build the widget update for today
            RemoteViews updateViews = buildUpdate(this);

            // Push update for this widget to the home screen
            ComponentName thisWidget = new ComponentName(this, WeatherWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
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
        	/*
            // Pick out month names from resources
            Resources res = context.getResources();
            String[] monthNames = res.getStringArray(R.array.month_names);

            // Find current month and day
            Time today = new Time();
            today.setToNow();

            // Build the page title for today, such as "March 21"
            String pageName = res.getString(R.string.template_wotd_title,
                    monthNames[today.month], today.monthDay);
            String pageContent = null;

            try {
                // Try querying the Wiktionary API for today's word
                SimpleWikiHelper.prepareUserAgent(context);
                pageContent = SimpleWikiHelper.getPageContent(pageName, false);
            } catch (ApiException e) {
                Log.e("WordWidget", "Couldn't contact API", e);
            } catch (ParseException e) {
                Log.e("WordWidget", "Couldn't parse API response", e);
            }
			*/
            RemoteViews views = null;
            boolean hasUpdate = false;
            //do the API call...
            /*
            WeatherHelper weatherSource = WeatherHelperFactory.getWeatherHelper();
            List<Place> places = weatherSource.getPlaces();
            for (Place place : places) {
                Log.d("weatherbeaver", place.getName());
                WeatherData weather = weatherSource.getWeather(place);
                Log.d("weatherbeaver","Current Temp: " + weather.getCurrentTemp());
                Log.d("weatherbeaver","Current Conditions: " + weather.getCurrentConditions());
                Log.d("weatherbeaver","High: " + weather.getTodayHigh());
                Log.d("weatherbeaver","Low: " + weather.getTodayLow());
                Log.d("weatherbeaver","Upcoming Conditions: " + weather.getUpcomingConditions());
            }
            */
            //Matcher matcher = Pattern.compile(WOTD_PATTERN).matcher(pageContent);
            if (hasUpdate) {
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
                views.setTextViewText(R.id.message, context.getString(R.string.widget_error));
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
    }//updateService
    
    
}
