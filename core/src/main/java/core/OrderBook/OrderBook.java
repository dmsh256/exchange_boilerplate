package core.OrderBook;

public interface OrderBook {

    void placeBuyLimit(double price, long quantity);
    void placeSellLimit(double price, long quantity);

    void placeBuyMarket(long quantity);
    void placeSellMarket(long quantity);
}