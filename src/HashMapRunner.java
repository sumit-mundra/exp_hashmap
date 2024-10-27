import java.util.*;
import java.util.concurrent.*;


class HashMapRunner {
    static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final int MAX_ID_VALUE = 10000;
    private static final int MAX_LOOP = 10000;

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Thread.sleep(5000);
        long startSystem = System.nanoTime();
        final CustomerService service = new CustomerService(MAX_ID_VALUE);
        List<Future<Long>> readFutures = new ArrayList<>(MAX_LOOP);
        List<Future<Long>> writeFutures = new ArrayList<>(MAX_LOOP);
        Random random = new Random(5);
        for (int i = 0; i < MAX_LOOP; i++) {
            Integer k = random.nextInt(MAX_ID_VALUE);
            writeFutures.add(service.upsert(k, new Customer(k, "Foo" + k, "Bar")));
        }
        for (int i = 0; i < MAX_LOOP; i++) {
            Integer l = random.nextInt(MAX_ID_VALUE);
            readFutures.add(service.readCustomer(l));
        }
        Long writeTotal = 0L;
        Long readTotal = 0L;
        for (Future<Long> writeFuture : writeFutures) {
            Long data = writeFuture.get();
            writeTotal += data;
        }
        System.out.println("After all write futures have finished");
        for (Future<Long> readFuture : readFutures) {
            Long data = readFuture.get();
            readTotal += data;
        }
        System.out.println("After all read futures have finished");
        System.out.println("readTotal = " + readTotal + ", writeTotal = " + writeTotal);
        System.out.printf("For %d cycles, Total read duration %.2f s Total write duration %.2f s \n", MAX_LOOP, readTotal / (1000 * 1000 * 1000f), writeTotal / (1000 * 1000 * 1000f));
        System.out.printf("Average read %.2f ops/s, write %.2f ops/s\n", (Integer.valueOf(MAX_LOOP).doubleValue() * 1000 * 1000 * 1000) / readTotal, (Integer.valueOf(MAX_LOOP).doubleValue() * 1000 * 1000 * 1000) / writeTotal);
        executor.shutdown();
        System.out.println("Duration::" + (System.nanoTime() - startSystem));
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
            this.store = new ConcurrentHashMap<>(maxId);
//            this.store = Collections.synchronizedMap(new HashMap<>(maxId));
        }

        public Future<Long> readCustomer(Integer id) {
            final long startTime = System.nanoTime();
            return executor.submit(() -> {
                Customer customer = store.get(id);
//                System.out.println("customer = " + customer);
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