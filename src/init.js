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
load('Container.js');

/*********   GLOBALS   ***************/

let topic_out 				= '/out';
let topic_in 				= '/in';
let topic_feed 				= '/feed';
//1D291A
//2F808B

let STATUS_TOPIC			= '/live';
let SHADOW_GET_TOPIC 		= '$aws/things/esp8266_1D291A/shadow/get';
let SHADOW_GET_ACK_TOPIC	= '$aws/things/esp8266_1D291A/shadow/get/accepted';
let SHADOW_UPDATE_TOPIC 	= '$aws/things/esp8266_1D291A/shadow/update';
let SHADOW_DOCUMENT_TOPIC 	= '$aws/things/esp8266_1D291A/shadow/update/documents';
let SHADOW_UPDATE_ACK_TOPIC = '$aws/things/esp8266_1D291A/shadow/update/accepted';

let shadow_update_frequency = 600; //seconds
let subbed = false;
let feedTimerEnable			= 1;
let feedTime				= 0;

let crush_cnt				= 0;
let is_opened 				= 0;
let shadow_state			= 0;
/****************************************/
 
/*********  PINOUT **********************/
//let D0 = 16;------ Built-in led 2
//let D1 = 5; ------ Photoresistor VCC
//let D2 = 4; ------ Photoresistor data
//let D3 = 0; ------ Container led
//let D4 = 2; ------ Built-in led 1
//let D5 = 14;------ Servo PWM
//let D6 = 12;------ 
//let D7 = 13;------ 
//let D8 = 15;------ 
/****************************************/


/*********** MANUAL MAPPING ****************/
let led1 		= D4;  //2
let led2 		= D0;  //16


let photoVCC	= D1;
let photoIN		= D2;
let servoPWM	= D5;
let contled 	= D3;
let button 		= D8;

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
ContainerLed.init(contled);

//-------------------------
let devicesStatus = checkAllDevices();
if (devicesStatus) 
	print('=I==============================================I=');
	print('=I=====   All devices were initialized   =======I=');
	print('=I==============================================I=');
/** Devices init finish **/





/**********************************************************************
********************** FUNCTIONS **************************************
***********************************************************************/


function custom_mqtt_handler(topic, msg){}

function update_shadow() {
	print('=I= Updating shadow state');
	print('=I= Publishing to', SHADOW_UPDATE_TOPIC);
	let adc_reading = adc.read();
	let container_reading = Container.check();
	shadow_state = JSON.stringify({'state': {'reported': {'container':container_reading,'adc':adc_reading,'crush_count':crush_cnt}}});
	
}

function post_shadow() {
	let shadow_res = MQTT.pub(SHADOW_UPDATE_TOPIC,shadow_state,1);
	print('=I= Publish:' shadow_res ? 'SUCCESS' : 'FAIL');
	MQTT.pub(STATUS_TOPIC,"Updated shadow state",0);
}

GPIO.set_button_handler(button, GPIO.PULL_UP, GPIO.INT_EDGE_NEG, 200, function(x) {
	servo.feed();
	is_opened = 1;
}, null);

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
	
	
	let beep_res = MQTT.pub(STATUS_TOPIC,"beep",1);
	if (beep_res === 1) {print("=I= beep");}
	
	//print ('photo: ',Container.check());
	
	//check feed timer
	let currentTime = Timer.now();
	let delta 		= currentTime % 86400 - feedTime + 2*3600;
	print("=I= Feed timeout: ",delta);
	if (currentTime > 1538593435) {
		if (delta < 60 && delta > 0 && feedTimerEnable) {
			servo.feed();
			feedTimerEnable = 0;
		}
	}
	
}, null);

Timer.set(2*1000, true, function() {
	
	if (is_opened) {
		is_opened = is_opened + 1;
	}
	
	if (is_opened === 2) {
		update_shadow();
	}
	
	
	if (is_opened > 7) {
		is_opened = 0; 
		post_shadow();
	}
}, null);


Timer.set(shadow_update_frequency*1000, true, update_shadow, null);

/**********************************************************************
********************** AWS INTERFACE  *********************************
***********************************************************************/

function mqtt_in_handler(conn, topic, msg){
	let response = "<<<EMPTY RESPONSE>>>";
	
	print('=I= Inbound msg on',topic);
	print('=I= MSG: ',msg);

	if (msg === 'update_shadow') {
		update_shadow();
		Sys.usleep(1000*1000);
		post_shadow();
	}

	if (topic === SHADOW_UPDATE_ACK_TOPIC ) {
		let obj = JSON.parse(msg);
		for (let key in obj.state.reported) {
			if (key === 'feed_time'){
				feedTime = obj.state.reported.feed_time;
				print('=I= New feed time', obj.state.reported.feed_time);
			}
		}
	}
	
	if (msg === 'feed') {
		MQTT.pub(STATUS_TOPIC,"feed_ack",1);
		print('=I= Feeding');
		//servo.open();
		servo.feed();
		is_opened = 1;
		Sys.usleep(1000*1000);
		//update_shadow();
		let now_f = Timer.fmt("%H:%M",Timer.now());
		//MQTT.pub(STATUS_TOPIC,"Feed request complete " + now_f,0);
	}	
}



MQTT.setEventHandler(function(conn, ev, edata) {
	if (ev !== 0) print('=I= MQTT event handler: got', ev);
	if (ev === 5) { crush_cnt = crush_cnt + 1; }
	
	if(ev === MQTT.EV_CONNACK){
		GPIO.write(led2, 0);
		if(subbed){	
			print('=I= CONNACK already subscribed');
			return;	
		}
		print('=I= CONNACK subcribing to ', topic_in);
		MQTT.sub(topic_in, mqtt_in_handler);
		MQTT.sub(topic_feed, mqtt_in_handler);
		MQTT.sub(SHADOW_UPDATE_ACK_TOPIC, mqtt_in_handler);
		
		subbed = true;
	}
	else if (ev === 5){
		GPIO.write(led2, 1);
	}
}, null);