package core.MatchingEngine;

import core.Order.Order;
import core.Order.OrderType;
import core.OrderBook.OrderBook;
import core.OrderPool.SPSCOrderPool;
import core.RingBuffer.RingBuffer;

public class MatchingEngine {

    private final OrderBook orderBook;
    public final RingBuffer<Order> ringBuffer;
    public final SPSCOrderPool orderPool;

    public MatchingEngine(OrderBook orderBook, RingBuffer<Order> ringBuffer, SPSCOrderPool orderPool) {
        this.orderBook = orderBook;
        this.ringBuffer = ringBuffer;
        this.orderPool = orderPool;
    }

    public void matchOne() {
        Order order = ringBuffer.poll();
        if (order == null)
            return;

        switch (order.type) {
            case BUY_LIMIT -> orderBook.placeBuyLimit(order);
            case SELL_LIMIT -> orderBook.placeSellLimit(order);
            case BUY_MARKET -> orderBook.placeBuyMarket(order);
            case SELL_MARKET -> orderBook.placeSellMarket(order);
        }

        if (order.type == OrderType.BUY_LIMIT || order.type == OrderType.SELL_LIMIT) {
            if (order.quantity == 0)
                orderPool.release(order); //release only filled LIMIT orders
        } else {
            orderPool.release(order);
        }
    }
}
