package com.example.tv.monapplicationmqtt;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;


public class ClientMQTT {
    private static final String TAG = "ClientMQTT";
    Context context;
    public MqttAndroidClient mqttAndroidClient = null;
    String serverUri = "tcp://test.mosquitto.org";
    int portTTN = 1883;
    String clientId = "LeVelo";
    String subscriptionTopic = "Freebike/251996/Lock";
    String publishTopic = "Freebike/251996/Geoloc";
    Location localisation;

    //String username = "mes_ruches";
    //String password = "ttn-account-v2.vC-aqMRnLLzGkNjODWgy81kLqzxBPAT8_mE-L7U2C_w";
    LocationManager locationManager = null;
    private String fournisseur;
    TextView textViewLatitude;
    TextView textViewLongitude;

    public ClientMQTT(Context context) {
        this.context = context;
        creer();
        //connecter();
    }

    public ClientMQTT(Context context, String serverTTN, int portTTN, String applicationId, String deviceId) {
        this.context = context;
        this.serverUri = "tcp://" + serverTTN + ":" + portTTN;
        this.portTTN = portTTN;
        this.clientId = applicationId;
        this.subscriptionTopic = applicationId + "/devices/" + deviceId + "/up";
        this.publishTopic = applicationId + "/devices/" + deviceId + "/down";
        //this.username = applicationId;
        //this.password = password;
        Log.w(TAG, "MqttAndroidClient : serverUri -> " + serverUri);
        Log.w(TAG, "MqttAndroidClient : applicationId -> " + applicationId);
        Log.w(TAG, "MqttAndroidClient : deviceId -> " + deviceId);
        Log.w(TAG, "MqttAndroidClient : subscriptionTopic -> " + subscriptionTopic);
        Log.w(TAG, "MqttAndroidClient : publishTopic -> " + publishTopic);

        creer();
        //connecter();
    }

    public void creer() {
        Log.w(TAG, "MqttAndroidClient.creer : serverUri -> " + serverUri);
        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.w(TAG, "MqttAndroidClient : connectComplete -> " + s + " (" + b + ")");
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.w(TAG, "MqttAndroidClient : connectionLost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w(TAG, "MqttAndroidClient : messageArrived -> " + mqttMessage.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                Log.w(TAG, "MqttAndroidClient : deliveryComplete");
            }
        });
    }

    public void setCallback(MqttCallbackExtended callback) {
        mqttAndroidClient.setCallback(callback);
    }

    public void connecter() {
        //Log.w(TAG, "MqttAndroidClient.connecter : username -> " + username);
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        //mqttConnectOptions.setUserName(username);
        //mqttConnectOptions.setPassword(password.toCharArray());

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w(TAG, "connecter : onSuccess");
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    souscrire();
                    initialiserLocalisation();
                    //publier("hello-world");
                    Log.d("GPS","PUBLISH OK");
                    publier(localisation.getLatitude() + " , " + localisation.getLongitude(), publishTopic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w(TAG, "connecter : onFailure -> " + serverUri + exception.toString());
                }
            });
        } catch (MqttException ex) {
            ex.printStackTrace();
            Log.e(TAG, "connecter : exception");
        }
    }
    public void reconnecter() {
        Log.w(TAG, "MqttAndroidClient : reconnecter");
        if (estConnecte()) {
            deconnecter();
        }
        connecter();
    }

    public void deconnecter() {
        Log.w(TAG, "MqttAndroidClient : deconnecter");
        Thread deconnexion = new Thread(new Runnable() {
            public void run() {
                try {
                    mqttAndroidClient.disconnect();
                } catch (MqttException e) {
                    e.printStackTrace();
                    Log.e(TAG, "MqttAndroidClient : deconnecter -> exception");
                }
            }
        });
        // Démarrage
        deconnexion.start();
    }

    public boolean estConnecte() {
        Log.w(TAG, "MqttAndroidClient : estConnecte -> " + mqttAndroidClient.isConnected());
        return mqttAndroidClient.isConnected();
    }

    public boolean souscrire(String topic) {
        Log.w(TAG, "MqttAndroidClient : souscrire -> " + topic);
        try {
            final boolean[] retour = {false};
            mqttAndroidClient.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w(TAG, "souscrire : onSuccess");
                    retour[0] = true;
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w(TAG, "souscrire : onFailure");
                    retour[0] = false;
                }
            });
            return retour[0];
        } catch (MqttException ex) {
            Log.e(TAG, "souscrire : exception");
            ex.printStackTrace();
            return false;
        }
    }

    private void souscrire() {
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w(TAG, "souscrire : onSuccess");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w(TAG, "souscrire : onFailure");
                }
            });

        } catch (MqttException ex) {
            Log.e(TAG, "souscrire : exception");
            ex.printStackTrace();
        }
    }

    LocationListener ecouteurGPS = new LocationListener() {
        @Override
        public void onLocationChanged(Location Locale) {
            Toast.makeText(context.getApplicationContext(), fournisseur + " localisation", Toast.LENGTH_SHORT).show();

            Log.d("GPS", "localisation : " + Locale.toString());
            String coordonnees = String.format("Latitude : %f - Longitude : %f\n", Locale.getLatitude(), Locale.getLongitude());
            Log.d("GPS", coordonnees);
            String autres = String.format("Vitesse : %f - Altitude : %f - Cap : %f\n", Locale.getSpeed(), Locale.getAltitude(), Locale.getBearing());
            Log.d("GPS", autres);
            //String timestamp = String.format("Timestamp : %d\n", localisation.getTime());
            //Log.d("GPS", "timestamp : " + timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            Date date = new Date(Locale.getTime());
            Log.d("GPS", sdf.format(date));

            String strLatitude = String.format("Latitude : %f", Locale.getLatitude());
            String strLongitude = String.format("Longitude : %f", Locale.getLongitude());
            textViewLatitude.setText(strLatitude);
            textViewLongitude.setText(strLongitude);
        }

        @Override
        public void onProviderDisabled(String fournisseur) {
            Toast.makeText(context.getApplicationContext(), fournisseur + " désactivé !", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(String fournisseur) {
            Toast.makeText(context.getApplicationContext(), fournisseur + " activé !", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStatusChanged(String fournisseur, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.AVAILABLE:
                    Toast.makeText(context.getApplicationContext(), fournisseur + " état disponible", Toast.LENGTH_SHORT).show();
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Toast.makeText(context.getApplicationContext(), fournisseur + " état indisponible", Toast.LENGTH_SHORT).show();
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Toast.makeText(context.getApplicationContext(), fournisseur + " état temporairement indisponible", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(context.getApplicationContext(), fournisseur + " état : " + status, Toast.LENGTH_SHORT).show();
            }
        }
    };

    public void initialiserLocalisation() {
        if (locationManager == null) {
            locationManager = (LocationManager) context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
            Log.d("GPS", "locationManager : " + locationManager);

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                Log.d("GPS", "no permissions !");
                return;
            }
            Log.d("GPS", "auth : " + "ok");
        }

        localisation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if(localisation != null)
        {
            // on notifie la localisation
            ecouteurGPS.onLocationChanged(localisation);
        }
        Log.d("GPS", "ecouteur ok : ");

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, ecouteurGPS);

        Log.d("GPS", "locationManager : " + locationManager);
    }

    public void arreterLocalisation()
    {
        if(locationManager != null)
        {
            locationManager.removeUpdates(ecouteurGPS);
            ecouteurGPS = null;
        }
    }

    public void publier(String publishMessage, String publishTopic) {

        final MqttMessage message = new MqttMessage();
        message.setPayload(publishMessage.getBytes());
            //Declare the timer
        //Timer t = new Timer();
        //t.scheduleAtFixedRate(new TimerTask() {

                try {
                    mqttAndroidClient.publish(publishTopic, message);
                } catch (MqttException e) {
                    e.printStackTrace();
                }

/**
            },
//Set how long before to start calling the TimerTask (in milliseconds)
                    0,
//Set the amount of time between each execution (in milliseconds)
                    1000);
         **/

    }



    public void setParametres(String serverTTN, int portTTN, String applicationId, String deviceId)
    {
        this.serverUri = "tcp://" + serverTTN + ":" + portTTN;
        this.portTTN = portTTN;
        this.clientId = applicationId;
        this.subscriptionTopic = applicationId + "/devices/" + deviceId + "/up";
        this.publishTopic = applicationId + "/devices/" + deviceId + "/down";
        //this.username = applicationId;
        //this.password = password;
        Log.w(TAG, "MqttAndroidClient : serverUri -> " + serverUri);
        Log.w(TAG, "MqttAndroidClient : applicationId -> " + applicationId);
        Log.w(TAG, "MqttAndroidClient : deviceId -> " + deviceId);
        Log.w(TAG, "MqttAndroidClient : subscriptionTopic -> " + subscriptionTopic);
        Log.w(TAG, "MqttAndroidClient : publishTopic -> " + publishTopic);
    }


}
