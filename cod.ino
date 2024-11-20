#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <FirebaseClient.h>
#include <Adafruit_AHTX0.h>

#define WIFI_SSID ""
#define WIFI_PASSWORD ""
#define API_KEY ""
#define USER_EMAIL ""
#define USER_PASSWORD ""
#define DATABASE_URL "https://projeto-7-1806-default-rtdb.firebaseio.com/"
#define RELAY_PIN D3
#define RELAY_PIN2 D4

void printResult(AsyncResult &aResult);
void reconnectWiFi();
void reconnectFirebase();

DefaultNetwork network;
UserAuth user_auth(API_KEY, USER_EMAIL, USER_PASSWORD);
FirebaseApp app;
Adafruit_AHTX0 aht;
WiFiClientSecure ssl_client;
using AsyncClient = AsyncClientClass;
AsyncClient aClient(ssl_client, getNetwork(network));
RealtimeDatabase Database;
AsyncResult aResult_no_callback;

unsigned long previousMillis = 0;
const long relayInterval = 5000; // Intervalo de 5 segundos para verificar o relé
unsigned long previousPrintMillis = 0;
const long printInterval = 30000; // Intervalo de 30 segundos para imprimir temperatura e umidade

void setup() {
    Serial.begin(115200);
    Serial.println("Iniciando...");

    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    reconnectWiFi();

    Firebase.printf("Firebase Client v%s\n", FIREBASE_CLIENT_VERSION);
    Serial.println("Inicializando o aplicativo...");

    ssl_client.setInsecure();
    initializeApp(aClient, app, getAuth(user_auth), aResult_no_callback);
    app.getApp<RealtimeDatabase>(Database);
    Database.url(DATABASE_URL);

    if (!aht.begin()) {
        Serial.println("Não foi possível inicializar o sensor AHTX0!");
        while (1);
    }

    pinMode(RELAY_PIN, OUTPUT);
    pinMode(RELAY_PIN2, OUTPUT);

    Database.get(aClient, "/relay", aResult_no_callback);
}

void loop() {
    if (WiFi.status() != WL_CONNECTED) {
        reconnectWiFi();
    }

    app.loop();
    Database.loop();

    unsigned long currentMillis = millis();

    // Intervalo para impressão de temperatura e umidade
    if (currentMillis - previousPrintMillis >= printInterval) {
        previousPrintMillis = currentMillis;

        sensors_event_t humidity, temp;
        aht.getEvent(&humidity, &temp);
        Serial.print("Temperature: ");
        Serial.print(temp.temperature);
        Serial.println("*C");
        Serial.print("Humidity: ");
        Serial.print(humidity.relative_humidity);
        Serial.println("%rH");

        // Enviar a temperatura para o Firebase
        String tempPath = "/temperature";
        Database.set(aClient, tempPath, temp.temperature, aResult_no_callback);
    }

    // Intervalo para verificação do relé
    if (currentMillis - previousMillis >= relayInterval) {
        previousMillis = currentMillis;

        Database.get(aClient, "/relay", aResult_no_callback);
        String payload = aResult_no_callback.c_str();

        if (aResult_no_callback.available()) {
            if (payload == "false") {
                digitalWrite(RELAY_PIN, HIGH);
                digitalWrite(RELAY_PIN2, HIGH);
                Serial.println("Relé desligado");
            } else {
                digitalWrite(RELAY_PIN, LOW);
                digitalWrite(RELAY_PIN2, LOW);
                Serial.println("Relé ligado");
            }

            printResult(aResult_no_callback);
        } else {
            Serial.println("Nenhum dado disponível no Firebase.");
        }

        if (aResult_no_callback.isError()) {
            Serial.println("Erro ao acessar o Firebase:");
            printResult(aResult_no_callback);
            reconnectFirebase();
        }
    }
}

void printResult(AsyncResult &aResult) {
    if (aResult.isEvent()) {
        Serial.printf("Evento: %s\n", aResult.c_str());
    } else if (aResult.isDebug()) {
        Serial.printf("Debug: %s\n", aResult.c_str());
    } else if (aResult.isError()) {
        Serial.printf("Erro: %s\n", aResult.c_str());
    } else if (aResult.available()) {
        Serial.printf("Dados: %s\n", aResult.c_str());
    }
}

void reconnectWiFi() {
    Serial.print("Reconectando ao Wi-Fi...");
    while (WiFi.status() != WL_CONNECTED) {
        Serial.print(".");
        WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
        delay(5000);
    }
    Serial.println("Conectado.");
}

void reconnectFirebase() {
    Serial.println("Reconectando ao Firebase...");
    initializeApp(aClient, app, getAuth(user_auth), aResult_no_callback);
    app.getApp<RealtimeDatabase>(Database);
    Database.url(DATABASE_URL);
    Serial.println("Reconectado ao Firebase.");
}
