package com.example.tv.monapplicationmqtt;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * @class ParametresConnexionActivity
 * @brief Activité de paramétrage de connexion MySQL
 */
public class ParametresConnexionActivity extends AppCompatActivity
{
    private static final String TAG = "ParametresConnexion"; //!< le TAG de la classe pour les logs
    String serverTTN;
    int portTTN = 1883;
    String applicationId;
    String deviceId;
    String password;
    // les éléments de l'IHM
    EditText edtServerTTN;
    EditText edtPortTTN;
    EditText edtApplicationId;
    EditText edtDeviceId;
    //EditText edtPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parametres_connexion);

        Intent intent = getIntent();
        serverTTN = intent.getStringExtra("serverTTN");
        portTTN = intent.getIntExtra("portTTN", portTTN);
        applicationId = intent.getStringExtra("applicationId");
        deviceId = intent.getStringExtra("deviceId");
        password = intent.getStringExtra("password");

        edtServerTTN = (EditText) this.findViewById(R.id.edtServerTTN);
        edtPortTTN = (EditText) this.findViewById(R.id.edtPortTTN);
        edtApplicationId = (EditText) this.findViewById(R.id.edtApplicationId);
        edtDeviceId = (EditText) this.findViewById(R.id.edtDeviceId);
        //edtPassword = (EditText) this.findViewById(R.id.edtPassword);

        edtServerTTN.setText(serverTTN, TextView.BufferType.EDITABLE);
        Integer port = portTTN;
        edtPortTTN.setText(port.toString(), TextView.BufferType.EDITABLE);
        edtApplicationId.setText(applicationId, TextView.BufferType.EDITABLE);
        edtDeviceId.setText(deviceId, TextView.BufferType.EDITABLE);
        //edtPassword.setText(password, TextView.BufferType.EDITABLE);
    }

    public void valider(View view) // Ok
    {
        Intent intent = new Intent();
        serverTTN = edtServerTTN.getText().toString();
        portTTN = Integer.parseInt(edtPortTTN.getText().toString());
        applicationId = edtApplicationId.getText().toString();
        deviceId = edtDeviceId.getText().toString();
        //password = edtPassword.getText().toString();
        intent.putExtra("serverTTN", serverTTN);
        intent.putExtra("portTTN", portTTN);
        intent.putExtra("applicationId", applicationId);
        intent.putExtra("deviceId", deviceId);
        //intent.putExtra("password", password);
        setResult(RESULT_OK, intent);
        finish();
    }
    public void annuler(View view) // Annuler
    {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }
}
