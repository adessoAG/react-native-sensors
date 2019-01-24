package com.sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.support.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class Orientation extends ReactContextBaseJavaModule implements SensorEventListener {

    private final ReactApplicationContext reactContext;
    private final SensorManager sensorManager;
    private final Sensor sensor;
    private double lastReading = (double) System.currentTimeMillis();
    private int interval;
    private Arguments arguments;
    private int accuracy = -1;
    private float[] orientation = new float[3];
    private float[] rotationMatrix = new float[9];

    public Orientation(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.sensorManager = (SensorManager) reactContext.getSystemService(reactContext.SENSOR_SERVICE);
        this.sensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    // RN Methods
    @ReactMethod
    public void isAvailable(Promise promise) {
        if (this.sensor == null) {
            // No sensor found, throw error
            promise.reject(new RuntimeException("No Rotation Sensor found"));
            return;
        }
        promise.resolve(null);
    }

    @ReactMethod
    public void setUpdateInterval(int newInterval) {
        this.interval = newInterval;
    }


    @ReactMethod
    public void startUpdates() {
        // Milisecond to Mikrosecond conversion
        this.sensorManager.registerListener(this, this.sensor, this.interval * 1000);
    }

    @ReactMethod
    public void stopUpdates() {
        this.sensorManager.unregisterListener(this);
    }

    @Override
    public String getName() {
        return "Orientation";
    }

    // SensorEventListener Interface
    private void sendEvent(String eventName, @Nullable WritableMap params) {
        try {
            this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        } catch (RuntimeException e) {
            Log.e("ERROR", "java.lang.RuntimeException: Trying to invoke Javascript before CatalystInstance has been set!");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double tempMs = (double) System.currentTimeMillis();
        if (tempMs - this.lastReading >= this.interval) {
            this.lastReading = tempMs;

            Sensor mySensor = sensorEvent.sensor;
            WritableMap map = arguments.createMap();

            if (mySensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                // calculate th rotation matrix
                SensorManager.getRotationMatrixFromVector(this.rotationMatrix, sensorEvent.values);
                // get the azimuth value (orientation[0]) in degree

                final float[] orientation = SensorManager.getOrientation(this.rotationMatrix, this.orientation);
                final double degreesX = Math.toDegrees(orientation[0]);
                final double degreesZ = Math.toDegrees(orientation[2]);
                int newAzimuth = (int) (((((degreesX + 360) % 360) - degreesZ) + 360) % 360);

                map.putDouble("azimuth", newAzimuth);
                map.putDouble("timestamp", tempMs);
                map.putDouble("accuracy", this.accuracy);
                this.sendEvent("Orientation", map);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            this.accuracy = accuracy;
        }
    }
}
