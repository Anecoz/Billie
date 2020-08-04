package com.anecoz.billie;

import android.util.Log;

public class BluetoothAPIResponseParser {
    private final String TAG = "ResponseParser";

    public BluetoothAPIResponseParser() {
    }

    private short bytesToShort(byte hi, byte lo) {
        return (short)(((hi & 0xFF) << 8) | (lo & 0xFF));
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
            double val = bytesToShort(bytes[7], bytes[6]) / 100.0;
            response._val = val + " km";
        }
        else if (response._command == (byte)RegMap.M365_SPEED_REG) {
            double val = bytesToShort(bytes[7], bytes[6]) / 1000.0;
            response._val = val + " km/h";
        }
        else if (response._command == (byte)RegMap.M365_TRIP_KM_REG) {
            double val = bytesToShort(bytes[7], bytes[6]) / 100.0;
            response._val = val + " km";
        }
        else if (response._command == (byte)RegMap.M365_TRIPSPEED_REG) {
            double val = bytesToShort(bytes[7], bytes[6]) / 1000.0;
            response._val = val + " km/h";
        }
        else if (response._command == (byte)RegMap.M365_TRIPTIME_REG) {
            short val = bytesToShort(bytes[7], bytes[6]);
            response._val = val + " s";
        }
        else if (response._command == (byte)RegMap.BATT_VOLTAGE_REG) {
            double val = bytesToShort(bytes[7], bytes[6]) / 100.0;
            response._val = val + " V";
        }
        else if (response._command == (byte)RegMap.BATT_CURRENT_REG) {
            double val = bytesToShort(bytes[7], bytes[6]) / 100.0;
            response._val = val + " A";
        }

        return response;
    }
}
