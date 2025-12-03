package core.OrderBook;

import core.Order.Order;
import core.Order.OrderType;

import java.util.*;

/**
 * Experimental
 */
public final class HashMapTreeSetOrderBookTwoMethods implements ExperimentalOrderBook {

    private long id = 1;

    public final Map<Double, Queue<Order>> bids = new HashMap<>();
    public final Map<Double, Queue<Order>> asks = new HashMap<>();

    public final TreeSet<Double> bidPrices = new TreeSet<>(Comparator.reverseOrder());
    public final TreeSet<Double> askPrices = new TreeSet<>();

    @Override
    public void placeBuyLimit(double price, long quantity) {
        bids.computeIfAbsent(price, _ -> new ArrayDeque<>())
                .add(new Order(id++, OrderType.BUY_LIMIT, price, quantity, false));
        bidPrices.add(price);
    }

    @Override
    public void placeSellLimit(double price, long quantity) {
        asks.computeIfAbsent(price, _ -> new ArrayDeque<>())
                .add(new Order(id++, OrderType.SELL_LIMIT, price, quantity, false));
        askPrices.add(price);
    }

    @Override
    public void placeBuyMarket(long quantity) {
        while (quantity > 0 && !askPrices.isEmpty()) {
            Double best = askPrices.first();
            Queue<Order> level = asks.get(best);
            if (level == null || level.isEmpty()) {
                askPrices.remove(best);
                asks.remove(best);
                continue;
            }

            Order order = level.peek();
            long trade = Math.min(quantity, order.quantity);
            quantity -= trade;
            order.quantity -= trade;

            if (order.quantity == 0) {
                level.remove();
                if (level.isEmpty()) {
                    askPrices.remove(best);
                    asks.remove(best);
                }
            }
        }
    }

    @Override
    public void placeSellMarket(long quantity) {
        while (quantity > 0 && !bidPrices.isEmpty()) {
            Double best = bidPrices.first();
            Queue<Order> level = bids.get(best);
            if (level == null || level.isEmpty()) {
                bidPrices.remove(best);
                bids.remove(best);
                continue;
            }

            Order order = level.peek();
            long trade = Math.min(quantity, order.quantity);
            quantity -= trade;
            order.quantity -= trade;

            if (order.quantity == 0) {
                level.remove();
                if (level.isEmpty()) {
                    bidPrices.remove(best);
                    bids.remove(best);
                }
            }
        }
    }

    public void print() {
        System.out.println("\n┌──────────────────────────────── ORDER BOOK ─────────────────────────┐");
        System.out.println("│         BIDS                        │                ASKS             │");
        System.out.println("├─────────────────────────────────────┼─────────────────────────────────┤");

        List<Map.Entry<Double, Queue<Order>>> bidList = new LinkedList<>(bids.entrySet());
        List<Map.Entry<Double, Queue<Order>>> askList = new LinkedList<>(asks.entrySet());

        int maxRows = Math.max(bidList.size(), askList.size());

        for (int i = 0; i < maxRows; i++) {
            String bidLine = i < bidList.size()
                    ? String.format("%10.2f → %,8d", bidList.get(i).getKey(),
                    bidList.get(i).getValue().stream().mapToLong(order -> order.quantity).sum())
                    : "";

            String askLine = i < askList.size()
                    ? String.format("%10.2f → %,8d", askList.get(i).getKey(),
                    askList.get(i).getValue().stream().mapToLong(order -> order.quantity).sum())
                    : "";

            System.out.printf("│ %-34s │ %-34s │%n", bidLine, askLine);
        }

        System.out.println("└─────────────────────────────────────┴─────────────────────────────────┘\n");
    }
}