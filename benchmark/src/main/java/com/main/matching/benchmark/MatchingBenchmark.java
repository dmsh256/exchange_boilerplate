package com.main.matching.benchmark;

import core.MatchingEngine.MatchingEngine;
import core.Order.Order;
import core.Order.OrderType;
import core.OrderBook.*;
import core.OrderPool.SPSCOrderPool;
import core.RingBuffer.FakeRingBufferForBenchmark;
import org.openjdk.jmh.annotations.*;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(0)
public class MatchingBenchmark {

    private static final int RING_SIZE = 131072;
    private static final int PRELOAD_ORDERS = 2_000_000;

    private MatchingEngine engine;
    Queue<Order> preloadQueue = new ConcurrentLinkedQueue<>();

    @Setup(Level.Trial)
    public void setup() {
        SPSCOrderPool orderPool = new SPSCOrderPool(PRELOAD_ORDERS);

        FakeRingBufferForBenchmark<Order> ringBuffer = new FakeRingBufferForBenchmark<>(preloadQueue, () -> {
            Order order = engine.orderPool.borrow();
            // does not affect performance, but requires a large pool
            /*order.type = ThreadLocalRandom.current().nextBoolean() ?
                    (ThreadLocalRandom.current().nextBoolean() ? OrderType.BUY_MARKET : OrderType.SELL_MARKET):
                    (ThreadLocalRandom.current().nextBoolean() ? OrderType.BUY_LIMIT : OrderType.SELL_LIMIT);*/
            order.type = ThreadLocalRandom.current().nextBoolean() ? OrderType.BUY_MARKET : OrderType.SELL_MARKET;
            order.price = 65000 + ThreadLocalRandom.current().nextInt(0, 30);
            order.quantity = ThreadLocalRandom.current().nextInt(10, 50);

            return order;
        });

        engine = new MatchingEngine(
                new ArrayBucketOrderBook(
                        65000.0, 65500.0, 0.01,
                        _ -> {},
                        Order::reset
                ),
                ringBuffer,
                orderPool
        );
    }

    @Setup(Level.Iteration)
    public void refill() {
        int inserted = 0;
        while (inserted < RING_SIZE - 1) {
            Order order = engine.orderPool.borrow();
            order.type = ThreadLocalRandom.current().nextBoolean() ? OrderType.BUY_LIMIT : OrderType.SELL_LIMIT;
            order.price = 65000 + ThreadLocalRandom.current().nextInt(0, 30);
            order.quantity = ThreadLocalRandom.current().nextInt(10, 5000);

            preloadQueue.offer(order);

            inserted++;
        }

        engine.ringBuffer.setPreloadQueue(preloadQueue);

        System.out.println("Inserted: " + inserted);
    }

    @Benchmark
    public void matchThreadOnly() {
        engine.matchOne();
    }
}
