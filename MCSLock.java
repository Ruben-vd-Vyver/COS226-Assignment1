import java.util.concurrent.atomic.AtomicReference;

public class MCSLock implements Lock {

    private static class QNode {
        volatile QNode next = null;
        volatile boolean locked = false;
    }

    private final AtomicReference<QNode> tail = new AtomicReference<>(null);

    private final ThreadLocal<QNode> myNode = ThreadLocal.withInitial(QNode::new);

    @Override
    public void lock() {
        QNode node = myNode.get();
        node.next = null;
        node.locked = true;

        QNode pred = tail.getAndSet(node);

        if (pred != null) {
            pred.next = node;
            while (node.locked) {
            }
        }
    }

    @Override
    public void unlock() {
        QNode node = myNode.get();

        if (node.next == null) {

            if (tail.compareAndSet(node, null)) {
                return;
            }

            while (node.next == null) {
            }
        }

        node.next.locked = false;
        node.next = null;
    }
}