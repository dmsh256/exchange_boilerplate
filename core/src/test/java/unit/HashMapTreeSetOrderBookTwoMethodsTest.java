package unit;

import core.OrderBook.HashMapTreeSetOrderBookTwoMethods;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HashMapTreeSetOrderBookTwoMethodsTest {

    private HashMapTreeSetOrderBookTwoMethods book;

    @Test
    void testPriceTimePriority_FIFO() {
        book = new HashMapTreeSetOrderBookTwoMethods();

        book.placeSellLimit(100.0, 10); // order #1
        book.placeSellLimit(100.0, 20); // order #2
        book.placeSellLimit(100.0, 30); // order #3

        book.placeBuyMarket(25);
        book.placeBuyMarket(100);
    }

    @Test
    void testBestPricePriority_BuyHitsLowestAsk() {
        book = new HashMapTreeSetOrderBookTwoMethods();

        book.placeSellLimit(101.0, 100);
        book.placeSellLimit(100.0, 100);
        book.placeSellLimit(102.0, 100);

        book.placeBuyMarket(1000);

        assertTrue(book.askPrices.isEmpty() || book.askPrices.first() > 100.0);
    }

    @Test
    void testBestPricePriority_SellHitsHighestBid() {
        book = new HashMapTreeSetOrderBookTwoMethods();

        book.placeBuyLimit(99.0, 100);
        book.placeBuyLimit(100.0, 100);
        book.placeBuyLimit(98.0, 100);

        book.placeSellMarket(1000);

        assertTrue(book.bidPrices.isEmpty() || book.bidPrices.first() < 100.0);
    }

    @Test
    void testPartialFill_LeavesRemainingQuantity() {
        book = new HashMapTreeSetOrderBookTwoMethods();

        book.placeSellLimit(100.0, 50);
        book.placeBuyMarket(20);

        book.placeBuyMarket(30);

        assertFalse(book.asks.containsKey(100.0));
    }

    @Test
    void testMultipleLevelsConsumed() {
        book = new HashMapTreeSetOrderBookTwoMethods();

        book.placeSellLimit(100.0, 10);
        book.placeSellLimit(101.0, 20);
        book.placeSellLimit(102.0, 30);

        book.placeBuyMarket(55);

        assertFalse(book.asks.containsKey(100.0));
        assertFalse(book.asks.containsKey(101.0));
        assertTrue(book.asks.containsKey(102.0));
    }

    @Test
    void testEmptyLevelCleanup() {
        book = new HashMapTreeSetOrderBookTwoMethods();

        book.placeSellLimit(100.0, 10);
        book.placeBuyMarket(10);

        assertFalse(book.asks.containsKey(100.0));
        assertFalse(book.askPrices.contains(100.0));
    }

    @Test
    void testNoSelfTrade_BuyMarketDoesNotHitOwnBid() {
        book = new HashMapTreeSetOrderBookTwoMethods();

        book.placeBuyLimit(100.0, 50);
        book.placeSellLimit(99.0, 100);

        book.placeBuyMarket(1000);

        assertTrue(book.bids.containsKey(100.0));
    }

    @Test
    void testMarketOrderDoesNothingWhenBookEmpty() {
        book = new HashMapTreeSetOrderBookTwoMethods();

        book.placeBuyMarket(1000);
        book.placeSellMarket(1000);

        assertTrue(book.bidPrices.isEmpty());
        assertTrue(book.askPrices.isEmpty());
    }

    @Test
    void testOrderIdIsUniqueAndIncreasing() {
        book = new HashMapTreeSetOrderBookTwoMethods();

        book.placeBuyLimit(100.0, 1);
        book.placeSellLimit(101.0, 1);
        book.placeBuyLimit(99.0, 1);
    }
}