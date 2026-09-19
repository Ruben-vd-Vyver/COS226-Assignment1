import java.util.concurrent.atomic.AtomicReference;

public class CLHLock implements Lock {

    private static class QNode {
        volatile boolean locked = false;
    }

    private final AtomicReference<QNode> tail;
    private final ThreadLocal<QNode> myNode = ThreadLocal.withInitial(QNode::new);
    private final ThreadLocal<QNode> myPred = ThreadLocal.withInitial(() -> null);

    public CLHLock() {
        QNode sentinel = new QNode();
        sentinel.locked = false;
        tail = new AtomicReference<>(sentinel);
    }

    @Override
    public void lock() {
        QNode node = myNode.get();
        node.locked = true;

        QNode pred = tail.getAndSet(node);
        myPred.set(pred);
        while (pred.locked) {
        }
    }

    @Override
    public void unlock() {
        QNode node = myNode.get();
        node.locked = false;

        myNode.set(myPred.get());
    }
}