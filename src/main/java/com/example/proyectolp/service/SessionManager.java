package com.example.proyectolp.service;

import com.example.proyectolp.model.Session;
import java.time.Clock;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public final class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager(Clock.systemUTC());
    private static final ReentrantReadWriteLock REQUEST_LOCK = new ReentrantReadWriteLock(true);
    private final Clock clock;
    private final CopyOnWriteArrayList<Consumer<String>> expiredListeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"auth-token-refresh");t.setDaemon(true);return t;});
    private volatile ScheduledFuture<?> refreshTask;
    private volatile Session session;
    private volatile long generation;
    private volatile boolean remembered;
    public SessionManager(Clock clock) { this.clock = clock; }
    public static SessionManager getInstance() { return INSTANCE; }
    public Session current() { return session; }
    public boolean active() { return session != null; }
    public boolean rememberSession(){return remembered;}
    public void setRemembered(boolean value){remembered=value;}
    public boolean needsRefresh(Session value) { return value == null || !value.expiresAt().isAfter(clock.instant().plusSeconds(60)); }
    public long generation() { return generation; }
    public boolean isCurrent(long token) { return token == generation && session != null; }
    public synchronized void set(Session value) {
        Session previous=session;
        if(previous==null || value==null || !previous.userId().equals(value.userId())) generation++;
        session = value;
    }
    public synchronized void clear() { session = null;remembered=false; generation++;if(refreshTask!=null)refreshTask.cancel(false); }
    public synchronized void scheduleRefresh(AuthService auth,Session value) {
        if(refreshTask!=null)refreshTask.cancel(false);
        long delay=Math.max(0,value.expiresAt().minusSeconds(60).getEpochSecond()-clock.instant().getEpochSecond());
        refreshTask=scheduler.schedule(()->{synchronized(SessionManager.this){Session now=session;if(now==null||!now.userId().equals(value.userId())||!now.refreshToken().equals(value.refreshToken()))return;try{auth.refresh(now.refreshToken(),remembered);}catch(AuthException e){auth.clearStoredRefresh();emitExpired("La sesión expiró. Inicia sesión de nuevo.");}}},delay,TimeUnit.SECONDS);
    }
    public void onExpired(Consumer<String> listener) { expiredListeners.add(listener); }
    public void emitExpired(String message) { clear(); expiredListeners.forEach(l -> l.accept(message)); }
    public static ReentrantReadWriteLock requestLock() { return REQUEST_LOCK; }
}
