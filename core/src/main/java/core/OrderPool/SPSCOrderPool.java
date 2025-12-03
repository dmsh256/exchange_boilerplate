package core.OrderPool;

import core.Order.Order;

public class SPSCOrderPool {
    private final Order[] stack;
    private final int capacity;
    private int top;

    public SPSCOrderPool(int capacity) {
        this.capacity = capacity;
        this.stack = new Order[capacity];
        this.top = capacity;

        for (int i = 0; i < capacity; i++)
            stack[i] = new Order();
    }

    public Order borrow() {
        if (top == 0)
            return null;

        return stack[--top];
    }

    public void release(Order order) {
        if (order == null)
            return;

        if (top == capacity)
            throw new IllegalStateException("Pool overflow");

        order.reset();
        stack[top++] = order;
    }

    @Override
    public String toString() {
        return String.format("Pool size: %d, top is: %d ", stack.length, top);
    }
}
