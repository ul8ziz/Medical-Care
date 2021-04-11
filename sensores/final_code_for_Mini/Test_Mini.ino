#include <Wire.h>
#include <SoftwareSerial.h>
#include "MAX30100_PulseOximeter.h"
#define REPORTING_PERIOD_MS     1000

// PulseOximeter is the higher level interface to the sensor
// it offers:
//  * beat detection reporting
//  * heart rate calculation
//  * SpO2 (oxidation level) calculation

PulseOximeter pox;

SoftwareSerial portOne(10, 11);// RX, TX
SoftwareSerial portTow(3, 4);// RX, TX
uint32_t tsLastReport = 0;
String St="" ;
int timeout=0 , timein =1001;


// Callback (registered below) fired when a pulse is detected
void onBeatDetected()
{
  portOne.println("Beat!");
}
bool val = true ;
void setup()
{
   portOne.begin(115200);

   portOne.print("Initializing pulse oximeter..");
    // Initialize the PulseOximeter instance
    // Failures are generally due to an improper I2C wiring, missing power supply
    // or wrong target chip
    if (!pox.begin()) {
       portOne.println("FAILED");
        for(;;);
    } else {
       portOne.println("SUCCESS");
    }
    // The default current for the IR LED is 50mA and it could be changed
    //   by uncommenting the following line. Check MAX30100_Registers.h for all the
    //   available options.
    // pox.setIRLedCurrent(MAX30100_LED_CURR_7_6MA);
    // Register a callback for the beat detection
    pox.setOnBeatDetectedCallback(onBeatDetected);

    /// Serial Print to Bluetooth
     portTow.begin(9600);
     Serial.begin(9600);

    
}

void loop()
{
 //portOne.listen();

   pox.update();

 if (millis() - tsLastReport > REPORTING_PERIOD_MS) {
     //  Serial.print("time = ");
     //  Serial.println(millis());
       Serial.print("H=");
       Serial.print(pox.getHeartRate());
     Serial.print(" ,O=");
        Serial.println(pox.getSpO2());
      
        tsLastReport = millis();
    
   }
 
      
  portTow.listen();

  if (portOne.isListening()) {
  // Serial.print("P1Ls");
}else{
 //  Serial.print("P1nLS");
}
  if (  portTow.overflow()) { //Serial.println("FLW"); 
    }
                     if (portTow.isListening()) 
                     {

                           if(portTow.available() > 0 )
                           { 
                                St = portTow.readString(); 
                              
                            }
                          //  delay(400);
                           // Serial.println(" , P2Ls");
                       }
                        
                         else{
                          //   Serial.println("  , p2nLs");
                                }
// delay(500);
    // Make sure to call update as fast as possible
   

    // Asynchronously dump heart rate and oxidation levels to the serial
    // For both, a value of 0 means "invalid"
   
   
       if( St!="")
       {
           
            Serial.println(St);
            St="";
       }
       
//delay(100);
}
