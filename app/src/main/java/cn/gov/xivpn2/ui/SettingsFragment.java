package cn.gov.xivpn2.ui;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

import cn.gov.xivpn2.BuildConfig;
import cn.gov.xivpn2.R;

public class SettingsFragment extends PreferenceFragmentCompat {


    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        findPreference("feedback").setOnPreferenceClickListener(preference -> {
            openUrl("https://github.com/Exclude0122/xivpn/issues/new");
            return true;
        });

        findPreference("privacy_policy").setOnPreferenceClickListener(preference -> {
            openUrl("https://exclude0122.github.io/docs/privacy-policy.html");
            return true;
        });

        findPreference("source_code").setOnPreferenceClickListener(preference -> {
            openUrl("https://github.com/Exclude0122/xivpn");
            return true;
        });

        findPreference("open_source_licenses").setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireContext(), LicensesActivity.class));
            return true;
        });

        findPreference("black_background").setOnPreferenceChangeListener((preference, newValue) -> {
            Toast.makeText(getContext(), R.string.restart_to_apply, Toast.LENGTH_SHORT).show();
            return true;
        });

        findPreference("app_version").setSummary(BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");

        findPreference("geoip_geosite").setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getContext(), GeoAssetsActivity.class));
            return true;
        });

        findPreference("split_tunnel_apps").setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getContext(), SplitTunnelActivity.class));
            return true;
        });

        findPreference("backup_or_restore").setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(getContext(), BackupActivity.class));
            return true;
        });

        findPreference("donation").setOnPreferenceClickListener(preference -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.donation)
                    .setItems(R.array.donation, (dialog, which) -> {
                        String walletAddress = "";
                        switch (which) {
                            case 0: walletAddress = "TTpzvVJ7cv2RZVihd48GGZXg1896WFgQuJ"; break;
                            case 1: walletAddress = "0x593065aDE108505356abaD9c58bE950115678593"; break;
                            case 2: walletAddress = "CyRPKfkGnrAtVKijorcFYocL5fY37tX9j1atm2m8cY8m"; break;
                            case 3: walletAddress = "84iR4Tz29wFKxDpceeFhZQc3msh7N59PdNqxhEY9HjtZKs7wHqGLhw5AJ5p5zkxHMpU7DKHmhjjHmV7jaoVteoWsQs81tf3"; break;
                        }

                        String finalWalletAddress = walletAddress;
                        new AlertDialog.Builder(requireContext())
                                .setTitle(R.string.wallet_address)
                                .setMessage(walletAddress)
                                .setPositiveButton(R.string.copy, (dialog1, which1) -> {
                                    ClipboardManager clipboardManager = requireContext().getSystemService(ClipboardManager.class);
                                    clipboardManager.setPrimaryClip(ClipData.newPlainText("Wallet Address", finalWalletAddress));
                                })
                                .show();
                    })
                    .show();
            return true;
        });
    }

    private void openUrl(String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (ActivityNotFoundException e) {
            Log.e("SettingsFragment", "open browser", e);
        }
    }

    public int dp2px(float dp) {
        return (int) (dp * ((float) requireContext().getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
