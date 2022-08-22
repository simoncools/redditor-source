import java.util.concurrent.*;

public class TimeLimiter {

    public TimeLimiter(Runnable stuffToDo){
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future future = executor.submit(stuffToDo);
        executor.shutdown();
        try {
            future.get(120, TimeUnit.SECONDS);
            executor.awaitTermination(200,TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException ie) {
            System.out.println("Interrupted Exception");

        }
        catch (ExecutionException ee) {
            System.out.println("Execution Exception");

        }
        catch (TimeoutException te) {
            System.out.println("Timeout Exception");

        }

        if (!executor.isTerminated()) {
            executor.shutdownNow(); // If you want to stop the code that hasn't finished.
            System.out.println("TASK NOT FULLY EXECUTED, KILLING");
        }
    }
}
