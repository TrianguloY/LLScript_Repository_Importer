package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
/**
 * Basic activity used by Lightning launcher to load the script
 *
 * Class kept to avoid crashes on old versions of Lightning launcher. To remove in future releases
 */
@Deprecated
public class loadInLauncher extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent data = new Intent();
        data.putExtra(Constants.INTENT_EXTRA_SCRIPT_ID, R.raw.executor);
        data.putExtra(Constants.INTENT_EXTRA_SCRIPT_NAME, getString(R.string.script_name));
        data.putExtra(Constants.INTENT_EXTRA_SCRIPT_FLAGS, 0);
        data.putExtra(Constants.INTENT_EXTRA_EXECUTE_ON_LOAD, false);
        data.putExtra(Constants.INTENT_EXTRA_DELETE_AFTER_EXECUTION, true);
        setResult(RESULT_OK, data);
        finish();
    }
}