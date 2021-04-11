

#include <Wire.h>
#include <SoftwareSerial.h>
#include <Wire.h>
#include <Adafruit_MLX90614.h>

#include "MAX30100_PulseOximeter.h"

#define REPORTING_PERIOD_MS     1000
SoftwareSerial portOne(10, 11);
Adafruit_MLX90614 mlx = Adafruit_MLX90614();
// PulseOximeter is the higher level interface to the sensor
// it offers:
//  * beat detection reporting
//  * heart rate calculation
//  * SpO2 (oxidation level) calculation

PulseOximeter pox;
int wa = 0;
uint32_t tsLastReport = 0;
int x , y ;

// Callback (registered below) fired when a pulse is detected
void onBeatDetected()
{
     portOne.println("Beat!");
}

void setup()
{
    portOne.begin(115200);
        while (!Serial) {

    ; // wait for serial port to connect. Needed for native USB port only

       }

    portOne.print("Initializing pulse oximeter..");
         Serial.begin(9600);
         Serial.println("Adafruit MLX90614 test");  
         pinMode(10, INPUT); // Setup for leads off detection LO +
         pinMode(11, INPUT); // Setup for leads off detection LO -
          mlx.begin();  
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
}

void loop()
{

    // Make sure to call update as fast as possible
    pox.update();

    // Asynchronously dump heart rate and oxidation levels to the serial
    // For both, a value of 0 means "invalid"
    if (millis() - tsLastReport > REPORTING_PERIOD_MS) {
      portOne.print("Heart rate:");
         x = pox.getHeartRate();
       portOne.print( x );
       portOne.print("bpm / SpO2:");
           y = pox.getSpO2();
       portOne.print(y);
    
     portOne.println("%");
            printSer();
        tsLastReport = millis();

      
    }   
}

void printSer()
{
               if((digitalRead(10) == 1)||(digitalRead(11) == 1)){
                      Serial.println('!');
                    }
                    
              else{
                    // send the value of analog input 0:
                   //  Serial.print("H:");
                  Serial.println(analogRead(A0));
                }
                  //Wait for a bit to keep serial data from saturating
                delay(100);
            Serial.print("h:");
            Serial.print(x);
            Serial.print(" b ");
            Serial.print(y);
            Serial.println();

              Serial.println();
}
