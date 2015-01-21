package net.pierrox.lightning_launcher.template;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.trianguloy.llscript.repository.R;

import java.util.List;

/**
 * Public Domain.
 * You are free to use, modify and redistribute this software in any way you wnat.
 */
public class ApplyTemplates extends Activity implements View.OnClickListener {
    private static final Uri LL_URI = Uri.parse("market://details?id=net.pierrox.lightning_launcher");
    private static final Uri LLX_URI = Uri.parse("market://details?id=net.pierrox.lightning_launcher_extreme");

    private static final int DIALOG_LL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.apply_templates);

        LinearLayout container = (LinearLayout) findViewById(R.id.button_container);

        Intent filter = new Intent(LLTemplateAPI.INTENT_QUERY_TEMPLATE);
        filter.setPackage(getPackageName());
        PackageManager pm = getPackageManager();
        List<ResolveInfo> ris = pm.queryIntentActivities(filter, 0);
        for(ResolveInfo ri : ris) {
            getLayoutInflater().inflate(R.layout.template_button, container);
            Button btn = (Button)container.getChildAt(container.getChildCount()-1);
            Drawable d = ri.activityInfo.loadIcon(pm);
            btn.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
            btn.setText(ri.loadLabel(pm));
            btn.setTag(ri.activityInfo.name);
            btn.setOnClickListener(this);
            //container.addView(btn);
        }
        int count = ris.size();
        container.setWeightSum(count);

        if(count == 1) {
            applyTemplate(ris.get(0).activityInfo.name);
        }
    }

    @Override
    public void onClick(View view) {
        applyTemplate(view.getTag().toString());
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case DIALOG_LL:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.dialog_ll_not_here_ttl);
                builder.setMessage(R.string.dialog_ll_not_here_msg);
                builder.setPositiveButton(R.string.dialog_ll_not_here_llx, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(Intent.ACTION_VIEW, LLX_URI));
                    }
                });
                builder.setNeutralButton(R.string.dialog_ll_not_here_ll, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(Intent.ACTION_VIEW, LL_URI));
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                return builder.create();
        }
        return super.onCreateDialog(id);
    }

    private void applyTemplate(String class_name) {
        Intent intent = new Intent(LLTemplateAPI.INTENT_APPLY_TEMPLATE);
        intent.putExtra(LLTemplateAPI.INTENT_TEMPLATE_COMPONENT_NAME, new ComponentName(getPackageName(), class_name));
        try {
            startActivity(intent);
            finish();
        } catch(ActivityNotFoundException e) {
            showDialog(DIALOG_LL);
        }
    }
}
