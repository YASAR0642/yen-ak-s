/*
  KTD GMR / KTD 3D ANALYZER, ESP32-WROOM-32 firmware v8.0 - stable 50 SPS magnetic streaming
  Arduino-ESP32 2.x/3.x, target: ESP32 Dev Module (classic, with Bluetooth SPP)
  ADS1115 at 0x48: upper OUT A0, upper REF A1, lower OUT A2, lower REF A3.
  ALL 4 ADS pins require identical 22k/27k voltage dividers.
  UART2 optional HC-05 on RX2=GPIO16, TX2=GPIO17 at 9600 baud.
  C=800/Z is an uncalibrated inverse-signal INDEX, not measured depth. Not GPR.

  Android expects: numeric Z raw-like signal 0..1023, comma-separated ASCII.
  Transmitted line: "Z=512.00 C=%.5f" (labeled SensorParser format).
  Z is CENTER+calibrated gradiometer difference; C=800/Z (unitless index).
*/
#include <Arduino.h>
#include <Wire.h>
#include <BluetoothSerial.h>
#include <math.h>

// ---------- Wiring & calibration ----------
static const uint8_t ADS_ADDR=0x48;
static const uint8_t SDA_PIN=21, SCL_PIN=22;
static const uint8_t HC05_RX=16, HC05_TX=17;
static const bool USE_HC05_UART=false; // change to true only if HC-05 is wired
static const bool REQUIRE_ADS1115=true; // on failure, do NOT invent sensor readings
static const bool INVERT_GRADIENT=false;
static const float SENSOR_SEPARATION_M=0.75f;
// ADS1115 set to +/-4.096V -> 0.125mV/count, ADS must be supplied at 3.3 V.
// Sensor signal pins MUST be within 0..3.3V (and not below GND).
static const float ADS_VOLTS_PER_COUNT=0.000125f;
// 22 kOhm series (sensor->ADS) and 27 kOhm to GND on EACH A0..A3.
// ADC voltage = sensor voltage * 27/(22+27). Recover input voltage:
static const float DIVIDER_RECOVERY=49.0f/27.0f;
static const float EXPECTED_REF_MV=2500.0f;
static const uint8_t ZERO_READS=32;
// No sample duplication/interpolation: one ADC read group -> at most one transmitted frame.

// User calibration: how many raw millivolts of A1-A0 correspond to one app unit.
// Factory/field calibrated later; this DOES NOT assert nT units.
static float MV_PER_APP_UNIT=2.0f; // test: 2 mV/app unit; inspect RAW/HEALTH for noise
static const float APP_CENTER=512.0f;
static const float APP_LIMIT=480.0f;
static const uint16_t STARTUP_WARMUP=32;
static const uint32_t MIN_PERIOD_MS=12,MAX_PERIOD_MS=1000;

BluetoothSerial bt;
HardwareSerial hc05(2);
bool bluetoothReady=false,adsReady=false,streaming=false,gprMode=false,groundMode=false;
uint32_t periodMs=20,nextTick=0,measurements=0,errors=0;
uint32_t lastDebugMs=0, sampleTimeUs=0, maxReadTimeUs=0, lastHealthMs=0;
uint32_t badFrames=0, txFrames=0;
float lastZ=512.0f;
// Sens Pro ATmega328P legacy comparator (A1 10-bit ADC, AVcc reference).
// Measurement source: lower EMS100 OUT voltage, recovered from ADS input divider.
// The old hardware wiring used one analog channel, so this is a diagnostic approximation,
// not evidence of a physical metal/void distinction.
bool sensProEnabled=true; // diagnostic by default; SENSPRO_OFF disables extra field
static const float LEGACY_AVCC_MV=5000.0f; // adjust after measuring Nano supply
int sensProAdc10=-1;
const char* sensProRange(int v){
  if(v<0)return "INVALID";
  if(v<180)return "RANGE_A_LT180";
  if(v>=200&&v<=260)return "RANGE_B_200_260";
  if(v>=366)return "RANGE_C_GE366";
  return "UNCLASSIFIED";
}

bool serialSamples=false;
float upperZeroMv=0,lowerZeroMv=0;
float upperRefMv=0,lowerRefMv=0;
float upperMv=0,lowerMv=0;
float filterUpperMv=0,filterLowerMv=0;
float filteredGradientMv=0;
float driftMv=0;
bool groundReferenceTaken=false;
bool filtersSeeded=false;
bool calibrated=false;
bool enableDriftRemoval=false; // disabled by default; app's mineral filtering stays authoritative
bool enableNoiseFilter=true;
float filterAlpha=0.55f; // anti-noise IIR smoothing, higher = more responsive
String cmdUSB,cmdBT,cmdUART;

bool adsWriteRegister(uint8_t reg,uint16_t val){
  Wire.beginTransmission(ADS_ADDR); Wire.write(reg);Wire.write((uint8_t)(val>>8));Wire.write((uint8_t)val);
  return Wire.endTransmission()==0;
}
bool adsReadRegister(uint8_t reg,uint16_t &v){
  Wire.beginTransmission(ADS_ADDR);Wire.write(reg);
  if(Wire.endTransmission(false)!=0)return false;
  if(Wire.requestFrom((uint8_t)ADS_ADDR,(uint8_t)2)!=(uint8_t)2)return false;
  v=((uint16_t)Wire.read()<<8)|Wire.read();return true;
}
bool readAdc(uint8_t channel,int16_t &raw){
  if(channel>3)return false;
  // Single-shot, 860 samples/second, PGA +/-4.096 V, comparator off.
  // channel 0: AIN0-GND mux=100; channel 1: AIN1-GND mux=101.
  uint16_t mux=0x4000u+((uint16_t)channel<<12);
  uint16_t cfg=0x8000u|mux|0x0200u|0x0100u|0x00E0u|0x0003u;
  if(!adsWriteRegister(0x01,cfg))return false;
  // ADS conversion takes ~1.2ms at 860 SPS; 2ms prevents stale reads.
  delayMicroseconds(2000);
  uint16_t flags=0;if(!adsReadRegister(0x01,flags)||!(flags&0x8000u))return false;
  uint16_t sample=0;if(!adsReadRegister(0x00,sample))return false;
  raw=(int16_t)sample;return true;
}
bool readSensors(float &up,float &down){
  int16_t r0=0,r1=0,r2=0,r3=0;
  if(!adsReady||!readAdc(0,r0)||!readAdc(1,r1)||!readAdc(2,r2)||!readAdc(3,r3))return false;
  float outUp=r0*ADS_VOLTS_PER_COUNT*1000.0f*DIVIDER_RECOVERY;
  upperRefMv=r1*ADS_VOLTS_PER_COUNT*1000.0f*DIVIDER_RECOVERY;
  float outDown=r2*ADS_VOLTS_PER_COUNT*1000.0f*DIVIDER_RECOVERY;
  lowerRefMv=r3*ADS_VOLTS_PER_COUNT*1000.0f*DIVIDER_RECOVERY;
  // Recover actual output voltage. Identical threshold arithmetic to AVR ADC10,
  // but Nano firmware voltage/electrical filtering may differ.
  float legacyInputMv=outDown;
  sensProAdc10=constrain((int)lroundf(legacyInputMv*1023.0f/LEGACY_AVCC_MV),0,1023);
  // Reject unplugged reference wire or invalid ADC input.
  if(upperRefMv<2000||upperRefMv>3000||lowerRefMv<2000||lowerRefMv>3000)return false;
  if(outUp<0||outUp>5500||outDown<0||outDown>5500)return false;
  up=outUp-upperRefMv;
  down=outDown-lowerRefMv;
  return isfinite(up)&&isfinite(down);
}
void reply(const String &s){
  Serial.println(s);
  // Android SensorParser interprets stray numbers as sensor readings.
  // Keep diagnostic STATUS messages off the application data channel.
  if(s.startsWith("REF:")){
    if(bluetoothReady&&bt.hasClient())bt.println(s);
    if(USE_HC05_UART)hc05.println(s);
  }
}
void zeroSensors(){
  // Call only while holding device stationary and away from known targets.
  float a=0,b=0;int n=0;
  for(int i=0;i<ZERO_READS;i++){
    float u,d;if(readSensors(u,d)){a+=u;b+=d;n++;}
    delay(3);
  }
  if(n<(ZERO_READS*3/4)){calibrated=false;reply("STATUS,ZERO_FAILED,ADS_READ_ERROR");return;}
  upperZeroMv=a/n;lowerZeroMv=b/n;calibrated=true;
  filtersSeeded=false;filteredGradientMv=0;driftMv=0;
  reply("STATUS,ZERO_OK");
}
float computeZ(){
  float up=0,down=0;
  if(!readSensors(up,down)){errors++;badFrames++;return NAN;}
  upperMv=up;lowerMv=down;
  if(!filtersSeeded){filterUpperMv=up;filterLowerMv=down;filtersSeeded=true;}
  if(enableNoiseFilter){filterUpperMv+=(up-filterUpperMv)*filterAlpha;filterLowerMv+=(down-filterLowerMv)*filterAlpha;}
  else{filterUpperMv=up;filterLowerMv=down;}
  // Retain signed anomalies, no synthetic target response or fake hyperbola.
  float diffMv=((filterLowerMv-lowerZeroMv)-(filterUpperMv-upperZeroMv));
  if(INVERT_GRADIENT)diffMv=-diffMv;
  filteredGradientMv=diffMv;
  if(enableDriftRemoval){driftMv+=(diffMv-driftMv)*0.002f;diffMv-=driftMv;}
  lastZ=constrain(APP_CENTER+diffMv/MV_PER_APP_UNIT, APP_CENTER-APP_LIMIT,APP_CENTER+APP_LIMIT);
  return lastZ;
}
void emitSample(float x,float y,bool withXY){
  uint32_t t0=micros();
  float z=computeZ();
  sampleTimeUs=micros()-t0;
  if(sampleTimeUs>maxReadTimeUs)maxReadTimeUs=sampleTimeUs;
  if(!isfinite(z)||!calibrated)return;
  measurements++;
  char line[128];
  float c = z>0.0001f ? 800.0f/z : NAN;
  if(sensProEnabled){
    if(withXY)snprintf(line,sizeof(line),"X=%.2f Y=%.2f Z=%.2f C=%.5f ADC10=%d",x,y,z,c,sensProAdc10);
    else snprintf(line,sizeof(line),"Z=%.2f C=%.5f ADC10=%d",z,c,sensProAdc10);
  }else{
    if(withXY)snprintf(line,sizeof(line),"X=%.2f Y=%.2f Z=%.2f C=%.5f",x,y,z,c);
    else snprintf(line,sizeof(line),"Z=%.2f C=%.5f",z,c);
  }
  if(bluetoothReady&&bt.hasClient()){bt.println(line);txFrames++;}
  if(USE_HC05_UART)hc05.println(line);
  if(serialSamples)Serial.println(line);
}
void processCommand(String line){
  line.trim();line.toUpperCase();if(line.length()==0)return;
  if(line=="LIVE_START" || line=="START"){
    if(!adsReady){reply("STATUS,ADC_NOT_READY");return;}
    if(!calibrated){zeroSensors();if(!calibrated)return;}
    gprMode=false;groundMode=false;groundReferenceTaken=false;streaming=true;nextTick=millis();reply("STATUS,LIVE_STARTED");
  }else if(line=="LIVE_STOP" || line=="STOP"){
    streaming=false;reply("STATUS,LIVE_STOPPED");
  }else if(line.startsWith("RATE=")){
    long v=line.substring(5).toInt();
    if(v>=MIN_PERIOD_MS&&v<=MAX_PERIOD_MS){periodMs=(uint32_t)v;reply("STATUS,PERIOD_MS,"+String(periodMs));}
    else reply("STATUS,BAD_RATE_USE_MS_10_TO_1000");
  }else if(line=="GROUND_START"){
    streaming=false;groundMode=true;groundReferenceTaken=false;filtersSeeded=false;
    // First SAMPLE press takes reference. GROUND_START must not emit a reference or data.
    reply("STATUS,GROUND_ARMED_FIRST_SAMPLE_IS_REFERENCE");
  }else if(line=="REF"){
    streaming=false;groundMode=true;groundReferenceTaken=false;
    // REF is a reset/arm command; first SAMPLE performs reference and sends REF packet.
    reply("STATUS,GROUND_RESET_FIRST_SAMPLE_IS_REFERENCE");
  }else if(line.startsWith("SAMPLE")){
    if(!adsReady){reply("STATUS,ADC_NOT_READY");return;}
    float x=0,y=0;
    int n=sscanf(line.c_str(),"SAMPLE X=%f Y=%f",&x,&y);
    // Ground mode NEVER streams automatically; exactly one response per SAMPLE.
    if(groundMode){
      if(!groundReferenceTaken){
        // Baseline is from current stationary sensor measurements (32 complete sets).
        zeroSensors();
        if(!calibrated)return;
        groundReferenceTaken=true;
        reply("REF:512.00");
        return;
      }
      if(!calibrated)return;
      emitSample(x,y,n==2);
      return;
    }
    // Ignore unrelated SAMPLE commands in live mode to avoid accidental frame mixing.
    reply("STATUS,SAMPLE_REQUIRES_GROUND_START");
  }else if(line=="ZERO"||line=="CAL"||line=="REFERANS"){
    bool previous=streaming;streaming=false;zeroSensors();streaming=previous;nextTick=millis()+periodMs;
  }else if(line=="STATUS?"||line=="PING"){
    reply("STATUS,KTD_GMR,ADS="+String(adsReady?"OK":"ERROR")+",PERIOD_MS="+String(periodMs)+",STREAM="+String(streaming?1:0)+",COUNT="+String(measurements)+",ERR="+String(errors));
  }else if(line=="DEBUG_ON"){
    serialSamples=true;reply("STATUS,DEBUG_ON_USB_ONLY");
  }else if(line=="DEBUG_OFF"){
    serialSamples=false;reply("STATUS,DEBUG_OFF");
  }else if(line=="HEALTH?"){
    // Health never enters Bluetooth data channel; parser must not receive diagnostics.
    Serial.printf("HEALTH,ADS=%d,CAL=%d,PERIOD_MS=%lu,LAST_Z=%.2f,UP_MV=%.3f,LOW_MV=%.3f,GRAD_MV=%.3f,READ_US=%lu,MAX_READ_US=%lu,COUNT=%lu,TX=%lu,ERROR=%lu\n",
      adsReady?1:0,calibrated?1:0,(unsigned long)periodMs,lastZ,upperMv,lowerMv,filteredGradientMv,
      (unsigned long)sampleTimeUs,(unsigned long)maxReadTimeUs,(unsigned long)measurements,(unsigned long)txFrames,(unsigned long)errors);
  }else if(line=="SENSPRO_ON"){
    sensProEnabled=true;
    reply("STATUS,SENSPRO_DIAGNOSTIC_ON");
  }else if(line=="SENSPRO_OFF"){
    sensProEnabled=false;
    reply("STATUS,SENSPRO_DIAGNOSTIC_OFF");
  }else if(line=="POLARITY?"){
    // USB diagnostic only: no additional Bluetooth sample packets.
    // Delta sign is from the actual calibrated two-sensor gradiometer Z.
    const float delta=lastZ-APP_CENTER;
    const char* sign=(delta>8.0f)?"POS":((delta< -8.0f)?"NEG":"BASELINE");
    Serial.printf("KTD_POLARITY,Z=%.3f,DELTA=%.3f,SIGN=%s,ADC10=%d,SENSPRO=%s\n",
       lastZ,delta,sign,sensProAdc10,sensProRange(sensProAdc10));
  }else if(line=="SENSPRO?"){
    float u=0,d=0;bool ok=readSensors(u,d);
    Serial.printf("SENSPRO,OK=%d,ADC10=%d,CLASS=%s,OUT_MV=%.2f,VREF_ASSUMED_MV=%.1f\n",ok?1:0,sensProAdc10,sensProRange(sensProAdc10),d+lowerRefMv,LEGACY_AVCC_MV);
  }else if(line=="RAW?"){
    float u=0,d=0;bool ok=readSensors(u,d);
    Serial.printf("RAW,OK=%d,UP_OUT_MINUS_REF_MV=%.3f,LOW_OUT_MINUS_REF_MV=%.3f,UP_REF_MV=%.3f,LOW_REF_MV=%.3f,ZERO_UP_MV=%.3f,ZERO_LOW_MV=%.3f,ERR=%lu\n",ok?1:0,u,d,upperRefMv,lowerRefMv,upperZeroMv,lowerZeroMv,(unsigned long)errors);
  }else if(line.startsWith("SCALE_MV=")){
    float v=line.substring(9).toFloat();
    if(v>=0.25f&&v<=50.0f){MV_PER_APP_UNIT=v;reply("STATUS,SCALE_MV="+String(v,3));}
    else reply("STATUS,SCALE_RANGE_0_25_TO_50_MV");
  }else if(line.startsWith("ALPHA=")){
    float value=line.substring(6).toFloat();
    if(value>=0.10f&&value<=1.0f){filterAlpha=value;filtersSeeded=false;reply("STATUS,ALPHA_OK");}
    else reply("STATUS,ALPHA_RANGE_0_10_TO_1_00");
  }else if(line=="FILTER_ON"){
    enableNoiseFilter=true;filtersSeeded=false;reply("STATUS,FILTER_ON");
  }else if(line=="FILTER_OFF"){
    enableNoiseFilter=false;filtersSeeded=false;reply("STATUS,FILTER_OFF");
  }else if(line=="DRIFT_ON"){
    enableDriftRemoval=true;reply("STATUS,DRIFT_ON");
  }else if(line=="DRIFT_OFF"){
    enableDriftRemoval=false;reply("STATUS,DRIFT_OFF");
  }else if(line=="GPR_START"||line=="GPR_DEMO_START"){
    // Not a radar front end. No fabricated physical ns or traces.
    streaming=false;gprMode=false;reply("STATUS,GPR_HARDWARE_NOT_INSTALLED");
  }else if(line=="GPR_STOP"){
    gprMode=false;reply("STATUS,GPR_STOPPED");
  }else{
    reply("STATUS,UNKNOWN_COMMAND");
  }
}
void readCommands(Stream &port,String &buffer){
  uint8_t maxBytes=64;
  while(port.available()&&maxBytes--){
    char c=(char)port.read();
    if(c=='\r'||c=='\n'){
      if(buffer.length()){processCommand(buffer);buffer="";}
    }else if(c>=32&&c<=126){
      if(buffer.length()<72)buffer+=c;else buffer="";
    }
  }
}
void captureAndTransmit(){
  emitSample(0,0,false);
}
void setup(){
  Serial.begin(115200);
  Wire.begin(SDA_PIN,SCL_PIN,400000);
  Wire.setTimeOut(20);
  Wire.beginTransmission(ADS_ADDR);adsReady=(Wire.endTransmission()==0);
  // Conversion is >= 4 channels * 2ms ~=8ms; 20ms cadence is reliable target.
  // RATE=100 requests 100ms, which is slower, not faster.
  if(USE_HC05_UART)hc05.begin(9600,SERIAL_8N1,HC05_RX,HC05_TX);
  bluetoothReady=bt.begin("KTD GMR");
  delay(300);
  if(adsReady){zeroSensors();}
  Serial.println("KTD EMS100 dual OUT-REF gradiometer V4; 0.75m vertical baseline");
  reply(String("STATUS,KTD_GMR_READY,ADS=")+(adsReady?"OK":"ERROR"));
  nextTick=millis()+periodMs;
}
void loop(){
  static bool previousClient=false;
  bool currentClient=bluetoothReady&&bt.hasClient();
  if(previousClient&&!currentClient){streaming=false;groundMode=false;cmdBT="";}
  previousClient=currentClient;
  readCommands(Serial,cmdUSB);
  if(bluetoothReady&&bt.hasClient())readCommands(bt,cmdBT);
  if(USE_HC05_UART)readCommands(hc05,cmdUART);
  if(streaming&&adsReady){
    uint32_t now=millis();
    if((int32_t)(now-nextTick)>=0){
      captureAndTransmit();
      nextTick+=periodMs;
      if((int32_t)(millis()-nextTick)>=(int32_t)periodMs)nextTick=millis()+periodMs;
    }
  }
  delay(1);
}
