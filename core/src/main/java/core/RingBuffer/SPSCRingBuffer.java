package core.RingBuffer;

import java.util.Queue;

public class SPSCRingBuffer<T> implements RingBuffer<T> {
    private final Object[] buffer;
    private final int mask;

    private volatile long head = 0;
    private volatile long tail = 0;

    public SPSCRingBuffer(int capacityPow2) {
        if ((capacityPow2 & (capacityPow2 - 1)) != 0)
            throw new IllegalArgumentException("Capacity must be power of two");

        buffer = new Object[capacityPow2];
        mask = capacityPow2 - 1;
    }

    public boolean offer(T value) {
        long h = head;
        long t = tail;

        if (h - t >= buffer.length) // full
            return false;

        int index = (int)(h & mask);
        buffer[index] = value;

        head = h + 1;

        return true;
    }

    @SuppressWarnings("unchecked")
    public T poll() {
        long t = tail;
        long h = head;

        if (t >= h)
            return null;

        int index = (int)(t & mask);
        T value = (T) buffer[index];
        buffer[index] = null;

        tail = t + 1;

        return value;
    }

    public boolean isEmpty() {
        return tail >= head;
    }

    public void setPreloadQueue(Queue<T> preloadQueue) {
        return;
    }
}
