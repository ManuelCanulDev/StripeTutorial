package com.manuel.stripeexample;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.exception.AuthenticationException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    EditText edtTarjeta,edtFecha,edtCvv;

    Stripe stripe;
    String amount;
    Card card;
    String tok;

    String tarjetaNumero;
    String tarjetaFecha;
    String tarjetaCvv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //INSTANCIAMOS STRIPE PARA PODER LLARMARLO EN CUALQUIER PARTE DEL CODIGO
        try {
            stripe = new Stripe("AQUI_TU_KEY_PUBLICA_DE_STRIPE");
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }

        //INSTANCIAMOS LOS EDITTEXT PARA LOS DATOS DE LA TARJETA
        edtTarjeta = (EditText)findViewById(R.id.edtNumeroTarjeta);
        edtFecha = (EditText)findViewById(R.id.edtDate);
        edtCvv = (EditText)findViewById(R.id.edtCvv);
    }

    public void OnClickProcesarPago(View view) {

        //OBTENEMOS LOS VALORES DE LA TARJETA
        tarjetaNumero = edtTarjeta.getText().toString();
        tarjetaFecha = edtFecha.getText().toString();
        tarjetaCvv = edtCvv.getText().toString();

        //VALIDAMOS LOS CAMPOS DE TEXTO
        if (tarjetaNumero.equals("") || tarjetaNumero.length() < 16) {
            Toast.makeText(MainActivity.this, "Ingresa un numero valido", Toast.LENGTH_SHORT).show();
        } else if (tarjetaFecha.equals("")) {
            Toast.makeText(MainActivity.this, "Ingresa una fecha valida", Toast.LENGTH_SHORT).show();
        } else if (tarjetaCvv.equals("") || tarjetaCvv.length() < 3) {
            Toast.makeText(MainActivity.this, "Ingresa un cvv valido", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Tarjeta Agregada", Toast.LENGTH_SHORT).show();

            //SEPARAMOS EL MES Y LA FECHA
            String string = edtFecha.getText().toString();
            String[] parts = string.split("/");

            //CREAMOS NUESTRA TARJETA CON -> NUMERO DE TARJETA | MES | AÑO | CVV DE LA TARJETA
            card = new Card(
                    edtTarjeta.getText().toString(),
                    Integer.valueOf(parts[0]),
                    Integer.valueOf(parts[1]),
                    edtCvv.getText().toString()
            );

            //LE ASIGNAMOS LA MONEDA DE LA TARJETA
            card.setCurrency("mxn");

            //LE ASIGNAMOS EL NOMBRE DEL TITULAR DE LA TARJETA -> SE RECOMIENDA PEDIRLO EN UNA CAJA DE TEXTO
            card.setName("Nombre");

            //CREAMOS EL TOKEN DE STRIPE EN DONDE SE LE PASA LA TARJETA Y LA KEY PUBLICA
            stripe.createToken(card, "AQUI_TU_KEY_PUBLICA_DE_STRIPE", new TokenCallback() {

                public void onSuccess(Token token) {
                    // TODO: Send Token information to your backend to initiate a charge

                    Toast.makeText(getApplicationContext(), "Token created: " + token.getId(), Toast.LENGTH_LONG).show();

                    //OBTENEMOS EL ID
                    tok = token.getId();

                    //OBTENEMOS EL MONTO EN TEXTO
                    amount = "120.00";

                    //LO CONVERTIMOS EN DOUBLE PARA PODER MULTIPLICARLO POR 100
                    double previo = Double.parseDouble(amount)*100; //--> ESTO SE HACE PORQUE ESTAMOS USANDO PESOS MEXICANOS SI FUERAN EUROS SERIAN 1000 ETC ETC

                    int entero = (int) previo;

                    int inte =Integer.parseInt(String.valueOf(entero));

                    //AGREGAMOS LA DESCRIPCION DEL PAGO
                    String descripcion = "Pago de X/Y cosa";

                    //NUEVAMENTE LA MONEDA EN COMO SE COBRA EL EFECTIVO
                    String moneda = "mxn";

                    //LLAMAMOS PROCESAR PAGO Y LE PASAMOS LA --> DESCRIPCION - TOKEN QUE REALIZAMOS - MONTO Y MONEDA (CURRENCY)
                    procesarPago(descripcion,tok,"" + inte,moneda);
                }

                public void onError(Exception error) {
                    Log.d("Stripe", error.getLocalizedMessage());
                }
            });
        }
    }

    public void procesarPago(String _description, String _token,String _amount, String moneda){

        //SOLO LLAMAMOS A NUESTRA CLASE PAGO QUE ES DONDE NOS COMUNICAREMOS CON NUESTRO WEBSERVICE
        Pago pago = new Pago(_amount,_token,_description,moneda);
        pago.execute();

    }

    class Pago extends AsyncTask<Void, Void, String> {

        //DECLARAMOS LAS VARIABLES DE TRABAJO
        String _amount;
        String _token;
        String _description;
        String _currency;

        //DECLARAMOS EL CONSTRUCTOR PARA ASIGNAR A LAS VARIABLES
        public Pago(String _amount, String _token, String _description, String _currency) {
            this._amount = _amount;
            this._token = _token;
            this._description = _description;
            this._currency = _currency;
        }

        //CREAMOS UN PROGRESSDIALOG PARA EL USUARIO
        ProgressDialog pdLoading = new ProgressDialog(MainActivity.this);

        //LO MOSTRAMOS ANTES DE EJECUCION Y DURANTE
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //this method will be running on UI thread
            pdLoading.setMessage("\tProcesando Pago...");
            pdLoading.setCancelable(false);
            pdLoading.show();
        }


        //MIENTRAS ESTO OCURRE NOSOTROS OBTENEMOS LAS VARIABLES Y LAS MAPEAMOS CON AYUDA DE NUESTRO REQUEST HANDLER (CLASE)
        //Y LLAMAMOS AL METODO SENDPOSTREQUEST CON LOS PARAMETROS DE FUNCIONAMIENTO
        @Override
        protected String doInBackground(Void... voids) {
            //creating request handler object
            RequestHandler requestHandler = new RequestHandler();

            //creating request parameters
            HashMap<String, String> params = new HashMap<>();
            params.put("Content-Type", "application/json");
            params.put("method", "charge");
            params.put("amount", _amount);
            params.put("currency", _currency);
            params.put("source", _token);
            params.put("description", _description);

            //returing the response
            return requestHandler.sendPostRequest("https://tuhosting/tucarpetadeservicio/charge.php", params);
        }

        //CUANDO NOS RETORNA INFORMACION
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //OCULTAMOS EL PROGRESSDIALOG
            pdLoading.dismiss();

            try {
                //respuesta de conversión a objeto json
                JSONObject obj = new JSONObject(s);
                //RECORDEMOS QUE EN NUESTRO WEBSERVICES DEVOLVEMOS TRUE/FALSE EN ERROR SI ERROR ES FALO SIGNIFICA QUE EL PAGO SE PROCESO Y
                //MOSTRAMOS LA RESPUESTA EN UN TOAST
                if (!obj.getBoolean("error")) {

                    Toast.makeText(getApplicationContext(), obj.getString("message"), Toast.LENGTH_LONG).show();

                }else{

                    //SI EL PAGO NO SE EFECTUO TAMBIEN MOSTRAMOS EL ERROR
                    Toast.makeText(getApplicationContext(), obj.getString("message"), Toast.LENGTH_LONG).show();

                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Exception: " + e, Toast.LENGTH_LONG).show();
            }
        }

    }
}
