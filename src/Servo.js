let servo = {	
	/*************************/
	INIT: false,
	VCCPIN: -1,
	PWMPIN: -1,
	CLOSE: 0.01,
	OPEN:  0.3,
	FREQ: 50,
	DELAY: 2000*1000,
	STATE: -1,
	/****************************/
	init: function(pwmpin){
		print('=I= Initializing Servo');
		
		//this.VCCPIN =  vccpin;
		this.PWMPIN = pwmpin;
		
		//GPIO.set_mode(this.VCCPIN,  GPIO.MODE_OUTPUT);
		GPIO.set_mode(this.PWMPIN,  GPIO.MODE_OUTPUT);
		
		print('=I= Servo initialized successfully');
		this.INIT = true;
	},
	
	move: function(pos){
		
		//GPIO.write(this.VCCPIN,1);
		

		PWM.set(this.PWMPIN, this.FREQ, pos);
		//Timer.set(this.DELAY,false,function(){},null);
		
		//Sys.usleep(this.DELAY);
		//GPIO.write(this.VCCPIN,0);
		
	},
	
	open: function(){
		if (this.INIT === false) return '=I= Servo not initialized';
		this.move(this.OPEN);
	},
	
	close: function(){
		if (this.INIT === false) return '=I= Servo not initialized';
		this.move(this.CLOSE);
	},
	
	toggle: function(){
		if (this.INIT === false) return '=I= Servo not initialized';
		if(this.STATE === 0){
			this.open();
			this.STATE = 1;
		}else{
			this.close();
			this.STATE = 0;
		}
	},
};
