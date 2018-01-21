load('api_config.js');
load('api_gpio.js');
load('api_mqtt.js');
load('api_net.js');
load('api_sys.js');
load('api_timer.js');
load('api_pwm.js');

let led = Cfg.get('pins.led');
let button = Cfg.get('pins.button');
let topic_out = '/out';
let topic_in = '/in';
let subbed = false;

/**
 * Initial config
 */

 
/****************************************/
/* D - pinout mapping */
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

/*************ENV************************/
let tick_tock = false;
/****************************************/




// SERVO DEFINE
let servoGpio = D1;
let dutyCycle = 0.3; 
GPIO.set_mode(servoGpio, GPIO.MODE_OUTPUT);
let servoPushDuration = 1000;


//let servoStart = 0.055;
//let servoEnd = 0.095;
//let servoState = 0;

/************************************/
GPIO.set_mode(led, GPIO.MODE_OUTPUT);
GPIO.set_mode(D0,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D1,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D2,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D3,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D4,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D5,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D6,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D7,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D8,  GPIO.MODE_OUTPUT);
/************************************/



/************************************/
//GPIO.write(D0, 0);
//GPIO.write(D1, 0);
//GPIO.write(D2, 0);
//GPIO.write(D3, 0);
////GPIO.write(D4, 0);
//GPIO.write(D5, 0);
//GPIO.write(D6, 0);
//GPIO.write(D7, 0);
//GPIO.write(D8, 0);
/************************************/





function moveServo()
{
	print('moving servo');
	
	
	//move to position 0
	//PWM.set(servoGpio, 50, servoEnd);
	
	//wait for servo
	Timer.set(servoPushDuration, false , function() {
			
			//PWM.set(servoGpio, 50, servoStart);
			PWM.set(servoGpio, 50, dutyCycle);
			//wait for servo
			
			//Timer.set(1000, false , function() {
			//	//turn off
			//	PWM.set(servoGpio, 50, 0);
			//	GPIO.set_mode(servoGpio, GPIO.MODE_INPUT);
			//	print('servo off');
			//}, null);
	}, null);
}

//moveServo();


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
	
	if (s === 0) let value = GPIO.toggle(D0);
	if (s === 1) let value = GPIO.toggle(D1);
	if (s === 2) let value = GPIO.toggle(D2);
	if (s === 3) let value = GPIO.toggle(D3);
	if (s === 4) let value = GPIO.toggle(D4);
	if (s === 5) let value = GPIO.toggle(D5);
	if (s === 6) let value = GPIO.toggle(D6);
	if (s === 7) let value = GPIO.toggle(D7);
	if (s === 8) let value = GPIO.toggle(D8);
	
	//if (msg === "1") print('=I= I`ve got 1!!!!!1111')
	//
	//let value = GPIO.toggle(D0);
	//GPIO.toggle(D1);
	//GPIO.toggle(D2);
	//GPIO.toggle(D3);
	////GPIO.toggle(D4);
	//GPIO.toggle(D5);
	//GPIO.toggle(D6);
	//GPIO.toggle(D7);
	//GPIO.toggle(D8);
	
	
	print('=I= Publishing response to',topic_out);
	let res = MQTT.pub(topic_out,JSON.stringify({led_state: value ? 'Off': 'On'}),1);
	print('=I= Publish:' res ? 'SUCCESS' : 'FAIL');
}

// Blink built-in LED every second

Timer.set(1000 /* 1 sec */, true /* repeat */, function() {
  //let value = GPIO.toggle(D0);
  let value = GPIO.toggle(led);
  if (tick_tock) print('=I=', value ? 'Tick' : 'Tock',"::" ,getInfo());
  //GPIO.toggle(D4);
  //let message = getInfo();
  //let ok = MQTT.pub(topic, message, 1);
  //print('=I= Published:', ok, topic, '->', message);
}, null);

MQTT.setEventHandler(function(conn, ev, edata) {
	if (ev !== 0) print('=I= MQTT event handler: got', ev);
  
	if(ev === MQTT.EV_CONNACK){
		GPIO.write(16, 0); //LED
		if(subbed){	
			print('=I= CONNACK already subscribed');
			return;	
		}
		print('=I= CONNACK subcribing to ', topic_in);
		MQTT.sub(topic_in, mqtt_in_handler);
		subbed = true;
	}
}, null);


// Publish to MQTT topic on a button press. Button is wired to GPIO pin 0
/**
GPIO.set_button_handler(button, GPIO.PULL_UP, GPIO.INT_EDGE_NEG, 200, function() {
  let message = getInfo();
  let ok = MQTT.pub(topic, message, 1);
  print('Published:', ok, topic, '->', message);
}, null);
*/

// Monitor network connectivity.
/**
Net.setStatusEventHandler(function(ev, arg) {
  let evs = '???';
  if (ev === Net.STATUS_DISCONNECTED) {
    evs = 'DISCONNECTED';
  } else if (ev === Net.STATUS_CONNECTING) {
    evs = 'CONNECTING';
  } else if (ev === Net.STATUS_CONNECTED) {
    evs = 'CONNECTED';
  } else if (ev === Net.STATUS_GOT_IP) {
    evs = 'GOT_IP';
  }
  print('== Net event:', ev, evs);
}, null);
*/