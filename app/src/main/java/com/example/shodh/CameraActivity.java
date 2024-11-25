package com.example.shodh;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = "CameraActivity";
    private TextureView textureView;
    private Button captureButton;
    private Button recordButton;

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private MediaRecorder mediaRecorder;
    private ImageReader imageReader;

    private boolean isRecordingVideo = false;
    private File videoFile;
    private File imageFile;

    // Background thread and handler
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        textureView = findViewById(R.id.textureView);
        captureButton = findViewById(R.id.captureButton);
        recordButton = findViewById(R.id.recordButton);

        captureButton.setOnClickListener(view -> takePicture());
        recordButton.setOnClickListener(view -> {
            if (isRecordingVideo) {
                stopRecordingVideo();
            } else {
                startRecordingVideo();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
    };

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG, "Error stopping background thread: " + e.getMessage());
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0]; // Using the first camera
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
                return;
            }

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    startCameraPreviewSession(); // Start the camera preview session once the camera is opened
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                    cameraDevice = null;
                    Log.e(TAG, "Error opening camera: " + error);
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error opening camera: " + e.getMessage());
        }
    }

    private void startCameraPreviewSession() {
        try {
            if (cameraDevice == null) {
                Log.e(TAG, "startCameraPreviewSession: cameraDevice is null");
                return;
            }

            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;

            texture.setDefaultBufferSize(textureView.getWidth(), textureView.getHeight());
            Surface surface = new Surface(texture);

            imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            captureRequestBuilder.addTarget(imageReader.getSurface());

            cameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) {
                        Log.e(TAG, "Camera device is null during configuration");
                        return;
                    }

                    captureSession = session;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(CameraActivity.this, "Configuration failed", Toast.LENGTH_SHORT).show();
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error creating camera preview session: " + e.getMessage());
        }
    }

    private void updatePreview() {
        if (cameraDevice == null) {
            Log.e(TAG, "updatePreview: cameraDevice is null");
            return;
        }

        try {
            captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error updating camera preview: " + e.getMessage());
        }
    }

    private void takePicture() {
        try {
            if (cameraDevice == null) {
                Log.e(TAG, "CameraDevice is null");
                return;
            }

            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());

            // Use the device's rotation for orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, rotation);

            captureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(CameraActivity.this, "Picture taken!", Toast.LENGTH_SHORT).show();
                    updatePreview(); // Restart preview after taking the picture
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error taking picture: " + e.getMessage());
        }
    }

    private ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    saveImage(image);
                }
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }
    };

    private void saveImage(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        // Save the image to external storage
        imageFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "image_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream output = new FileOutputStream(imageFile)) {
            output.write(bytes);
            Toast.makeText(this, "Image saved: " + imageFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Error saving image: " + e.getMessage());
        }
    }

    private void startRecordingVideo() {
        if (cameraDevice == null || !prepareMediaRecorder()) {
            Toast.makeText(this, "Failed to prepare MediaRecorder", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            Surface surface = new Surface(texture);
            Surface mediaRecorderSurface = mediaRecorder.getSurface();

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureRequestBuilder.addTarget(mediaRecorderSurface);
            captureRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(mediaRecorderSurface, surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    captureSession = session;
                    updatePreview();
                    runOnUiThread(() -> {
                        mediaRecorder.start();
                        isRecordingVideo = true;
                        recordButton.setText("Stop Recording");
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(CameraActivity.this, "Configuration failed", Toast.LENGTH_SHORT).show();
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error starting video recording: " + e.getMessage());
        }
    }

    private void stopRecordingVideo() {
        mediaRecorder.stop();
        mediaRecorder.reset();
        isRecordingVideo = false;
        recordButton.setText("Start Recording");
        Toast.makeText(this, "Video saved: " + videoFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
    }

    private boolean prepareMediaRecorder() {
        mediaRecorder = new MediaRecorder();
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setVideoEncodingBitRate(10000000);
            mediaRecorder.setVideoFrameRate(30);
            mediaRecorder.setVideoSize(1920, 1080);

            // Set output file location
            videoFile = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "video_" + System.currentTimeMillis() + ".mp4");
            mediaRecorder.setOutputFile(videoFile.getAbsolutePath());
            mediaRecorder.setPreviewDisplay(new Surface(textureView.getSurfaceTexture()));
            mediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepareMediaRecorder: " + e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
        stopBackgroundThread();
    }

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is needed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
