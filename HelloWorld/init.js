load('api_gpio.js'); // load is a special function in Mongoose OS
load('api_timer.js');

let led = 4;

// Blink built-in LED every second
GPIO.set_mode(led, GPIO.MODE_OUTPUT);
Timer.set(1000 /* 1 sec */ , true /* repeat */ , function() {
 print("Toggling LED");
 let value = GPIO.toggle(led);
}, null);
