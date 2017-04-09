package com.github.doghere;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.sql.SQLException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by dog on 4/6/17.
 */
final public class Model<E> {
    
    private Reader<E> reader ;
    private Writer<E> []writer;
    private Factory<E> factory;
    private int ringBufferSize=2^10;
    private    Disruptor<E> disruptor ;

    public Factory<E> getFactory() {
        return factory;
    }

    public Model setFactory(Factory<E> factory) {
        this.factory = factory;
        return this;
    }


    public Model setRingBufferSize(int ringBufferSize) {
        this.ringBufferSize = ringBufferSize;
        return this;
    }

    public Model setReader(Reader<E> reader) {
        this.reader = reader;
        return this;
    }

    public Model setWriter(Writer<E> []writer) {
        this.writer = writer;
        return this;
    }

    public Disruptor<E> getDisruptor() {
        return disruptor;
    }

    public Model setDisruptor(Disruptor<E> disruptor) {
        this.disruptor = disruptor;
        return this;
    }

    public void start() throws Exception {

        try {
            long startTime = System.currentTimeMillis();
            reader.before();
            for (Writer w : writer) {
                w.before();
            }
            Executor executor = Executors.newCachedThreadPool();
            disruptor = new Disruptor<E>(getFactory(),
                    ringBufferSize,
                    executor,
                    ProducerType.SINGLE,
                    new BlockingWaitStrategy());
            disruptor.handleEventsWithWorkerPool(writer);
            disruptor.start();


            for (Writer w : writer) {
                w.setDisruptor(disruptor);
            }


            RingBuffer<E> ringBuffer = disruptor.getRingBuffer();
            reader.setRingBuffer(ringBuffer);


            long count = 0;
            for (long l = 0; reader.hasRemaining(); l++) {
                reader.read();
                if (l % 3000 == 0) {
                    System.out.println("read rows:\t" + l + "\tcost " + (System.currentTimeMillis() - startTime));
                }
                count = l;
            }
            long endTime = System.currentTimeMillis();
            System.out.println("total read rows:\t" + count + "\tcost " + (endTime - startTime));
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception(e);
        }finally {
            System.out.println("finally");
            if(disruptor!=null) disruptor.shutdown(10,TimeUnit.SECONDS);
            reader.after();
            for (Writer w : writer) w.after();
        }


    }


}
