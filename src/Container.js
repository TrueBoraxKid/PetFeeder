let Container = {	
	/*************************/
	
	/****************************/
	
	check: function(){
		
		if (!ContainerLed.enable()){
			return 'Failed to enable container led';
		}
		let res = photo.read();
		ContainerLed.disable();
		return res;
	},
};

let ContainerLed = {
	/*************************/
	
	INIT: false,
	PIN: -1,
		
	/****************************/
	
	
	init: function(pin){
				
		this.PIN = pin;
		
		GPIO.set_mode(this.PIN,GPIO.MODE_OUTPUT);
		
		print('=I= ContainerLed initialized successfully');
		this.INIT = true;
	},
	
	enable: function(){
		if (this.INIT === false) return '=I= ContainerLed not initialized';
		GPIO.write(this.PIN,1);	
	},
	
	disable: function(){
		if (this.INIT === false) return '=I= ContainerLed not initialized';
		GPIO.write(this.PIN,0);	
	},
}