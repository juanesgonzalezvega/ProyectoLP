package com.example.proyectolp.service;

import javafx.animation.*;
import javafx.beans.property.*;
import javafx.util.Duration;

/** Temporizador sin bloquear JavaFX. Los cambios se realizan en el hilo de JavaFX. */
public class RestTimer {
    public enum State { READY, RUNNING, PAUSED, FINISHED }
    private final IntegerProperty remainingSeconds=new SimpleIntegerProperty();
    private final ObjectProperty<State> state=new SimpleObjectProperty<>(State.READY);
    private final Timeline timeline;
    private int configuredSeconds;
    private boolean scheduled;
    public RestTimer(){this(90);}
    public RestTimer(int seconds){if(seconds<1)throw new IllegalArgumentException("La duración debe ser positiva");configuredSeconds=seconds;remainingSeconds.set(seconds);timeline=new Timeline(new KeyFrame(Duration.seconds(1),e->tick()));timeline.setCycleCount(Animation.INDEFINITE);}
    public void start(){begin(true);}
    /** Inicia el conteo lógico sin scheduler, útil para pruebas o un reloj externo. */
    public void startClockless(){begin(false);}
    private void begin(boolean schedule){if(state.get()==State.FINISHED)reset();state.set(State.RUNNING);scheduled=schedule;if(schedule)timeline.play();}
    public void pause(){if(state.get()==State.RUNNING){if(scheduled)timeline.pause();state.set(State.PAUSED);}}
    public void reset(){if(scheduled)timeline.stop();scheduled=false;remainingSeconds.set(configuredSeconds);state.set(State.READY);}
    public void setDurationSeconds(int seconds){if(seconds<1)throw new IllegalArgumentException("La duración debe ser positiva");configuredSeconds=seconds;reset();}
    private void tick(){if(remainingSeconds.get()<=1){remainingSeconds.set(0);if(scheduled)timeline.stop();scheduled=false;state.set(State.FINISHED);}else remainingSeconds.set(remainingSeconds.get()-1);}
    /** Punto de entrada determinista para pruebas y relojes alternativos. */
    public void elapseOneSecond(){if(state.get()==State.RUNNING)tick();}
    public IntegerProperty remainingSecondsProperty(){return remainingSeconds;} public ObjectProperty<State> stateProperty(){return state;}
    public int getRemainingSeconds(){return remainingSeconds.get();} public State getState(){return state.get();}
    public String getDisplay(){return String.format("%02d:%02d",getRemainingSeconds()/60,getRemainingSeconds()%60);}
}
