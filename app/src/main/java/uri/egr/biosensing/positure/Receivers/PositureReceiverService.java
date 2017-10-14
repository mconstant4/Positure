package uri.egr.biosensing.positure.Receivers;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;

/**
 * Created by mcons on 3/11/2017.
 */

abstract public class PositureReceiverService extends BroadcastReceiver {
    public static final IntentFilter POSITURE_INTENT_FILTER = new IntentFilter("uri.egr.vapegate.update");
    public static final String EXTRA_CONNECTION_UPDATE = "uri.egr.vapegate.connection_update";
    public static final String EXTRA_READ_UPDATE = "uri.egr.vapegate.read_update";
}