let servo = {	
	/*************************/
	INIT: false,
	VCCPIN: -1,
	PWMPIN: -1,
	CLOSE: 0.05,//0.120,	duty=(1.0+angle/180.0)/20.0
	OPEN:  0.1,//0.03,
	FREQ: 50,
	DELAY: 1000*1000,
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
	
	duty: function(angle){
		return (1.0+angle/180.0)/20.0;
	},
	
	move: function(pos){
		
		//GPIO.write(this.VCCPIN,1);
		
		print('=I= Setting PWM Duty cyle: ',pos);
		let res = PWM.set(this.PWMPIN, this.FREQ, pos);
		print('=I= ',res ? "SUCCESS":"FAIL");
		//Timer.set(this.DELAY,false,function(){},null);
		
		//Sys.usleep(this.DELAY);
		//GPIO.write(this.VCCPIN,0);
		
	},
	
	open: function(){
		if (this.INIT === false) return '=I= Servo not initialized';
		this.move(this.duty(360.0));
	},
	
	close: function(){
		if (this.INIT === false) return '=I= Servo not initialized';
		this.move(this.duty(10.0));
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
	
	feed: function(){
		this.open();
		Sys.usleep(this.DELAY);
		this.close();
		Sys.usleep(this.DELAY);
		//Timer.set(this.DELAY, 0, function(){print('=I= Servo closed');},null);
		//this.move(this.duty(90.0));
		//Sys.usleep(this.DELAY);
		//this.move(this.duty(180.0));
		//Sys.usleep(this.DELAY);
		//this.move(this.duty(270.0));
		//Sys.usleep(this.DELAY);
		//this.move(this.duty(360.0));
		//Sys.usleep(this.DELAY);
		//this.move(this.duty(270.0));
		//Sys.usleep(this.DELAY);
		//this.move(this.duty(180.0));
		//Sys.usleep(this.DELAY);
		//this.move(this.duty(90.0));
		//Sys.usleep(this.DELAY);
		//this.move(this.duty(0.0));
	}
	
};
