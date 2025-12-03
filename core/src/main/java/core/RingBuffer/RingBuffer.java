package core.RingBuffer;

import java.util.Queue;

public interface RingBuffer<T> {
    public boolean offer(T value);
    public T poll();
    public void setPreloadQueue(Queue<T> preloadQueue);
}
