load('api_config.js');
load('api_gpio.js');
load('api_mqtt.js');
load('api_net.js');
load('api_sys.js');
load('api_timer.js');

let led = Cfg.get('pins.led');
let button = Cfg.get('pins.button');
let topic_out = '/PetFeeder_response';
let topic_in = '/PetFeeder_request';
let subbed = false;

//print('LED GPIO:', led, 'button GPIO:', button);

let getInfo = function() {
  return JSON.stringify({
    uptime: Sys.uptime(),
	total_ram: Sys.total_ram(),
    free_ram: Sys.free_ram()
  });
};

function mqtt_in_handler(conn, topic, msg){
	print('=I= Inbound msg on',topic,':',JSON.parse(msg));
	print('=I= Publishing response to',topic_out);
	let res = MQTT.pub(topic_out,'Beep!',1);
}

// Blink built-in LED every second
GPIO.set_mode(led, GPIO.MODE_OUTPUT);
Timer.set(1000 /* 1 sec */, true /* repeat */, function() {
  let value = GPIO.toggle(led);
  //print('=I=', value ? 'Tick' : 'Tock');
  //let message = getInfo();
  //let ok = MQTT.pub(topic, message, 1);
  //print('=I= Published:', ok, topic, '->', message);
}, null);

MQTT.setEventHandler(function(conn, ev, edata) {
	if (ev !== 0) print('=I= MQTT event handler: got', ev);
  
	if(ev === MQTT.EV_CONNACK){
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