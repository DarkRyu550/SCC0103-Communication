package net.xn__n6x.communication.control;

import android.os.SystemClock;
import android.util.Log;
import net.xn__n6x.communication.identity.Id;

import java.util.*;
import java.util.stream.Collectors;

public class Router {
    /** By default, how many milliseconds a packet should live for. (2 minutes) */
    public static final long DEFAULT_TIME_TO_LIVE = 120000;

    /** Holds the messages queued for every target peer. */
    protected final HashMap<Id, PacketQueue> packetQueueSet;
    /** Holds the messages queued for a general audience. */
    protected final ArrayDeque<QueuedPacket> packetCache;

    /** Holds the set of the peers we are currently connected to. */
    protected final HashSet<Id> reachablePeers;

    /** The Id of this device. */
    protected final Id id;

    /** Create a new, blank router.
     * @param id The {@link Id} of the current device. */
    public Router(Id id) {
        this.id = id;
        this.packetQueueSet = new HashMap<>();
        this.reachablePeers = new HashSet<>();
        this.packetCache = new ArrayDeque<>();
    }

//    /** Queries whether there is a message that hasn't yet expired waiting to be sent to the peer
//     * with the given {@link Id}. Note that this is a hint, and in case of {@code true} the return
//     * value of {@link Router#getNextMessageForPeer(Id)} is still not guaranteed to have a value.
//     * <br><br>
//     * This happens because a message may still expire between calls to this function and subsequent
//     * calls to {@link Router#getNextMessageForPeer(Id)}. This function will remove every element in
//     * the queue that has already expired.
//     *
//     * @param peer The peer whose queue is to be checked.
//     * @return Whether there may still be any valid messages in queue for this peer.
//     */
//    public boolean hasNextMessageForPeer(Id peer) {
//        PacketQueue queue = this.packetQueueSet.get(peer);
//        if(queue == null) return false;
//
//        /* Trim all expired packets. */
//        queue.removeIf(QueuedPacket::expired);
//
//        /* Just check if we have anything at the head. */
//        return queue.peek() != null;
//    }

    /** Queries the first {@link Packet} queued up for the peer with the given {@link Id} that has
     * not yet expired. This function will remove every element in the queue that has already
     * expired.
     * @param peer The peer whose queue is to be queried.
     * @return The {@link Packet} next in line for delivery to the given peer, if any.
     */
    public Optional<Packet> getNextMessageForPeer(Id peer) {
        PacketQueue queue = this.packetQueueSet.get(peer);
        if (queue == null)
            return Optional.empty();
        return queue.take();
    }

    /** Gets the set of peers which have messages waiting to be delivered to them.
     * @return A {@link HashSet} of the {@link Id}s of targeted peers.
     */
    public HashSet<Id> getTargetedReachablePeers() {
        return this.packetQueueSet
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().hasNext())
            .filter(entry -> this.reachablePeers.contains(entry.getKey()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(HashSet::new));
    }

    /** Registers the given {@link Id} as reachable.
     * @param id The {@link Id} of the peer to be marked as reachable.
     */
    public void register(Id id) {
        this.reachablePeers.add(id);
    }

    /** Retains all the elements in the given collection as being reachable,
     * removing every other other from the reachability set.
     * @param elements A collection of the elements to be kept.
     */
    public void retain(Collection<Id> elements) {
        this.reachablePeers.retainAll(elements);
    }

    /** Given a packet, figure out the set of peers we have to forward it to.
     * @param p Packet to be forwarded.
     * @return A {@link HashSet<Id>} of all peers to whom this packet needs to be forwarded.
     */
    protected HashSet<Id> forwardSet(Packet p) {
        HashSet<Id> targets = new HashSet<>(32);
        HashSet<Id> inRoute = Arrays.stream(p.route).collect(Collectors.toCollection(HashSet::new));

        /* Trim any cycles by refusing to forward back to peers that have
         * already seen this message, they shouldn't need to receive it again.
         */
        targets.removeIf(inRoute::contains);

        if(this.reachablePeers.contains(p.target))
            /* We can reach our target directly. */
            targets.add(p.target);
        else
            /* Forward it to everyone in case we can't connect to them directly. */
            targets.addAll(this.reachablePeers);

        return targets;
    }

    /** Pass a given packet message onward.
     * @param p Packet to be forwarded.
     * @param timeToLive How long the message is allowed to be in the queue.
     */
    public void forward(Packet p, long timeToLive) {
        HashSet<Id> forward = this.forwardSet(p);

        /* Here is where I'd put my HashMap::intersect. IF I HAD ONE. */
        for(Id forwardId : forward) {
            /* Initialize if none. */
            PacketQueue queue = this.packetQueueSet.get(forwardId);
            if(queue == null) {
                queue = new PacketQueue();
                this.packetQueueSet.put(forwardId, queue);
            }

            queue.enqueueIfNew(p, timeToLive);
        }
    }

//    /** Signal the sending of a packet has failed, and that it should be retired.
//     * @param p The packet whose sending has failed.
//     * @param failed The {@link Id}
//     */
//    public void retry(Packet p, Id failed) {
//        if(this.reachablePeers.contains(failed))
//            this.
//    }

    protected static class PacketQueue {
        /** The packets currently waiting, in queue order. */
        public final ArrayDeque<QueuedPacket> queue;
        /** The set of all packets currently queued up. */
        public final HashSet<Packet> catalogue;

        public PacketQueue() {
            this.catalogue = new HashSet<>();
            this.queue = new ArrayDeque<>();
        }

        /** Enqueues the given {@link Packet} if it's not already in the queue.
         * @param p The Packet to be forwarded.
         */
        public void enqueueIfNew(Packet p, long timeToLive) {
            if(!catalogue.contains(p)){
                queue.add(QueuedPacket.wrap(p, timeToLive));
                catalogue.add(p);
            }
        }

        /** Takes the first {@link Packet} in this queue that still has not expired.
         * @return The {@link Packet}, if any.
         */
        public Optional<Packet> take() {
            while(this.queue.size() > 0) {
                QueuedPacket packet = this.queue.pop();
                this.catalogue.remove(packet.packet);

                if(!packet.expired())
                    return Optional.of(packet.packet);
            }
            return Optional.empty();
        }

        public boolean hasNext() {
            return this.queue.size() > 0;
        }
    }

    /** Wraps a packet with time information so that we can have packets expire
     * when they have been queued for so long sending them is probably not
     * meaningful anymore. */
    protected static class QueuedPacket {
        /** Packet waiting in queue to be sent. */
        public final Packet packet;
        /** Time this packet will be valid for while waiting in queue, in milliseconds. */
        protected final long timeToLive;
        /** Monotonic timestamp in milliseconds of the time this packet was queued. */
        protected final long postTime;

        public QueuedPacket(Packet packet, long timeToLive, long postTime) {
            this.packet = packet;
            this.timeToLive = timeToLive;
            this.postTime = postTime;
        }

        /** Wrap a packet with a given time to live and register it with the current time.
         * @param packet The packet to be wrapped.
         * @param timeToLive The time this packet should be valid for.
         * @return The given packet, wrapped with its information time.
         */
        public static QueuedPacket wrap(Packet packet, long timeToLive) {
            long currTime = SystemClock.uptimeMillis();
            return new QueuedPacket(packet, timeToLive, currTime);
        }

        /** Has this packet exceeded its time to live property?
         * @return Whether this packet should be ignored for having expired.
         */
        public boolean expired() {
            long currTime = SystemClock.uptimeMillis();
            long delta = currTime - this.postTime;

            if(delta < 0)
                Log.w(
                    "Router/QueuedPacket",
                    "Time delta for a packet has been calculated to be less than zero.");
            return delta > this.timeToLive;
        }
    }

    public HashSet<Id> getReachablePeers() {
        return reachablePeers;
    }
}
