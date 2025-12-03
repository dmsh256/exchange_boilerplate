package core.OrderBook;

import core.Order.Order;

import java.util.*;

public class TreeMapOrderBook implements ExperimentalOrderBook {
    public final NavigableMap<Double, Deque<Order>> bids = new TreeMap<>(Comparator.reverseOrder());
    public final NavigableMap<Double, Deque<Order>> asks = new TreeMap<>();

    @Override
    public void placeBuyLimit(Order order) {
        bids.computeIfAbsent(order.price, _ -> new ArrayDeque<>())
                .add(order);
    }

    @Override
    public void placeSellLimit(Order order) {
        asks.computeIfAbsent(order.price, _ -> new ArrayDeque<>())
                .add(order);
    }

    @Override
    public void placeBuyMarket(Order order) {
        long quantity = order.quantity;
        while (quantity > 0 && !asks.isEmpty()) {
            Map.Entry<Double, Deque<Order>> bestAskEntry = asks.firstEntry();
            double bestAskPrice = bestAskEntry.getKey();

            Deque<Order> level = bestAskEntry.getValue();
            Order ask = level.peekFirst();

            if (ask == null)
                break;

            long tradeQty = Math.min(quantity, ask.quantity);

            quantity -= tradeQty;
            ask.quantity -= tradeQty;

            if (ask.quantity == 0) {
                level.removeFirst();

                if (level.isEmpty())
                    asks.remove(bestAskPrice);
            }
        }
    }

    @Override
    public void placeSellMarket(Order order) {
        long quantity = order.quantity;
        while (quantity > 0 && !bids.isEmpty()) {
            Map.Entry<Double, Deque<Order>> bestBidEntry = bids.firstEntry();
            double bestBidPrice = bestBidEntry.getKey();

            Deque<Order> level = bestBidEntry.getValue();
            Order bid = level.peekFirst();

            if (bid == null)
                break;

            long tradeQty = Math.min(quantity, bid.quantity);

            quantity -= tradeQty;
            bid.quantity -= tradeQty;

            if (bid.quantity == 0) {
                level.removeFirst();
                if (level.isEmpty())
                    bids.remove(bestBidPrice);
            }
        }
    }
}