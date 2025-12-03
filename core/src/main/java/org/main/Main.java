package org.main;

import core.Order.Order;
import core.Order.OrderType;
import core.OrderBook.TreeMapOrderBook;
import core.OrderPool.SPSCOrderPool;
import core.RingBuffer.SPSCRingBuffer;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    static void main(String[] args) throws InterruptedException {
        String version = System.getProperty("java.version");
        System.out.println("Java Version: " + version);

        //HashMapTreeSetOrderBookOneMethod book = new HashMapTreeSetOrderBookOneMethod();
//
        //book.limit(65000, 10, Side.SELL);
        //book.limit(65000, 4, Side.SELL);
        //book.limit(65010,  5, Side.SELL);
        //book.print();
//
        //book.limit(65005, 7, Side.BUY);
        //book.print();
//
        //book.market(4, Side.BUY);
        //book.print();
//
        //HashMapTreeSetOrderBookTwoMethods book2 = new HashMapTreeSetOrderBookTwoMethods();
//
        //book2.placeSellLimit(65000, 10);
        //book2.placeSellLimit(65000, 4);
        //book2.placeSellLimit(65010,  5);
        //book.print();
//
        //book2.placeBuyLimit(65005, 7);
        //book2.print();
//
        //book2.placeBuyMarket(4);
        //book2.print();

        SPSCOrderPool pool = new SPSCOrderPool(1_048_576);
        SPSCRingBuffer<Order> ring = new SPSCRingBuffer<>(131072);
        TreeMapOrderBook book = new TreeMapOrderBook();

        Thread producer = getProducerThread(pool, ring);
        Thread matcher = getMatcherThread(ring, book, pool);

        producer.start();
        matcher.start();

        producer.join();
        matcher.join();
    }

    private static Thread getMatcherThread(SPSCRingBuffer<Order> ring, TreeMapOrderBook orderBook, SPSCOrderPool orderPool) {
        return new Thread(() -> {
            while (true) {
                Order order = ring.poll();
                if (order == null) {
                    Thread.onSpinWait();
                    continue;
                }

                switch (order.type) {
                    case BUY_LIMIT -> orderBook.placeBuyLimit(order);
                    case SELL_LIMIT -> orderBook.placeSellLimit(order);
                    case BUY_MARKET -> orderBook.placeBuyMarket(order);
                    case SELL_MARKET -> orderBook.placeSellMarket(order);
                }

                orderPool.release(order);
            }
        }, "matcher");
    }

    private static Thread getProducerThread(SPSCOrderPool orderPool, SPSCRingBuffer<Order> ring) {
        AtomicLong counter = new AtomicLong();

        return new Thread(() -> {
            Random rnd = new Random();
            while (true) {
                Order order = orderPool.borrow();
                while (order == null) {
                    Thread.onSpinWait();
                    order = orderPool.borrow();
                }

                order.id = counter.incrementAndGet();
                order.price = 100 + rnd.nextInt(10);
                order.quantity = 1 + rnd.nextInt(5);
                order.type = rnd.nextBoolean() ?
                        (rnd.nextBoolean() ? OrderType.BUY_LIMIT : OrderType.SELL_LIMIT) :
                        (rnd.nextBoolean() ? OrderType.BUY_MARKET : OrderType.SELL_MARKET);

                while (!ring.offer(order)) {
                    Thread.onSpinWait();
                }
            }
        }, "producer");
    }
}