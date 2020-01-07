<?php
define('STRIPE_SECRET_KEY','AQUI_TU_KEY_SECRETA_DE_STRIPE');
define('STRIPE_PUBLIC_KEY','AQUI_TU_KEY_PUBLICA_DE_STRIPE');
header('Content-Type: application/json');

$results = array();

require 'vendor/autoload.php';

\Stripe\Stripe::setApiKey(STRIPE_SECRET_KEY);

if(isset($_POST['method'])){

    $method = $_POST['method'];

    if($method =="charge"){

        //RECIBIMOS LOS DATOS PARA COBRAR
        $amount = $_POST['amount'];
        $currency = $_POST['currency'];
        $source = $_POST['source'];
        $description = $_POST['description'];
       
        try {

            // CREAMOS LA CARGA CON EL METODO DE STRIPE
            $charge = \Stripe\Charge::create(array( 
               'amount'   => $amount, 
               'currency' => $currency, 
               'source' => $source,
               'description' => $description, 
            )); 

            //OBTENEMOS EL RESPONSE Y VALIDAMOS CAMPOS QUE NOS INDIQUEN QUE EL PAGO FUE EXITOSO
            $chargeJson = $charge->jsonSerialize(); 
            if($chargeJson['amount_refunded'] == 0 && empty($chargeJson['failure_code']) && $chargeJson['paid'] == 1 && $chargeJson['captured'] == 1){ 

                //RETORNAMOS AL JSON QUE TODO SALIO BIEN
                $results['error'] = false;
                $results['response'] = "Pago Realizado";
                $results['message'] = "Pago Realizado";                 
        	}

            //SI OCURRIO UN ERROR LO CONTROLAMOS CON CATCH
            //AQUI SE VEN LOS ERRORES SI LA TARJETA NO TIENE FONDOS SUFICIENTES O FUE BLOQUEADA O RECHAZADA.
        } catch(\Stripe\Exception\CardException $e) {
                        // Since it's a decline, \Stripe\Exception\CardException will be caught
                        $results['error'] = true;
                        $results['response'] = "Error";

                        $e_json = $e->getJsonBody();
                        $error_code_decline = $e_json['error'];

                        if($error_code_decline['decline_code'] == "insufficient_funds"){
                            $results['message'] = "Su tarjeta tiene fondos insuficientes.";
                        }

                        if($error_code_decline['decline_code'] == "lost_card"){
                            $results['message'] = "Tu tarjeta fue rechazada.";
                        }

                        if($error_code_decline['decline_code'] == "stolen_card"){
                            $results['message'] = "Esta tarjeta esta reportada como robada.";
                        }

                        if($e->getError()->code == "expired_card"){
                            $results['message'] = "Su tarjeta ha expirado.";
                        }

                        if($e->getError()->code == "incorrect_cvc"){
                            $results['message'] = "El código de seguridad de su tarjeta (CVC) es incorrecto.";
                        }

                        if($e->getError()->code == "processing_error"){
                            $results['message'] = "Se produjo un error al procesar su tarjeta. Inténtalo de nuevo en un momento.";
                        }

                        if($e->getError()->code == "incorrect_number"){
                            $results['message'] = $e->getError()->code;
                        }  
        } catch (\Stripe\Exception\RateLimitException $e) {
                      // Too many requests made to the API too quickly
                        $results['error'] = true;
                        $results['response'] = "Error";
                        $results['message'] = "Demasiadas solicitudes hechas a la API demasiado rápido, pago no realizado.";
        } catch (\Stripe\Exception\InvalidRequestException $e) {
                      // Invalid parameters were supplied to Stripe's API
                        $results['error'] = true;
                        $results['response'] = "Error";
                        $results['message'] = "Se proporcionaron parámetros no válidos a la API de Stripe, pago no realizado.";
        } catch (\Stripe\Exception\AuthenticationException $e) {
                      // Authentication with Stripe's API failed
                      // (maybe you changed API keys recently)
                        $results['error'] = true;
                        $results['response'] = "Error";
                        $results['message'] = "La autenticación con la API de Stripe falló, pago no realizado.";
        } catch (\Stripe\Exception\ApiConnectionException $e) {
                      // Network communication with Stripe failed
                        $results['error'] = true;
                        $results['response'] = "Error";
                        $results['message'] = "La comunicación de red con Stripe falló, pago no realizado.";
        } catch (\Stripe\Exception\ApiErrorException $e) {
                      // Display a very generic error to the user, and maybe send
                      // yourself an email
                        $results['error'] = true;
                        $results['response'] = "Error";
                        $results['message'] = "Error del Servidor, pago no realizado.";
        }

    echo json_encode($results);     
    }else {
        $results['error'] = true;
        $results['response'] = "Error";
        $results['messsage'] = "El nombre del método no es correcto";
        echo json_encode($results);
    }
}else {
    $results['error'] = true;
    $results['response'] = "Error";
    $results['message'] = "No se ha establecido ningún método.";
    echo json_encode($results);
}



