package com.anecoz.billie;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class FirstFragment extends Fragment {

    private ControlSwitchListener _listener;
    Switch _tailSwitch;
    Switch _lockSwitch;
    Switch _logSwitch;
    Button _recenterBtn;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ControlSwitchListener) {
            _listener = (ControlSwitchListener)context;
        }
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        _tailSwitch = (Switch) view.findViewById(R.id.switch_taillight);
        _lockSwitch = (Switch) view.findViewById(R.id.switch_lock);
        _logSwitch = (Switch) view.findViewById(R.id.switch_log);
        _recenterBtn = (Button) view.findViewById(R.id.button_recenter);

        _tailSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                _listener.onTaillight(b);
            }
        });

        _lockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                _listener.onLock(b);
            }
        });

        _logSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                _listener.onLog(b);
            }
        });

        _recenterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _listener.onRecenter();
            }
        });
    }

    public void setSpeed(String speed) {
        TextView tv =getView().findViewById(R.id.textview_currentspeed);
        tv.setText(speed);
    }

    public void setBatteryCharge(String charge) {
        TextView tv =getView().findViewById(R.id.textview_batterycharge);
        tv.setText(charge);
    }

    public void setBatteryVoltage(String voltage) {
        TextView tv =getView().findViewById(R.id.textview_batteryvoltage);
        tv.setText(voltage);
    }

    public void setOdometer(String odometer) {
        TextView tv =getView().findViewById(R.id.textview_odometer);
        tv.setText(odometer);
    }

    public void setTripKm(String trip) {
        TextView tv =getView().findViewById(R.id.textview_tripkm);
        tv.setText(trip);
    }

    public void setTripTime(String time) {
        TextView tv =getView().findViewById(R.id.textview_triptime);
        tv.setText(time);
    }

    public void setTemp(String temp) {
        TextView tv =getView().findViewById(R.id.textview_temp);
        tv.setText(temp);
    }

    public void setRange(String range) {
        TextView tv =getView().findViewById(R.id.textview_rangekm);
        tv.setText(range);
    }

    public void setTailLight(boolean val) {
        if (_tailSwitch.isChecked() == val) return;
        _tailSwitch.setChecked(val);
    }

    public void setLock(boolean val) {
        if (_lockSwitch.isChecked() == val) return;
        _lockSwitch.setChecked(val);
    }
}