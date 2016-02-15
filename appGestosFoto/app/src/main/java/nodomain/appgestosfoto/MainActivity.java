package nodomain.appgestosfoto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.camera2.*;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.view.MotionEventCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/*
Apliación que reconoce un patrón de movimientos en la pantalla y realiza una foto. No he conseguido
que la foto se hiciera automáticamente, la tenemos que hacer nosotros manualmente y confirmarla.
Después, podemos ver la miniatura de la foto en la apliación, y repetir el proceso con un botón de
reinicio.

Referencias:
Detección de gesto: http://developer.android.com/training/gestures/detector.html
Toma de la foto y muestra posterior: http://developer.android.com/training/camera/photobasics.html

 */


public class MainActivity extends Activity
{

    // Gesture status:
    // 1- pulsando pantalla
    // 2- movimiento a la izquierda
    // 3- movimiento a la derecha
    // 4- movimiento a la izquierda (gesto completo)
    private int gestureStatus;
    final private int GESTURE_COMPLETION = 4;

    private boolean stopGesture=false;

    private TextView tvInfo, tvInstructions;
    private ImageView mImageView;
    private Button bResetButton;

    private double currentDistX, currentDistY;
    private double lastX, lastY;

    private double screenSizeX, screenSizeY;


    static final int REQUEST_IMAGE_CAPTURE = 1;

    /**
     * Inicialización de variables/interfaz
     * @param savedInstanceState Instancia con datos recibida por la actividad
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);

        // Display de imagen
        mImageView = (ImageView) findViewById(R.id.mImageTaken);

        tvInfo = (TextView) findViewById(R.id.tvInfo);
        tvInstructions = (TextView) findViewById(R.id.tvInstructions);

        // Botón de reset
        bResetButton = (Button) findViewById(R.id.bResetButton);
        bResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gestureStatus=0;
                stopGesture=false;
                mImageView.setImageBitmap(null);
            }
        });

        // Guardamos las dimesiones de la pantalla
        Point sz = new Point();
        getWindowManager().getDefaultDisplay().getSize(sz);

        screenSizeX =(double) sz.x;
        screenSizeY =(double) sz.y;
    }


    /**
     * Función de procesado de la entrada táctil del dispositivo. En ella, vamos a detectar
     * un petrón de movimientos (izquierda 1/4 de pantalla, derecha 1/2 de pantalla, izquierda 1/4
     * de pantalla. Al completarla, lanzará la cámara.
     * @param event evento táctil de movimiento
     * @return evento procesado con éxito
     */
    @Override
    public boolean onTouchEvent(MotionEvent event){
        int action = MotionEventCompat.getActionMasked(event);

        switch (action){

            case (MotionEvent.ACTION_DOWN) :
                // Guardamos la primera posicion
                lastX = event.getX()/screenSizeX;
                lastY = event.getY()/screenSizeY;
                // Comenzamos el gesto
                gestureStatus = 1;
                return true;

            case (MotionEvent.ACTION_UP) :
                if(!stopGesture)
                    tvInstructions.setText(getResources().getString(R.string.instructions_default));
                // Paramos el gesto
                gestureStatus = 0;
                currentDistX=0;
                currentDistY=0;
                return true;

            case (MotionEvent.ACTION_MOVE) :
                if(!stopGesture)
                    detectMotionGesture(event.getX() / screenSizeX, event.getY() / screenSizeY);

                if (gestureStatus == GESTURE_COMPLETION && !stopGesture){
                    tvInstructions.setText(getResources().getString(R.string.completion_text));
                    // Paramos gestos, lo desactivaremos al pulsar el boton reiniciar
                    stopGesture = true;
                    // Hacer foto
                    pic();
                }
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    /**
     * Este método lanza la cámara con un Intent, para que lo recoja onActivityResult
     */
    public void pic(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    /**
     * Esta función se encarga de realizar el reconocimiento de la secuencia de movimientos, y
     * cambiar las variables adecuadas para indicar su completitud.
     * @param currentX valor x actual del cursor (tactil) en la pantalla (relativo al tamaño de la pantalla)
     * @param currentY valor y actual del cursor (tactil) en la pantalla (relativo al tamaño de la pantalla)
     */
    // currentX y currentY son relativas al tamaño de la pantalla (están entre 0 y 1)
    public void detectMotionGesture(double currentX, double currentY){
        currentDistX+=currentX-lastX;
        currentDistY +=currentY-lastY;


        // Buscamos
        if(Math.abs(currentDistY) > 0.0625){
            gestureStatus = 0;
            tvInstructions.setText(getResources().getString(R.string.prompt_cancel));
        }
        if(gestureStatus == 1){
            tvInstructions.setText(getResources().getString(R.string.prompt_left));
            // Comenzamos buscando un movimiento a la izquierda 0.25
            if(currentDistX < -0.25){
                // Reiniciamos la cuenta de distancia
                currentDistX =0;
                currentDistY = 0;
                // Pasamos al siguiente esatdo
                gestureStatus+=1;
            }
        }
        else if (gestureStatus == 2){
            tvInstructions.setText(getResources().getString(R.string.prompt_right));
            // Buscamos un movimiento a la derecha 0.5
            if(currentDistX > 0.5){
                currentDistX=0;
                currentDistY=0;
                gestureStatus+=1;
            }
        }
        else if (gestureStatus == 3){
            tvInstructions.setText(getResources().getString(R.string.prompt_left));
            // Buscamos un movimiento a la izquierda 0.25
            if(currentDistX < -0.25){
                currentDistX=0;
                currentDistY=0;
                gestureStatus+=1;
            }
        }
        // Guardamos los valores actuales para la siguiente llamada
        lastX = currentX;
        lastY = currentY;
    }

    /**
     * Función encargada de recibir el resultado de la foto y mostrarla en la interfaz de la aplicación
     * @param requestCode Código de petición de foto
     * @param resultCode Código de resultado de foto
     * @param data Datos asociados a la respuesta (la propia foto)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(imageBitmap);

            tvInstructions.setText(getResources().getString(R.string.instructions_reset));
        }
    }
}
