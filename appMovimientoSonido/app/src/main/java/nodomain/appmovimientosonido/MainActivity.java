package nodomain.appmovimientosonido;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;

/*
Esta aplicación reproduce un sonido cuando realizamos un movimiento muy brusco con el teléfono.
Primero, dejamos el dispositivo sobre una superficie plana y quieto, pulsamos el botón rojo, y
esperamos a que se vuelva azul. Cambiará el mensaje del botón, y podemos coger el teléfono y
utilizar la aplicación.

Referencias:
Cómo reproducir sonido: http://stackoverflow.com/questions/18254870/play-a-sound-from-res-raw
Cómo detectar movimiento: http://stackoverflow.com/questions/14574879/how-to-detect-movement-of-an-android-device
Sonido: http://soundbible.com/1375-Whip-Crack.html
 */

public class MainActivity extends Activity implements SensorEventListener {


    // Reproducción de sonidos

    private Button button;
    private MediaPlayer mPlayer;
    private boolean currentPlayback =false;

    // Detección de movimiento
    private SensorManager mSensorMan;
    private Sensor mAccelSensor;

    // Detección de gesto
    private int movementStatus;
    private double upperLimit=22, lowerLimit=5;

    // Var. para calibrar
    private boolean calibration=false, calibrated=false;
    private int nOfSamples;
    private double acumX, acumY, acumZ;
    private double baseX, baseY, baseZ;

    private float[] mGravity;
    private double mAccel;
    private double mAccelCurrent;
    private double mAccelLast;

    /**
     * Inicialización de variables/interfaz
     * @param savedInstanceState Instancia con datos recibida por la actividad
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        // Sensor de movimiento
        mSensorMan = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelSensor = mSensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.0f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast=mAccelCurrent;

        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calibrateSensor();
            }
        });
    }

    // Funciones de sens. movimiento

    /**
     * Función de calibrado del sensor. Va a recoger datos durante 3 segundos para
     * intentar reducir el ruido de la señal que recibimos.
     */
    public void calibrateSensor(){
        nOfSamples=0;
        acumX=0;
        acumY=0;
        acumZ=0;
        calibrated=false;

        calibration=true;

        // Cambiamos el color del boton
        button.setBackgroundColor(getResources().getColor(R.color.colorAccent));

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                calibration=false;

                // Promediamos los valores
                baseX = acumX/(double)nOfSamples;
                baseY = acumY/(double)nOfSamples;
                baseZ = acumZ/(double)nOfSamples;

                button.setBackgroundColor(getResources().getColor(R.color.colorPrimary));

                calibrated=true;
            }
        }, 3000);
    }

    /**
     * Registramos el listener de eventos
     */
    @Override
    public void onResume() {
        super.onResume();
        mSensorMan.registerListener(this, mAccelSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    /**
     * Eliminamos el listener de eventos
     */
    @Override
    protected void onPause() {
        super.onPause();
        mSensorMan.unregisterListener(this);
    }

    /**
     * Procesamos el evento del sensor. Si es un evento de aceleración, lo procesamos:
     * Si estamos calibrando, tomamos los valores y los acumulamos.
     * Una vez dejamos de calibrar, calculamos los valores de la calibración, y pasamos a procesar
     * los nuevos datos que vamos obteniendo. Para ello, llevamos una suma de las aceleraciones,
     * que vamos atenuando en cada iteración por un factor, en este caso 0.4, y sumándole la
     * aceleración actual. Este es el valor en el que nos vamos a fijar para lanzar la acción de sonido
     * @param event Evento de aceleración recibido desde el sensor.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){

            mGravity = event.values.clone();
            // Shake detection
            double x = mGravity[0];
            double y = mGravity[1];
            double z = mGravity[2];

            // Calibramos el sensor, tomanddo los valores actuales a 0
            if(calibration){
                // Contamos muestra
                nOfSamples+=1;
                acumX += x;
                acumY += y;
                acumZ += z;
            }
            else if(calibrated) {
                x-=baseX;
                y-=baseY;
                z-=baseZ;

                // Ahora vamos a comprobar el estado de "equilibrio" del que vamos a partir siempre
                // x: Mover a la derecha = positivo
                // x: Mover a la izquierda = negativo

                mAccelLast = mAccelCurrent;
                mAccelCurrent = Math.sqrt(x * x + y * y + z * z);
                double delta = mAccelCurrent - mAccelLast;
                mAccel = mAccel * 0.4f + delta;


                if(mAccel > upperLimit){
                    playSound(R.raw.whipcrack);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }


    // Funciones de rep. sonido

    /**
     * Función de reproducción de sonnido. Le pasamos la id del sonido que queramos. Para reproducir,
     * comprueba si hay un sonido actualmente en reproducción, lo para si lo hay, y después indica al
     * reproductor el nuevo sonido que tiene que reproducir. Al acabar una reproducción,
     * se para el sonido.
     * @param rid id del recurso de sonido a reproducir
     */
    private void playSound(int rid){
        if(currentPlayback)
            stopSound();


        mPlayer = MediaPlayer.create(this,rid);
        mPlayer.setOnCompletionListener( new MediaPlayer.OnCompletionListener(){
            @Override
            public void onCompletion(MediaPlayer mediaPlayer){
                stopSound();
            }
        });
        mPlayer.start();
        currentPlayback =true;
    }

    /**
     * Función compañera de la anterior; si tenemos un objeto reproductor de sonido, lo libera e
     * indica que no se está reproduciendo ningún sonido.
     */
    private void stopSound(){
        if(mPlayer != null){
            mPlayer.release();
            currentPlayback =false;
            mPlayer=null;
        }
    }
}
