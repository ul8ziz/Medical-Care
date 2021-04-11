#include <Wire.h>
#include <Adafruit_MLX90614.h>
Adafruit_MLX90614 mlx = Adafruit_MLX90614();
int H = 0 ;
void setup(){
// initialize the serial communication:
Serial.begin(9600);
pinMode(4, INPUT); // Setup for leads off detection LO +
pinMode(5, INPUT); // Setup for leads off detection LO -
  
  Serial.println("Adafruit MLX90614 test");  
  mlx.begin(); 
}
 
void loop() {
 
if((digitalRead(4) == 1)||(digitalRead(5) == 1)){
//Serial.println('!');
}
else{
// send the value of analog input 0:
H =analogRead(A0);
}
//Wait for a bit to keep serial data from saturating
delay(50);
  Serial.print("R="); Serial.print(H);
// Serial.print("Ambient = "); Serial.print(mlx.readAmbientTempC()); 
  Serial.print(" , C="); Serial.print(mlx.readObjectTempC());
  delay(1300);

// Serial.print("Ambient = "); Serial.print(mlx.readAmbientTempF()); 
// Serial.print("*F\tObject = "); Serial.print(mlx.readObjectTempF()); Serial.println("*F");
//Serial.println();
//delay(500);

}
