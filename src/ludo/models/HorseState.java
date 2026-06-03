package ludo.models;

public enum HorseState {
    IN_BASE,    // Trong chuồng gốc
    ON_PATH,    // Trên đường đi vòng quanh bàn cờ
    IN_HOME,    // Đang leo bậc chuồng đích
    FINISHED    // Đã cán đích hoàn thành (Bậc 6)
}