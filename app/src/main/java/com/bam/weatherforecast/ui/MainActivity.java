package com.bam.weatherforecast.ui;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bam.weatherforecast.R;
import com.bam.weatherforecast.weather.Current;
import com.bam.weatherforecast.weather.Forecast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Forecast mForecast;

    @BindView(R.id.timeLabel) TextView mTimeLabel;
    @BindView(R.id.temperatureLabel) TextView mTemperatureLabel;
    @BindView(R.id.humidityLabel) TextView mHumidityLabel;
    @BindView(R.id.humidityValue) TextView mHumidityValue;
    @BindView(R.id.precipValue) TextView mPrecipValue;
    @BindView(R.id.summaryLabel) TextView mSummaryLabel;
    @BindView(R.id.iconImageView) ImageView mIconImageView;
    @BindView(R.id.refreshImageView) ImageView mRefreshImageView;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mProgressBar.setVisibility(View.INVISIBLE);

        final double latitude = 37.8267;
        final double longitude = -122.423;

        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getForecast(latitude, longitude);
            }
        });

        getForecast(latitude, longitude);
    }

    private void getForecast(double latitude, double longitude) {
        String apiKey = "683265aacaa81297d1f26bcf1f0f3484";
        String forecastUrl = String.format("https://api.forecast.io/forecast/%s/%f,%f", apiKey, latitude, longitude);

        if(isNetworkAvailable()) {
            toggleRefresh();
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastUrl).build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    alertUserAboutError();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    try {
                        String jsonData = response.body().string();
                        Log.v(TAG, jsonData);
                        if (response.isSuccessful()) {
                            mForecast = parseForecastDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });
                        } else {
                            alertUserAboutError();
                        }
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }
            });
        } else {
            Toast.makeText(this, R.string.network_unavailable_message,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void toggleRefresh() {
        if(mProgressBar.getVisibility() == View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }

    private void updateDisplay() {
        Current current = mForecast.getCurrent();
        mTemperatureLabel.setText(String.valueOf(current.getTemp()));
        mTimeLabel.setText("At " + current.getFormattedTime() + " it will be");
        mHumidityValue.setText(String.valueOf(current.getHumidity()));
        mPrecipValue.setText(String.valueOf(current.getPrecipChange() + "%"));
        mSummaryLabel.setText(current.getSummary());
        Drawable drawable = ContextCompat.getDrawable(this, current.getIconId());
        mIconImageView.setImageDrawable(drawable);
    }

    private Forecast parseForecastDetails(String jsonData) throws JSONException {
        Forecast forecast = new Forecast();
        forecast.setCurrent(getCurrentDetails(jsonData));
        return forecast;
    }

    private Current getCurrentDetails(String jsonData)
            throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        JSONObject currently = forecast.getJSONObject("currently");
        Current current = new Current();
        current.setHumidity(currently.getDouble("humidity"));
        current.setTimeZone(forecast.getString("timezone"));
        current.setTime(currently.getLong("time"));
        current.setIcon(currently.getString("icon"));
        current.setPrecipChange(currently.getDouble("precipProbability"));
        current.setSummary(currently.getString("summary"));
        current.setTemp(currently.getDouble("temperature"));
        return current;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager)
                getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void alertUserAboutError() {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), "error_dialog");
    }
}
