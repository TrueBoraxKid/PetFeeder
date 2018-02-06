load('api_config.js');
load('api_gpio.js');
load('api_mqtt.js');
load('api_net.js');
load('api_sys.js');
load('api_timer.js');
load('api_pwm.js');
load('api_adc.js');
load('api_aws.js');

load('photoresistor.js'); 
load('ADC.js');
load('autoconfig.js');
load('Servo.js');

/*********   GLOBALS   ***************/
let topic_out = '/out';
let topic_in = '/in';
let subbed = false;
let tick_tock = false;
/****************************************/
 
/*********  PINOUT ****************/
//let D0 = 16;					
//let D1 = 5;				
//let D2 = 4;				
//let D3 = 0	;				
//let D4 = 2;				
//let D5 = 14;			
//let D6 = 12;			
//let D7 = 13;			
//let D8 = 15;
/****************************************/


/*********** MANUAL CONFIG ****************/
let led1 		= D4;  //2
let led2 		= D0;  //16


let photoVCC	= D1;
let photoIN		= D2;
let servoVCC	= D3;
let servoPWM	= D5;

//let = ;
//let = ;
/****************************************/


/************ INITIAL CONFIG *********/
GPIO.set_pull(led2, GPIO.PULL_DOWN);
GPIO.write(led2,  1);

GPIO.write(servoVCC,1);
/****************************************/


/**********************************************************************
********************** FLOW *******************************************
***********************************************************************/



photo.init(photoVCC,photoIN);
adc.init();
servo.init(servoVCC,servoPWM);



/**********************************************************************
********************** FUNCTIONS **************************************
***********************************************************************/

function custom_mqtt_handler(topic, msg){
}

/**********************************************************************
********************** TIMERS *****************************************
***********************************************************************/


Timer.set(2000, true, function() {
	let value = GPIO.toggle(led1);
	if (tick_tock) print('=I=', value ? 'Tick' : 'Tock',"::" ,getInfo());
	
}, null);


/**********************************************************************
********************** AWS INTERFACE  *********************************
***********************************************************************/

function mqtt_in_handler(conn, topic, msg){
	let response = "<<<EMPTY RESPONSE>>>";
	
	print('=I= Inbound msg on',topic);
	print('=I= MSG: ',msg);
//	let jmsg = JSON.parse(msg);
	
	if (msg === 'readphoto'){
		let reading  = photo.read();
		response = JSON.stringify({'Photoresistor reading':reading});
	}else if(msg === 'readadc'){
		let reading  = adc.read();
		response = JSON.stringify({'ADC reading':reading});
	}else if(msg === 'openservo'){
		servo.open();
		response = JSON.stringify({'Servo ': 'opened'});
	}else if(msg === 'closeservo'){
		servo.close();
		response = JSON.stringify({'Servo ': 'closed'});
	}else if(msg === 'toggleservo'){
		servo.toggle();
		response = JSON.stringify({'Servo ': servo.STATE});
	}else{
		//
	};
	
	print('=I= Publishing response to',topic_out);
	let res = MQTT.pub(topic_out,response,1);
	print('=I= Publish:' res ? 'SUCCESS' : 'FAIL');
}



MQTT.setEventHandler(function(conn, ev, edata) {
	if (ev !== 0) print('=I= MQTT event handler: got', ev);
  
	if(ev === MQTT.EV_CONNACK){
		GPIO.write(led2, 0);
		if(subbed){	
			print('=I= CONNACK already subscribed');
			return;	
		}
		print('=I= CONNACK subcribing to ', topic_in);
		MQTT.sub(topic_in, mqtt_in_handler);
		subbed = true;
	}
	else if (ev === 5){
		GPIO.write(led2, 1);
	}
}, null);