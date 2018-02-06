load('api_config.js');
load('api_gpio.js');
load('api_mqtt.js');
load('api_net.js');
load('api_sys.js');
load('api_timer.js');
load('api_pwm.js');
load('api_adc.js');
load('api_aws.js');

/*********   GLOBALS   ***************/
let topic_out = '/out';
let topic_in = '/in';
let subbed = false;
let tick_tock = false;
/****************************************/

/************ SHADOW STATE ******************/

let shadowState = { 
	photoState: 0, 
	counter: 0 
};

/****************************************/

 
/*********  PINOUT ****************/
let D0 = 16;					
let D1 = 5;				
let D2 = 4;				
let D3 = 0;				
let D4 = 2;				
let D5 = 14;			
let D6 = 12;			
let D7 = 13;			
let D8 = 15;			
/****************************************/


/*********** FUNCTION CONFIG ****************/
let photoIN 	= D1;
let servoPWM 	= D2;
let button		= D3;
let led1 		= D4;  //2
let led2 		= D0;  //16
let adcpin 		= 0;  
//let photoVCC	= D7;
//let servoVCC 	= D6;
//let = ;
//let = ;
/****************************************/


/************ INITIAL CONFIG *********/

/* SERVO CONFIG */
let dutyCycle = 0.15; 
let servoPushDuration = 1000;
//let servoStart = 0.055;
//let servoEnd = 0.095;
let servoState = 0;
//GPIO.set_mode(servoVCC, GPIO.MODE_OUTPUT);
//GPIO.write(servoVCC,1);

/* ADC CONFIG */
let adcen = ADC.enable(adcpin);
print("=I= ADC_ENABLE: ",adcen);

/* PHOTORESISTOR INPUT */
GPIO.set_mode(photoIN,  GPIO.MODE_INPUT);
/****************************************/

/************************************/
GPIO.set_mode(D0,  GPIO.MODE_OUTPUT);
//GPIO.set_mode(D1,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D2,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D4,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D5,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D6,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D7,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D8,  GPIO.MODE_OUTPUT);
/************************************/

/************************************/
GPIO.write(led1,1);
GPIO.write(led2,1);
GPIO.write(D8,1);
/************************************/

function getPhoto()
{
	let photoRead = GPIO.read(photoIN);
	return photoRead;
}



function moveServo()
{
	if (servoState){
		print('=I= moving servo: ',servoState,' -> ',1-servoState);
		PWM.set(servoPWM, 50, 0.055);
		servoState = 0;
	}else{
		print('=I= moving servo: ',servoState,' -> ',1-servoState);
		PWM.set(servoPWM, 50, 0.1);
		servoState = 1;
	}
}

let getInfo = function() {
  return JSON.stringify({
    uptime: Sys.uptime(),
	total_ram: Sys.total_ram(),
    free_ram: Sys.free_ram()
  });
};

function mqtt_in_handler(conn, topic, msg){
	print('=I= Inbound msg on',topic);
	print('=I= MSG: ',msg);
	let s = JSON.parse(msg);
	let message = "<<<EMPTY RESPONSE>>>";
	
	if (s[message] === "get photo")
	{
		print('BUMP');
		let value = getPhoto();
		message = JSON.stringify({photoSatate: value})
		print(value);
	}
	//if (s === 0) let value = GPIO.toggle(D0);
	//if (s === 1) let value = GPIO.toggle(D1);
	//if (s === 2) let value = GPIO.toggle(D2);
	//if (s === 3) let value = GPIO.toggle(D3);
	//if (s === 4) let value = GPIO.toggle(D4);
	//if (s === 5) let value = GPIO.toggle(D5);
	//if (s === 6) let value = GPIO.toggle(D6);
	//if (s === 7) let value = GPIO.toggle(D7);
	//if (s === 8) let value = GPIO.toggle(D8);

	print('=I= Publishing response to',topic_out);
	let res = MQTT.pub(topic_out,message,1);
	print('=I= Publish:' res ? 'SUCCESS' : 'FAIL');
}

Timer.set(2000, true, function() {
  let value = GPIO.toggle(led1);
  if (tick_tock) print('=I=', value ? 'Tick' : 'Tock',"::" ,getInfo());
  //moveServo();
  
  
  //print('=I= ADC',adcpin, ': ',ADC.read(adcpin));
  
//  let photoState = getPhoto();
//  if (photoState === 1){
//	print('=I= PHOTORESISTOR STATE IS: ', photoState);
//	moveServo(1);
//  }else{
//	print('=I= PHOTORESISTOR STATE IS: ', photoState);
//	moveServo(0);
//  }
  
  //let message = getInfo();
  //let ok = MQTT.pub(topic, message, 1);
  //print('=I= Published:', ok, topic, '->', message);
}, null);


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