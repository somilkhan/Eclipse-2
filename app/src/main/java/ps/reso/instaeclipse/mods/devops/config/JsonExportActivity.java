package ps.reso.instaeclipse.mods.devops.config;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import ps.reso.instaeclipse.R;

public class JsonExportActivity extends Activity {

    private static final int SAVE_JSON_FILE = 5678;
    private String jsonContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        jsonContent = getIntent().getStringExtra("json_content");
        if (jsonContent == null || jsonContent.isEmpty()) {
            ps.reso.instaeclipse.utils.toast.IeToast.makeText(this, getString(R.string.export_no_config_data), Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // Recreated (rotation, or process death while the system file picker was open): the
        // picker launched by the first instance is still pending and will deliver its result
        // here. Launching a second one stacked pickers and could save the file twice.
        if (savedInstanceState != null) return;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        String fileName = getIntent().getStringExtra("file_name");
        intent.putExtra(Intent.EXTRA_TITLE, (fileName != null && !fileName.isEmpty()) ? fileName : "mc_overrides_exported.json");
        startActivityForResult(intent, SAVE_JSON_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SAVE_JSON_FILE) {
            if (resultCode == RESULT_OK && data != null) {
                Uri uri = data.getData();
                if (uri == null) {
                    ps.reso.instaeclipse.utils.toast.IeToast.makeText(this, getString(R.string.export_invalid_uri), Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                // "wt": truncate when overwriting an existing, longer file (plain "w" may leave
                // its tail behind on some providers, producing invalid JSON).
                try (OutputStream out = openTruncating(uri)) {
                    // assert is compiled out on Android; fail with a real message instead of an NPE.
                    if (out == null) throw new java.io.IOException("cannot open output stream");
                    out.write(jsonContent.getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    ps.reso.instaeclipse.utils.toast.IeToast.makeText(this, getString(R.string.export_config_success), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    ps.reso.instaeclipse.utils.toast.IeToast.makeText(this, getString(R.string.export_config_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                }
            }
            finish();
        }
    }

    /** "wt" where the provider supports it, otherwise the provider's default write mode. */
    private OutputStream openTruncating(Uri uri) throws java.io.IOException {
        try {
            return getContentResolver().openOutputStream(uri, "wt");
        } catch (IllegalArgumentException | UnsupportedOperationException | java.io.FileNotFoundException e) {
            return getContentResolver().openOutputStream(uri);
        }
    }
}
