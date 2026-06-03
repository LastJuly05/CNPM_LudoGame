package ludo.model;

public enum HorseState {
    IN_BASE,    // Trong chuồng
    ON_PATH,    // Đang trên đường đi
    IN_HOME,    // Đang lên chuồng đích (leo bậc)
    FINISHED    // Đã hoàn thành (vào đúng vị trí)
}