package core.OrderBook;

public interface ExperimentalOrderBook {

    void placeBuyLimit(double price, long quantity);
    void placeSellLimit(double price, long quantity);

    void placeBuyMarket(long quantity);
    void placeSellMarket(long quantity);
}