import java.util.concurrent.ThreadLocalRandom;
/* Helper Runner class: creates, starts and joins the bidder threads. */
public class Runner
{
    public final int numberOfThreads;
    public final int iterations;
    public final Auction auction;
    public final Lock lock;
    private final int[] bidsWon;         
    private final long[] totalWaitNanos; 
    private final long[] maxWaitNanos;

    public Runner(int numberOfThreads, int iterations, Auction auction, Lock lock)
    {
        this.numberOfThreads = numberOfThreads;
        this.iterations = iterations;
        this.auction = auction;
        this.lock = lock;

        this.bidsWon = new int[numberOfThreads];
        this.totalWaitNanos = new long[numberOfThreads];
        this.maxWaitNanos = new long[numberOfThreads];
    }

    public void run() throws InterruptedException
    {
        Thread[] threads = new Thread[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++)
        {
            final int bidderId = i;
            threads[i] = new Thread(() -> bidder(bidderId));
        }

        long startTime = System.nanoTime();

        for (Thread thread : threads)
        {
            thread.start();
        }

        for (Thread thread : threads)
        {
            thread.join();
        }

        long endTime = System.nanoTime();

        reportResults(endTime - startTime);
    }

    public void bidder(int bidderId)
    {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < iterations; i++)
        {
            // Increment chosen outside the critical section (no shared state needed).
            double increment = 1 + random.nextInt(500);

            long waitStart = System.nanoTime();
            lock.lock();
            long waited = System.nanoTime() - waitStart;

            try
            {
                //critical section
                double newBid = auction.getHighestBid() + increment;
                auction.placeBid(bidderId, newBid);

                // Confirm the bid was accepted (it always should be while the lock is held).
                if (auction.getHighestBidder() == bidderId && auction.getHighestBid() == newBid)
                {
                    bidsWon[bidderId]++;
                }
                // End critical section
            }
            finally
            {
                lock.unlock();
            }

            totalWaitNanos[bidderId] += waited;
            if (waited > maxWaitNanos[bidderId])
            {
                maxWaitNanos[bidderId] = waited;
            }
        }
    }

    /* Records and reports the results of the experiment. */
    public void reportResults(long executionTime)
    {
        int totalBids = 0;
        int minBids = Integer.MAX_VALUE;
        int maxBids = Integer.MIN_VALUE;
        long totalWait = 0;
        long maxWait = 0;

        for (int i = 0; i < numberOfThreads; i++)
        {
            totalBids += bidsWon[i];
            minBids = Math.min(minBids, bidsWon[i]);
            maxBids = Math.max(maxBids, bidsWon[i]);
            totalWait += totalWaitNanos[i];
            maxWait = Math.max(maxWait, maxWaitNanos[i]);
        }

        int expectedBids = numberOfThreads * iterations;
        double avgWaitMicros = (totalWait / (double) expectedBids) / 1000.0;

        System.out.println("Lock:               " + lock.getClass().getSimpleName());
        System.out.println("Threads:            " + numberOfThreads);
        System.out.println("Iterations/thread:  " + iterations);
        System.out.println("Item:               " + auction.getItemName());
        System.out.printf("Execution time:     %.3f ms%n", executionTime / 1_000_000.0);
        System.out.println("Total bids:         " + totalBids + " (expected " + expectedBids + ")"
                + (totalBids == expectedBids ? "  OK" : "  MISMATCH!"));
        System.out.printf("Final highest bid:  %.2f (by bidder %d)%n",
                auction.getHighestBid(), auction.getHighestBidder());
        System.out.printf("Avg lock wait:      %.3f us%n", avgWaitMicros);
        System.out.printf("Max lock wait:      %.3f us%n", maxWait / 1000.0);
        System.out.println("Bid spread (max-min): " + (maxBids - minBids));
        System.out.println("Bids won per bidder:");
        for (int i = 0; i < numberOfThreads; i++)
        {
            System.out.println("  Bidder " + i + ": " + bidsWon[i]);
        }
        System.out.println();
    }
}
