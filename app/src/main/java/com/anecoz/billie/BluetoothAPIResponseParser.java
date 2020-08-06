package com.anecoz.billie;

public class BluetoothAPIResponseParser {
    private final String TAG = "ResponseParser";

    public BluetoothAPIResponseParser() {
    }

    private short bytesToShort(byte hi, byte lo) {
        return (short)(((hi & 0xFF) << 8) | (lo & 0xFF));
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public Response parse(byte[] bytes) {
        if (bytes.length < 9) return null;

        Response response = new Response();
        response._command = bytes[5];

        if (response._command == (byte)RegMap.M365_BATT_REG) {
            response._val = String.valueOf(bytes[6]) + " %";
        }
        else if (response._command == (byte)RegMap.M365_KM_REMAIN_REG) {
            double val = bytesToShort(bytes[7], bytes[6]) / 100.0;
            response._val = val + " km";
        }
        else if (response._command == (byte)RegMap.M365_FRAMETEMP_REG) {
            double val = bytesToShort(bytes[7], bytes[6]) / 10.0;
            response._val = val + " C";
        }
        else if (response._command == (byte)RegMap.M365_ODOMETER_REG) {
            double val = round(bytesToShort(bytes[7], bytes[6]) / 1000.0, 2);
            response._val = val + " km";
        }
        else if (response._command == (byte)RegMap.M365_SPEED_REG) {
            double val = round(bytesToShort(bytes[7], bytes[6]) / 1000.0, 1);
            response._val = val + " km/h";
        }
        else if (response._command == (byte)RegMap.M365_TRIP_KM_REG) {
            double val = round(bytesToShort(bytes[7], bytes[6]) / 100.0, 2);
            response._val = val + " km";
        }
        else if (response._command == (byte)RegMap.M365_TRIPSPEED_REG) {
            double val = round(bytesToShort(bytes[7], bytes[6]) / 1000.0, 1);
            response._val = val + " km/h";
        }
        else if (response._command == (byte)RegMap.M365_TRIPTIME_REG) {
            short val = bytesToShort(bytes[7], bytes[6]);
            int minutes = val / 60;
            int seconds = val % 60;

            String minString = String.valueOf(minutes);
            if (minutes < 10) {
                minString = "0" + String.valueOf(minutes);
            }
            String secString = String.valueOf(seconds);
            if (seconds < 10) {
                secString = "0" + String.valueOf(seconds);
            }
            response._val = minString + ":" + secString;
        }
        else if (response._command == (byte)RegMap.BATT_VOLTAGE_REG) {
            double val = bytesToShort(bytes[7], bytes[6]) / 100.0;
            response._val = val + " V";
        }
        else if (response._command == (byte)RegMap.BATT_CURRENT_REG) {
            double val = bytesToShort(bytes[7], bytes[6]) / 100.0;
            response._val = val + " A";
        }
        else if (response._command == (byte)RegMap.BATT_TEMP_REG) {
            double tmp1 = bytes[7] - 20;
            double tmp2 = bytes[6] - 20;
            response._val = "TEMP1: " + tmp1 + " C, TEMP2: " + tmp2 + " C";
        }
        else if (response._command == (byte)RegMap.BATT_CHARGE_COUNT_REG) {
            short val = (short) (bytes[7] + bytes[6]);
            response._val = val + " times";
        }
        else if (response._command == (byte)RegMap.M365_CHECK_TAILLIGHT_REG) {
            response._bVal = bytes[6] == 2;
        }
        else if (response._command == (byte)RegMap.M365_CHECK_LOCK_REG) {
            response._bVal = bytes[6] == 2;
        }

        return response;
    }
}
