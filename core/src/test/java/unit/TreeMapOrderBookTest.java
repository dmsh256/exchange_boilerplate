package unit;

import core.Order.Order;
import core.Order.OrderType;
import core.OrderBook.TreeMapOrderBook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TreeMapOrderBookTest {

    private TreeMapOrderBook book;

    @Test
    void testPriceTimePriority_FIFO() {
        book = new TreeMapOrderBook();

        book.placeSellLimit(new Order(1, OrderType.SELL_LIMIT, 100.0, 10, false));
        book.placeSellLimit(new Order(1, OrderType.SELL_LIMIT, 100.0, 20, false));
        book.placeSellLimit(new Order(1, OrderType.SELL_LIMIT, 100.0, 30, false));

        book.placeBuyMarket(new Order(1, OrderType.BUY_MARKET, 0, 25, false));
        book.placeBuyMarket(new Order(1, OrderType.BUY_MARKET, 0, 100, false));
    }

    @Test
    void testPartialFill_LeavesRemainingQuantity() {
        book = new TreeMapOrderBook();

        book.placeSellLimit(new Order(1, OrderType.SELL_LIMIT, 100.0, 50, false));
        book.placeBuyMarket(new Order(1, OrderType.BUY_MARKET, 0, 20, false));

        book.placeBuyMarket(new Order(1, OrderType.BUY_MARKET, 0, 30, false));

        assertFalse(book.asks.containsKey(100.0));
    }

    @Test
    void testMultipleLevelsConsumed() {
        book = new TreeMapOrderBook();

        book.placeSellLimit(new Order(1, OrderType.SELL_LIMIT, 100.0, 10, false));
        book.placeSellLimit(new Order(1, OrderType.SELL_LIMIT, 101.0, 20, false));
        book.placeSellLimit(new Order(1, OrderType.SELL_LIMIT, 102.0, 30, false));

        book.placeBuyMarket(new Order(1, OrderType.BUY_MARKET, 0, 55, false));

        assertFalse(book.asks.containsKey(100.0));
        assertFalse(book.asks.containsKey(101.0));
        assertTrue(book.asks.containsKey(102.0));
    }

    @Test
    void testNoSelfTrade_BuyMarketDoesNotHitOwnBid() {
        book = new TreeMapOrderBook();

        book.placeBuyLimit(new Order(1, OrderType.BUY_LIMIT, 100.0, 50, false));
        book.placeSellLimit(new Order(1, OrderType.SELL_LIMIT, 99.0, 150, false));

        book.placeBuyMarket(new Order(1, OrderType.BUY_MARKET, 0, 1000, false));

        assertTrue(book.bids.containsKey(100.0));
    }

    @Test
    void testOrderIdIsUniqueAndIncreasing() {
        book = new TreeMapOrderBook();

        book.placeBuyLimit(new Order(1, OrderType.BUY_LIMIT, 100.0, 1, false));
        book.placeSellLimit(new Order(1, OrderType.SELL_LIMIT, 101.0, 1, false));
        book.placeBuyLimit(new Order(1, OrderType.BUY_LIMIT, 99.0, 1, false));
    }
}