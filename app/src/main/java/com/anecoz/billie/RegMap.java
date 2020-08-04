package com.anecoz.billie;

public class RegMap {
    public static int REG_READ = 0x01;
    public static int REG_WRITE = 0x03;

    // First register, scooter ECU?
    public static int MASTER_TO_M365 = 0x20;
    public static int M365_TO_MASTER = 0x23;

    public static int M365_KM_REMAIN_REG = 0x25; // ex 123 = 1.23km
    public static int M365_KM_REMAIN_LEN = 2;

    public static int M365_TRIPTIME_REG = 0x3b;
    public static int M365_TRIPTIME_LEN = 2;

    public static int M365_FRAMETEMP_REG = 0x3e; // in degrees * 10, ex 123 = 12.3 celsius
    public static int M365_FRAMETEMP_LEN = 2;

    public static int M365_FRAMETEMP2_REG = 0xbb; // same as above, 123 = 12.3. Displayed on dashboard?
    public static int M365_FRAMETEMP2_LEN = 2;

    public static int M365_ECO_MODE_REG = 0x75;
    public static int M365_ECO_MODE_LEN = 2;

    public static int M365_BATT_REG = 0xb4; // in percent
    public static int M365_BATT_LEN = 2;

    public static int M365_SPEED_REG = 0xb5; // in m/h?
    public static int M365_SPEED_LEN = 2;

    public static int M365_TRIPSPEED_REG = 0xb6; // in m/h?
    public static int M365_TRIPSPEED_LEN = 2;

    public static int M365_ODOMETER_REG = 0xb7; // scooter mileage meters, the 4 bits are concatenated 0xb8 is the most significant
    public static int M365_ODOMETER_LEN = 4;

    public static int M365_TRIP_KM_REG = 0xb9;
    public static int M365_TRIP_KM_LEN = 2;

    // Second register, for battery ECU?
    public static int MASTER_TO_BATT = 0x22;
    public static int BATT_TO_MASTER = 0x25;

    public static int BATT_CAPACITY_REG = 0x18;
    public static int BATT_CAPACITY_LEN = 2;

    public static int BATT_REMAIN_MAH_REG = 0x31;
    public static int BATT_REMAIN_MAH_LEN = 2;

    public static int BATT_CURRENT_REG = 0x33; // battery current in amps * 100, may be negative (charge)
    public static int BATT_CURRENT_LEN = 2;

    public static int BATT_VOLTAGE_REG = 0x34; // battery voltage * 10
    public static int BATT_VOLTAGE_LEN = 2;

    public static int BATT_TEMP_REG = 0x35; // cell temperature MSB temp1 and LSB temp2, in degrees plus 20. ex 31 = 11degrees
    public static int BATT_TEMP_LEN = 2;

    public static int BATT_HEALTH_REG = 0x3b; // health of battery 0-100, error if less than 60
    public static int BATT_HEALTH_LEN = 2;
}
