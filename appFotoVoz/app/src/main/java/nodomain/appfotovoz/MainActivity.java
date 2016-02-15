package nodomain.appfotovoz;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;

/*
Referencias:
Imagen: https://commons.wikimedia.org/wiki/File:Windrose.png
Código base para brújula: http://www.javacodegeeks.com/2013/09/android-compass-code-example.html
Código base para rec. de voz: http://www.tutorialeshtml5.com/2013/03/tutorial-simple-reconocimiento-de-voz.html

Con el botón iniciar comenzamos el reconocimiento de voz, y una vez hayamos introducido la coordenada,
el fondo cambia gradualmente de azul a rojo según cómo de lejos/cerca estemos del objetivo.
Cuando lo alcanzamos, el fondo se queda rojo, y se indica con un mensaje que se ha alcanzado la posición deseada
 */

public class MainActivity extends Activity implements SensorEventListener {
    // Cuadros de texto
    TextView tvInfo, tvHeading;

    // Rec. voz
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1;
    private Button btStart;
    private LinearLayout background;

    // Brújula
    private int targetOrientation;
    private double errorMargin;

    private ImageView image;

    private float currentDegree = 0f;
    private boolean currentlySearching = false;

    // device sensor manager
    private SensorManager mSensorManager;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    /**
     * Inicialización de variables/interfaz
     * @param savedInstanceState Instancia con datos recibida por la actividad
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        tvInfo = (TextView) findViewById(R.id.tvInfo);
        tvHeading = (TextView) findViewById(R.id.tvCurrentHeading);
        background = (LinearLayout) findViewById(R.id.background);

        image = (ImageView) findViewById(R.id.imageView);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        btStart = (Button) findViewById(R.id.initButton);
        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Lanzamos el reconoimiento de voz
                startVoiceRecognitionActivity();
            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    /**
     * Comienza la actividad de reconocimiento de voz, a través de un Intent
     * @see Intent
     */
    private void startVoiceRecognitionActivity() {
        // Definición del intent para realizar en análisis del mensaje
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        // Indicamos el modelo de lenguaje para el intent
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Definimos el mensaje que aparecerá
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getResources().getString(R.string.voice_prompt));
        // Lanzamos la actividad esperando resultados
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    @Override

    /**
     * Recogemos los resultados del reconocimiento de voz, y lo procesamos.
     * Para, ello, guardamos en nuestra variable orientacion N/S/E/W codificados como 12/3/6/9
     * También tomamos el margen de error que el usuario indica.
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String[] words = matches.get(0).toString().split(" ");
            if (words.length > 1) {
                tvInfo.setText("Buscando: " + words[0] + " " + words[1] + " " + Integer.toString(targetOrientation));

                if (words[0].toUpperCase().equals(getResources().getString(R.string.N)))
                    targetOrientation = 12;
                else if (words[0].toUpperCase().equals(getResources().getString(R.string.S)))
                    targetOrientation = 6;
                else if (words[0].toUpperCase().equals(getResources().getString(R.string.E)))
                    targetOrientation = 3;
                else if (words[0].toUpperCase().equals(getResources().getString(R.string.W)))
                    targetOrientation = 9;
                else
                    tvInfo.setText(getResources().getString(R.string.try_again));

                errorMargin = Double.parseDouble(words[1]);

                // Activamos el tracking
                currentlySearching = true;
            }
        }
    }


    /**
     * Registramos el listener de eventos de orientación
     */
    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    /**
     * Eliminamos el listener
     */
    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }


    /**
     * Este método se encarga de procesar los eventos de sensores, en este caso, la rotación
     * del dispositivo y el movimiento en consecuencia de la imagen de la rosa de los vientos.
     * @param event Evento que ha generado la llamada al Listener, conteniendo datos de rotación.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // get the angle around the z-axis rotated

        float degree = Math.round(event.values[0]);


        tvHeading.setText(getResources().getString(R.string.heading) + Float.toString(degree) + getResources().getString(R.string.degrees));

        // create a rotation animation (reverse turn degree degrees)

        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);

        // how long the animation will take place
        ra.setDuration(210);

        // set the animation after the end of the reservation status
        ra.setFillAfter(true);

        // Start the animation
        image.startAnimation(ra);
        currentDegree = -degree;

        if (currentlySearching)
            checkTargetOrientation();
    }

    public void checkTargetOrientation() {
        int currentOrientation = (int) (-currentDegree);
        int lowerLimit, upperLimit, target, d1, d2, min, margin;
        int blue, red;
        double gradient;

        margin = (int) errorMargin;
        target = (int) targetOrientation * 30;
        lowerLimit = ((target - margin) % 360);
        upperLimit = ((target + margin) % 360);

        // Calculamos la distancia entre el target y el current, y entre el current y el target
        // La operación % no funciona de la forma esperada?
        d1 = (target - currentOrientation);
        d2 = (currentOrientation - target);
        // Los pasamos a su complemento a 360 (hacer d%360)
        if (d1 < 0)
            d1 = 360 + d1;
        if (d2 < 0)
            d2 = 360 + d2;

        min = d1;
        if (d2 < min)
            min = d2;

        // Cambiamos el color de acuerdo a la distancia
        // Azul mide lejanía, rojo cercanía
        gradient = ((double) min) / 180;
        // Lo llevamos de [0,1] a [0,255]
        gradient *= 245;
        // Si gradient == 1, azul (frío)
        blue = 10 + (int) gradient;
        // Si gradient == 0, rojo (caliente)
        red = 255 - ((int) gradient);


        if (min < margin) {
            currentlySearching = false;
            tvInfo.setText(getResources().getString(R.string.status_completion));
        } else {
            background.setBackgroundColor(Color.parseColor("#" + Integer.toHexString(red) + "00" + Integer.toHexString(blue)));
            tvInfo.setText(getResources().getString(R.string.info_target) + " >" + Integer.toString(lowerLimit) + " <" + Integer.toString(upperLimit));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }


    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://nodomain.appfotovoz/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://nodomain.appfotovoz/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}