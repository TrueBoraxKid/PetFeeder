let adc = {	
	/*************************/
	INIT: false,
	/****************************/
	init: function(){
		print('=I= Initializing ADC');
		let res = ADC.enable(0);
		if(res) {
			print('=I= ADC initialized successfully');
			this.INIT = true;
		}
		else print('=I= Failded to initialize ADC');
	},
	
	read: function(){
		if (this.INIT === false) return '=I= ADC not initialized';
		print('=I= Reading ADC');
		let res = ADC.read(0);		
		print('=I= Read: ', res);
		return res;
	},
	
};
