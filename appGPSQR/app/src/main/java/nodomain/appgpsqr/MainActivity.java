package nodomain.appgpsqr;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import android.support.v4.content.ContextCompat;


import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;


import android.net.Uri;
import android.util.Log;
import android.view.View;


import android.content.Intent;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;


/*
 Aplicación que hace uso del GPS y del lector de códigos QR. Para empezar, lee un código QR con
 el formato correcto, lo decodifica, guarda la localización obtenida como el objetivo a alcanzar,
 la muestra en el mapa, y después espera a que el usuario la alcance con un margen de 15 metros.
 Durante el proceso, la aplicación indica al usuario la distancia que le queda por recorrer en línea
 recta hasta el objetivo.

 No he conseguido hacer la parte final, en la que muestra el recorrido seguido.

 Referencias:
 Lector de QR: https://androidcookbook.com/Recipe.seam?recipeId=3324

 Tutoriales para el uso de mapas:
 http://stackoverflow.com/questions/5096192/create-an-android-gps-tracking-application
 http://www.androidhive.info/2013/08/android-working-with-google-maps-v2/
 https://github.com/googlemaps/android-samples/blob/master/ApiDemos/app/src/main/java/com/example/mapdemo/MyLocationDemoActivity.java
 */



public class MainActivity extends FragmentActivity
        implements
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    // Cliente de Google API
    private GoogleApiClient client;

    // Gestor de localizaciones
    private LocationManager myLocManager;
    // Escucha de localizaciones
    private android.location.LocationListener myLocListener;

    // Booleano para marcar permiso denegado
    private boolean mPermissionDenied = false;

    // Instancia de Google Map
    public GoogleMap mMap;

    // Latitud/longitud objetivo y actual
    LatLng targetCoordinates;
    LatLng currentCoordinates;

    // Localizaciones actual y objetivo
    Location currentLocation, targetLocation;

    // Latitud y longitud del objetivo y actuales
    double targetLat, targetLon, currentLat, currentLon;
    // Margen de metros dentro del que consideramos dos lugares iguales
    double equality_margin;

    // Elementos UI
    Button button;
    TextView tvQRStatus, tvResult, tvTargetLatitudeVal, tvTargetLongitudeVal;

    /**
     * Inicialización de variables/interfaz
     * @param savedInstanceState Instancia con datos recibida por la actividad
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        equality_margin = 15;

        // UI
        tvResult = (TextView) findViewById(R.id.tvResult);
        tvQRStatus = (TextView) findViewById(R.id.tvQRStatus);
        tvTargetLatitudeVal = (TextView) findViewById(R.id.tvLatitudeValue);
        tvTargetLongitudeVal = (TextView) findViewById(R.id.tvLongitudeValue);

        currentLocation = new Location("dummy_provider");

        tvResult.setText(String.format(getResources().getString(R.string.distance_to),0.0f));


        button = (Button)  findViewById(R.id.butQR);
        HandleClick hc = new HandleClick();
        button.setOnClickListener(hc);

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        // Inicializamos el loc. listener aquí
        startLocationUpdates();


        SupportMapFragment mapFragment = (SupportMapFragment) this.getSupportFragmentManager()
                .findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this);
    }

    /**
     * Conecta el cliente de la API de Google, para realizar la lectura de QR.
     */
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
                Uri.parse("android-app://nodomain.appgpsqr/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);


    }

    /**
     * Desconecta el cliente de la API de Google.
     */
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
                Uri.parse("android-app://nodomain.appgpsqr/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    /**
     * Función que maneja el evento de clicks. Esta es algo diferente de las utilizadas en el resto
     * de aplicaciones; en lugar de ser específica de cada botón, esta se dispara ante cualquier
     * evento de click, reconoce si el click ha sido en el botón, y actúa en consecuencia.
     * En nuestro caso, lanza el lector de códigos QR.
     */
    private class HandleClick implements OnClickListener {
        public void onClick(View arg0) {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            switch (arg0.getId()) {
                case R.id.butQR:
                    intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                    break;
            }
            startActivityForResult(intent, 0);    //Barcode Scanner to scan for us
        }
    }

    /**
     * Función llamada cuando el mapa está listo. En ella, activamos permisos, obtenemos nuestro
     * objeto mapa, e inicializamos el servicio de localización.
     * @param googleMap Parámetro mapa pasado a la función de manejo del evento mapa listo.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            mPermissionDenied = true;
        } else if (mMap != null)
            mMap.setMyLocationEnabled(true);

    }

    /**
     * Función auxiliar que actualiza los valorse actuales de posición del usuario, y detecta
     * si está suficientemente cerca del objetivo para considerar que ha llegado al mismo.
     * @param currentLoc Localización actual GPS
     */
    public void updateCurrentCoordinates(Location currentLoc){
        // Actualizamos los valores
        currentLat = currentLoc.getLatitude();
        currentLon = currentLoc.getLongitude();

        currentCoordinates = new LatLng(currentLat, currentLon);
        currentLocation.setLatitude(currentLat);
        currentLocation.setLongitude(currentLon);

        // Comprobamos si hemos llegado al objetivo
        if((currentLocation != null) && (targetLocation != null)){
            tvResult.setText(String.format(getResources().getString(R.string.distance_to), targetLocation.distanceTo(currentLocation)));
            if(targetLocation.distanceTo(currentLocation) < equality_margin)
                tvResult.setText(String.format(getResources().getString(R.string.status_target_reached), equality_margin));
        }
    }

    /**
     * Función encargada de comenzar las actualizaciones de posición, cada 5 segundos
     */
    public void startLocationUpdates() {
        // Asignamos el location manager al recurso del sistema
        myLocManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Creamos un nuevo loc. listener.
        myLocListener = new android.location.LocationListener() {

            // Cuando cambie de estado, actualizamos la posición
            public void onStatusChanged(String provider, int status, Bundle extras) {
                try {
                    Location loc = myLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    updateCurrentCoordinates(loc);
                } catch (SecurityException e) {
                    Log.d("dbg", "Acceso no autorizado");
                }
            }
            // Cuando se habilite el proveedor GPS, actualizamos localización
            public void onProviderEnabled(String provider) {
                try {
                    Location loc = myLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    updateCurrentCoordinates(loc);
                } catch (SecurityException e) {
                    Log.d("dbg", "Acceso no autorizado");
                }
            }

            public void onProviderDisabled(String provider) {
            }


            // Cuando se cambia la localización, se actualiza la l. actual
            @Override
            public void onLocationChanged(Location location) {
                if(location != null)
                    updateCurrentCoordinates(location);
            }
        };


        // Activamos las actualizaciones gps periódicas
        try {
            myLocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0,
                     myLocListener);
        } catch (SecurityException e) {
            Log.d("dbg", "Acceso no autorizado");
        }
    }


    /**
     * Función que recibe los datos de la petición de lectura de código QR
     * @param requestCode Código de la petición que genera la respuesta
     * @param resultCode Código de la respuesta
     * @param intent Los datos de la propia petición
     */
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            TextView tvStatus = (TextView) findViewById(R.id.tvQRStatus);
            if (resultCode == RESULT_OK) {
                // Mostramos el resultado obtenido en la lectura de QR
                tvStatus.setText(intent.getStringExtra("SCAN_RESULT_FORMAT"));
                fetchCoordinates(intent.getStringExtra("SCAN_RESULT"));
            } else if (resultCode == RESULT_CANCELED) {
                tvStatus.setText(getResources().getString(R.string.qr_def));
                tvStatus.setText(getResources().getString(R.string.qr_err));
            }

            // Comenzar navegación
            // Colocamos un marcador en la posición objetivo
            mMap.addMarker(new MarkerOptions().position(targetCoordinates).title("Objetivo"));
            // Ahora centramos la camara alrededor de la localización objetivo y la actual
            if(targetCoordinates != null && currentCoordinates != null)
                includePointsOnView(targetCoordinates, currentCoordinates);
        }
    }

    /**
     * Función que incluye los puntos p1 y p2 en el mapa, con un pequeño margen extra, mediante una animación
     * Para ello, calculamos dos puntos que corersponden a las esquinas superior derecha e inferior izquierda
     * a partir de los puntos a mostrar.
     * @param p1 Punto a incluir en el mapa
     * @param p2 Otro punto a incluir en el mapa
     */
    public void includePointsOnView(LatLng p1, LatLng p2){
        // Calculamos los límites superior derecho e inferior izquierdo
        double lat_n, lon_e;
        double lat_s, lon_w;

        // Asumimos p1=ne p2=sw
        lat_n = p1.latitude;
        lon_e = p1.longitude;

        lat_s = p2.latitude;
        lon_w = p2.longitude;

        // Comprobamos los límites
        if(p2.latitude > lat_n)
            lat_n=p2.latitude;
        if(p2.longitude > lon_e)
            lon_e=p2.longitude;
        if(p1.latitude < lat_s)
            lat_s=p1.latitude;
        if(p1.longitude < lon_w)
            lon_w=p1.longitude;

        // Añadimos un poco de margen (30% de la diferencia de cada coordenada)
        double diffLat, diffLong;
        diffLat = Math.abs(lat_n - lat_s)*0.3;
        diffLong = Math.abs(lon_e - lon_w)*0.3;
        lat_n+=diffLat;
        lon_e+=diffLong;

        lat_s-=diffLat;
        lon_w-=diffLong;


        LatLngBounds bounds = new LatLngBounds(new LatLng(lat_s,lon_w), new LatLng(lat_n,lon_e));

        this.mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds,1));
    }


    /**
     * Función que se encarga de decodificar el texto recibido del lector de QR, y transformarlo a
     * un par de coordenadas latitud/longitud con la ayuda de decodeCoordinates(...)
     * @param coordinatesText texto en bruto obtenido por el lector de QR
     */
    public void fetchCoordinates(String coordinatesText) {
        // Pasamos el texto recibido a unas coordenadas:
        // Primero, separamos el string con las "_"
        String[] parts = coordinatesText.split("_");

        if(parts.length == 4 && parts[0].equals("LATITUD") && parts[2].equals("LONGITUD")) {
            // Decodificamos las cadenas y las mostramos al usuario
            tvTargetLatitudeVal.setText(String.format(getResources().getString(R.string.latitude_value), decodeCoordinate(parts[0], parts[1])));
            tvTargetLongitudeVal.setText(String.format(getResources().getString(R.string.longitude_value), decodeCoordinate(parts[2], parts[3])));

            // Actualizamos las coordenadas objetivo una vez las tenemos
            this.targetCoordinates = new LatLng(this.targetLat, this.targetLon);
            // Creamos la localización objetivo con un proveedor falso
            this.targetLocation = new Location("dummy_provider");
            this.targetLocation.setLatitude(targetLat);
            this.targetLocation.setLongitude(targetLon);
        }
    }

    /**
     * Función auxiliar a fetchCoordinates. Esta función recibe un tipo (latitud/longitud) y un
     * número correspondiente a la latitud o longitud de dicho tipo, y asigna a las variables de
     * estado de la aplicación los valores correspondientes
     * @param type tipo de dato coordenada (latitud/longitud)
     * @param data valor de la coordenada
     * @return Cadena de texto con los valores correctamente formateados para lectura del usuario
     */
    public String decodeCoordinate(String type, String data){

        // Tendremos en parts LATITUD | latitud | LONGITUD | longitud
        // Pasamos la longitud y latitud a grados, minutos, segundos, orientación
        // Primero, latitud. Pasamos la latitud a g,m,s,o
        double original, coordinate = Double.parseDouble(data);
        original = coordinate;

        // Tomamos la orientación
        String orientation;

        if(type.equals("LATITUD")){
            targetLat = original;
            if(original < 0)
                orientation = "S";
            else
                orientation = "N";
        }
        else{
            targetLon = original;
            if(original < 0)
                orientation = "W";
            else
                orientation = "E";
        }

        // Nos quedamos con el valor absoluto
        coordinate = Math.abs(coordinate);

        // Ahora en lat tenemos el valor de la latitud
        int deg = (int) coordinate;// Math.floor(coordinate);
        // Restamos los grados
        coordinate = coordinate-deg;
        // Pasamos a minutos
        coordinate = coordinate*60;

        // Repetimos el proceso
        int min = (int) coordinate;// Math.floor(coordinate);
        // Restamos los minutos
        coordinate = coordinate-min;
        // Pasamos a segundos
        coordinate = coordinate*60;

        // Redondeamos los segundos al segundo decimal
        double sec = Math.round(coordinate*10);
        sec = sec/10;

        // Introducimos cada valor en un string
        String finalText;

        finalText = Integer.toString(deg) + "º" + Integer.toString(min) + "'" + String.valueOf(sec) + "''" + orientation;

        return finalText;
    }
}
