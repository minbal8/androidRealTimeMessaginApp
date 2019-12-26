package com.ktuedu.rtMessaging;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.TransitionDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ktuedu.rtMessaging.Models.ModelPost;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class tempFunctions extends AppCompatActivity implements SensorEventListener, LocationListener {


    private SensorManager sensorManager;
    private Sensor senAcelerometer;
    private Sensor senMagnetic;

    TextView xValue;
    TextView yValue;
    TextView zValue;
    TextView orientationValue;

    boolean updateValues = false;

    Button startStopBtn;
    TextView coordinates;
    TextView coordinatesNetwork;
    LocationManager locationManager;
    private static final String TAG = "AndroidCameraApi";
    private Button takePictureBtn;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private String cameraID;
    CameraDevice cameraDevice;
    CameraCaptureSession cameraCaptureSession;
    CaptureRequest.Builder captureRequestBuilder;
    Size imageDimension;
    ImageReader imageReader;
    File file;
    static final int REQUEST_CAMERA_PERMISSION = 200;
    Handler mBackgroundHandler;
    HandlerThread mBackgroundThread;
    private Sensor mRotationV;
    TextView tvHeading;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_temp_functions);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAcelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senMagnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mRotationV = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);


        xValue = findViewById(R.id.x_value);
        yValue = findViewById(R.id.y_value);
        zValue = findViewById(R.id.z_value);
        orientationValue = findViewById(R.id.orientation_value);
        startStopBtn = findViewById(R.id.start_and_stop);

        startStopBtn.setOnClickListener(startStop);
        coordinates = findViewById(R.id.coordinates);
        coordinatesNetwork = findViewById(R.id.coordinates_network);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        tvHeading = findViewById(R.id.compass_value);

        textureView = findViewById(R.id.textureView);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        takePictureBtn = findViewById(R.id.take_photo);
        assert takePictureBtn != null;
        takePictureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });

    }

    private void shutDown(){
        finishAffinity();
        System.exit(0);
    }

    protected void takePicture() {
        if (cameraDevice == null) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            final File file = new File(Environment.getExternalStorageDirectory() + "pic.jpg");

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Image image = null;
                    try {
                        image = imageReader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (output != null) {
                            output.close();
                        }
                    }
                }
            };

            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);

            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(tempFunctions.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "Camera is opened");

        try {
            cameraID = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraID);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(tempFunctions.this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraID, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }


    View.OnClickListener startStop = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!updateValues) {
                sensorManager.registerListener(tempFunctions.this, senAcelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                sensorManager.registerListener(tempFunctions.this, senMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
                sensorManager.registerListener(tempFunctions.this, mRotationV, SensorManager.SENSOR_DELAY_NORMAL);
                startStopBtn.setText("Stop");

            } else {
                sensorManager.unregisterListener(tempFunctions.this, senAcelerometer);
                sensorManager.unregisterListener(tempFunctions.this, senMagnetic);
                sensorManager.unregisterListener(tempFunctions.this, mRotationV);
                startStopBtn.setText("Start");
            }
            updateValues = !updateValues;
        }
    };

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int i) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(tempFunctions.this, "Saved: " + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };


    private void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) {
                        return;
                    }
                    cameraCaptureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(tempFunctions.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null) {
            Log.e(TAG, "updatePreview error! return");
            return;
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "We need this permission!!!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    int mAzimuth;
    float[] rMat = new float[9];
    float[] orientation = new float[3];
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    boolean taken = false;

    private float[] averageX= new float[10];
    private float[] averageY= new float[10];
    private float[] averageZ= new float[10];
    int current = 0;

    int status = 0; //1 untouched, 2 set100 1 set0

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {


            if(sensorEvent.values[2] < -9){
                shutDown();
            }

            averageX[current] = Math.round(1000 * sensorEvent.values[0]) / 1000;
            averageY[current] = Math.round(1000 * sensorEvent.values[1]) / 1000;
            averageZ[current] = Math.round(1000 * sensorEvent.values[2]) / 1000;
            current++;
            if (current > 9) current = 0;

            float[] temp = new float[3];
            for (int i = 0; i < 10; i++) {
                temp[0] += averageX[i];
                temp[1] += averageY[i];
                temp[2] += averageZ[i];
            }
            for (int i = 0; i < 3; i++) {
                temp[i] /= 10;
            }

            xValue.setText(String.valueOf(temp[0]));
            yValue.setText(String.valueOf(temp[1]));
            zValue.setText(String.valueOf(temp[2]));

            if (sensorEvent.values[0] > 5) {
                orientationValue.setText("Turned Left");
            } else if (sensorEvent.values[0] < -5) {
                orientationValue.setText("Turned Right");
            } else if (sensorEvent.values[1] > 5) {
                orientationValue.setText("Turned Up");
            } else {
                orientationValue.setText("Turned upside down");
            }

            if (sensorEvent.values[2] > 9 && status > 0) {
                status = 0;
                saveBrightness(this, 0);
            } else if (sensorEvent.values[1] > 9 && status < 2) {
                status = 2;
                saveBrightness(this, 254);
            }

            xValue.setText(String.valueOf(temp[0]));
            yValue.setText(String.valueOf(temp[1]));
            zValue.setText(String.valueOf(temp[2]));
        }

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rMat, sensorEvent.values);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(sensorEvent.values, 0, mLastAccelerometer, 0, sensorEvent.values.length);
            mLastAccelerometerSet = true;
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(sensorEvent.values, 0, mLastMagnetometer, 0, sensorEvent.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(rMat, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(rMat, orientation);
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        }

        mAzimuth = Math.round(mAzimuth);

        String where = "NW";

        if (mAzimuth >= 350 || mAzimuth <= 10) {
            where = "N";
            if (!taken) {
                takePicture();
                taken = true;
            }
        }
        if (mAzimuth < 350 && mAzimuth > 280) where = "NW";
        if (mAzimuth <= 280 && mAzimuth > 260) where = "W";
        if (mAzimuth <= 260 && mAzimuth > 190) where = "SW";
        if (mAzimuth <= 190 && mAzimuth > 170) where = "S";
        if (mAzimuth <= 170 && mAzimuth > 100) where = "SE";
        if (mAzimuth <= 100 && mAzimuth > 80) where = "E";
        if (mAzimuth <= 80 && mAzimuth > 10) where = "NE";

        String text = mAzimuth + "° " + where;
        tvHeading.setText(text);
    }

    public static void saveBrightness(Activity activity, int brightness) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(activity)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        }
        else {
            try{
                Uri uri = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS);
                Settings.System.putInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
                // resolver.registerContentObserver(uri, true, myContentObserver);
                activity.getContentResolver().notifyChange(uri, null);
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (senAcelerometer != null) {
            sensorManager.unregisterListener(tempFunctions.this, senAcelerometer);
        }
        if (senMagnetic != null) {
            sensorManager.unregisterListener(tempFunctions.this, senMagnetic);
        }
        if (mRotationV != null) {
            sensorManager.unregisterListener(tempFunctions.this, mRotationV);
        }
        stopBackgroundThread();
        closeCamera();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        this.locationManager.removeUpdates(this);


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (senAcelerometer != null && updateValues) {
            sensorManager.registerListener(tempFunctions.this, senAcelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (senMagnetic != null && updateValues) {
            sensorManager.registerListener(tempFunctions.this, senMagnetic, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mRotationV != null && updateValues) {
            sensorManager.registerListener(tempFunctions.this, mRotationV, SensorManager.SENSOR_DELAY_UI);
        }
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        this.locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 400, 1, this);
        this.locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 400, 1, networkLocationListener);


    }

    LocationListener networkLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            coordinatesNetwork.setText("Latitude: " + location.getLatitude()
                    + "\nLongtitude: " + location.getLongitude());
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            coordinates.setText("Latitude: " + location.getLatitude()
                    + "\nLongtitude: " + location.getLongitude());
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}