/*
 KTD 3D ANALYZER v3.9.1 - ESP32-WROOM-32 / ESP32U CLASSIC BT (SPP)
 Modlar:
   LIVE_START / LIVE_STOP       -> GPIO34 analog manyetik test okumasi (Z:0..1023)
   GPR_START / GPR_STOP         -> UART2'deki GERCEK zaman-ornekli radar izi
                                  paketlerini Bluetooth'a aktarir
   GPR_DEMO_START              -> SADECE ekran testi, sentetik iz; olcum degil
   GPR_INFO                    -> kimlik / baglanti bilgisi
   REF                        -> mevcut analog sinyal referans raporu

 GPR donanimi harici bir ornekleyici/iz yakalayici icermelidir. ESP32'nin
 tek bir analogRead() degerinden nanosaniyelik bir radar izi OLUSTURULAMAZ.
 Harici radar iz yakalayicisi UART2 (RX GPIO16, TX GPIO17), 115200 8N1
 uzerinden su ASCII paketlerini gondermelidir:
   KTDGPR,META,NS_PER_SAMPLE,0.5,DX_M,0.05,SOURCE,FRONTEND\n
   KTDGPR,TRACE,1,16,0,3,5,-2,... toplam 16 int16 ornek\n
 Buradaki ns/örnek sadece harici yakalayicinin olculmus zaman adimi
 verisidir. SOURCE=FRONTEND tek basina bilimsel dogrulama degildir.

 TEST DEMO modunda SOURCE,DEMO. Telefon bu veriyi GERCEK saymaz.
 Arduino IDE board: ESP32 Dev Module (klasik ESP32, ESP32-S3/C3 degil).
 */

#include <Arduino.h>
#include <BluetoothSerial.h>
#include <math.h>

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bu sketch ESP32 Classic Bluetooth (BT SPP) destekleyen ESP32-WROOM-32 gerektirir.
#endif

BluetoothSerial SerialBT;
HardwareSerial GprFrontend(2);

static const uint8_t MAG_ADC_PIN = 34;
static const int GPR_RX_PIN = 16;
static const int GPR_TX_PIN = 17;
static const uint32_t UART_BAUD = 115200;

enum Mode { IDLE, MAG_LIVE, GPR_LIVE, GPR_DEMO };
Mode currentMode = IDLE;
String btCommand, frontendLine;
uint32_t lastMagMs = 0;
uint32_t lastDemoMs = 0;
uint32_t sequence = 0;

void writeBoth(const String &s) {
  Serial.println(s);
  if (SerialBT.hasClient()) SerialBT.println(s);
}

void sendStatus(const String &s) { writeBoth("KTDGPR,STATUS," + s); }

int analog0to1023() {
  const int raw = analogRead(MAG_ADC_PIN); // ESP32 ADC 12-bit 0..4095
  return constrain((raw * 1023L + 2047) / 4095L, 0L, 1023L);
}

void setMode(Mode next) {
  currentMode = next;
  if (next == GPR_LIVE) {
    // Onceki ornek/ezberlenmis zaman ayari harici donanimdan alinmali.
    sequence=0;
    sendStatus("GPR_FRONTEND_BEKLENIYOR");
  } else if (next == GPR_DEMO) {
    sequence=0;
    writeBoth("KTDGPR,META,NS_PER_SAMPLE,1.0,DX_M,0.05,SOURCE,DEMO");
    sendStatus("DEMO_SYNTHETIC_NOT_MEASUREMENT");
  } else if (next == MAG_LIVE) {
    writeBoth("REF:" + String(analog0to1023()));
    sendStatus("MAG_ADC_TEST");
  } else sendStatus("DURDU");
}

void command(String s) {
  s.trim(); s.toUpperCase();
  if (s=="GPR_START" || s=="MODE=GPR") setMode(GPR_LIVE);
  else if (s=="GPR_DEMO_START") setMode(GPR_DEMO);
  else if (s=="GPR_STOP" || s=="GPR_DEMO_STOP") setMode(IDLE);
  else if (s=="LIVE_START" || s=="MAG_START") setMode(MAG_LIVE);
  else if (s=="LIVE_STOP" || s=="MAG_STOP") setMode(IDLE);
  else if (s=="REF" || s=="ZERO") writeBoth("REF:" + String(analog0to1023()));
  else if (s=="GPR_INFO") sendStatus("ESP32_BT_SPP_V391_UART2_GPR_FRONTEND");
  else if (s.startsWith("RATE=")) sendStatus("RATE_MAG_FIXED_20MS");
  else if (s=="ADC_READ") writeBoth("KTDGPR,ADC,"+String(millis())+","+String(analogRead(MAG_ADC_PIN)));
  else if (s.length()) sendStatus("UNKNOWN_COMMAND");
}

// UART2 izlerinden yalniz taninan protokol satirlari BT'ye aktarilir.
// Ilk META bilgisini harici sampler uretmeli: aksi halde ns ekseni bilinmez.
void handleFrontendLine(String s) {
  s.trim();
  if (currentMode!=GPR_LIVE) return;
  if (s.startsWith("KTDGPR,META,") || s.startsWith("KTDGPR,TRACE,") || s.startsWith("KTDGPR,STATUS,")) {
    if (s.length()<=10000) writeBoth(s);
  }
}

void emitDemoTrace() {
  const int N=128;
  String line="KTDGPR,TRACE,"+String(++sequence)+","+String(N);
  // Gecici SIMULASYON: gercek ADC, anten, RF ya da yeraltindan veri DEGIL.
  const float center=38+10*sinf(sequence*0.21f);
  for(int i=0;i<N;i++) {
    float x=(i-center)/9.0f;
    float wave=220.0f*expf(-.5f*x*x)*sinf(i*.27f+sequence*.23f);
    wave+=40.0f*sinf(i*.09f+sequence*.08f);
    int v=constrain((int)roundf(wave),-32000,32000);
    line+=","+String(v);
  }
  writeBoth(line);
}

void setup() {
  Serial.begin(115200);
  analogReadResolution(12);
  pinMode(MAG_ADC_PIN,INPUT);
  GprFrontend.setRxBufferSize(8192);
  GprFrontend.begin(UART_BAUD,SERIAL_8N1,GPR_RX_PIN,GPR_TX_PIN);
  if (!SerialBT.begin("KTD_GPR_V1")) {
    Serial.println("HATA: Bluetooth Classic baslatilamadi");
  }
  btCommand.reserve(120);
  frontendLine.reserve(8000);
  sendStatus("HAZIR_KTD_GPR_V1");
}

void loop() {
  while(SerialBT.available()) {
    char c=(char)SerialBT.read();
    if(c=='\r' || c=='\n') {
      if(btCommand.length()) {command(btCommand);btCommand="";}
    } else if(btCommand.length()<120) btCommand+=c;
    else btCommand="";
  }

  // UART2 verisi mevcutsa ham GPR izini geri Bluetooth'a aktar.
  while(GprFrontend.available()) {
    char c=(char)GprFrontend.read();
    if(c=='\r'||c=='\n') {
      if(frontendLine.length()) {handleFrontendLine(frontendLine);frontendLine="";}
    } else if(frontendLine.length()<10000) frontendLine+=c;
    else frontendLine="";
  }

  uint32_t now=millis();
  if(currentMode==MAG_LIVE && now-lastMagMs>=20) {
    lastMagMs=now;
    writeBoth("Z:"+String(analog0to1023()));
  }
  if(currentMode==GPR_DEMO && now-lastDemoMs>=150) {
    lastDemoMs=now;
    emitDemoTrace();
  }
}
