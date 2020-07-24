package net.xn__n6x.communication.watchdog;

import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.*;
import android.os.*;
import android.util.Log;
import android.widget.Toast;
import net.xn__n6x.communication.Assertions;
import net.xn__n6x.communication.R;
import net.xn__n6x.communication.android.DeviceIdentity;
import net.xn__n6x.communication.control.Packet;
import net.xn__n6x.communication.control.Router;
import net.xn__n6x.communication.identity.Id;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** The Watchdog. This service is the beating heart of this application.
 * It implements the transmission protocol over Android's Wifi P2P interfaces.
 *
 * At its core, the Watchdog is a finite state machine driven by connection
 * events in Wifi P2P. First, it discovers and profiles every device in its
 * surroundings. Then, once that is done, it transmits all the messages it
 * had queued for every device to them. */
public class Watchdog extends Service {
    /** Port for the Watchdog TCP server. */
    public static final short TCP_PORT = 20666;

    /** The manager for our Wifi P2P state. */
    protected WifiP2pManager wifiManager;
    /** The Wifi P2P channel we are operating in. */
    protected WifiP2pManager.Channel wifiChannel;
    /** The manager for our Bluetooth state. */
    protected BluetoothManager bluetoothManager;
    /** Our identity. */
    protected DeviceIdentity identity;
    /** The router managing our known peers. */
    protected Router router;
    /** Id lookup table for MAC addresses. */
    protected HashMap<String, Id> macToId;
    /** MAC address lookup table for Ids. */
    protected HashMap<Id, String> idToMac;
    /** Connection targets for discovery. */
    protected ArrayDeque<String> discoveryQueue;
    /** The MAC address of our host device. */
    protected String macAddress;
    /** Current state of the watchdog. */
    protected State watchdogState;
    /** Server for performing data transmission between peers. */
    protected ServerSocket watchdogServer;
    /** Inbound packets, in the order they were received. */
    protected HashMap<Id, ArrayDeque<Packet>> inboundQueue;
    /** Listeners for new inbound messages. */
    protected HashMap<Id, ArrayList<OnMessage>> inboundListeners;
    /** Listeners for onFinishedDiscovery events. */
    protected ArrayList<OnFinishedDiscovery> finishedDiscoveryListeners;

    /** Because Wifi P2P is used for both discovery and data transmission
     * operations, and because we can't interleave these operations, we
     * need a way to keep track of which one we are currently doing, and
     * which one we can or need to transition to next. */
    protected enum State {
        /** We're discovering all the devices we have near us.
         * <br><br>
         * This state has the following transitions:
         * <ul>
         *     <li>
         *         Upon having finished discovering all unknown devices and,
         *         if there are any pending packets to be sent, switches to
         *         {@link State#TRANSMISSION}.
         *     </li>
         *     <li>
         *         Upon having finished discovering all unknown devices and,
         *         if there aren't any pending packets to be sent, switches to
         *         {@link State#DOCKED}.
         *     </li>
         * </ul>
         */
        DISCOVERY,
        /** We're transmitting data between ourselves and the devices we found
         * in the discovery phase.
         * <br><br>
         * This state has the following transitions:
         * <ul>
         *     <li>
         *         Upon having finished sending all the remaining packets and,
         *         if there are any new devices that have been discovered,
         *         switches to {@link State#DISCOVERY}.
         *     </li>
         *     <li>
         *         Upon having finished sending all the remaining packets and,
         *         if there aren't any new devices that have been discovered,
         *         switches to {@link State#DOCKED}.
         *     </li>
         * </ul>
         */
        TRANSMISSION,
        /** We have finished either discovery or transmission with no target
         * to connect to next. Since the state machine only progresses between
         * connections, it will stay in this state until an external event
         * restarts the discovery-transmission loop. This state exists in order
         * to signal these external events that they need to restart the cycle
         * manually, instead of just leaving their data to be processed by the
         * discovery-transmission loop. */
        DOCKED
    }

    public Watchdog() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        /* Make sure we have access to Wifi P2P. */
        this.wifiManager = (WifiP2pManager) this.getSystemService(Context.WIFI_P2P_SERVICE);
        if(this.wifiManager == null) {
            Toast.makeText(this, R.string.wifip2p_unavailable, Toast.LENGTH_LONG).show();
            this.stopSelf();

            return;
        }
        this.wifiManager.initialize(this, getMainLooper(), null);

        /* Make sure we have access to Bluetooth. */
        this.bluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        if(this.bluetoothManager == null) {
            Toast.makeText(this, R.string.bluetoothle_unavailable, Toast.LENGTH_LONG).show();
            this.stopSelf();

            return;
        }

        /* Load our identity. */
        this.identity = DeviceIdentity.load(this)
            .orElseThrow(() -> new RuntimeException("Expected a valid identity."));

        /* Initialize ourselves. */
        this.router = new Router(this.identity.getId());
        this.macToId = new HashMap<>();
        this.idToMac = new HashMap<>();
        this.discoveryQueue = new ArrayDeque<>();
        this.inboundQueue = new HashMap<>();
        this.inboundListeners = new HashMap<>();
        this.finishedDiscoveryListeners = new ArrayList<>();
        try {
            this.watchdogServer = new ServerSocket(Watchdog.TCP_PORT);
        } catch (IOException e) {
            e.printStackTrace();

            this.stopSelf();
            return;
        }

        /* Start ourselves docked. */
        this.watchdogState = State.DOCKED;

        /* Register the broadcast handler. */
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        this.registerReceiver(new BroadcastHandler(), filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    protected void onPeersChanged(WifiP2pDeviceList list) {
        Collection<WifiP2pDevice> devices = list.getDeviceList();
        devices.stream()
            .filter(t -> !this.macToId.containsKey(t.deviceAddress))
            .forEach(t -> this.discoveryQueue.addLast(t.deviceAddress));

        /* Trim all the devices we can't communicate to from the router. */
        ArrayList<Id> reachable = devices.stream()
            .filter(t -> this.macToId.containsKey(t.deviceAddress))
            .map(t -> t.deviceAddress)
            .map(this.macToId::get)
            .collect(Collectors.toCollection(ArrayList::new));
        this.router.retain(reachable);

        if(this.watchdogState == State.DOCKED && this.discoveryQueue.size() > 0) {
            /* If we're docked, connect to the first element. */
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = this.discoveryQueue.removeFirst();

            this.watchdogState = State.DISCOVERY;
            this.wifiManager.connect(
                this.wifiChannel,
                config,
                null);
        }
    }

    protected void onPacketReceived(Packet p) {
        Id current = this.identity.getId();
        if(!p.getTarget().equals(current))
            /* Forward packets we don't know on. */
            this.router.forward(p, Router.DEFAULT_TIME_TO_LIVE);
        else {
            /* And keep the ones that we should receive. */
            ArrayDeque<Packet> packets = this.inboundQueue.get(p.getSource());
            if(packets == null) {
                packets = new ArrayDeque<>(1);
                this.inboundQueue.put(p.getSource(), packets);

                /* Notify the listeners. */
                Optional.ofNullable(this.inboundListeners.get(p.getSource()))
                    .ifPresent(list -> list.forEach(val -> val.onMessage(p.getSource())));
            }

            packets.addLast(p);
        }
    }

    protected void onTransmissionConnectionToPeer(Socket peer) throws WatchdogException, IOException {
        WatchdogProtocol proto = new WatchdogProtocol(peer);
        proto.sendMagic();
        proto.sendId(this.identity.getId());
        proto.sendString(this.macAddress);

        Supplier<WatchdogException> missing = () -> new WatchdogException("Missing required element");

        proto.getValidMagic().orElseThrow(missing);
        Id other = proto.getId().orElseThrow(missing);
        proto.getString().orElseThrow(missing);

        /* Send all of the packets we have queued for this peer. */
        ArrayList<Packet> packets = new ArrayList<>();
        for(
            Optional<Packet> p = this.router.getNextMessageForPeer(other);
            p.isPresent();
            p = this.router.getNextMessageForPeer(other)) {

            packets.add(p.get());
        }

        proto.sendInt(packets.size());
        for(Packet p : packets)
            proto.sendPacket(p.tag(other));

        /* Receive all the inbound packets from this peer. */
        int inbound = proto.getInt().orElseThrow(missing);
        for(int i = 0; i < inbound; ++i)
            this.onPacketReceived(proto.getValidPacket().orElseThrow(missing));
    }

    protected void onDiscoveryConnectionToPeer(Socket peer) throws WatchdogException, IOException {
        WatchdogProtocol proto = new WatchdogProtocol(peer);
        proto.sendMagic();
        proto.sendId(this.identity.getId());
        proto.sendString(this.macAddress);

        Supplier<WatchdogException> missing = () -> new WatchdogException("Missing required element");

        proto.getValidMagic().orElseThrow(missing);
        Id other   = proto.getId().orElseThrow(missing);
        String mac = proto.getString().orElseThrow(missing);

        /* Haha pseudo-bidimap go brrrrrr. */
        this.idToMac.put(other, mac);
        this.macToId.put(mac, other);

        this.router.register(other);
    }

    protected void onConnectionChanged(WifiP2pInfo info) {
        if(info.groupOwnerAddress == null) {
            Log.d("Watchdog", "Called onConnectionChanged() with null owner address");
            return;
        }

        try {
            Socket peer;
            if (info.isGroupOwner) {
                /* Expect a connection to our server. */
                peer = this.watchdogServer.accept();
            } else {
                /* Connect to the group owner. */
                peer = new Socket();
                peer.connect(new InetSocketAddress(info.groupOwnerAddress, TCP_PORT));
            }

            switch(this.watchdogState) {
                case DISCOVERY:
                    this.onDiscoveryConnectionToPeer(peer);

                    /* Advance the state machine. */
                    String next = this.discoveryQueue.removeFirst();
                    if(next == null) {
                        /* We've finished discovering things. Fire all of the
                         * discovery finished listeners and then, if there are
                         * any targets for transmission, switch to transmission,
                         * otherwise dock ourselves. */
                        for(OnFinishedDiscovery listener : this.finishedDiscoveryListeners)
                            listener.onFinishedDiscovery(this.router.getReachablePeers());

                        if (this.router.getTargetedReachablePeers().size() > 0) {
                            Id nextId = this.router.getTargetedReachablePeers().iterator().next();
                            next = this.idToMac.get(nextId);
                            if(next == null)
                                Assertions.fail("We are targeting a peer we don't know: %s", nextId);

                            WifiP2pConfig config = new WifiP2pConfig();
                            config.deviceAddress = next;

                            this.watchdogState = State.TRANSMISSION;
                            this.dropCurrentConnectionThenConnectTo(config);
                        } else
                            /* No more targets to change state to. */
                            this.watchdogState = State.DOCKED;
                    } else {
                        /* Continue with discovery. */
                        WifiP2pConfig config = new WifiP2pConfig();
                        config.deviceAddress = next;

                        this.watchdogState = State.DISCOVERY;
                        this.dropCurrentConnectionThenConnectTo(config);
                    }

                    break;
                case TRANSMISSION:
                    this.onTransmissionConnectionToPeer(peer);

                    /* Advance the state machine. */
                    if(!this.router.getTargetedReachablePeers().iterator().hasNext()) {
                        /* Try to change state to discovery. */
                        next = this.discoveryQueue.removeFirst();
                        if (next != null) {
                            WifiP2pConfig config = new WifiP2pConfig();
                            config.deviceAddress = next;

                            this.watchdogState = State.DISCOVERY;
                            this.dropCurrentConnectionThenConnectTo(config);
                        } else
                            /* No more targets to change state to. */
                            this.watchdogState = State.DOCKED;
                    } else {
                        /* Continue with transmission. */
                        Id nextId = this.router.getTargetedReachablePeers().iterator().next();
                        next = this.idToMac.get(nextId);
                        if(next == null)
                            Assertions.fail("We are targeting a peer we don't know: %s", nextId);

                        WifiP2pConfig config = new WifiP2pConfig();
                        config.deviceAddress = next;

                        this.watchdogState = State.TRANSMISSION;
                        this.dropCurrentConnectionThenConnectTo(config);
                    }
            }
        } catch(IOException | WatchdogException e) {
            e.printStackTrace();
            this.stopSelf();
        }
    }

    protected void dropCurrentConnectionThenConnectTo(WifiP2pConfig connection) {
        this.wifiManager.removeGroup(this.wifiChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Watchdog.this.wifiManager.connect(
                    Watchdog.this.wifiChannel,
                    connection,
                    null);
            }

            @Override
            public void onFailure(int reason) {
                Log.w("Watchdog", "Could not disconnect due to: " + reason);
                Log.w("Watchdog", "Connecting to next peer anyway.");

                Watchdog.this.wifiManager.connect(
                    Watchdog.this.wifiChannel,
                    connection,
                    null);
            }
        });
    }

    protected class BroadcastHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() == null) return;
            switch(intent.getAction()) {
                case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                    /* The state of the Wifi P2P system has changed. */
                    int state = intent.getIntExtra(
                        WifiP2pManager.EXTRA_WIFI_STATE,
                        WifiP2pManager.WIFI_P2P_STATE_DISABLED);

                    switch(state) {
                        case WifiP2pManager.WIFI_P2P_STATE_ENABLED:
                            /* Indicate this somehow, I guess? Android is a
                             * mess. */
                            break;
                        case WifiP2pManager.WIFI_P2P_STATE_DISABLED:
                            /* There's no point in running when we can't even
                             * talk to anyone anymore, just stop ourselves. */
                            Watchdog.this.stopSelf();
                            break;
                        default:
                            /* Are there even valid values besides these two?
                             * None that I'm aware of. Crashing here makes sure
                             * I'll be able to quickly catch these during
                             * testing. */
                            Assertions.fail("Got unknown value for EXTRA_WIFI_STATE: 0x%08x", state);
                    }
                    break;
                case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                    /* Our peer list has been changed. */
                    Watchdog.this.wifiManager.requestPeers(
                        Watchdog.this.wifiChannel,
                        Watchdog.this::onPeersChanged);
                    break;
                case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                    /* The state of the connection has changed. */
                    Watchdog.this.wifiManager.requestConnectionInfo(
                        Watchdog.this.wifiChannel,
                        Watchdog.this::onConnectionChanged);
                    break;
                case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                    /* We can extract our own MAC address from this intent by
                     * using the parcelable extra EXTRA_WIFI_P2P_DEVICE, which
                     * will store a parcelable WifiP2pDevice structure,
                     * containing the data we need. Apparently nowhere else in
                     * the API is this functionality available.
                     *
                     * Android has a very well designed API what are you
                     * talking about.
                     */
                    WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                    if(device == null)
                        Assertions.fail("The EXTRA_WIFI_P2P_DEVICE parcelable is null");
                    Watchdog.this.macAddress = device.deviceAddress;

                    break;
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    /** Whatever this is will be local, no need for IPC.
     * I just want off Mr. Android's wild ride already.
     */
    public class Binder extends android.os.Binder {
        /** Tries to pull in a message from the given peer, if any. The
         * underlying functionality of this method should be that of a queue.
         * No messages must be dropped, no matter how long a client takes
         * to pick them up.
         * @param from The {@link Id} of the peer whose queue is to be queried.
         * @return The data of the first message in the queue, if any. */
        public Optional<byte[]> tryReceive(Id from) {
            return Optional.ofNullable(Watchdog.this.inboundQueue.get(from))
                .flatMap(queue -> Optional.ofNullable(queue.removeFirst()))
                .map(Packet::getPayload);
        }

        /** Package up and submit the given data to the network to network to be
         * delivered to the specified device.
         * @param data The data to be delivered. Not encrypted during
         *             transmission, it's expected that the sender take care of
         *             encryption and signing of the data themselves.
         * @param to The intended recipient of the message.
         */
        public void send(byte[] data, Id to) {
            /* Wrap the message into a new packet. */
            Packet p = new Packet(
                Watchdog.this.identity.getId(),
                new Id[] { Watchdog.this.identity.getId() },
                to, data);

            /* And send it to the router. */
            Watchdog.this.router.forward(p, Router.DEFAULT_TIME_TO_LIVE);
        }

        /** Registers the given listener to listen for the event in which we
         * have received any messages originating from the specified peer.
         * @param subject The subject of the listening.
         * @param listener The listener that should be fired.
         */
        public void listen(Id subject, OnMessage listener) {
            ArrayList<OnMessage> listeners = Watchdog.this.inboundListeners.get(subject);
            if(listeners == null) {
                listeners = new ArrayList<>();
                Watchdog.this.inboundListeners.put(subject, listeners);
            }
            listeners.add(listener);
        }

        /** Registers the given listener to listen for whenever the
         * discovery phase has ended and we have a complete peer list.
         * @param listener The listener that should be fired.
         */
        public void watchDiscovery(OnFinishedDiscovery listener) {
            Watchdog.this.finishedDiscoveryListeners.add(listener);
        }
    }

    public interface OnMessage {
        /** Fired when a new message is available to be received.
         * @param source The {@link Id} of the peer that has become ready.
         */
        void onMessage(Id source);
    }

    public interface OnFinishedDiscovery {
        /** Fired when the discovery phase has ended.
         * @param reachable A set of {@link Id} of the peers that were
         *                  reachable at the end of discovery.
         */
        void onFinishedDiscovery(HashSet<Id> reachable);
    }
}
