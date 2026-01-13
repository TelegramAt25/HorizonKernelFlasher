package xzr.hkf;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.activity.OnBackPressedCallback;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {
    static final boolean DEBUG = false;

    TextView logView;
    ScrollView scrollView;
    FloatingActionButton fab;
    LinearLayout lin;

    enum status {
        flashing,
        flashing_done,
        error,
        normal
    }

    static status cur_status;

    int dpToPx(int px) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (px * scale + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.enableEdgeToEdge(getWindow());

        lin = new LinearLayout(this);
        scrollView = new ScrollView(this);
        logView = new TextView(this);
        fab = new FloatingActionButton(this);

        ViewCompat.setOnApplyWindowInsetsListener(lin, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            ViewGroupCompat.installCompatInsetsDispatch(v);
            return WindowInsetsCompat.CONSUMED;
        });

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (cur_status != status.flashing) {
                    finish();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
        LinearLayout.LayoutParams ps = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        ps.weight = 1;
        scrollView.setLayoutParams(ps);

        logView.setTextIsSelectable(true);
        logView.setTypeface(Typeface.MONOSPACE);
        scrollView.addView(logView);

        fab.setOnClickListener(v -> flash_new());
        fab.setOnLongClickListener(v -> {
            aboutDialog();
            return false;
        });
        LinearLayout.LayoutParams pf = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        int mar = dpToPx(36);
        pf.gravity = Gravity.BOTTOM | Gravity.END;
        pf.setMargins(mar, mar, mar, mar);
        fab.setLayoutParams(pf);

        lin.setOrientation(LinearLayout.VERTICAL);
        lin.setFocusable(false);

        lin.addView(scrollView);
        lin.addView(fab);
        setContentView(lin);

        flash_new();
    }

    void update_title() {
        runOnUiThread(() -> {
            switch (cur_status) {
                case error:
                    fab.setImageDrawable(ResourcesCompat.getDrawable(
                        getResources(), R.drawable.fab_failure, null));
                    break;
                case flashing:
                    fab.setImageDrawable(ResourcesCompat.getDrawable(
                        getResources(), R.drawable.fab_ongoing, null));
                    break;
                case flashing_done:
                    fab.setImageDrawable(ResourcesCompat.getDrawable(
                        getResources(), R.drawable.fab_success, null));
                    break;
                default:
                    fab.setImageDrawable(ResourcesCompat.getDrawable(
                        getResources(), R.drawable.fab_default, null));
            }
        });
    }

    void flash_new() {
        if (cur_status == MainActivity.status.flashing) {
            Toast.makeText(this, R.string.task_running, Toast.LENGTH_SHORT).show();
            return;
        }

        logView.setText("");
        cur_status = status.normal;
        update_title();
        Toast.makeText(this, R.string.please_select_kzip, Toast.LENGTH_LONG).show();
        runWithFilePath(this, new Worker(this));
    }

    void aboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about)
                .setMessage(R.string.about_msg)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton("Github", (dialog1, which1) -> MainActivity.this.startActivity(new Intent() {{
                    setAction(Intent.ACTION_VIEW);
                    setData(Uri.parse("https://github.com/TelegramAt25/HorizonKernelFlasher"));
                }})).create().show();
    }

    public static void _appendLog(String log, Activity activity) {
        activity.runOnUiThread(() -> {
            ((MainActivity) activity).logView.append(log + "\n");
            ((MainActivity) activity).scrollView.fullScroll(ScrollView.FOCUS_DOWN);
        });
    }

    public static void appendLog(String log, Activity activity) {
        if (DEBUG) {
            _appendLog(log, activity);
            return;
        }
        if (!log.startsWith("ui_print"))
            return;
        log = log.replace("ui_print", "");
        _appendLog(log, activity);
    }

    static class fileWorker extends Thread {
        public Uri uri;
    }

    private static fileWorker file_worker;

    public static void runWithFilePath(Activity activity, @SuppressWarnings("ClassEscapesDefinedScope") fileWorker what) {
        MainActivity.file_worker = what;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        activity.startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            file_worker.uri = data.getData();
            if (file_worker != null) {
                file_worker.start();
                file_worker = null;
            }
        }
    }
}
