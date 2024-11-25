package com.example.shodh;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SaveShapesActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "folderPrefs";
    private static final String FOLDER_URI_KEY = "folderUri";
    private String kmlContent;
    private Uri selectedFolderUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_shapes);

        kmlContent = getIntent().getStringExtra("KML_CONTENT");

        Button selectFolderButton = findViewById(R.id.select_folder_button);
        Button saveAsKMLButton = findViewById(R.id.save_as_kml_button);
        Button saveAsKMZButton = findViewById(R.id.save_as_kmz_button);
        Button saveBothButton = findViewById(R.id.save_both_button);

        selectFolderButton.setOnClickListener(v -> openFolderPicker());

        saveAsKMLButton.setOnClickListener(v -> showFileNameDialog("kml"));
        saveAsKMZButton.setOnClickListener(v -> showFileNameDialog("kmz"));
        saveBothButton.setOnClickListener(v -> {
            showFileNameDialog("kml");
            showFileNameDialog("kmz");
        });

        // Retrieve saved folder URI (if any)
        loadSavedFolderUri();
    }

    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        folderPickerLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> folderPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri folderUri = result.getData().getData();
                    if (folderUri != null) {
                        // Grant permission to access the selected folder
                        getContentResolver().takePersistableUriPermission(
                                folderUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        );

                        selectedFolderUri = folderUri; // Store the selected URI
                        saveFolderUri(folderUri); // Save the folder URI in SharedPreferences
                        Toast.makeText(this, "Folder selected", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void grantUriPermission(Uri uri) {
        getContentResolver().takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        );
    }

    private void showFileNameDialog(String fileType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter File Name");

        final EditText input = new EditText(this);
        input.setHint("File name (without extension)");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String fileName = input.getText().toString().trim();
            if (!fileName.isEmpty()) {
                if (fileType.equals("kml")) {
                    saveKML(fileName + ".kml");
                } else if (fileType.equals("kmz")) {
                    saveKMZ(fileName + ".kmz");
                }
            } else {
                Toast.makeText(this, "File name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void saveKML(String fileName) {
        if (selectedFolderUri != null) {
            saveFile(fileName, kmlContent.getBytes(), "application/vnd.google-earth.kml+xml");
        } else {
            Toast.makeText(this, "Please select a folder first", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveKMZ(String fileName) {
        if (selectedFolderUri != null) {
            try (OutputStream outputStream = getOutputStream(fileName, "application/vnd.google-earth.kmz")) {
                if (outputStream != null) {
                    try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
                        zos.putNextEntry(new ZipEntry("shapes.kml"));
                        zos.write(kmlContent.getBytes());
                        zos.closeEntry();
                    }
                    Toast.makeText(this, "KMZ saved successfully", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Error saving KMZ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please select a folder first", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveFile(String fileName, byte[] content, String mimeType) {
        try (OutputStream outputStream = getOutputStream(fileName, mimeType)) {
            if (outputStream != null) {
                outputStream.write(content);
                Toast.makeText(this, fileName + " saved successfully", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error saving " + fileName + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private OutputStream getOutputStream(String fileName, String mimeType) throws Exception {
        if (selectedFolderUri != null) {
            Uri fileUri = DocumentsContract.createDocument(
                    getContentResolver(), selectedFolderUri, mimeType, fileName);
            if (fileUri != null) {
                return getContentResolver().openOutputStream(fileUri);
            } else {
                throw new Exception("Failed to create document URI");
            }
        }
        throw new Exception("No folder selected");
    }

    private void saveFolderUri(Uri uri) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(FOLDER_URI_KEY, uri.toString()).apply();
    }

    private void loadSavedFolderUri() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String uriString = prefs.getString(FOLDER_URI_KEY, null);
        if (uriString != null) {
            selectedFolderUri = Uri.parse(uriString);
        }
    }
}
