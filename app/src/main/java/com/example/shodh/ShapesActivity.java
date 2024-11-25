package com.example.shodh;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ShapesActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Circle circle;
    private Polygon polygon;
    private Polyline polyline;

    private List<LatLng> polygonPoints = new ArrayList<>();
    private List<LatLng> polylinePoints = new ArrayList<>();
    private List<Marker> vertexMarkers = new ArrayList<>();

    public enum ShapeType {
        CIRCLE, POLYGON, POLYLINE
    }

    private ShapeType selectedShape = ShapeType.CIRCLE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shapes);

        Intent intent = getIntent();
        String shapeType = intent.getStringExtra("SHAPE_TYPE");

        if (shapeType != null) {
            switch (shapeType) {
                case "CIRCLE":
                    selectedShape = ShapeType.CIRCLE;
                    break;
                case "POLYGON":
                    selectedShape = ShapeType.POLYGON;
                    break;
                case "POLYLINE":
                    selectedShape = ShapeType.POLYLINE;
                    break;
            }
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        Button generateButton = findViewById(R.id.save_shapes_button);
        generateButton.setOnClickListener(v -> {
            generateAndSendKML();
            generatePDF();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        setupMapInteractions();

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {}

            @Override
            public void onMarkerDrag(Marker marker) {
                if (marker.getTag() != null) {
                    int index = (int) marker.getTag();
                    LatLng newPosition = marker.getPosition();

                    if (selectedShape == ShapeType.POLYGON && index >= 0 && index < polygonPoints.size()) {
                        polygonPoints.set(index, newPosition);
                        redrawPolygon();
                    } else if (selectedShape == ShapeType.POLYLINE && index >= 0 && index < polylinePoints.size()) {
                        polylinePoints.set(index, newPosition);
                        redrawPolyline();
                    }
                }
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                if (marker.getTag() != null) {
                    int index = (int) marker.getTag();
                    LatLng newPosition = marker.getPosition();

                    if (selectedShape == ShapeType.POLYGON && index >= 0 && index < polygonPoints.size()) {
                        polygonPoints.set(index, newPosition);
                        redrawPolygon();
                    } else if (selectedShape == ShapeType.POLYLINE && index >= 0 && index < polylinePoints.size()) {
                        polylinePoints.set(index, newPosition);
                        redrawPolyline();
                    }
                    Toast.makeText(ShapesActivity.this, "Vertex moved", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupMapInteractions() {
        mMap.setOnMapLongClickListener(this::handleMapLongClick);
    }

    private void handleMapLongClick(LatLng latLng) {
        switch (selectedShape) {
            case CIRCLE:
                if (circle != null) circle.remove();
                circle = mMap.addCircle(new CircleOptions()
                        .center(latLng)
                        .radius(100)
                        .strokeColor(0x44008000)
                        .fillColor(0x44FF0000));
                break;
            case POLYGON:
                polygonPoints.add(latLng);
                redrawPolygon();
                addDraggableMarkers(polygonPoints);
                break;
            case POLYLINE:
                polylinePoints.add(latLng);
                redrawPolyline();
                addDraggableMarkers(polylinePoints);
                break;
        }
    }

    private void redrawPolygon() {
        if (polygon != null) polygon.remove();
        polygon = mMap.addPolygon(new PolygonOptions()
                .addAll(polygonPoints)
                .strokeColor(0xAA00FF00)
                .fillColor(0x4400FF00));
        updateVertexMarkers(polygonPoints);
    }

    private void redrawPolyline() {
        if (polyline != null) polyline.remove();
        polyline = mMap.addPolyline(new PolylineOptions()
                .addAll(polylinePoints)
                .color(0xFF0000FF)
                .width(5));
        updateVertexMarkers(polylinePoints);
    }

    private void addDraggableMarkers(List<LatLng> points) {
        clearExistingMarkers();
        for (int i = 0; i < points.size(); i++) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(points.get(i))
                    .draggable(true)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            if (marker != null) {
                marker.setTag(i);
                vertexMarkers.add(marker);
            }
        }
    }

    private void updateVertexMarkers(List<LatLng> points) {
        if (points.size() != vertexMarkers.size()) {
            addDraggableMarkers(points);
        } else {
            for (int i = 0; i < vertexMarkers.size(); i++) {
                vertexMarkers.get(i).setPosition(points.get(i));
            }
        }
    }

    private void clearExistingMarkers() {
        for (Marker marker : vertexMarkers) {
            marker.remove();
        }
        vertexMarkers.clear();
    }

    private String generateKML() {
        StringBuilder kmlBuilder = new StringBuilder();
        kmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        kmlBuilder.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
        kmlBuilder.append("<Document>\n");

        if (circle != null) {
            kmlBuilder.append("<Placemark>\n<name>Circle</name>\n<Circle>\n");
            kmlBuilder.append("<center>")
                    .append(circle.getCenter().longitude).append(",")
                    .append(circle.getCenter().latitude).append("</center>\n");
            kmlBuilder.append("<radius>").append(circle.getRadius()).append("</radius>\n");
            kmlBuilder.append("</Circle>\n</Placemark>\n");
        }

        if (!polygonPoints.isEmpty()) {
            kmlBuilder.append("<Placemark>\n<name>Polygon</name>\n<Polygon>\n");
            kmlBuilder.append("<outerBoundaryIs><LinearRing><coordinates>\n");
            for (LatLng point : polygonPoints) {
                kmlBuilder.append(point.longitude).append(",").append(point.latitude).append(",0\n");
            }
            kmlBuilder.append("</coordinates></LinearRing></outerBoundaryIs>\n</Polygon>\n</Placemark>\n");
        }

        if (!polylinePoints.isEmpty()) {
            kmlBuilder.append("<Placemark>\n<name>Polyline</name>\n<LineString><coordinates>\n");
            for (LatLng point : polylinePoints) {
                kmlBuilder.append(point.longitude).append(",").append(point.latitude).append(",0\n");
            }
            kmlBuilder.append("</coordinates></LineString>\n</Placemark>\n");
        }

        kmlBuilder.append("</Document>\n</kml>");
        return kmlBuilder.toString();
    }

    private void generateAndSendKML() {
        String kmlContent = generateKML();
        Intent intent = new Intent(this, SaveShapesActivity.class);
        intent.putExtra("KML_CONTENT", kmlContent);
        startActivity(intent);
    }

    private void generatePDF() {
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 600, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        Paint paint = new Paint();
        int yPosition = 25;

        paint.setColor(Color.BLACK);
        page.getCanvas().drawText("Shape Coordinates", 10, yPosition, paint);

        if (circle != null) {
            yPosition += 20;
            page.getCanvas().drawText("Circle Center: " + circle.getCenter().toString(), 10, yPosition, paint);
        }

        if (!polygonPoints.isEmpty()) {
            yPosition += 20;
            page.getCanvas().drawText("Polygon Points:", 10, yPosition, paint);
            for (LatLng point : polygonPoints) {
                yPosition += 15;
                page.getCanvas().drawText(point.toString(), 10, yPosition, paint);
            }
        }

        if (!polylinePoints.isEmpty()) {
            yPosition += 20;
            page.getCanvas().drawText("Polyline Points:", 10, yPosition, paint);
            for (LatLng point : polylinePoints) {
                yPosition += 15;
                page.getCanvas().drawText(point.toString(), 10, yPosition, paint);
            }
        }

        pdfDocument.finishPage(page);

        File pdfDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Shodh");
        if (!pdfDir.exists()) pdfDir.mkdirs();

        File pdfFile = new File(pdfDir, "shapes_data.pdf");
        try {
            pdfDocument.writeTo(new FileOutputStream(pdfFile));
            Toast.makeText(this, "PDF saved: " + pdfFile.getPath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save PDF", Toast.LENGTH_SHORT).show();
        } finally {
            pdfDocument.close();
        }
    }
}
