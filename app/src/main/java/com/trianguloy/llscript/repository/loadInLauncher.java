package com.trianguloy.llscript.repository;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class loadInLauncher extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent data = new Intent();
        data.putExtra(Constants.INTENT_EXTRA_SCRIPT_ID, R.raw.manager);
        data.putExtra(Constants.INTENT_EXTRA_SCRIPT_NAME, getString(R.string.script_name));
        data.putExtra(Constants.INTENT_EXTRA_SCRIPT_FLAGS, Constants.FLAG_CUSTOM_MENU);
        data.putExtra(Constants.INTENT_EXTRA_EXECUTE_ON_LOAD, true);
        data.putExtra(Constants.INTENT_EXTRA_DELETE_AFTER_EXECUTION, false);
        setResult(RESULT_OK, data);
        finish();
    }
}
