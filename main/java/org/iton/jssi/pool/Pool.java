/*
 *
 *  The MIT License
 *
 *  Copyright 2019 ITON Solutions.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package org.iton.jssi.pool;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.iton.jssi.pool.event.IPoolEvent;
import org.iton.jssi.pool.event.NodeReply;
import org.iton.jssi.pool.network.INetworkHandler;
import org.iton.jssi.pool.network.NetworkHandler;
import org.libsodium.jni.SodiumException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 *
 * @author ITON Solutions
 */
public class Pool {
    
    private static final Logger LOG = LoggerFactory.getLogger(Pool.class);
    
    private String name;
    private int poolId;
    private long timeout;
    private long extended;
    private long active;
    private int limit;
    private String[] preordered;
    private ZMQ.Socket receiver;
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    public Pool(){}
    
    public Pool(String poolName, int poolId, long timeout, long extended, long active, int limit, String[] preordered){
        this.name = poolName;
        this.poolId = poolId;
        this.timeout = timeout;
        this.extended = extended;
        this.active = active;
        this.limit = limit;
        this.preordered = preordered;
    }
    
    
    public void execute(ZMQ.Socket receiver){
        this.receiver = receiver;
        Runnable worker = new Process(receiver, name, poolId, timeout, extended, active, limit, preordered);
        executor.execute(worker);
    }

    class Process implements Runnable {

        private final INetworkHandler network;
        private final IPoolHandler processor;
        private final CommandHandler commander;

        private final Deque<IPoolEvent> events = new LinkedList<>();
        private final AtomicBoolean terminal = new AtomicBoolean(false);

        /**
         *
         * @param receiver socket that receive commands from app
         * @param poolName
         * @param poolId
         * @param timeout timeout
         * @param extended extended timeout
         * @param active active timeout
         * @param limit Connections limit
         * @param preordered Preordered nodes
         */
        public Process(ZMQ.Socket receiver, String poolName, int poolId, long timeout, long extended, long active, int limit, String[] preordered) {
            this.network = new NetworkHandler(active, limit, preordered);
            this.processor = new PoolHandler(network, poolName, poolId, timeout, extended);
            this.commander = new CommandHandler(receiver);
        }

        @Override
        public void run() {
            while (true) {

                poll();
                if (loop().get()) {
                    LOG.debug("Terminated");
                    return;
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        private AtomicBoolean loop() {

            while (!events.isEmpty()) {
                IPoolEvent event = events.pollFirst();

                try {
                    processor.handleEvent(event);
                } catch (SodiumException | JsonProcessingException e) {
                    LOG.error(String.format("Error %s", e));
                    break;
                }
            }
            terminal.set(processor.isTerminal());
            return terminal;
        }

        private void poll() {

            List<ZMQ.PollItem> items = network.getPollItems();
            LOG.debug(String.format("Pool items size %s", items.size()));
            ZMQ.Poller poller = new ZContext().createPoller(items.size());

            for (ZMQ.PollItem item : items) {
                poller.register(item);
            }

            poller.register(commander.getPollItem());
            poller.poll();

            if (poller.pollin(items.size())) {
                events.addLast(commander.fetchEvents());
            }

            List<IPoolEvent> result = network.fetchEvents(items.toArray(new ZMQ.PollItem[items.size()]));
            for (IPoolEvent event : result) {
                NodeReply reply = (NodeReply) event;
                events.add(reply);
                LOG.debug(String.format("Received pool event %s: %s %s", event.getEvent(), reply.alias, reply.reply));
            }
        }
    }
    
    
    public String getName() {
        return name;
    }

    public int getPoolId() {
        return poolId;
    }

    public ZMQ.Socket getReceiver() {
        return receiver;
    }
}
