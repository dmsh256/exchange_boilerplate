package core.Order;

/**
 * A little messy but supports two order books
 */
public final class Order {

    public long id;
    public OrderType type;
    public double price;
    public boolean isMarket;
    public long quantity;
    public Order next;
    public Order prev;
    public int priceIndex;

    public Order() {}

    public Order(long id, OrderType type, double price, long quantity, boolean isMarket) {
        this.id = id;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.isMarket = isMarket;
    }

    public void reset() {
        this.id = 0;
        this.price = 0;
        this.quantity = 0;
        this.isMarket = false;
        next = prev = null;
        priceIndex = -1;
    }

    @Override
    public String toString() {
        return String.format("Order#%d %s %.2f × %,d (rem %,d)",
                id, type, price, id, quantity);
    }
}
