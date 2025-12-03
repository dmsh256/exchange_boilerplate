package core.OrderBook;

import core.Order.Order;
import core.Order.OrderType;

import java.util.function.Consumer;

/**
 * Array-bucket order book.
 * <p>
 * - Price discretization: price -> index in [0..capacity-1]
 * - Two arrays of Level: bids (descending indexes) and asks (ascending)
 * - Intrusive doubly-linked list of Order within each Level
 * <p>
 * Usage:
 *  - create: new ArrayBucketOrderBook(minPrice, maxPrice, tickSize)
 *  - add a LIMIT order: placeLimit(order)  (order.price must map to an index)
 *  - match a MARKET order: placeMarket(order)
 *  - cancel: cancel(order)
 * <p>
 * Note: Limits must be unique objects stored in the book. Market orders should be pooled.
 */
public final class ArrayBucketOrderBook implements OrderBook {

    private static final class Level {
        Order head;
        Order tail;
        long totalQty;

        void addLast(Order order) {
            order.prev = tail;
            order.next = null;
            if (tail != null)
                tail.next = order;

            tail = order;
            if (head == null)
                head = order;

            totalQty += order.quantity;
        }

        void remove(Order order) {
            if (order.prev != null)
                order.prev.next = order.next;
            else
                head = order.next;

            if (order.next != null)
                order.next.prev = order.prev;
            else
                tail = order.prev;

            totalQty -= order.quantity;
            order.next = order.prev = null;
        }

        boolean isEmpty() {
            return head == null;
        }
    }

    private final double minPrice;
    private final double tickSize;
    private final int capacity;

    private final Level[] bids;
    private final Level[] asks;

    private int bestBidIndex = -1;
    private int bestAskIndex = -1;

    private final Consumer<Trade> tradeConsumer;
    private final Consumer<Order> poolReleaser;

    public static final class Trade {
        public final double price;
        public final long qty;
        public final boolean buyInitiator;

        public Trade(double price, long qty, boolean buyInitiator) {
            this.price = price;
            this.qty = qty;
            this.buyInitiator = buyInitiator;
        }

        @Override
        public String toString() {
            return String.format("Trade{price=%.8f, qty=%d, buyInitiator=%s}", price, qty, buyInitiator);
        }
    }

    /**
     * Construct the book.
     *
     * @param minPrice inclusive minimum price mapped to index 0
     * @param maxPrice inclusive maximum price mapped to index capacity-1
     * @param tickSize price tick resolution (> 0)
     * @param tradeConsumer optional consumer to receive executed trades (could be null)
     * @param poolReleaser optional pool release callback (could be null)
     */
    public ArrayBucketOrderBook(double minPrice, double maxPrice, double tickSize,
                                Consumer<Trade> tradeConsumer,
                                Consumer<Order> poolReleaser) {
        if (tickSize <= 0)
            throw new IllegalArgumentException("tickSize > 0");
        if (maxPrice <= minPrice)
            throw new IllegalArgumentException("maxPrice > minPrice");

        this.minPrice = minPrice;
        this.tickSize = tickSize;
        long buckets = (long)Math.floor((maxPrice - minPrice) / tickSize) + 1L;
        if (buckets > Integer.MAX_VALUE)
            throw new IllegalArgumentException("too many buckets");

        this.capacity = (int)buckets;

        this.bids = new Level[capacity];
        this.asks = new Level[capacity];
        for (int i = 0; i < capacity; i++) {
            bids[i] = new Level();
            asks[i] = new Level();
        }

        this.tradeConsumer = tradeConsumer;
        this.poolReleaser = poolReleaser;
    }

    /** Map a price to an index. Caller must ensure price is in range. */
    public int priceToIndex(double price) {
        long idx = Math.round((price - minPrice) / tickSize);
        if (idx < 0 || idx >= capacity)
            throw new IndexOutOfBoundsException("price out of range: " + price);

        return (int) idx;
    }

    public double indexToPrice(int index) {
        return minPrice + index * tickSize;
    }

    public void placeBuyLimit(Order order) {
        int idx = priceToIndex(order.price);
        order.priceIndex = idx;
        Level level = bids[idx];
        level.addLast(order);

        if (bestBidIndex < idx)
            bestBidIndex = idx;
    }

    public void placeSellLimit(Order order) {
        int idx = priceToIndex(order.price);
        order.priceIndex = idx;
        Level level = asks[idx];

        level.addLast(order);
        if (bestAskIndex == -1 || bestAskIndex > idx)
            bestAskIndex = idx;
    }

    public void placeBuyMarket(Order marketOrder) {
        if (marketOrder.quantity <= 0) {
            maybeReleaseMarket(marketOrder);

            return;
        }

        while (marketOrder.quantity > 0) {
            int idx = bestAskIndex;
            if (idx == -1)
                break;
            Level lvl = asks[idx];

            Order head = lvl.head;
            if (head == null) {
                advanceBestAsk();
                continue;
            }

            long quantity = Math.min(marketOrder.quantity, head.quantity);
            double tradePrice = indexToPrice(idx);
            consumeFromLevel(lvl, head, quantity, true);

            marketOrder.quantity -= quantity;
            if (tradeConsumer != null)
                tradeConsumer.accept(new Trade(tradePrice, quantity, true));
        }

        maybeReleaseMarket(marketOrder);
    }

    public void placeSellMarket(Order marketOrder) {
        if (marketOrder.quantity <= 0) {
            maybeReleaseMarket(marketOrder);

            return;
        }

        while (marketOrder.quantity > 0) {
            int idx = bestBidIndex;
            if (idx == -1)
                break;
            Level lvl = bids[idx];

            Order head = lvl.head;
            if (head == null) {
                retreatBestBid();
                continue;
            }

            long quantity = Math.min(marketOrder.quantity, head.quantity);
            double tradePrice = indexToPrice(idx);
            consumeFromLevel(lvl, head, quantity, false);

            marketOrder.quantity -= quantity;
            if (tradeConsumer != null)
                tradeConsumer.accept(new Trade(tradePrice, quantity, false));
        }

        maybeReleaseMarket(marketOrder);
    }

    /**
     * Not in the hot path, could use 'if' inside
     */
    public boolean cancel(Order order) {
        int idx = order.priceIndex;
        if (idx < 0 || idx >= capacity)
            return false;

        Level level = (order.type == OrderType.BUY_MARKET || order.type == OrderType.BUY_LIMIT) ? bids[idx] : asks[idx];
        if (order.prev == null && order.next == null && level.head != order)
            return false;

        level.remove(order);
        if ((order.type == OrderType.BUY_MARKET || order.type == OrderType.BUY_LIMIT) && level.isEmpty()) {
            if (bestBidIndex == idx)
                retreatBestBid();
        } else if ((order.type == OrderType.SELL_MARKET || order.type == OrderType.SELL_LIMIT) && level.isEmpty()) {
            if (bestAskIndex == idx)
                advanceBestAsk();
        }

        if (poolReleaser != null)
            poolReleaser.accept(order);

        return true;
    }

    /**
     * Handles partial fills and removal
     * TODO split sell and buy paths
     */
    private void consumeFromLevel(Level level, Order head, long quantity, boolean initiatorIsBuy) {
        if (quantity <= 0)
            return;

        if (head == null)
            return;

        if (quantity >= head.quantity) {
            level.remove(head);
            if (poolReleaser != null)
                poolReleaser.accept(head);

            if (level.isEmpty()) {
                if (initiatorIsBuy)
                    advanceBestAsk();
                else
                    retreatBestBid();
            }
        } else {
            head.quantity -= quantity;
            level.totalQty -= quantity;
        }
    }

    /**
     * Move bestAskIndex forward to next non-empty ask or -1
     */
    private void advanceBestAsk() {
        int i = bestAskIndex;
        if (i == -1)
            return;

        while (i < capacity && asks[i].isEmpty())
            i++;

        bestAskIndex = (i < capacity) ? i : -1;
    }

    /**
     * Move bestBidIndex backward to next non-empty bid or -1
     */
    private void retreatBestBid() {
        int i = bestBidIndex;
        if (i == -1)
            return;

        while (i >= 0 && bids[i].isEmpty())
            i--;

        bestBidIndex = (i >= 0) ? i : -1;
    }

    private void skipEmptyAsksForward() {
        if (bestAskIndex == -1)
            return;

        while (bestAskIndex < capacity && asks[bestAskIndex].isEmpty())
            bestAskIndex++;

        if (bestAskIndex >= capacity)
            bestAskIndex = -1;
    }

    private void skipEmptyBidsBackward() {
        if (bestBidIndex == -1)
            return;

        while (bestBidIndex >= 0 && bids[bestBidIndex].isEmpty())
            bestBidIndex--;

        if (bestBidIndex < 0)
            bestBidIndex = -1;
    }

    private void maybeReleaseMarket(Order marketOrder) {
        if (poolReleaser != null)
            poolReleaser.accept(marketOrder);
    }

    public double getBestBidPrice() {
        if (bestBidIndex == -1)
            return Double.NaN;

        return indexToPrice(bestBidIndex);
    }

    public double getBestAskPrice() {
        if (bestAskIndex == -1)
            return Double.NaN;

        return indexToPrice(bestAskIndex);
    }

    public long getTotalAtPriceIndex(boolean buy, int idx) {
        Level l = buy ? bids[idx] : asks[idx];

        return l.totalQty;
    }

    public int capacity() { return capacity; }
}

