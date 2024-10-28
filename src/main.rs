use std::thread;
use std::time::{Duration, Instant};

use rand::{thread_rng, Rng};
use server::{Customer, CustomerService};

fn main() {
    println!("Start!! {}", thread::available_parallelism().unwrap());
    let server = CustomerService::new();
    // let mut write_handles = Vec::new();
    const MAX_ID_VALUE: isize = 10000;
    let operations: Vec<isize> = vec![100, 200, 500, 1000, 2000, 3000, 4000, 6000, 7000, 8000, 9000, 10000];
    // let x = server.fill(Instant::now());
    // println!("Time to fill is {:?}\n\n", &x);

    println!("====Start====");
    for ops in operations {
        // const DURATION_ZERO: Duration = Duration::from_secs(0);
        let mut rd_ar = Vec::new();
        let mut wr_ar = Vec::new();
        let start = Instant::now();
        
        for _ in 0..ops {
            let j = thread_rng().gen_range(0..MAX_ID_VALUE);
            // write_handles.push(server.upsert_async(Customer::new(k, &format!("Kamal {}", &k), "Hasan")));
            let t = Instant::now();
            let name = &format!("Foo {}", &j);
            wr_ar.push(server.upsert_async(Customer::new(j, name, "Bar"), t));
        }
        for _ in 0..ops {
            let k = thread_rng().gen_range(0..MAX_ID_VALUE);
            let t2 = Instant::now();
            rd_ar.push(server.print_async(k, t2));
        }
        let total_writes = wr_ar.len() as f64;
        let mut total_write_duration = Duration::from_micros(0);
        for handle in wr_ar {
            total_write_duration += handle.join().unwrap();
        }
        // println!("After all write handles threads have finished");
        // println!("Finished writes in {:?}", start.elapsed());
        // let write_finished = Instant::now();

        let total_reads = rd_ar.len() as f64;
        let mut total_read_duration = Duration::from_micros(0);
        for handle in rd_ar {
            total_read_duration += handle.join().unwrap();
        }
        // println!("After all read handles threads have finished");
        // let read_finished = write_finished.elapsed();
        // println!("Finished reads in {:?}", &read_finished);
        println!("For {} cycles",ops);
        println!("Total duration (reads, write) = ({}, {})ms",total_read_duration.as_millis(),
        total_write_duration.as_millis());
        // println!(
        //     "For {} cycles, latencies (reads, write) ({:?}, {:?})ms",
        //     ops,
        //     f32::from(total_read_duration.as_millis())/(ops as f32),
        //     f128::from(total_write_duration.as_millis())/(ops as f32)
        // );
        println!(
            "Average (read, write) = ({}, {}) ops/s",
            total_reads / (total_read_duration.as_secs_f64()),
            total_writes / (total_write_duration.as_secs_f64())
        );
        println!("Test Duration:: {:?}", Instant::now() - start);
        println!("====End====");
    }
}

/// This module is intended to be backend managing map.
/// allowing concurrent access to hashmap and update it
/// map contains <customer id, customer data object>
mod server {
    // use rustc_hash::FxHashMap;
    use std::collections::HashMap;
    use std::sync::{Arc, Mutex};
    use std::thread;
    use std::thread::JoinHandle;
    use std::time::{Duration, Instant};
    pub struct CustomerService {
        store: Arc<Mutex<HashMap<isize, Customer>>>,
    }

    impl CustomerService {
        pub fn new() -> CustomerService {
            CustomerService {
                store: Arc::new(Mutex::new(HashMap::default())),
            }
        }

        pub fn upsert_async(&self, customer: Customer, instant: Instant) -> JoinHandle<Duration> {
            // println!("Inside upsert");
            let arc = Arc::clone(&self.store);
            thread::spawn(move || {
                loop {
                    match arc.try_lock() {
                        Ok(mut guard) => {
                            // println!("thread");
                            let _c = guard.insert(customer.id, customer);
                            return instant.elapsed();
                        }
                        Err(_) => {
                            // thread::sleep(Duration::from_millis(100));
                            continue;
                        }
                    }
                }
                // println!(".");
            })
        }

        pub fn fill(&self, instant: Instant) -> Duration {
            // println!("Inside upsert");
            let arc = Arc::clone(&self.store);
            let mut k = arc.lock().unwrap();
            for i in 0..10000 {
                let _c = &k.insert(i, Customer::new(i, &"FN"[..], &"LN"[..]));
            }
            instant.elapsed()
        }

        pub fn print_async(&self, id: isize, instant: Instant) -> JoinHandle<Duration> {
            let arc = Arc::clone(&self.store);
            thread::spawn(move || {
                loop {
                    match arc.try_lock() {
                        Ok(guard) => {
                            let _c = guard.get(&id);
                            match _c {
                                None => {
                                    //println!("Not yet filled {}", id)
                                },
                                Some(_customer) => {
                                    _customer.print();
                                }
                            };
                            return instant.elapsed();
                        }
                        Err(_) => {
                            // thread::sleep(Duration::from_millis(1));
                            continue;
                        }
                    }
                }
                
            })
        }
    }

    /**
    This struct holds basic customer data of first name and last name with a numeric identifier.
    This struct does not guarantee uniqueness checks on creation
    */
    pub struct Customer {
        pub id: isize,
        pub first_name: String,
        pub last_name: String,
    }

    impl Customer {
        pub fn new(id: isize, first_name: &str, last_name: &str) -> Customer {
            Customer {
                id,
                first_name: first_name.to_string(),
                last_name: last_name.to_string(),
            }
        }

        /// To print the customer struct on console
        pub fn print(&self) {
            // println!(
            //     "id: {}, FN: {}, LN: {}",
            //     self.id, self.first_name, self.last_name
            // )
        }
    }
}
