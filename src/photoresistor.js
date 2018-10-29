let photo = {	
	/*************************/
	VCCPIN: -1,
	DATAPIN: -1,
	NAME: 'Hello, World! I`m photoresistor',
	INIT: false,
	DELAY: 1000000,
	
	
	
	/****************************/
	init: function(pin_vcc,pin_data){
		print('=I= Initializing Photoresistor');
		this.VCCPIN = pin_vcc;
		this.DATAPIN = pin_data;
		
		GPIO.set_mode(this.VCCPIN,  GPIO.MODE_OUTPUT);
		GPIO.set_mode(this.DATAPIN, GPIO.MODE_INPUT);
		
		this.INIT = true;
		print('=I= Photoresistor initialized successfully');
	},
	
	read: function(){
		if (this.INIT === false) return '=I= Photo resistor not initialized';
		print('=I= Reading photoresistor');
		GPIO.write(this.VCCPIN,1);
		Sys.usleep(this.DELAY);
		let res = GPIO.read(this.DATAPIN);
		Sys.usleep(this.DELAY);
		GPIO.write(this.VCCPIN,0);
		print('=I= Read: ', res);
		return res;
	},
	
};
