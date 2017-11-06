package com.frostmouns.coolweather;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.frostmouns.coolweather.gson.Forecast;
import com.frostmouns.coolweather.gson.Weather;
import com.frostmouns.coolweather.util.HttpUtil;
import com.frostmouns.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView mSvWeather;
    private TextView mTvCityName;
    private TextView mTvUpdateTime;
    private TextView mTvDegree;
    private TextView mTvWeatherInfo;
    private LinearLayout mLlForecast;
    private TextView mTvAQI;
    private TextView mTvPm25;
    private TextView mTvComfort;
    private TextView mTvCarWash;
    private TextView mTvSport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        mSvWeather = (ScrollView) findViewById(R.id.sv_weather);
        mTvCityName = (TextView) findViewById(R.id.tv_city_name);
        mTvUpdateTime = (TextView) findViewById(R.id.tv_update_time);
        mTvDegree = (TextView) findViewById(R.id.tv_degree);
        mTvWeatherInfo = (TextView) findViewById(R.id.tv_weather_info);
        mLlForecast = (LinearLayout) findViewById(R.id.ll_forecast);
        mTvAQI = (TextView) findViewById(R.id.tv_aqi);
        mTvPm25 = (TextView) findViewById(R.id.tv_pm25);
        mTvComfort = (TextView) findViewById(R.id.tv_comfort);
        mTvCarWash = (TextView) findViewById(R.id.tv_car_wash);
        mTvSport = (TextView) findViewById(R.id.tv_sport);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = preferences.getString("weather", null);
        if(weatherString != null){
            Weather weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
        }else {
            String weatherId = getIntent().getStringExtra("weather_id");
            mSvWeather.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }
    }

    private void requestWeather(final String weatherId) {
        //注册后得到可以写到此处
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=9e8f2dac13e949f1afaee16a0a25064b";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather != null && "ok".equals(weather.status)){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        }else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;

        mTvCityName.setText(cityName);
        mTvUpdateTime.setText(updateTime);
        mTvDegree.setText(degree);
        mTvWeatherInfo.setText(weatherInfo);

        mLlForecast.removeAllViews();

        for (Forecast f : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, mLlForecast, false);
            TextView tvDate = view.findViewById(R.id.tv_date);
            TextView tvinfo = view.findViewById(R.id.tv_info);
            TextView tvMax = view.findViewById(R.id.tv_max);
            TextView tvMin = view.findViewById(R.id.tv_min);
            tvDate.setText(f.date);
            tvinfo.setText(f.more.info);
            tvMax.setText(f.temperature.max);
            tvMin.setText(f.temperature.min);
            mLlForecast.addView(view);
        }
        if(weather.aqi != null){
            mTvAQI.setText(weather.aqi.city.aqi);
            mTvPm25.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度: " + weather.suggestion.comfort.info;
        String carWash = "洗车指数: " + weather.suggestion.carWash.info;
        String sport = "运动建议: " + weather.suggestion.sport.info;

        mTvComfort.setText(comfort);
        mTvCarWash.setText(carWash);
        mTvSport.setText(sport);
        mSvWeather.setVisibility(View.VISIBLE);
    }
}
