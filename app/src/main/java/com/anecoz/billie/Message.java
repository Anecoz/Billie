package com.anecoz.billie;

public class Message {
    private byte[] _data;
    private int _numParams;

    public int _command;

    public Message(int direction, int rw, int command, byte[] params) {
        _numParams = params.length;
        _command = command;

        _data = new byte[_numParams + 8];

        _data[0] = (byte)0x55; // start byte
        _data[1] = (byte)0xAA; // start byte
        _data[2] = (byte)(_numParams + 2); // Length, 3 for requests
        _data[3] = (byte)direction;  // Direction, master_to_365
        _data[4] = (byte)rw; // Read/Write, (0x01 for READ and 0x03 for WRITE)
        _data[5] = (byte)command; // Command

        if (_numParams > 0) System.arraycopy(params, 0, _data, 6, _numParams);

        int checksum = _numParams + 2; // length
        checksum += direction; // direction
        checksum += rw; // RW
        checksum += command; // command

        for (int i = 0; i < _numParams; ++i) {
            checksum += params[i];
        }

        checksum = checksum ^ 0xFFFF; // xor

        _data[6 + _numParams] = (byte)(checksum & 0xFF);
        _data[7 + _numParams] = (byte)(checksum >> 8);
    }

    public byte[] getData() {
        return _data;
    }
}
