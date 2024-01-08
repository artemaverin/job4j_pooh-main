package ru.job4j.pooh;

import java.util.concurrent.*;

public class QueueSchema implements Schema {
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Receiver>> receivers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BlockingQueue<String>> data = new ConcurrentHashMap<>();
    private final Condition condition = new Condition();

    @Override
    public void addReceiver(Receiver receiver) {
        receivers.putIfAbsent(receiver.name(), new CopyOnWriteArrayList<>());
        receivers.get(receiver.name()).add(receiver);
        condition.on();
    }

    @Override
    public void publish(Message message) {
        data.putIfAbsent(message.name(), new LinkedBlockingQueue<>());
        data.get(message.name()).add(message.text());
        condition.on();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            for (var queueKey : receivers.keySet()) {
                var queue = data.getOrDefault(queueKey, new LinkedBlockingQueue<>());
                var receiversByQueue = receivers.get(queueKey);
                int i = 0;
                while (!queue.isEmpty()) {
                    var data = queue.poll();
                    Receiver receiver = receiversByQueue.get(i++);
                    receiver.receive(data);
                    if (i >= receiversByQueue.size()) {
                        i = 0;
                    }
                }
            }
            condition.off();
            try {
                condition.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
