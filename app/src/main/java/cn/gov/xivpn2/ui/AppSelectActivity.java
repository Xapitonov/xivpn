package cn.gov.xivpn2.ui;

import android.content.Intent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import cn.gov.xivpn2.R;

public class AppSelectActivity extends BaseAppListActivity {

    @Override
    protected Set<String> loadSelectedPackages() {
        ArrayList<String> list = getIntent().getStringArrayListExtra("SELECTED_APPS");
        if (list == null) {
            return new HashSet<>();
        }
        return new HashSet<>(list);
    }

    @Override
    protected void saveSelectedPackages(Set<String> packages) {
        Intent data = new Intent();
        data.putStringArrayListExtra("SELECTED_APPS", new ArrayList<>(packages));
        setResult(RESULT_OK, data);
    }

    @Override
    protected int getTitleResId() {
        return R.string.applications;
    }
}
