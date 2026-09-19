import java.util.concurrent.atomic.AtomicLong;
/*Optional Helper Runner Class*/
public class Runner 
{

    public final int numberOfThreads;
    public final int iterations;
    public final Auction auction;
    public final Lock lock;

    private final AtomicLong totalWaitingTime = new AtomicLong(0);

    public Runner(int numberOfThreads,int iterations,Auction auction,Lock lock) 
    {
        this.numberOfThreads = numberOfThreads;
        this.iterations = iterations;
        this.auction = auction;
        this.lock = lock;
    }

    public void run() throws InterruptedException 
    {
        Thread[] threads = new Thread[numberOfThreads];

        for(int i = 0; i < numberOfThreads; i++) 
        {
            final int bidderId = i;

            threads[i] = new Thread(() -> {
                bidder(bidderId);
            });
        }

        long startTime = System.nanoTime();

        for(Thread thread : threads) 
        {
            thread.start();
        }

        for(Thread thread : threads) 
        {
            thread.join();
        }

        long endTime = System.nanoTime();

        reportResults(endTime - startTime);
    }

    /*Defines the behaviour of an individual bidder. Note you have to decide how to incorporate your lock.*/
    public void bidder(int bidderId) 
    {
	    double myBid = this.auction.getHighestBid();
	    //brute force rng 
	    Random random = new Random();
	    int maxSpend=random.nextInt(10000000);
	    while (maxSpend<500000) {
		    maxSpend = random.nextInt(10000000);
	    }

	    //lock goes here:
	    //
	    //will do the do while spinning loop once Lucian makes locks >:(
	    if(myBid < auction.getHighestBid() && myBid < maxSpend) {
		    auction.placeBid(bidderId, auction.getHighestBid+500);
		    myBid = auction.getHighestBid();
		    //Will change above interval once testing.
	    } else if (myBid < auction.getHighestBid() && myBid < maxSpend+30000) {
		    auction.placeBid(bidderId, auction.getHighestBid+20);
	    } else {
		    //release lock and break the loop
	    }


    }

    /*Optional Helper: Records and reports the results of the experiment.*/
    public void reportResults(long executionTime) 
    {

    }
}
