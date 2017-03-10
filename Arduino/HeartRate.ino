String value;
String out;
int counter;
int flag;
String string;
int counterLine;

#define led

void setup() {
  // initialize the serial communication:
  Serial.begin(9600);
  pinMode(LED_BUILTIN, OUTPUT);
  out="";
  pinMode(10, INPUT); // Setup for leads off detection LO +
  pinMode(11, INPUT); // Setup for leads off detection LO -
  flag=0;

}

void loop() {
  if (Serial.available() > 0) {
    byte a = Serial.read();
    if (a==(byte) 'm'){
      counterLine=0;
      counter=0;
      flag=1;
    }
  }
  if (flag==1 && counter<1500){
    value=analogRead(A0);
    out=out+value;
    counter++;
    counterLine++;
    if (counterLine==20){
      Serial.println(out);
      counterLine=0;
      out="";
      digitalWrite(LED_BUILTIN, HIGH);
    }else{
      out=out+" ";
    }
  }
  
  delay(6);
  digitalWrite(LED_BUILTIN, LOW);
}
