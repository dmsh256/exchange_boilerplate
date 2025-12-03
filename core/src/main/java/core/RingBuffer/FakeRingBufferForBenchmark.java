package core.RingBuffer;

import java.util.Queue;
import java.util.function.Supplier;

public class FakeRingBufferForBenchmark<T> implements RingBuffer<T> {
    private Queue<T> preloadQueue;
    private final Supplier<T> fallbackSupplier;

    public FakeRingBufferForBenchmark(Queue<T> preloadQueue, Supplier<T> fallbackSupplier) {
        this.preloadQueue = preloadQueue;
        this.fallbackSupplier = fallbackSupplier;
    }

    public T poll() {
        T order = preloadQueue.poll();
        if (order != null)
            return order;

        return fallbackSupplier.get();
    }

    public boolean offer(T order) {
        return preloadQueue.offer(order);
    }

    public boolean isEmpty() {
        return preloadQueue.isEmpty();
    }

    public void setPreloadQueue(Queue<T> preloadQueue) {
        this.preloadQueue = preloadQueue;
    }
}