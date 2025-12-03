package core.OrderBook;

import core.Order.OrderType;

public interface OneMethodOrderBook {
    void limit(double price, long qty, OrderType orderType);
    void market(long qty, OrderType orderType);
}