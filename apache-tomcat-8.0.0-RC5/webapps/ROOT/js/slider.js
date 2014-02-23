//Slider Code

function touchHandler(event)
{
    var touches = event.changedTouches,
        first = touches[0],
        type = "";
         switch(event.type)
    {
        case "touchstart": type = "mousedown"; break;
        case "touchmove":  type="mousemove"; break;        
        case "touchend":   type="mouseup"; break;
        default: return;
    }

    var simulatedEvent = document.createEvent("MouseEvent");
    simulatedEvent.initMouseEvent(type, true, true, window, 1, 
                              first.screenX, first.screenY, 
                              first.clientX, first.clientY, false, 
                              false, false, false, 0/*left*/, null);

                                                                                 first.target.dispatchEvent(simulatedEvent);
    event.preventDefault();
}

function Slider(canvas, options){
	canvas.slider = this;
	this.canvas = canvas;
	for (o in options)
		this[o] = options[o];
	if (this.value == undefined)
		this.value = (this.minVal + this.maxVal)/2;
	
	this.ctx = canvas.getContext('2d');
	
	canvas.onmousedown = function(evt){
		this.slider.mousedown(evt);
	}
	
	canvas.onmousemove = function(evt){
		this.slider.mousemove(evt);
	}
	
	canvas.onmouseup = function(evt){
		this.slider.mouseup(evt);
	}
	
	canvas.addEventListener("touchstart", touchHandler, true);
    canvas.addEventListener("touchmove", touchHandler, true);
    canvas.addEventListener("touchend", touchHandler, true);
    canvas.addEventListener("touchcancel", touchHandler, true);    
	
	this.paint();
}

Slider.prototype.onValueChange = function(){};
Slider.prototype.minVal = 0;
Slider.prototype.maxVal = 99;
Slider.prototype.sliderColor = '#EEEEEE';
Slider.prototype.bgColor = '#CCCCCC';
Slider.prototype.strokeColor = 'black';

Slider.prototype.mousePressed = false;
Slider.prototype.mousedown = function(evt){
	evt.preventDefault();
	this.mousePressed = true;
	var mx = evt.clientX - this.canvas.getBoundingClientRect().left;
	this.value = (mx/this.canvas.width)*(this.maxVal - this.minVal) + this.minVal;
	this.onValueChange();
	this.paint();
}
Slider.prototype.mousemove = function(evt){
	if (this.mousePressed){
		evt.preventDefault();
		var mx = evt.clientX - this.canvas.getBoundingClientRect().left;
		this.value = (mx/this.canvas.width)*(this.maxVal - this.minVal) + this.minVal;
		this.onValueChange();
		this.paint();
	}
}
Slider.prototype.mouseup = function(evt){
	this.mousePressed = false;
}

Slider.prototype.setValue = function(val){
	this.value = val;
	this.paint();
}
Slider.prototype.sliderWidth = function(){
	return Math.floor(Math.min(0.1*this.canvas.width, 10));
}
Slider.prototype.range = function(){
	return this.maxVal - this.minVal;
}
Slider.prototype.paint = function(){
	var sliderWidth = this.sliderWidth();
	var sliderPos = this.canvas.width*(this.value - this.minVal)/this.range();
	var sliderPosStart = Math.floor(sliderPos - this.sliderWidth()/2);
	
	//Background
	this.ctx.beginPath();
	this.ctx.moveTo(0, 0);
	this.ctx.lineTo(this.canvas.width, 0);
	this.ctx.lineTo(this.canvas.width, this.canvas.height);
	this.ctx.lineTo(0, this.canvas.height);
	this.ctx.closePath();
	
	this.ctx.fillStyle = this.bgColor;
	this.ctx.fill();
	this.ctx.strokeStyle = this.strokeColor;
	this.ctx.stroke();
	
	this.ctx.beginPath();
	this.ctx.moveTo(sliderPosStart, 0);
	this.ctx.lineTo(sliderPosStart+sliderWidth, 0);
	this.ctx.lineTo(sliderPosStart+sliderWidth, this.canvas.height);
	this.ctx.lineTo(sliderPosStart, this.canvas.height);
	this.ctx.closePath();
	
	this.ctx.fillStyle = this.sliderColor;
	this.ctx.fill();
	this.ctx.stroke();
}
