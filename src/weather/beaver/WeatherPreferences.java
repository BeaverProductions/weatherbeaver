package weather.beaver;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class WeatherPreferences extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        PreferenceManager.setDefaultValues(this, R.xml.weather_preferences, false);

    }

    
    
}