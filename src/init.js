load('api_config.js');
load('api_gpio.js');
load('api_mqtt.js');
load('api_net.js');
load('api_sys.js');
load('api_timer.js');
load('api_aws.js');

print('=======================================');
print('=======================================');
print('=======================================');
print('=======================================');
print('=======================================');

let led = Cfg.get('pins.led');
let button = 0;//Cfg.get('pins.button');
let topic = 'PetFeeder';


let state = { on: false, counter: 0 };  // device state: shadow metadata

// Upon startup, report current actual state, "reported"
// When cloud sends us a command to update state ("desired"), do it

AWS.Shadow.setStateHandler(function(data, event, reported, desired) {
  print('-I- INSIDE AWS.Shadow HANDLER');
  
  if (event === AWS.Shadow.CONNECTED) {
    AWS.Shadow.update(0, {reported: state});  // Report device state
  } else if (event === AWS.Shadow.UPDATE_DELTA) {
    for (let key in state) {
      if (desired[key] !== undefined) state[key] = desired[key];
    }
    AWS.Shadow.update(0, {reported: state});  // Report device state
  }
  print(JSON.stringify(reported), JSON.stringify(desired));
}, null);

//print('LED GPIO:', led, 'button GPIO:', button);


let getInfo = function() {
  return JSON.stringify({
    total_ram: Sys.total_ram(),
    free_ram: Sys.free_ram()
  });
};

//print(Sys.uptime(), getInfo());
//GPIO.set_mode(led, GPIO.MODE_OUTPUT);
//GPIO.write(led,1);


// Blink built-in LED every second
/*******************************************
GPIO.set_mode(led, GPIO.MODE_OUTPUT);
Timer.set(100, true, 
	function() {
		let value = GPIO.toggle(led);
		print(value ? 'Tick' : 'Tock', 'uptime:', Sys.uptime(), getInfo());
}, null);
*******************************************/

// Publish to MQTT topic on a button press. Button is wired to GPIO pin 0


//GPIO.set_button_handler(button, GPIO.PULL_UP,GPIO.INT_EDGE_NEG, 50, function(){
//	
//	let ok = MQTT.pub(topic, message, 1);
//	GPIO.toggle(led);
//	print('Published:', ok, topic, '->', message);
//	
//},true);


GPIO.set_mode(led, GPIO.MODE_OUTPUT);
Timer.set(1000, true, 
	function() {
		let value = GPIO.toggle(led);
		
		print('-I- MQTT IN');
		let ok = MQTT.pub(topic, JSON.stringify('Beep-beep'), 1);
		if(ok) print('sent mqtt')
		else print('failed mqtt');
		print('-I- MQTT OUT');

		//print('-I- Published:', ok, topic, '->', message);
		print('-I- ', value ? 'Tick' : 'Tock');
}, null);

//print('-I- Updating AWS.Shadow');
//AWS.Shadow.update(0,{desired: {on: state.on, counter: state.counter + 1}});



MQTT.setEventHandler(function(conn, ev, edata) 
{
	if (ev === MQTT.EV_CONNACK)
	{
		//let topic = Cfg.get('mqtt.user') + '/feeds/' + feedName;
		let ok = MQTT.pub(topic, JSON.stringify(1), 1);
		if(ok) print('sent mqtt')
		else print('failed mqtt');
		MQTT.setEventHandler(function(){}, null);
	}
}, null);


// Monitor network connectivity.


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

