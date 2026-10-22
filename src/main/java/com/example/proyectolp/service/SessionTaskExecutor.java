package com.example.proyectolp.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Pool común para cancelar trabajos pendientes al cambiar de cuenta. */
public final class SessionTaskExecutor {
    private static final ThreadLocal<Long> SUBMITTED_GENERATION=new ThreadLocal<>();
    private static ExecutorService executor=create();
    private SessionTaskExecutor(){}
    private static ExecutorService create(){return new ThreadPoolExecutor(4,4,0,TimeUnit.MILLISECONDS,new java.util.concurrent.LinkedBlockingQueue<>(),r->{Thread t=new Thread(r,"session-data-worker");t.setDaemon(true);return t;}){@Override public void execute(Runnable command){long generation=SessionManager.getInstance().generation();super.execute(()->{if(SessionManager.getInstance().generation()!=generation||!SessionManager.getInstance().active())return;SUBMITTED_GENERATION.set(generation);try{command.run();}finally{SUBMITTED_GENERATION.remove();}});}};}
    public static synchronized ExecutorService get(){return executor;}
    public static Long submittedGeneration(){return SUBMITTED_GENERATION.get();}
    public static synchronized void reset(){executor.shutdownNow();executor=create();}
}
