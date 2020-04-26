# coding-challenge

## Requirements

These are the requirements as I understood them:

1. Build a server that accepts socket connections on port 4000
1. The server should actively accept data from no more than 5 clients.
1. The client will send nine-digit numerics over the socket. The input will be zero-padded and newline-terminated.
1. The client may also send the text 'terminate'; if this happens, the server should quickly close all active connections and shut down cleanly.
1. Any other input should be rejected, and the server should close the client connection.
1. Each unique digit should be written to a file called `numbers.log`. No duplicates should be written to this file.
1. Every 10 seconds, the server should report system status to standard out. This status must include:
   - The number of unique numbers received in the last 10 seconds
   - The number of duplicate numbers received in the last 10 seconds
   - The total number of unique numbers received over the lifetime of the server

As far as I can tell, this implementation meets all of these requirements.

## Assumptions

The code makes a couple of assumptions that probably won't fly for something you'd run in production:

- The log component assumes that writing to `numbers.log` always works. The code is wrapped in exception handling, but it doesn't notify the rest of the system that logging cannot continue.
- For the most part, the code assumes that network operations are going to succeed. Again, it's mostly wrapped in exception handling, but the rest of the system isn't notified.
- There is a way to notify that things are going sideways - the `terminationSignal` atomic boolean is threaded throughout the code, so the various components could always set that.

## A few thoughts on my implementation

First, some general notes:

- My machine at home is a Windows computer; I developed this using WSL2 running an Ubuntu VM. It _should_ work on any POSIX-like system but WSL is still weird about some things.
- I've got OpenJDK 11 installed and didn't want to fiddle with trying to get multiple versions working in the time I had, so this targets that JVM.
- As a consequence of that, I had to update the Gradle wrapper to a newer version, which meant in turn that I had to update the ShadowJar plugin too. I didn't update JUnit though.
- I'm using VS Code instead of IntelliJ, and the linter/formatter/parser seems a little iffy. I wasn't able to get it to accept `var` even though I'm using JDK 11.
- In general, Java isn't my strongest language. Is there a better way to nicely format strings than shoving `String.format` everywhere? I don't know.

About the program structure:

- I've had success in the past at handling high-volume traffic with a producer/consumer pipeline, so that's what I do here. The program is split into 4 components:
  - `NumberListener` handles the socket connection and does validation of the input. If it receives a number that fits the required format, it shoves it into a concurrent queue for later processing. That queue was originally a `BlockingQueue`, but having blocking stuff meant that checking for the termination signal became unreliable. The way that code ended up might mean that switching away from the blocking queue wasn't required, but that's all water under the bridge now.
  - `NumberReader` reads from the concurrent queue and handles the actual business logic of the program - checking for numeric uniqueness. It strikes me as I write this that validation of the input could have probably gone here too, but since there's a requirement to terminate the connection on bad input, it seemed cleaner to put that in the listener. Anyway, the reader handles uniqueness with a set view into a concurrent hash map, which seems fine but starts to slow down eventually. If a unique number is found, it's written to another concurrent queue, and either way, a couple of atomic integers are updated.
  - `NumberReporter` deals with those atomic numbers; they represent the number of uniques and duplicates the server has seen in the last 10 seconds. `NumberReporter` runs in a scheduled executor and safely consumes and resets those numbers.
  - `NumberLogger` reads from the output queue of `NumberReader` and writes the numbers to disk. This one is injected with a `BufferedWriter`; it's simpler to let it handle the file operations itself but that makes it a little harder to test.
- All of those components are dependency injected with the various concurrency primitives that bind this whole mess together; I didn't invest any time in pulling in a proper IoC container because it didn't seem worth it. It makes `main` a little messy but it's not _too_ bad.
- Along those same lines, I didn't try to navigate Java's Kafkaesque logging ecosystem and just stuck to the built-in global logger. I probably wouldn't do that in a serious application, but here it works okay.
- One of the primary considerations was to keep things simple, so the orchestration code is all in `main`.
- Properly handling the `terminate` command is gross. I tried for a long time and couldn't come up with a cleaner way to do this that works reliably, so I ended up with a bit of a hack - I run a `gatekeeperService` on a 1-second interval that checks for the termination signal and, if it's set, forceably kills the various services and sockets. This was the only way I could get the program to reliably shut down. It reliably throws an exception in the socket server code anyway, sigh.
- One of the consequences of the way I handle termination is that I have to keep track of every socket listener so that I can close the socket if the program needs to shut down. I don't know of a way to remove listeners from that list, so if the program runs long enough, it'll slowly leak memory. The sample client I wrote triggers the worst-case scenario here - it creates new connections every second, so the list grows faster than it might in other situations.
- Maybe using a Guava service manager to handle all the services would have been cleaner? It seems totally possible after looking through the documentation again, but at the same time, that's a lot of library to bring in just for this.
- There are tests! Yay! But they mostly test business logic. Boo! I didn't really push them into the far-off-the-happy-path situations by simulating exceptions from the various input/output channels. I'd definitely do that in a real situation if it seemed warranted!

The reality of this application is this - it's mostly how I would have structured it if I were writing it in Go: a bunch of "goroutines" linked together by concurrent channels. I've maintained high-traffic Java systems on the Browser team, and this looks a little like that, but I don't pretend to be an expert at Java concurrency. The various `Executors` are used in ways that make sense to me, but it's possible or even likely that I'm handling them or the services they're running in some dumb way that limits performance.

## Performance

I tested the program on a Windows 10 Pro PC running Ubuntu 18.10 in a WSL2 environment. My computer has a Ryzen 7 3700X processor with 8 physical cores, 32 gigs of DDR5 memory, and a very fast SSD. I used the sample client in `/client` to test; it uses 5 threads to generate a large number of random integers every second. The commands to build and run it are the same as the server (I just copied and pasted the bootstrap project again to make it). It's hard-coded to localhost and port 4000.

I also have a debug-only service in `main` to report some stats on the system; it runs every second and reports the number of active connections on the socket and the backpressure on the two concurrent queues. I commented it out, but it's kind of neat to see some instrumentation about what's happening inside.

The requirements list mentions that a "robust implementation" handles 2M requests per 10 second period, so that was my target. Under my testing (which, to be fair, _only_ tests the happy path - every number it sends is valid), handled 2M/10sec or 40K requests from 5 clients without any apparent pressure. At ~8M/10sec, I started to see occasional backpressure from the logging component and - eventually - some backpressure on the reader component, usually at around 15 billion numbers in the hash set. The reader component runs on a single thread, and it could be expanded, but the uniqueness requirement makes the `Set` a bottleneck eventually. At ~12.5M/10sec, backpressure came quickly on both queues. Typically if I stop the client, the server recovers quickly, but that seems to be the limit for this implementation.

## Commands

### Build

To build the project on Linux or MacOS run the command `./gradlew build` in a shell terminal. This will build the source code in
`src/main/java`, run any tests in `src/test/java` and create an output
jar file in the `build/libs` folder.

To clean out any intermediate files run `./gradlew clean`. This will
remove all files in the `build` folder.

### Run

You first must create a shadow jar file. This is a file which contains your project code and all dependencies in a single jar file. To build a shadow jar from your project run `./gradlew shadowJar`. This will create a `codeing-challenge-shadow.jar` file in the `build/libs` directory.

You can then start your application by running the command
`java -jar ./build/lib/coding-challenge-shadow.jar`
