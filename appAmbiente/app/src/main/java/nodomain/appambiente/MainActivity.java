package nodomain.appambiente;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/*
 Aplicación que hace uso de los sensorse ambientales disponibles en el dispositivo para mostrar
 datos de temperatura, humedad, presión y altura, según su disponibilidad.
 Referencias:
 http://developer.android.com/guide/topics/sensors/sensors_environment.html
 ecuación de altura implementada (despejando la altura h de la ecuación)
 http://www.regentsprep.org/regents/math/algtrig/atp8b/exponentialresource.htm
 */

public class MainActivity extends Activity implements SensorEventListener {


    SensorManager mSensorMan;
    Sensor mTempSensor, mLightSensor, mPressureSensor, mHumiditySensor;
    private boolean hasTempSensor, hasLightSensor, hasPressureSensor, hasHumiditySensor;

    private TextView tvTemp, tvLight, tvPressure, tvHeight, tvHumidity;

    float [] buffer;

    float currentTemp, currentLight, currentPressure, currentHumidity, currentHeightEstimate;

    /**
     * Inicialización de variables/interfaz
     * @param savedInstanceState Instancia con datos recibida por la actividad
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        // Inicializamos los tv
        tvTemp = (TextView) findViewById(R.id.tvTemp);
        tvLight = (TextView) findViewById(R.id.tvLight);
        tvPressure = (TextView) findViewById(R.id.tvPressure);
        tvHeight = (TextView) findViewById(R.id.tvHeight);
        tvHumidity = (TextView) findViewById(R.id.tvHumidity);

        // Inicializamos los sensores
        mSensorMan = (SensorManager) getSystemService(SENSOR_SERVICE);
        mTempSensor = mSensorMan.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        mLightSensor = mSensorMan.getDefaultSensor(Sensor.TYPE_LIGHT);
        mPressureSensor = mSensorMan.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mHumiditySensor = mSensorMan.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);


    }

    /**
     * Registramos los listeners de eventos
     */
    @Override
    public void onResume() {
        super.onResume();

        hasTempSensor=mSensorMan.registerListener(this, mTempSensor, SensorManager.SENSOR_DELAY_NORMAL);
        hasLightSensor=mSensorMan.registerListener(this, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        hasPressureSensor=mSensorMan.registerListener(this, mPressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        hasHumiditySensor=mSensorMan.registerListener(this, mHumiditySensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
    /**
     * Eliminamos los listeners de eventos
     */
    @Override
    protected void onPause() {
        mSensorMan.unregisterListener(this, mTempSensor);
        mSensorMan.unregisterListener(this, mLightSensor);
        mSensorMan.unregisterListener(this, mPressureSensor);
        mSensorMan.unregisterListener(this, mHumiditySensor);
        super.onPause();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /**
     * Ante un cambio de sensores, lanzamos cada función de actualización de datos correspondiente
     * @param event evento de actualización del valor de los sensores. Comprobamos su tipo y lo procesamos.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        buffer = event.values.clone();

        switch (event.sensor.getType()){
            case (Sensor.TYPE_AMBIENT_TEMPERATURE):
                updateTemperature(buffer[0]);
                break;
            case (Sensor.TYPE_LIGHT):
                updateLight(buffer[0]);
                break;
            case (Sensor.TYPE_PRESSURE):
                updatePressure(buffer[0]);
                break;
            case (Sensor.TYPE_RELATIVE_HUMIDITY):
                updateHumidity(buffer[0]);
                break;
        }
    }

    /**
     * Actualizamos la temperatura global con la actual
     * @param temp Nuevo valor de la temperatura recibido
     */
    private void updateTemperature(float temp){
        this.currentTemp = temp;

        this.tvTemp.setText(getResources().getString(R.string.temp_available) + currentTemp);
    }

    /**
     * Actualizamos la temperatura global con la actual
     * @param light Nuevo valor de la intensidad luminosa recibido
     */
    private void updateLight(float light){
        this.currentLight = light;

        this.tvLight.setText(getResources().getString(R.string.light_available) + currentLight);
    }

    /**
     * Actualizamos la presión global con la leída de los sensores, y actualizamos la altura estimada
     * @param pressure presión en milibares obtenida por el sensor de presión del dispositivo
     */
    private void updatePressure(float pressure){
        this.currentPressure = pressure;

        this.tvPressure.setText(getResources().getString(R.string.pressure_available) + currentPressure);
        updateHeight();
    }

    /**
     * Actualizamos el valor de la estimación de altura. Se llama después de que se actualice el
     * valor de la presión. No usa parámetros, únicamente las variables globales que tenemos.
     */
    private void updateHeight(){
        float pressure, pressureSeaLevel, airDensity, g0, temperature, height, scaleHeight;
        // Vamos a realizar una estimación de la altura actual sobre el nivel del mar con la presión

        // Vamos a usar la fórmula http://www.regentsprep.org/regents/math/algtrig/atp8b/exponentialresource.htm
        // Pero usando una altura de escala de 8.5 (más precisa)

        pressureSeaLevel = 1.013f;
        pressure = currentPressure/1000;    // Pasamos a bar
        scaleHeight = 8.5f;

        height = scaleHeight * (float)Math.log(pressureSeaLevel/pressure);

        currentHeightEstimate=height*1000;

        this.tvHeight.setText(getResources().getString(R.string.height_estimate) + currentHeightEstimate);
    }

    /**
     * Actualiza el valor de la humedad relativa según el recibido del sensor
     * @param humidity valor de humedad relativa del entorno en tanto por ciento.
     */
    private void updateHumidity(float humidity){
        this.currentTemp = humidity;

        this.tvHumidity.setText(getResources().getString(R.string.humidity_available) + currentHumidity);
    }
}