#include <WiFi.h>
#include "FirebaseESP32.h"
#include "DHT.h"
#include <Wire.h>
#include <LiquidCrystal_I2C.h>

const char* ssid = "Thuan Tuong"; // Ten Wifi
const char* password = "123456789"; // Mk Wifi


#define ID "Vuon1" // ID Vườn
#define DHTPin 32
#define DHTTYPE DHT11
#define LDR 35
#define Soil_Moisture 34
#define B_Switch 26
#define B_Up 25
#define B_Down 33
#define Pump 13
#define Fan 15
#define Fog 2
#define Light 16

#define DATABASE_URL "https://vuonthongminh-29205-default-rtdb.asia-southeast1.firebasedatabase.app/"
#define DATABASE_SECRET "MSLFxnWIFFivJvlusTYVAlsmdw9rfZF47UXBeZ2Y"  

FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

const String Truong_limitTemp =  String("/") + ID + "/Limit_temp";
const String Truong_limitHumi =  String("/") + ID + "/Limit_humi";
const String Truong_limitSoil =  String("/") + ID + "/Limit_soil";
const String Truong_limitLight =  String("/") + ID + "/Limit_light";
const String Truong_Temp  = String("/") + ID + "/Temp";
const String Truong_Humi  = String("/") + ID + "/Humi";
const String Truong_Soil  = String("/") + ID + "/Soil_moisture";
const String Truong_Light = String("/") + ID + "/Ambient_light";
const String Truong_Bom = String("/") + ID + "/Bom";
const String Truong_Quat = String("/") + ID + "/Quat";
const String Truong_Den = String("/") + ID + "/Den";
const String Truong_Suong = String("/") + ID + "/Suong";
const String Truong_AutoMode = String("/") + ID + "/Auto_mode";
const String Truong_ChangingResponse = String("/") + ID + "/Changing_response";
const String Truong_OnlineStatus = String("/") + ID + "/Online_status";


unsigned long Time_Check;
unsigned long lastOnline;
uint8_t Limit_Temp[2] = {30,25};
uint8_t Limit_Humi[2] = {50,70};
uint8_t Limit_Soil[2] = {50,70};
uint8_t Limit_Light[2]= {30,80};
uint8_t Value_Temp, Value_Humi, Value_Light, Value_Soil;
uint8_t ExValue_Temp, ExValue_Humi, ExValue_Light, ExValue_Soil;
uint8_t Status;
bool State_Pump, State_Fan, State_Fog, State_Light;
bool Config_Mode;
bool Auto_Mode;
bool ReadSwDTB;
uint16_t Value_ADC;


byte Char_Temp[8] = {0x04,0x0A,0x0A,0x0A,0x0E,0x1F,0x1F,0x0E};

byte Char_Humi[8] = {0x04,0x0A,0x0A,0x11,0x15,0x1F,0x1F,0x0E};

byte Char_Light[8] = {0x0E,0x11,0x15,0x1B,0x11,0x0E,0x0E,0x04};

byte Char_Soil[8] = {0x0E,0x11,0x19,0x15,0x0E,0x0E,0x0A,0x0A};

LiquidCrystal_I2C lcd(0x27,16,2);
DHT dht(DHTPin, DHTTYPE);

void Set_onineStatus();
bool Push_arr_to_dt3se(uint8_t limit[2], const String &Truong);
bool Read_arr_fr_dt3se(uint8_t limit[2], const String &Truong);
void Set_off_all();
void Read_all_arr();
void Push_all_arr();
void setValueSensortoDt3se();
void Read_sw_dt3se(bool *sw_state, const String &Truong);
void Read_all_Switch();
void Push_sw_to_dt3se(bool sw_state, const String &Truong);
void Read_allSwitch_and_allLimit();
void Gioithieu();
void Display_Auto_Change();
void Display_Read_Sensor();
void Display_On_Off_device();
void Display_Config();
void Display_Caution();
void Diplay_Config_Successful();
void Show();
void Up(uint8_t *Value, uint8_t Max, uint8_t Min);
void Down(uint8_t *Value, uint8_t Max, uint8_t Min);
void Switch_Status(uint8_t* Status, bool* State, unsigned long *Time_before, uint8_t Max, uint8_t Min);
void On_Off_Device();
void Read_and_Show_Sensor();
void Read_Key();

void setup() {
  Serial.begin(9600);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.println("Đang kết nối internet. Vui lòng chờ!");
  }
  Serial.println("WiFi connected!");
  config.database_url = DATABASE_URL;
  config.signer.tokens.legacy_token = DATABASE_SECRET;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
  
  lcd.init();                      
  lcd.backlight();

  lcd.createChar(0, Char_Temp);
  lcd.createChar(1, Char_Humi);
  lcd.createChar(2, Char_Light);
  lcd.createChar(3, Char_Soil);

  pinMode(Soil_Moisture, INPUT);
  pinMode(LDR, INPUT);
  pinMode(B_Switch, INPUT);
  pinMode(B_Up, INPUT);
  pinMode(B_Down, INPUT);
  pinMode(Pump, OUTPUT);
  pinMode(Fan, OUTPUT);
  pinMode(Fog, OUTPUT);
  pinMode(Light, OUTPUT);

  Set_off_all();
  Read_all_arr();
  Gioithieu();
  delay(500);
  Read_and_Show_Sensor();
  Time_Check = millis();
  lastOnline = millis();
  Set_onineStatus();
}

void loop() {
  if(millis() - lastOnline >= 2000){
    lastOnline = millis();
    Set_onineStatus();
  }
  if(millis() - Time_Check >= 3000){
    Read_and_Show_Sensor();
    ReadSwDTB = false;
    Time_Check = millis();
  }
  Read_allSwitch_and_allLimit();
  Read_Key();
  On_Off_Device();
}

void Read_and_Show_Sensor(){
  if(Config_Mode == false && Status == 0){
    uint16_t sum = 0;
    for (int i = 0; i < 4; i++) {
      sum += analogRead(LDR);
      delay(5);
    }
    Value_ADC = sum / 4;
    Value_Light = (uint8_t)(( (float)Value_ADC / 4095.0 ) * 100.0);

    sum = 0;
    for (int i = 0; i < 4; i++) {
      sum += analogRead(Soil_Moisture);
      delay(5);
    }
   Value_ADC = sum / 4;
   Value_Soil = (uint8_t)(( (float)Value_ADC / 4095.0 ) * 100.0);


   float t = dht.readTemperature();
   float h = dht.readHumidity();
   if (!isnan(t)) Value_Temp = (uint8_t)t;
   if (!isnan(h)) Value_Humi = (uint8_t)h;
   Display_Read_Sensor();
   setValueSensortoDt3se();
   Time_Check = millis();
  }
}

void Display_Read_Sensor() {
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.write(0); lcd.print(":"); lcd.print(Value_Temp); lcd.print("C");

  lcd.setCursor(8, 0);
  lcd.write(1); lcd.print(":"); lcd.print(Value_Humi); lcd.print("%");

  lcd.setCursor(0, 1);
  lcd.write(2); lcd.print(":"); lcd.print(Value_Light); lcd.print("%");

  lcd.setCursor(8, 1);
  lcd.write(3); lcd.print(":"); lcd.print(Value_Soil); lcd.print("%");
}

void Gioithieu(){
  lcd.clear();
  lcd.setCursor(0,0);
  lcd.print("DATN Nhom 6");
  lcd.setCursor(0,1);
  lcd.print("IDVTM: " + ID);
  delay(500);
}

void Display_On_Off_device(){

  lcd.clear();
  if(Status == 1){
    lcd.setCursor(0,0);
    lcd.print(">Bom");
    lcd.setCursor(13,0);
    lcd.print(State_Pump? "On":"Off");
    lcd.setCursor(0,1);
    lcd.print(" Quat");
    lcd.setCursor(13,1);
    lcd.print(State_Fan? "On":"Off");
  }
  else if(Status == 2){
    lcd.setCursor(0,0);
    lcd.print(" Bom");
    lcd.setCursor(13,0);
    lcd.print(State_Pump? "On":"Off");
    lcd.setCursor(0,1);
    lcd.print(">Quat");
    lcd.setCursor(13,1);
    lcd.print(State_Fan? "On":"Off");
  }
  else if (Status == 3){
    lcd.setCursor(0,0);
    lcd.print(">Suong");
    lcd.setCursor(13,0);
    lcd.print(State_Fog? "On":"Off");
    lcd.setCursor(0,1);
    lcd.print(" Den");
    lcd.setCursor(13,1);
    lcd.print(State_Light? "On":"Off");
  }
  else if (Status ==  4){
    lcd.setCursor(0,0);
    lcd.print(" Suong");
    lcd.setCursor(13,0);
    lcd.print(State_Fog? "On":"Off");
    lcd.setCursor(0,1);
    lcd.print(">Den");
    lcd.setCursor(13,1);
    lcd.print(State_Light? "On":"Off");
  }
}
void Display_Config() {
  lcd.clear();
  if (Status == 0) {
    lcd.setCursor(0, 0);
    lcd.print("Che do Auto:");
    lcd.setCursor(12, 1);
    lcd.print(">" + String(Auto_Mode ? "On" : "Off"));
  }
  else if (Status == 1) {
    lcd.setCursor(0, 0);
    lcd.print("Nhiet do(Quat):");
    lcd.setCursor(0, 1);
    lcd.print(">B:>" + String(Limit_Temp[0]) + "C");
    lcd.setCursor(8, 1);
    lcd.print(" T:<" + String(Limit_Temp[1]) + "C");
  }
  else if (Status == 2) {
    lcd.setCursor(0, 0);
    lcd.print("Nhiet do(Quat):");
    lcd.setCursor(0, 1);
    lcd.print(" B:>" + String(Limit_Temp[0]) + "C");
    lcd.setCursor(8, 1);
    lcd.print(">T:<" + String(Limit_Temp[1]) + "C");
  }
  else if (Status == 3) {
    lcd.setCursor(0, 0);
    lcd.print("Do am kk(Suong):");
    lcd.setCursor(0, 1);
    lcd.print(">B:<" + String(Limit_Humi[0]) + "%");
    lcd.setCursor(8, 1);
    lcd.print(" T:>" + String(Limit_Humi[1]) + "%");
  }
  else if (Status == 4) {
    lcd.setCursor(0, 0);
    lcd.print("Do am kk(Suong):");
    lcd.setCursor(0, 1);
    lcd.print(" B:<" + String(Limit_Humi[0]) + "%");
    lcd.setCursor(8, 1);
    lcd.print(">T:>" + String(Limit_Humi[1]) + "%");
  }
  else if (Status == 5) {
    lcd.setCursor(0, 0);
    lcd.print("Do sang(Den):");
    lcd.setCursor(0, 1);
    lcd.print(">B:<" + String(Limit_Light[0]) + "%");
    lcd.setCursor(8, 1);
    lcd.print(" T:>" + String(Limit_Light[1]) + "%");
  }
  else if (Status == 6) {
    lcd.setCursor(0, 0);
    lcd.print("Do sang(Den):");
    lcd.setCursor(0, 1);
    lcd.print(" B:<" + String(Limit_Light[0]) + "%");
    lcd.setCursor(8, 1);
    lcd.print(">T:>" + String(Limit_Light[1]) + "%");
  }
  else if (Status == 7) {
    lcd.setCursor(0, 0);
    lcd.print("Do am dat(Bom):");
    lcd.setCursor(0, 1);
    lcd.print(">B:<" + String(Limit_Soil[0]) + "%");
    lcd.setCursor(8, 1);
    lcd.print(" T:>" + String(Limit_Soil[1]) + "%");
  }
  else if (Status == 8) { 
    lcd.setCursor(0, 0);
    lcd.print("Do am dat(Bom):");
    lcd.setCursor(0, 1);
    lcd.print(" B:<" + String(Limit_Soil[0]) + "%");
    lcd.setCursor(8, 1);
    lcd.print(">T:>" + String(Limit_Soil[1]) + "%");
  }
}
void Display_Auto_Change(){
  lcd.clear();
  lcd.setCursor(0,0);
  lcd.print("Che do Auto");
  lcd.setCursor(0,1);
  lcd.print("Da " + String(Auto_Mode ? "Bat" : "Tat"));
  delay(1000);
}
void Display_Caution(){
  lcd.clear();
  lcd.setCursor(0,0);
  lcd.print("Dang che do Auto");
  lcd.setCursor(0,1);
  lcd.print("Khong dc B/T TBi");
  delay(2000);
  Status = 0;
}
void Diplay_Config_Successful(){
  lcd.clear();
  lcd.setCursor(0,0);
  lcd.print("Thiet lap xong");
  lcd.setCursor(0,1);
  lcd.print("Quay ve MH chinh");
  delay(2000);
}
void Show(){
  if(Config_Mode == false){
    if(Status == 0) return;
    else{
      if(Auto_Mode) Display_Caution();
      else Display_On_Off_device();
    }
  }
  else Display_Config();
}
void Up(uint8_t *Value, uint8_t Max, uint8_t Min) {
  if (digitalRead(B_Up) == LOW) {
    delay(20);
    while (digitalRead(B_Up) == LOW); // Chống dội phím

    if (Max == 1 && Min == 0) {
      // Chế độ boolean ON/OFF
      *Value = !(*Value);
    } else {
      // Chế độ số
      if (*Value < Max) (*Value)++;
      else *Value = Min;
    }
    Show();
  }
}

void Down(uint8_t *Value, uint8_t Max, uint8_t Min) {
  if (digitalRead(B_Down) == LOW) {
    delay(20);
    while (digitalRead(B_Down) == LOW); // Chống dội phím

    if (Max == 1 && Min == 0) {
      // Chế độ boolean ON/OFF
      *Value = !(*Value);
    } else {
      // Chế độ số
      if (*Value > Min) (*Value)--;
      else *Value = Max;
    }
    Show();
  }
}
void Switch_Status(uint8_t* Status, bool* State, unsigned long *Time_before, uint8_t Max, uint8_t Min) {
  if (digitalRead(B_Switch) == LOW) {
    *Time_before = millis();
    delay(20);
    while (digitalRead(B_Switch) == LOW);
    
    if ((millis() - *Time_before) >= 1000) {
      *State = !(*State); // đảo trạng thái
      *Status = Min;
    }
    else {
      if (*Status < Max) (*Status)++;
      else if (*Status == Max) *Status = Min;
    }
    Show();
  }
}
void On_Off_Device_State(bool *device, uint8_t Value_Sensor, uint8_t Limit[], bool State) {
  if (State) { 
    // State = true → kiểu "bật khi thấp hơn min, tắt khi cao hơn max"
    if (Value_Sensor <= Limit[0]) *device = true;   // nhỏ hơn min thì bật
    else if (Value_Sensor >= Limit[1]) *device = false; // lớn hơn max thì tắt
  } 
  else {
    // State = false → kiểu "bật khi cao hơn min, tắt khi thấp hơn max"
    if (Value_Sensor >= Limit[0]) *device = true;   // lớn hơn min thì bật
    else if (Value_Sensor <= Limit[1]) *device = false; // nhỏ hơn max thì tắt
  }
}

void On_Off_Device() {
  if (!Config_Mode) {  
    if (Auto_Mode) {
      On_Off_Device_State(&State_Pump,  Value_Soil,  Limit_Soil,  true);   
      On_Off_Device_State(&State_Fan,   Value_Temp,  Limit_Temp,  false); 
      On_Off_Device_State(&State_Fog,   Value_Humi,  Limit_Humi,  true);   
      On_Off_Device_State(&State_Light, Value_Light, Limit_Light, true);   
    }
    digitalWrite(Pump,  State_Pump);
    digitalWrite(Fan,   State_Fan);
    digitalWrite(Fog,   State_Fog);
    digitalWrite(Light, State_Light);
  }
}

void Check_Range(uint8_t Limit[], bool State){
  if(State){
    if(Limit[1] < Limit[0]) Limit[1] = Limit[0] + 1;
  }
  else{
    if(Limit[1] > Limit[0]) Limit[1] = Limit[0] - 1;
  }
}
void Read_Key(){
  if(Config_Mode == false){
    Switch_Status(&Status, &Config_Mode, &Time_Check, 5, 0);
    switch (Status){
      case 0: break;
      case 1:
        Up((uint8_t*)&State_Pump,1,0);
        Down((uint8_t*)&State_Pump,1,0);
        break;
      case 2:
        Up((uint8_t*)&State_Fan,1,0);
        Down((uint8_t*)&State_Fan,1,0);
        break;
      case 3:
        Up((uint8_t*)&State_Fog,1,0);
        Down((uint8_t*)&State_Fog,1,0);
        break;
      case 4:
        Up((uint8_t*)&State_Light,1,0);
        Down((uint8_t*)&State_Light,1,0);
        break;
      case 5: 
        Status = 0;
        Push_sw_to_dt3se(State_Pump, Truong_Bom);
        Push_sw_to_dt3se(State_Fan, Truong_Quat);
        Push_sw_to_dt3se(State_Fog, Truong_Suong);
        Push_sw_to_dt3se(State_Light, Truong_Den);
        break;
    }
  }
  else{
    Switch_Status(&Status, &Config_Mode, &Time_Check, 9, 0);
    switch (Status){
      case 0: 
        Up((uint8_t*)&Auto_Mode,1,0);
        Down((uint8_t*)&Auto_Mode,1,0);
        break;
      case 1:
        Up((uint8_t*)&Limit_Temp[0],50,1);
        Down((uint8_t*)&Limit_Temp[0],50,1);
        break;
      case 2:
        Check_Range(Limit_Temp,false);
        Up((uint8_t*)&Limit_Temp[1],Limit_Temp[0]-1,0);
        Down((uint8_t*)&Limit_Temp[1],Limit_Temp[0]-1,0);
        break;
      case 3:
        Up((uint8_t*)&Limit_Humi[0],99,0);
        Down((uint8_t*)&Limit_Humi[0],99,0);
        break;
      case 4:
        Check_Range(Limit_Humi,true);
        Up((uint8_t*)&Limit_Humi[1],100,Limit_Humi[0]+1);
        Down((uint8_t*)&Limit_Humi[1],100,Limit_Humi[0]+1);
        break;
      case 5:
        Up((uint8_t*)&Limit_Light[0],99,0);
        Down((uint8_t*)&Limit_Light[0],99,0);
        break;
      case 6:
        Check_Range(Limit_Light,true);
        Up((uint8_t*)&Limit_Light[1],100,Limit_Light[0]+1);
        Down((uint8_t*)&Limit_Light[1],100,Limit_Light[0]+1);
        break;
      case 7:
        Up((uint8_t*)&Limit_Soil[0],99,0);
        Down((uint8_t*)&Limit_Soil[0],99,0);
        break;
      case 8:
        Check_Range(Limit_Soil,true);
        Up((uint8_t*)&Limit_Soil[1],100,Limit_Soil[0]+1);
        Down((uint8_t*)&Limit_Soil[1],100,Limit_Soil[0]+1);
        break;
      case 9:
        Status = 0;
        Config_Mode = false;
        Push_sw_to_dt3se(Auto_Mode, Truong_AutoMode);
        Push_all_arr();
        Diplay_Config_Successful();
        break;
    }
  }
}

void setValueSensortoDt3se() {
  if (Firebase.ready()) {
    if (Value_Temp != ExValue_Temp){
      if (!Firebase.setInt(fbdo, Truong_Temp.c_str(), Value_Temp)) return;
      ExValue_Temp = Value_Temp;
    }
    if (Value_Humi != ExValue_Humi){
      if (!Firebase.setInt(fbdo, Truong_Humi.c_str(), Value_Humi)) return;
      ExValue_Humi = Value_Humi;
    }
    if (Value_Soil != ExValue_Soil){
      if (!Firebase.setInt(fbdo, Truong_Soil.c_str(), Value_Soil)) return;
      ExValue_Soil = Value_Soil;
    }
    if (Value_Light != ExValue_Light){
      if (!Firebase.setInt(fbdo, Truong_Light.c_str(), Value_Light)) return;
      ExValue_Light = Value_Light;
    }
  }
}


void Read_sw_dt3se(bool *sw_state, const String &Truong) {
  if (Firebase.ready()){
    if (Firebase.getBool(fbdo, Truong.c_str())) {
      *sw_state = fbdo.to<bool>();
    }
  }
}

void Read_allSwitch_and_allLimit() {
  if (ReadSwDTB == false && millis() - Time_Check >= 1500) {
    if (Firebase.ready()) {
      if (Firebase.getBool(fbdo, Truong_ChangingResponse.c_str())) {
        if (!fbdo.to<bool>()) {
          Read_all_arr();
          Push_sw_to_dt3se(true, Truong_ChangingResponse);
          Diplay_Config_Successful();
        }
      }

      // --- Đọc AutoMode ---
      if (Firebase.getBool(fbdo, Truong_AutoMode.c_str()) && !Config_Mode) {
        bool autoModeFromDB = fbdo.to<bool>();

        if (Auto_Mode != autoModeFromDB) {
          Auto_Mode = autoModeFromDB;
          Display_Auto_Change();

          if (!Auto_Mode ) {
            Firebase.setBool(fbdo, Truong_Bom.c_str(), false);
            Firebase.setBool(fbdo, Truong_Quat.c_str(), false);
            Firebase.setBool(fbdo, Truong_Den.c_str(), false);
            Firebase.setBool(fbdo, Truong_Suong.c_str(), false);
          }
        }

        if (!Auto_Mode && Status == 0 && !Config_Mode) {
          Read_sw_dt3se(&State_Pump, Truong_Bom);
          Read_sw_dt3se(&State_Fan, Truong_Quat);
          Read_sw_dt3se(&State_Light, Truong_Den);
          Read_sw_dt3se(&State_Fog, Truong_Suong);
          ReadSwDTB = true;
        }
      }
    }
  }
}



void Push_sw_to_dt3se(bool sw_state, const String &Truong) {
  if (Firebase.ready()){
    if (!Firebase.setBool(fbdo, Truong.c_str(), sw_state)) {
      Serial.printf("Lỗi set %s: %s\n", Truong.c_str(), fbdo.errorReason().c_str());
    }
  }
}

bool Read_arr_fr_dt3se(uint8_t limit[2], const String &Truong) {
  if (Firebase.getArray(fbdo, Truong.c_str())) {
    FirebaseJsonArray &arr = fbdo.jsonArray();
    FirebaseJsonData data;

    if (arr.get(data, 0) && data.success) {
      if (data.typeNum == FirebaseJson::JSON_INT || data.typeNum == FirebaseJson::JSON_DOUBLE || data.typeNum == FirebaseJson::JSON_FLOAT) {
        limit[0] = (uint8_t)data.to<int>();
      } 
      else return false;
    } 
    else return false;
    
    if (arr.get(data, 1) && data.success) {
      if (data.typeNum == FirebaseJson::JSON_INT || data.typeNum == FirebaseJson::JSON_DOUBLE || data.typeNum == FirebaseJson::JSON_FLOAT) {
        limit[1] = (uint8_t)data.to<int>();
      } else return false;
    } 
    else return false;
    return true;
  } 
  else {
    return false;
  }
}


bool Push_arr_to_dt3se(uint8_t limit[2], const String &Truong) {
  FirebaseJsonArray arr;
  arr.add((int)limit[0]);
  arr.add((int)limit[1]);

  if (Firebase.setArray(fbdo, Truong.c_str(), arr)) {
    return true;
  } 
  else {
    return false;
  }
}
void Read_all_arr() {
  if (Firebase.ready()){
    const uint8_t MAX_RETRY = 5;
    for (int i = 0; i < MAX_RETRY && !Read_arr_fr_dt3se(Limit_Temp, Truong_limitTemp); i++) delay(200);
    for (int i = 0; i < MAX_RETRY && !Read_arr_fr_dt3se(Limit_Humi, Truong_limitHumi); i++) delay(200);
    for (int i = 0; i < MAX_RETRY && !Read_arr_fr_dt3se(Limit_Soil, Truong_limitSoil); i++) delay(200);
    for (int i = 0; i < MAX_RETRY && !Read_arr_fr_dt3se(Limit_Light, Truong_limitLight); i++) delay(200);
  }
}

void Push_all_arr() {
  if (Firebase.ready()) {
    const uint8_t MAX_RETRY = 5;
    for (uint8_t i = 0; i < MAX_RETRY && !Push_arr_to_dt3se(Limit_Temp, Truong_limitTemp); i++) delay(200);
    for (uint8_t i = 0; i < MAX_RETRY && !Push_arr_to_dt3se(Limit_Humi, Truong_limitHumi); i++) delay(200);
    for (uint8_t i = 0; i < MAX_RETRY && !Push_arr_to_dt3se(Limit_Soil, Truong_limitSoil); i++) delay(200);
    for (uint8_t i = 0; i < MAX_RETRY && !Push_arr_to_dt3se(Limit_Light, Truong_limitLight); i++) delay(200);
  }
}

void Set_off_all(){
  Firebase.setBool(fbdo, Truong_Bom.c_str(), false);
  Firebase.setBool(fbdo, Truong_Quat.c_str(), false);
  Firebase.setBool(fbdo, Truong_Den.c_str(), false);
  Firebase.setBool(fbdo, Truong_Suong.c_str(), false);
  Firebase.setBool(fbdo, Truong_AutoMode.c_str(), false);
  Firebase.setBool(fbdo, Truong_ChangingResponse.c_str(), true);
}

void Set_onineStatus(){
  if (Firebase.ready()){
    Firebase.setBool(fbdo, Truong_OnlineStatus.c_str(), true);
  }
}
