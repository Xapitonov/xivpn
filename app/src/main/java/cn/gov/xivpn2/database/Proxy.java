package cn.gov.xivpn2.database;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.google.gson.annotations.Expose;

import java.util.Objects;

@Entity(
        indices = {
                @Index(value = {"label", "subscription"}, unique = true),
        }
)
public class Proxy {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @Expose
    public String subscription;
    @Expose
    public String protocol;
    @Expose
    public String label;
    @Expose
    public String config;

    @Ignore
    public int ping = 0;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Proxy proxy = (Proxy) o;
        return id == proxy.id && Objects.equals(subscription, proxy.subscription) && Objects.equals(protocol, proxy.protocol) && Objects.equals(label, proxy.label) && Objects.equals(config, proxy.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, subscription, protocol, label, config);
    }
}
