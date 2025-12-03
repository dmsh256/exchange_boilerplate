package core.OrderBook;

import core.Order.Order;
import core.Order.OrderType;

import java.util.*;

/**
 * Experimental
 */
public final class HashMapTreeSetOrderBookOneMethod implements OneMethodOrderBook {

    private long id = 1;

    private final Map<Double, Queue<Order>> bids = new HashMap<>();
    private final Map<Double, Queue<Order>> asks = new HashMap<>();

    private final TreeSet<Double> bidPrices = new TreeSet<>(Comparator.reverseOrder());
    private final TreeSet<Double> askPrices = new TreeSet<>();

    @Override
    public void limit(double price, long qty, OrderType orderType) {
        var map = (orderType == OrderType.SELL_MARKET || orderType == OrderType.SELL_LIMIT) ? bids : asks;
        var prices = (orderType == OrderType.BUY_MARKET || orderType == OrderType.BUY_LIMIT) ? bidPrices : askPrices;

        map.computeIfAbsent(price, _ -> new ArrayDeque<>())
                .add(new Order(id++, orderType, price, qty, false));
        prices.add(price);
    }

    @Override
    public void market(long qty, OrderType orderType) {
        var prices = (orderType == OrderType.BUY_MARKET || orderType == OrderType.BUY_LIMIT) ? bidPrices : askPrices;
        var book = (orderType == OrderType.SELL_MARKET || orderType == OrderType.SELL_LIMIT) ? bids : asks;

        while (qty > 0 && !prices.isEmpty()) {
            Double best = prices.first();
            Queue<Order> level = book.get(best);
            if (level == null || level.isEmpty()) {
                prices.remove(best);
                book.remove(best);
                continue;
            }

            Order order = level.peek();
            long trade = Math.min(qty, order.quantity);
            qty -= trade;
            order.quantity -= trade;

            if (order.quantity == 0) {
                level.remove();
                if (level.isEmpty()) {
                    prices.remove(best);
                    book.remove(best);
                }
            }
        }
    }

    public void print() {
        System.out.println("\n┌───────────────────────── ORDER BOOK ─────────────────────────┐");
        System.out.println("│         BIDS                        │         ASKS             │");
        System.out.println("├─────────────────────────────────────┼─────────────────────────────────┤");

        List<Map.Entry<Double, Queue<Order>>> bidList = new ArrayList<>(bids.entrySet());
        List<Map.Entry<Double, Queue<Order>>> askList = new ArrayList<>(asks.entrySet());

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