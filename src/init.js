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
GPIO.set_mode(D4,GPIO.MODE_OUTPUT);
let servoState 	= 0;
let servo0		= 0.055;
let servo1		= 0.1;
/****************************************/





function moveServo()
{
	if (servoState){
		print('=I= moving servo: ',servoState,' -> ',1-servoState);
		PWM.set(servoPWM, 50, servo0);
		servoState = 0;
	}else{
		print('=I= moving servo: ',servoState,' -> ',1-servoState);
		PWM.set(servoPWM, 50, servo1);
		servoState = 1;
	}
}

Timer.set(2000, true, function() {
  moveServo();
}, null);


Timer.set(1000, true, function() {
	GPIO.toggle(D4);
}, null);

