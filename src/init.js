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

/****************************************/
 
/*********  PINOUT **********************/
//let D0 = 16;------ Built-in led 2
//let D1 = 5; ------ Photoresistor VCC
//let D2 = 4; ------ Photoresistor data
//let D3 = 0; 
//let D4 = 2; ------ Built-in led 1
//let D5 = 14;------ Servo PWM
//let D6 = 12;
//let D7 = 13;
//let D8 = 15;
/****************************************/


/*********** MANUAL MAPPING ****************/
let led1 		= D4;  //2
let led2 		= D0;  //16


let photoVCC	= D1;
let photoIN		= D2;
//let servoVCC	= D3;
let servoPWM	= D5;

//let = ;
//let = ;
/****************************************/


/************ INITIAL CONFIG *********/
GPIO.set_pull(led2, GPIO.PULL_DOWN);
GPIO.write(led2,  1);

//GPIO.write(servoVCC,1);
/****************************************/


/**********************************************************************
********************** FLOW *******************************************
***********************************************************************/


/** Devices init start ****/

photo.init(photoVCC,photoIN);
adc.init();
servo.init(servoPWM);
ContainerLed.init();

//-------------------------
let devicesStatus = checkAllDevices();
/** Devices init finish **/





/**********************************************************************
********************** FUNCTIONS **************************************
***********************************************************************/

function custom_mqtt_handler(topic, msg){
}

/**********************************************************************
********************** TIMERS *****************************************
***********************************************************************/


Timer.set(2000, true, function() {
	GPIO.toggle(led1);
	if (!devicesStatus){
		print('=I==============================================I=');
		print('=I=====		Not all devices were		=======I=');
		print('=I=====		initialized!!!				=======I=');
		print('=I=====									=======I=');
		print('=I==============================================I=');
		devicesStatus = checkAllDevices();
	}
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
		let reading = servo.open();
		response = JSON.stringify({'Servo ': 'opened', 'return code':reading});
	}else if(msg === 'closeservo'){
		let reading = servo.close();
		response = JSON.stringify({'Servo ': 'closed', 'return code':reading});
	}else if(msg === 'toggleservo'){
		let reading = servo.toggle();
		response = JSON.stringify({'Servo ': servo.STATE, 'return code':reading});
	}else if(msg === 'checkcontainer'){
		let reading = Container.check();
		let status = reading ? 'Full':'Empty';
		response = JSON.stringify({'Container ': reading ? 'Full':'Empty', 'return code':reading});
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