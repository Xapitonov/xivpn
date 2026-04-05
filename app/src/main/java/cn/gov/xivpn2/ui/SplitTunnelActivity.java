package cn.gov.xivpn2.ui;

import java.util.HashSet;
import java.util.Set;

import cn.gov.xivpn2.R;

public class SplitTunnelActivity extends BaseAppListActivity {

    @Override
    protected Set<String> loadSelectedPackages() {
        return getSharedPreferences("XIVPN", MODE_PRIVATE).getStringSet("APP_LIST", new HashSet<>());
    }

    @Override
    protected void saveSelectedPackages(Set<String> packages) {
        getSharedPreferences("XIVPN", MODE_PRIVATE)
                .edit()
                .putStringSet("APP_LIST", packages)
                .apply();
    }

    @Override
    protected int getTitleResId() {
        return R.string.split_tunnel;
    }
}
