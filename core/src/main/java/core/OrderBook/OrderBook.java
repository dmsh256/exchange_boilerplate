package core.OrderBook;

import core.Order.Order;

public interface OrderBook {

    void placeBuyLimit(Order order);
    void placeSellLimit(Order order);

    void placeBuyMarket(Order order);
    void placeSellMarket(Order order);

    // TODO cancel Order
}