import java.util.*;
import java.util.concurrent.*;

class HashMapRunner {
    static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final int MAX_ID_VALUE = 10000;

    public static void main(String[] args) throws ExecutionException, InterruptedException {
    Thread.sleep(2000);
        
        int[] operations = new int[] { 100, 200, 500, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000 };
        for (int i = 0; i < operations.length; i++) {
            System.out.println("===Start===");
            int ops = operations[i];

            long startSystem = System.nanoTime();
            final CustomerService service = new CustomerService(MAX_ID_VALUE);
            List<Future<Long>> readFutures = new ArrayList<>(ops);
            List<Future<Long>> writeFutures = new ArrayList<>(ops);
            Random random = new Random(5);
            for (int j = 0; j < ops; j++) {
                Integer randInteger = random.nextInt(MAX_ID_VALUE);
                writeFutures.add(service.upsert(randInteger, new Customer(randInteger, "Foo" + randInteger, "Bar")));
                Integer randInteger2 = random.nextInt(MAX_ID_VALUE);
                readFutures.add(service.readCustomer(randInteger2));
            }
            Long writeTotal = 0L;
            Long readTotal = 0L;
            for (Future<Long> writeFuture : writeFutures) {
                Long data = writeFuture.get();
                writeTotal += data;
            }
            // System.out.println("After all write futures have finished");
            for (Future<Long> readFuture : readFutures) {
                Long data = readFuture.get();
                readTotal += data;
            }
            // System.out.println("After all read futures have finished");
            System.out.printf("For %d cycles\n", ops);
            System.out.printf("Total duration (read, write) = (%.2f, %.2f)ms \n", readTotal / (1000 * 1000f),
                    writeTotal / (1000 * 1000f));
            System.out.printf("Average (read, write) = (%.3f, %.3f) ops/s\n",
                    (ops * 1000d * 1000 * 1000) / readTotal,
                    (ops * 1000d * 1000 * 1000) / writeTotal);

            System.out.printf("Test Duration:: %.3fms \n", (System.nanoTime() - startSystem)/(1000*1000f));
            System.out.printf("===End===\n\n");
        }
        executor.shutdown();
    }

    static class Customer {
        Integer id;
        String firstName;
        String lastName;

        public Customer(Integer id, String firstName, String lastName) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        @Override
        public String toString() {
            return "id: " + this.id + ", FN: " + this.firstName + ", LN: " + this.lastName;
        }
    }

    static class CustomerService {
        private final Map<Integer, Customer> store;

        public CustomerService(int maxId) {
            // this.store = new ConcurrentHashMap<>(maxId);
            this.store = Collections.synchronizedMap(new HashMap<>(maxId));
        }

        public Future<Long> readCustomer(Integer id) {
            final long startTime = System.nanoTime();
            return executor.submit(() -> {
                store.get(id);
                // System.out.println("customer = " + customer);
                long endTime = System.nanoTime();
                return endTime - startTime;
            });
        }

        public Future<Long> upsert(Integer id, Customer customer) {
            final long startTime = System.nanoTime();
            return executor.submit(() -> {
                store.put(id, customer);
                long endTime = System.nanoTime();
                return endTime - startTime;
            });
        }
    }

}