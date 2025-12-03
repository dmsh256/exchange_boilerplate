package core.OrderBook;

import core.Order.Order;

public interface ExperimentalOrderBook {

    void placeBuyLimit(Order order);
    void placeSellLimit(Order order);

    void placeBuyMarket(Order order);
    void placeSellMarket(Order order);
}