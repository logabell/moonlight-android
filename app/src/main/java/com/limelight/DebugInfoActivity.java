package com.limelight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Bundle;
import android.os.CombinedVibration;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.limelight.preferences.PreferenceConfiguration;
import com.limelight.utils.DeviceUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DebugInfoActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tx_gamepad_info;
    private Vibrator vibrator;
    private Button bt_vibrator;
    private List<InputDevice> ids = new ArrayList<>();
    private Vibrator vibratorOnline;
    private VibratorManager vibratorManagerOnline;
    private Button bt_vibrator_value;
    private int simulatedAmplitude = 220;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_axitest);

        tx_gamepad_info = findViewById(R.id.tx_game_pad_info);
        TextView tx_content = findViewById(R.id.tx_content);
        bt_vibrator = findViewById(R.id.bt_vibrator);
        bt_vibrator_value = findViewById(R.id.bt_vibrator_value);

        vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String kernelVersion = System.getProperty("os.version");
        StringBuffer sb = new StringBuffer();
        sb.append(getString(R.string.debug_info_android_version) + DeviceUtils.getSDKVersionName());
        sb.append("\t" + getString(R.string.debug_info_api_version) + Build.VERSION.SDK_INT);
        sb.append("\n" + getString(R.string.debug_info_kernel_version) + kernelVersion);
        sb.append("\n" + getString(R.string.debug_info_brand_model) + DeviceUtils.getManufacturer() + "\t-\t" + DeviceUtils.getModel());
        tx_content.setText(sb.toString());

        boolean hasVibrator = ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).hasVibrator();
        String content = hasVibrator ? getString(R.string.debug_info_has_vibration_motor) : getString(R.string.debug_info_no_vibration_motor);
        bt_vibrator.setText(getString(R.string.debug_info_test_device_vibration, content));

        showSimlateAmp();
    }

    private void showSimlateAmp() {
        bt_vibrator_value.setText(getString(R.string.debug_info_vibration_amplitude, simulatedAmplitude));
    }

    private void cancleRumble() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && vibratorManagerOnline != null) {
            vibratorManagerOnline.cancel();
            vibratorManagerOnline = null;
        }
        if (vibratorOnline != null) {
            vibratorOnline.cancel();
            vibratorOnline = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.bt_vibrator_cancle) {
            cancleRumble();
            return;
        }
        // Device Vibration
        if (v.getId() == R.id.bt_vibrator) {
            String[] titles = new String[]{getString(R.string.debug_info_simple_vibration), getString(R.string.debug_info_continuous_hd_vibration)};
            new AlertDialog.Builder(this).setItems(titles, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    switch (which) {
                        case 0:
                            vibrator.vibrate(1000);
                            break;
                        case 1:
                            rumble(vibrator);
                            break;
                    }
                }
            }).setTitle(getString(R.string.debug_info_please_choose)).create().show();
            return;
        }

        // Gamepad Vibration
        if (v.getId() == R.id.bt_vibrator_gamepad) {
            if (ids.isEmpty()) {
                Toast.makeText(DebugInfoActivity.this, getString(R.string.debug_info_no_gamepad_detected), Toast.LENGTH_LONG).show();
                return;
            }
            String[] strings = new String[ids.size()];
            for (int i = 0; i < ids.size(); i++) {
                strings[i] = ids.get(i).getName();
            }
            new AlertDialog.Builder(this).setItems(strings, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    InputDevice inputDevice = ids.get(which);
                    if (hasGamepadRumble(inputDevice)) {
                        String[] titles = new String[]{getString(R.string.debug_info_simple_vibration), getString(R.string.debug_info_continuous_hd_vibration)};
                        new AlertDialog.Builder(DebugInfoActivity.this).setItems(titles, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which2) {
                                dialog.dismiss();
                                switch (which2) {
                                    case 0:
                                        vibrateGamepadOnce(inputDevice);
                                        break;
                                    case 1:
                                        rumbleGamepad(inputDevice);
                                        break;
                                }
                            }
                        }).setTitle(getString(R.string.debug_info_please_choose)).create().show();
                    } else {
                        Toast.makeText(DebugInfoActivity.this, getString(R.string.debug_info_no_vibrator), Toast.LENGTH_SHORT).show();
                    }
                }
            }).setTitle(getString(R.string.debug_info_please_choose)).create().show();
            return;
        }

        // Refresh Gamepad Info
        if (v.getId() == R.id.bt_update_gamepad) {
            updateGamePad();
            return;
        }

        if (v.getId() == R.id.bt_vibrator_value) {
            SeekBar mSeekBar = getSeekBar();
            AlertDialog.Builder editDialog = new AlertDialog.Builder(this);
            editDialog.setTitle(getString(R.string.debug_info_set_amplitude));
            editDialog.setView(mSeekBar);
            editDialog.create().show();
        }
    }

    private SeekBar getSeekBar() {
        SeekBar mSeekBar = new SeekBar(this);
        mSeekBar.setMax(255);
        mSeekBar.setProgress(simulatedAmplitude);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                simulatedAmplitude = progress;
                showSimlateAmp();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        return mSeekBar;
    }

    private void rumble(Vibrator vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(new long[]{1000}, new int[]{simulatedAmplitude}, 0));
        } else {
            long pwmPeriod = 20;
            long onTime = (long) ((simulatedAmplitude / 255.0) * pwmPeriod);
            long offTime = pwmPeriod - onTime;
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build();
            vibrator.vibrate(new long[]{0, onTime, offTime}, 0, audioAttributes);
        }
    }

    private boolean hasGamepadRumble(InputDevice inputDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && hasAnyVibrator(inputDevice.getVibratorManager())) {
            return true;
        }

        return inputDevice.getVibrator().hasVibrator();
    }

    private void vibrateGamepadOnce(InputDevice inputDevice) {
        cancleRumble();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && vibrateVibratorManager(inputDevice.getVibratorManager(), 1000)) {
            return;
        }

        Vibrator inputDeviceVibrator = inputDevice.getVibrator();
        if (inputDeviceVibrator.hasVibrator()) {
            inputDeviceVibrator.vibrate(1000);
        }
    }

    private void rumbleGamepad(InputDevice inputDevice) {
        cancleRumble();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && vibrateVibratorManager(inputDevice.getVibratorManager(), 60000)) {
            return;
        }

        Vibrator inputDeviceVibrator = inputDevice.getVibrator();
        if (inputDeviceVibrator.hasVibrator()) {
            vibratorOnline = inputDeviceVibrator;
            rumble(vibratorOnline);
        }
    }

    private boolean hasAnyVibrator(VibratorManager vibratorManager) {
        if (vibratorManager == null) {
            return false;
        }

        for (int vibratorId : vibratorManager.getVibratorIds()) {
            if (vibratorManager.getVibrator(vibratorId).hasVibrator()) {
                return true;
            }
        }

        return false;
    }

    private boolean hasDualAmplitudeControlledRumbleVibrators(VibratorManager vibratorManager) {
        return hasAmplitudeControlledVibrators(vibratorManager, 2);
    }

    private boolean hasQuadAmplitudeControlledRumbleVibrators(VibratorManager vibratorManager) {
        return hasAmplitudeControlledVibrators(vibratorManager, 4);
    }

    private boolean hasAmplitudeControlledVibrators(VibratorManager vibratorManager, int expectedCount) {
        if (vibratorManager == null || vibratorManager.getVibratorIds().length != expectedCount) {
            return false;
        }

        for (int vibratorId : vibratorManager.getVibratorIds()) {
            Vibrator vibrator = vibratorManager.getVibrator(vibratorId);
            if (!vibrator.hasVibrator() || !vibrator.hasAmplitudeControl()) {
                return false;
            }
        }

        return true;
    }

    private boolean vibrateVibratorManager(VibratorManager vibratorManager, long durationMillis) {
        if (vibratorManager == null) {
            return false;
        }

        CombinedVibration.ParallelCombination combo = CombinedVibration.startParallel();
        boolean addedVibrator = false;

        for (int vibratorId : vibratorManager.getVibratorIds()) {
            Vibrator vibrator = vibratorManager.getVibrator(vibratorId);
            if (!vibrator.hasVibrator()) {
                continue;
            }

            int amplitude = vibrator.hasAmplitudeControl() ? Math.max(1, simulatedAmplitude) : VibrationEffect.DEFAULT_AMPLITUDE;
            combo.addVibrator(vibratorId, VibrationEffect.createOneShot(durationMillis, amplitude));
            addedVibrator = true;
        }

        if (!addedVibrator) {
            return false;
        }

        vibratorManagerOnline = vibratorManager;

        VibrationAttributes.Builder vibrationAttributes = new VibrationAttributes.Builder();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrationAttributes.setUsage(VibrationAttributes.USAGE_MEDIA);
        }

        vibratorManager.vibrate(combo.combine(), vibrationAttributes.build());
        return true;
    }

    private void appendGamepadRumbleDiagnostics(StringBuffer sb, InputDevice dev) {
        PreferenceConfiguration prefConfig = PreferenceConfiguration.readPreferences(this);
        Vibrator inputDeviceVibrator = dev.getVibrator();

        sb.append("Rumble prefs: enableRumble=").append(prefConfig.enableRumble)
                .append(", forceDeviceRumble=").append(prefConfig.enableDeviceRumble)
                .append(", fallbackToDevice=").append(prefConfig.vibrateFallbackToDevice)
                .append(", fallbackStrength=").append(prefConfig.vibrateFallbackToDeviceStrength)
                .append("%\n");

        sb.append("Legacy Vibrator: hasVibrator=").append(inputDeviceVibrator.hasVibrator());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sb.append(", amplitudeControl=").append(inputDeviceVibrator.hasAmplitudeControl());
        }
        sb.append("\n");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = dev.getVibratorManager();
            int[] vibratorIds = vibratorManager.getVibratorIds();

            sb.append("VibratorManager IDs: ").append(Arrays.toString(vibratorIds)).append("\n");
            for (int vibratorId : vibratorIds) {
                Vibrator vibrator = vibratorManager.getVibrator(vibratorId);
                sb.append("  id ").append(vibratorId)
                        .append(": hasVibrator=").append(vibrator.hasVibrator())
                        .append(", amplitudeControl=").append(vibrator.hasAmplitudeControl())
                        .append("\n");
            }

            boolean dualManager = hasDualAmplitudeControlledRumbleVibrators(vibratorManager);
            boolean quadManager = hasQuadAmplitudeControlledRumbleVibrators(vibratorManager);
            sb.append("Artemis manager match: dual=").append(dualManager)
                    .append(", quad=").append(quadManager)
                    .append("\n");

            sb.append("Predicted attached-device rumble path: ");
            if (prefConfig.enableDeviceRumble) {
                sb.append("forced Android device vibrator");
            }
            else if (quadManager) {
                sb.append("VibratorManager quad motors (rumble + trigger rumble)");
            }
            else if (dualManager) {
                sb.append("VibratorManager dual motors (rumble)");
            }
            else if (inputDeviceVibrator.hasVibrator()) {
                sb.append("legacy InputDevice Vibrator");
            }
            else {
                sb.append("none from this InputDevice");
            }
            sb.append("\n");
        }
        else {
            sb.append("VibratorManager: unavailable before Android 12\n");
        }

        if (!prefConfig.enableRumble) {
            sb.append("Host rumble callbacks are disabled by the Enable Rumble preference.\n");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancleRumble();
    }

    private void updateGamePad() {
        ids.clear();
        StringBuffer sb = new StringBuffer();
        sb.append("\n");
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int deviceId : deviceIds) {
            InputDevice dev = InputDevice.getDevice(deviceId);
            int sources = dev.getSources();
            if (((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
                    || ((sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK)) {
                if (getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_X) != null &&
                        getMotionRangeForJoystickAxis(dev, MotionEvent.AXIS_Y) != null) {
                    // This is a gamepad
                    ids.add(dev);
                    sb.append(getString(R.string.debug_info_name) + dev.getName());
                    sb.append("\n");
                    sb.append(getString(R.string.debug_info_sensors));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        String sensor = "";
                        if (dev.getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
                            sensor += getString(R.string.debug_info_accelerometer);
                        }
                        if (dev.getSensorManager().getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
                            sensor += getString(R.string.debug_info_gyroscope);
                        }
                        if (sensor.length() == 0) {
                            sb.append(getString(R.string.debug_info_no_relevant_driver));
                        } else {
                            sb.append(sensor);
                        }
                        sb.append("\n");
                    } else {
                        sb.append(getString(R.string.debug_info_no_api_below_android12));
                        sb.append("\n");
                    }
                    sb.append(getString(R.string.debug_info_vid_pid) + dev.getVendorId() + "_" + dev.getProductId()
                            + "\t    [" + String.format("%04x", dev.getVendorId()) + "_" + String.format("%04x", dev.getProductId()) + "]");
                    sb.append("\n");
                    sb.append(getString(R.string.debug_info_vibration) + (hasGamepadRumble(dev) ? getString(R.string.debug_info_supported) : getString(R.string.debug_info_not_supported)));
                    sb.append("\n");
                    appendGamepadRumbleDiagnostics(sb, dev);
                    sb.append(getString(R.string.debug_info_details) + "\n");
                    sb.append(dev.toString());
                    sb.append("\n");
                }
            }
        }
        tx_gamepad_info.setText(getString(R.string.debug_info_number_of_gamepads) + ids.size() + "\n" + sb.toString());
    }

    private static InputDevice.MotionRange getMotionRangeForJoystickAxis(InputDevice dev, int axis) {
        InputDevice.MotionRange range;

        // First get the axis for SOURCE_JOYSTICK
        range = dev.getMotionRange(axis, InputDevice.SOURCE_JOYSTICK);
        if (range == null) {
            // Now try the axis for SOURCE_GAMEPAD
            range = dev.getMotionRange(axis, InputDevice.SOURCE_GAMEPAD);
        }

        return range;
    }
}
