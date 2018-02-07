let D0 = 16;					
let D1 = 5;				
let D2 = 4;				
let D3 = 0;				
let D4 = 2;				
let D5 = 14;			
let D6 = 12;			
let D7 = 13;			
let D8 = 15;
let pins = ['D3',1,'D4',3,'D2','D1',6,7,8,9,10,11,'D6','D7','D5','D8','D0'];

GPIO.set_mode(D0,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D1,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D2,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D3,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D4,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D5,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D6,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D7,  GPIO.MODE_OUTPUT);
GPIO.set_mode(D8,  GPIO.MODE_OUTPUT);

GPIO.set_pull(D0, GPIO.PULL_UP);
GPIO.set_pull(D1, GPIO.PULL_UP);
GPIO.set_pull(D2, GPIO.PULL_UP);
GPIO.set_pull(D3, GPIO.PULL_UP);
GPIO.set_pull(D4, GPIO.PULL_UP);
GPIO.set_pull(D5, GPIO.PULL_UP);
GPIO.set_pull(D6, GPIO.PULL_UP);
GPIO.set_pull(D7, GPIO.PULL_UP);
GPIO.set_pull(D8, GPIO.PULL_UP);

GPIO.write(D0,  0);
GPIO.write(D1,  0);
GPIO.write(D2,  0);
GPIO.write(D3,  0);
GPIO.write(D4,  0);
GPIO.write(D5,  0);
GPIO.write(D6,  0);
GPIO.write(D7,  0);
GPIO.write(D8,  0);

let devices = {
	'Photoresistor':	false,
	'Servo':			false,
	'ContainerLed':		false,
	'ADC':				false,
	//'Weight':			false,
}

updateDevices: function(){
	devices['Photoresistor'	] = photo.INIT,
	devices['Servo'			] = servo.INIT,
	devices['ContainerLed'  ] = ContainerLed.INIT,
	devices['ADC'  			] = adc.INIT,
	//devices['Weight'		] = ,
}

checkAllDevices: function(){
	let res = true;
	updateDevices();
	for(let key in devices)
		if (!devices[key]){
			print('=I= ',key,' is not initialized!');
			res = false;
		}
	return res;
}