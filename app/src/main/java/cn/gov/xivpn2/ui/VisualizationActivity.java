package cn.gov.xivpn2.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.ArraySet;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import cn.gov.xivpn2.BuildConfig;
import cn.gov.xivpn2.R;
import cn.gov.xivpn2.database.AppDatabase;
import cn.gov.xivpn2.database.DNS;
import cn.gov.xivpn2.database.Proxy;
import cn.gov.xivpn2.database.Rules;
import cn.gov.xivpn2.xrayconfig.DNSServer;
import cn.gov.xivpn2.xrayconfig.LabelSubscription;
import cn.gov.xivpn2.xrayconfig.Outbound;
import cn.gov.xivpn2.xrayconfig.ProxyChainSettings;
import cn.gov.xivpn2.xrayconfig.RoutingRule;
import cn.gov.xivpn2.xrayconfig.XrayDNS;

public class VisualizationActivity extends AppCompatActivity {

    private static final String TAG = "VisualizationActivity";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_visualization);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.routing_visualization);
        }

        // webview
        WebView webView = findViewById(R.id.webview);
        WebSettings settings = webView.getSettings();
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptEnabled(true);
        settings.setAllowContentAccess(false);
        settings.setBlockNetworkLoads(true);
        settings.setBlockNetworkImage(true);

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "onPageFinished");

                try {
                    GraphBuilder graphBuilder = new GraphBuilder(VisualizationActivity.this);
                    String build = graphBuilder.build();
                    Log.d(TAG, build);
                    view.evaluateJavascript("window.render(" + new Gson().toJson(build) + ")", null);
                } catch (IOException e) {
                    Log.e(TAG, "build graph", e);
                }


            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d(TAG, consoleMessage.message());
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                new AlertDialog.Builder(VisualizationActivity.this).setTitle(R.string.error).setMessage(message).setPositiveButton(R.string.ok, null).show();
                return true;
            }
        });

        webView.loadUrl("file:///android_asset/mermaid.html");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "on destroy");
        WebView webView = findViewById(R.id.webview);
        webView.destroy();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}


class GraphBuilder {

    private final StringBuffer sb;

    private final String ID_START = "start";
    private final String ID_INTERNET = "internet";
    private final String ID_BLACKHOLE = "blackhole";

    private final Context context;

    public GraphBuilder(Context context) {
        sb = new StringBuffer();
        this.context = context;
    }

    public String build() throws IOException {

        sb.append("flowchart TD\n");
        sb.append(ID_START).append("[Start]\n");

        SharedPreferences sp = context.getSharedPreferences("XIVPN", Context.MODE_PRIVATE);
        String selectedLabel = sp.getString("SELECTED_LABEL", "No Proxy (Bypass Mode)");
        String selectedSubscription = sp.getString("SELECTED_SUBSCRIPTION", "none");

        // rules
        List<RoutingRule> rules = Rules.readRules(context.getFilesDir());

        String lastRuleId = ID_START;
        for (int i = 0; i < rules.size(); i++) {
            RoutingRule rule = rules.get(i);
            String ruleId = "rule" + i;

            // add rule box
            sb.append(ruleId).append("{{").append(escape(rule.label)).append("}}").append("\n");

            Proxy outbound = AppDatabase.getInstance().proxyDao().find(rule.outboundLabel, rule.outboundSubscription);

            if (!outbound.protocol.equals("blackhole")) {
                // add outbound
                appendProxy(outbound, "");
                if (!outbound.protocol.equals("dns")) {
                    // outbound -> internet
                    sb.append(getIdForProxy(outbound)).append(" --> ").append(ID_INTERNET).append("\n");
                }
            }

            // current rule matches -> outbound
            if (!outbound.protocol.equals("blackhole")) {
                sb.append(ruleId).append(" -->|Yes| ").append(getIdForProxy(outbound)).append("\n");
            } else {
                sb.append(ruleId).append(" -->|Yes| ").append(ID_BLACKHOLE).append("\n");
            }

            // previous rule does not match -> current rule
            if (lastRuleId.equals(ID_START)) {
                sb.append(lastRuleId).append(" --> ").append(ruleId).append("\n");
            } else {
                sb.append(lastRuleId).append(" -->|No| ").append(ruleId).append("\n");
            }

            lastRuleId = ruleId;
        }

        // catch all
        Proxy catchAll = AppDatabase.getInstance().proxyDao().find(selectedLabel, selectedSubscription);
        appendProxy(catchAll, "");
        // catch all -> internet
        sb.append(getIdForProxy(catchAll)).append(" --> ").append(ID_INTERNET).append("\n");

        // last rule / start -> catch all
        if (!rules.isEmpty()) {
            sb.append(lastRuleId).append(" -->|No| ").append(getIdForProxy(catchAll)).append("\n");
        } else {
            sb.append(ID_START).append(" --> ").append(getIdForProxy(catchAll)).append("\n");
        }

        sb.append(ID_INTERNET).append("[Internet]\n");
        sb.append(ID_BLACKHOLE).append("[Blackhole]\n");

        return sb.toString();
    }

    private String escape(String s) {
        // TODO: escape
        return "\"" + s + "\"";
    }

    private String appendProxy(Proxy proxy, String prefix) throws IOException {
        String id = prefix + getIdForProxy(proxy);

        if ("proxy-chain".equals(proxy.protocol)) {
            sb.append("subgraph ").append(id).append("[").append(escape(proxy.label)).append("]").append("\n");
            sb.append("direction TD\n");

            Outbound<ProxyChainSettings> proxyChainSettings = new Gson().fromJson(proxy.config, new TypeToken<Outbound<ProxyChainSettings>>() { }.getType());

            List<LabelSubscription> proxyChains = proxyChainSettings.settings.proxies;

            String lastId = "";
            for (int i = 0; i < proxyChains.size(); i++) {
                LabelSubscription labelSubscription = proxyChains.get(i);
                Proxy p = AppDatabase.getInstance().proxyDao().find(labelSubscription.label, labelSubscription.subscription);
                String newId = appendProxy(p, prefix + getIdForProxy(proxy) + i);
                if (!lastId.isEmpty()) {
                    sb.append(lastId).append(" --> ").append(newId).append("\n");
                }
                lastId = newId;
            }

            sb.append("end\n");

        } else if ("dns".equals(proxy.protocol)) {

            sb.append("subgraph ").append(id).append("[Built-in DNS Server]").append("\n");
            sb.append("direction LR\n");

            XrayDNS dnsSettings = DNS.readDNSSettings(context.getFilesDir());

            int i = 0;
            for (String k : dnsSettings.hosts.keySet()) {
                String v = dnsSettings.hosts.get(k);
                sb.append(id).append("hosts_key").append(i).append("[").append(escape(k)).append("]").append("\n");
                sb.append(id).append("hosts_value").append(i).append("[").append(escape(v)).append("]").append("\n");
                sb.append(id).append("hosts_key").append(i).append(" --> ").append(id).append("hosts_value").append(i).append("\n");
                i++;
            }

            HashSet<String> domains = new HashSet<>();
            for (DNSServer server : dnsSettings.servers) {
                domains.addAll(server.domains);
            }

            i = 0;
            for (String domain : domains) {
                sb.append(id).append("domain_key").append(i).append("[").append(escape(k)).append("]").append("\n");
                sb.append(id).append("hosts_value").append(i).append("[").append(escape(v)).append("]").append("\n");
                sb.append(id).append("hosts_key").append(i).append(" --> ").append(id).append("hosts_value").append(i).append("\n");
            }

            sb.append("end\n");

        } else {
            sb.append(id).append("[").append(escape(proxy.label)).append("]\n");
        }

        return id;
    }

    @SuppressLint("DefaultLocale")
    private String getIdForProxy(Proxy proxy) {
        return String.format("id%d", proxy.id);
    }
}