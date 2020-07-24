package net.xn__n6x.communication.android;

import android.content.Context;
import android.content.SharedPreferences;
import net.xn__n6x.communication.identity.Id;
import net.xn__n6x.communication.identity.Profile;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/** Manages the stored profile for this user. */
public class DeviceIdentity {
    protected static final String SHARED_PREFERENCES = "DeviceIdentity";
    protected static final String KEY_ID   = "Id";
    protected static final String KEY_NAME = "Name";

    /** The shared preferences object storing the identity data. */
    protected SharedPreferences preferences;

    protected DeviceIdentity(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    /** Creates an interface for the identity data that is stored in this
     * device, if a complete one already exists.
     * @param c The {@link Context} that will be used for retrieval and storage.
     * @return A {@link DeviceIdentity} interface for this device.
     */
    public static Optional<DeviceIdentity> load(Context c) {
        SharedPreferences preferences = c.getSharedPreferences(
            DeviceIdentity.SHARED_PREFERENCES,
            Context.MODE_PRIVATE);
        DeviceIdentity identity = new DeviceIdentity(preferences);

        long missing = 0;
        if(!getIdStatic(preferences).isPresent()) ++missing;
        if(!getNameStatic(preferences).isPresent()) ++missing;

        if(missing > 0)
            /* We're missing keys. */
            return Optional.empty();

        /* We have all keys present. */
        return Optional.of(identity);
    }

    /** Creates a new device identity with the given profile and a randomly
     * generated {@link Id}.
     * @param c The {@link Context} that will be used for retrieval and storage.
     * @param p The {@link Profile} that will be used for creation.
     * @return A {@link DeviceIdentity} interface for this device.
     */
    public static DeviceIdentity createWith(Context c, Profile p) {
        SharedPreferences preferences = c.getSharedPreferences(
            DeviceIdentity.SHARED_PREFERENCES,
            Context.MODE_PRIVATE);
        DeviceIdentity identity = new DeviceIdentity(preferences);

        identity.setId(Id.random());
        identity.setName(p.getName());

        return identity;
    }

    public Id getId() {
        return getIdStatic(preferences)
            .orElseThrow(() -> new RuntimeException("Called getId() on invalid Id"));
    }

    public String getName() {
        return getNameStatic(preferences)
            .orElseThrow(() -> new RuntimeException("Called getName() on missing name"));
    }

    public void setId(Id id) {
        this.preferences.edit()
            .putString(KEY_ID, id.toString())
            .apply();
    }

    public void setName(String name) {
        this.preferences.edit()
            .putString(KEY_NAME, name)
            .apply();
    }

    /** Statically try to find an Id in the given preferences. */
    protected static Optional<Id> getIdStatic(SharedPreferences prefs) {
        String id = prefs.getString(KEY_ID, null);
        try{
            return Optional.ofNullable(id).map(Id::fromString);
        } catch(IllegalArgumentException ex) {
            ex.printStackTrace();
            return Optional.empty();
        }
    }

    /** Statically try to find a name in the given preferences. */
    protected static Optional<String> getNameStatic(SharedPreferences prefs) {
        return Optional.ofNullable(prefs.getString(KEY_NAME, null));
    }
}
