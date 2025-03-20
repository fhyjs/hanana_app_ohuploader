package org.eu.hanana.reimu.app.webui.ohuploader.util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public class TimerLoopThread<T> extends Thread{
    protected AtomicReference<T> value;
    protected Function<T,Boolean> func;
    protected long miles;
    public TimerLoopThread(AtomicReference<T> value, Function<T,Boolean> func, long miles){
        this.value=value;
        this.func=func;
        this.miles=miles;
    }
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()){
            try {
                Thread.sleep(miles);
            } catch (InterruptedException ignored) {
            }
            if (func.apply(value.get())){
                break;
            }
        }
    }
}
