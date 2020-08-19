package com.example.tv.monapplicationmqtt;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.LocationResult;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @class MainActivity
 * @brief Activité principale de l'application (Thread UI)
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "MainActivity"; //!< le TAG de la classe pour les logs
    final int ID_Intent_ParametresConnexion = 1; //!< l'ID de l'Intent ParametresConnexion
    ClientMQTT clientMQTT = null;
    MenuItem menuConnexion = null;

    // Paramètres TTN
    String serverTTN = "test.mosquitto.org";
    int portTTN = 1883;
    String applicationId = "LeVelo";
    String deviceId = "LeVelo251996";
    //String password = "ttn-account-v2.vC-aqMRnLLzGkNjODWgy81kLqzxBPAT8_mE-L7U2C_w";

    TextView txtEtatConnexion;
    TextView txtResultatRequete;
    String publishTopicStatus = "Freebike/251996/state";
    String publishAccel = "Freebike/251996/Accel";
    String publishStart = "Start!";
    String publishStop = "Stop!";
    String publishFinish = "Finish";
    private SensorManager sensorManager;
    private Sensor sensor;
    Float xAccel;
    Float yAccel;
    Float zAccel;





    int increment = 4;
    //MyLocation myLocation = new MyLocation();



    /**
     * @fn onCreate
     * @brief Création de l'activité principale
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(MainActivity.this,sensor,SensorManager.SENSOR_DELAY_NORMAL);

        // accès aux éléments de l'IHM
        txtEtatConnexion = (TextView) this.findViewById(R.id.txtEtatConnexion);
        txtResultatRequete = (TextView) this.findViewById(R.id.txtResultatRequete);

        clientMQTT = new ClientMQTT(this.getApplicationContext());



        clientMQTT.textViewLatitude = (TextView) this.findViewById(R.id.textViewLatitude);
        clientMQTT.textViewLongitude = (TextView) this.findViewById(R.id.textViewLongitude);

        //Button pause = findViewById(R.id.pause);
        //Button resume = findViewById(R.id.resume);
        final Button finish = findViewById(R.id.Finish);
        final Button startpause = findViewById(R.id.start);
        //pause.setOnClickListener(this);
        //resume.setOnClickListener(this);
        startpause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String check = startpause.getText().toString();
                if (check == "Start") {
                    clientMQTT.publier(publishStart,publishTopicStatus);

                    startpause.setText("Pause");
                    finish.setVisibility(View.VISIBLE);
                    Timer t = new Timer();
                    t.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            clientMQTT.publier("X = "+xAccel+", Y ="+yAccel+", Z ="+zAccel,publishAccel);
                        }},0,1000);
                }
                else {
                    clientMQTT.publier(publishStop,publishTopicStatus);
                    startpause.setText("Start");
                }
            }
        });
        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clientMQTT.publier(publishFinish,publishTopicStatus);
                startpause.setText("Start");
                finish.setVisibility(View.INVISIBLE);

            }
        });

        Log.d("GPS", "onCreate");
        clientMQTT.initialiserLocalisation();
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int j){

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent){
        Log.d(TAG, "onSensorChanged: ="+sensorEvent.values[0]+"Y:"+sensorEvent.values[1]+"Z:"+sensorEvent.values[2]);
        xAccel = sensorEvent.values[0];
        yAccel = sensorEvent.values[1];
        zAccel = sensorEvent.values[2];
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        clientMQTT.arreterLocalisation();
    }



    private void demarrerMQTT()
    {
        clientMQTT.reconnecter();


        clientMQTT.mqttAndroidClient.setCallback(new MqttCallbackExtended()
        {
            @Override
            public void connectComplete(boolean b, String s)
            {

                Log.w(TAG,"connectComplete");
                afficherEtatConnexion("Mosquitto Connecté");
                if(menuConnexion != null)
                    menuConnexion.setTitle("Déconnecter");
            }


            @Override
            public void connectionLost(Throwable throwable)
            {
                Log.w(TAG,"connectionLost");
                afficherEtatConnexion("Mosquitto Déconnecté");
                if(menuConnexion != null)
                    menuConnexion.setTitle("Connecter");
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception
            {
                Log.w(TAG, "messageArrived : " + mqttMessage.toString());
                txtResultatRequete.setText(mqttMessage.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken)
            {
                Log.w(TAG, "deliveryComplete");

            }

        });
    }

    private void arreterMQTT()
    {
        if(clientMQTT.estConnecte())
          clientMQTT.deconnecter();
    }

    /**
     * @fn afficherEtatConnexion
     * @brief Affichage l'état de la connexion MySQL
     */
    public void afficherEtatConnexion(String message)
    {
        txtEtatConnexion.setText(message);
    }

    /**
     * @fn afficherResultatRequete
     * @brief Affichage du résultat d'une requete
     */
    public void afficherResultatRequete(String resultat)
    {
        txtResultatRequete.setText(resultat);
    }

    /**
     * @fn parametrer
     * @brief Paramétrage de la connexion
     */
    private void parametrer()
    {
        // Crée et démarre une activité
        Intent intent = new Intent(MainActivity.this, ParametresConnexionActivity.class);
        // Passage de données
        intent.putExtra("serverTTN", serverTTN);
        intent.putExtra("portTTN", portTTN);
        intent.putExtra("applicationId", applicationId);
        intent.putExtra("deviceId", deviceId);
        //intent.putExtra("password", password);
        //startActivity(intent);
        startActivityForResult(intent, ID_Intent_ParametresConnexion);
    }

    /**
     * @fn onActivityResult
     * @brief
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Log.d(TAG, "requestCode=" + requestCode);
        Log.d(TAG, "resultCode=" + resultCode);
        if (requestCode == ID_Intent_ParametresConnexion) {
            //if(resultCode == RESULT_CANCELED)
            //...
            if (resultCode == RESULT_OK) {
                serverTTN = intent.getStringExtra("serverTTN");
                portTTN = intent.getIntExtra("portTTN", portTTN);
                applicationId = intent.getStringExtra("applicationId");
                deviceId = intent.getStringExtra("deviceId");
                //password = intent.getStringExtra("password");

                Log.d(TAG, "serverTTN : " + serverTTN);
                Log.d(TAG, "portTTN : " + portTTN);
                Log.d(TAG, "applicationId : " + applicationId);
                Log.d(TAG, "deviceId : " + deviceId);
                //Log.d(TAG,"password : " + password);

                clientMQTT.setParametres(serverTTN, portTTN, applicationId, deviceId);
            }
        }
    }

    /**
     * @fn onCreateOptionsMenu
     * @brief Création du menu Options (en haut à droite)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * @fn onOptionsItemSelected
     * @brief Sélection d'une entrée du du menu Options (en haut à droite)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Log.d(TAG, "onOptionsItemSelected -> titre : " + item.getTitle());

        if(menuConnexion == null)
            menuConnexion = item;
        int id = item.getItemId();

        switch(id)
        {
            case R.id.action_parametres:
                parametrer();
                break;
            case R.id.action_connexion:
                if(item.getTitle().toString().equals("Connecter"))
                {
                    if(clientMQTT.estConnecte())
                        item.setTitle("Déconnecter");
                    demarrerMQTT();

                }
                else if(item.getTitle().toString().equals("Déconnecter"))
                {
                    arreterMQTT();
                    item.setTitle("Connecter");
                    afficherEtatConnexion("Déconnecté");
                    afficherResultatRequete("");
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

   /* @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.pause:
                clientMQTT.publier(publishStop, publishTopicStatus);
                break;
            case R.id.resume:
                clientMQTT.publier(publishStart, publishTopicStatus);
                break;
        }*/

}
